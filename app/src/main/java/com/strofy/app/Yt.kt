package com.strofy.app

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request as OkRequest
import org.json.JSONArray
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File
import java.io.IOException

/** Music catalogue + stream resolution (region: Iraq, language: Arabic). */
object Yt {
    private var ready = false

    @Synchronized
    fun init() {
        if (ready) return
        NewPipe.init(HttpDownloader, Localization("ar", "IQ"), ContentCountry("IQ"))
        ready = true
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        init()
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(
            query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), ""
        )
        SearchInfo.getInfo(service, handler).relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { item ->
                Track(
                    id = Track.idOf(item.url),
                    title = item.name ?: "",
                    artist = item.uploaderName ?: "",
                    thumb = item.thumbnails.lastOrNull()?.url ?: "",
                    duration = item.duration
                )
            }
            .distinctBy { it.id }
    }

    /** Blocking – called from the player's loading thread. */
    fun streamUrl(id: String, quality: Int): String {
        init()
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$id")
        val list = info.audioStreams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl }
            .sortedByDescending { it.averageBitrate }
        val pick = when (quality) {
            1 -> list.getOrNull(list.size / 2)
            2 -> list.lastOrNull()
            else -> list.firstOrNull()
        } ?: list.firstOrNull() ?: throw IOException("No audio stream")
        return pick.content
    }
}

data class LyricLine(val t: Long, val text: String)

/** Synced lyrics from LRCLIB. */
object Lyrics {
    private val lrc = Regex("\\[(\\d+):(\\d+)(?:[.:](\\d+))?](.*)")

    suspend fun fetch(t: Track): List<LyricLine> = withContext(Dispatchers.IO) {
        val title = t.title.replace(Regex("\\(.*?\\)|\\[.*?]"), "").trim()
        var result = query(title, t.artist)
        if (result.isEmpty()) result = query(title, "")
        result
    }

    private fun query(title: String, artist: String): List<LyricLine> {
        val b = "https://lrclib.net/api/search".toHttpUrl().newBuilder().addQueryParameter("track_name", title)
        if (artist.isNotBlank()) b.addQueryParameter("artist_name", artist)
        val req = OkRequest.Builder().url(b.build()).header("User-Agent", "STROFY/1.0").build()
        HttpDownloader.client.newCall(req).execute().use { r ->
            val arr = JSONArray(r.body?.string() ?: "[]")
            var plain: String? = null
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val synced = o.optString("syncedLyrics", "")
                if (synced.isNotBlank() && synced != "null") return parse(synced)
                if (plain == null) o.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }?.let { plain = it }
            }
            return plain?.lines()?.map { LyricLine(-1, it) } ?: emptyList()
        }
    }

    private fun parse(text: String): List<LyricLine> = text.lines().mapNotNull { line ->
        val m = lrc.find(line.trim()) ?: return@mapNotNull null
        val min = m.groupValues[1].toLong()
        val sec = m.groupValues[2].toLong()
        val frac = m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        LyricLine(min * 60_000 + sec * 1000 + frac, m.groupValues[4].trim())
    }
}

/** Offline downloads. */
object Downloads {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val progress = mutableStateMapOf<String, Float>()

    fun start(t: Track) {
        if (progress.containsKey(t.id) || Store.isDownloaded(t.id)) return
        progress[t.id] = 0f
        scope.launch {
            val tmp = File(Store.fileFor(t.id).path + ".tmp")
            try {
                val url = Yt.streamUrl(t.id, 0)
                val req = OkRequest.Builder().url(url).header("User-Agent", HttpDownloader.UA).build()
                HttpDownloader.client.newCall(req).execute().use { r ->
                    val body = r.body ?: throw IOException("empty")
                    val total = body.contentLength().coerceAtLeast(1)
                    var done = 0L
                    body.byteStream().use { input ->
                        tmp.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                progress[t.id] = (done.toFloat() / total).coerceIn(0f, 1f)
                            }
                        }
                    }
                }
                tmp.renameTo(Store.fileFor(t.id))
                withContext(Dispatchers.Main) { Store.addDownload(t) }
            } catch (e: Exception) {
                tmp.delete()
            } finally {
                progress.remove(t.id)
            }
        }
    }
}
