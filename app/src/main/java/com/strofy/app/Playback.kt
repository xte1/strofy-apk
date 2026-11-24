package com.strofy.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class StrofyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Yt.init()
    }
}

/** UI-side playback state, backed by a MediaController bound to [PlaybackService]. */
object Playback {
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val known = HashMap<String, Track>()

    var current by mutableStateOf<Track?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var buffering by mutableStateOf(false); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var shuffle by mutableStateOf(false); private set
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF); private set
    val queue = mutableStateListOf<Track>()
    var queueIndex by mutableIntStateOf(0); private set

    fun connect(ctx: Context) {
        if (controller != null) return
        val app = ctx.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            attach(app, c)
        }, ContextCompat.getMainExecutor(app))
    }

    private fun attach(app: Context, c: MediaController) {
        c.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { this@Playback.isPlaying = isPlaying }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                val t = item?.mediaId?.let { known[it] }
                current = t
                queueIndex = c.currentMediaItemIndex
                t?.let { Store.recordPlay(it) }
            }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                duration = c.duration.coerceAtLeast(0L)
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { shuffle = shuffleModeEnabled }
            override fun onRepeatModeChanged(repeatMode: Int) { repeat = repeatMode }
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(app, "تعذّر تشغيل المقطع، جرّب غيره", Toast.LENGTH_SHORT).show()
            }
        })
        scope.launch {
            while (true) {
                position = c.currentPosition.coerceAtLeast(0L)
                if (c.duration > 0) duration = c.duration
                delay(400)
            }
        }
    }

    private fun item(t: Track): MediaItem {
        known[t.id] = t
        val uri = Uri.parse(t.url)
        return MediaItem.Builder()
            .setMediaId(t.id)
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist)
                    .setArtworkUri(if (t.thumb.isNotBlank()) Uri.parse(t.thumb) else null).build()
            )
            .build()
    }

    fun play(list: List<Track>, index: Int) {
        val c = controller ?: return
        if (list.isEmpty()) return
        queue.clear(); queue.addAll(list)
        c.setMediaItems(list.map { item(it) }, index.coerceIn(0, list.lastIndex), 0L)
        c.prepare(); c.play()
    }

    fun playShuffled(list: List<Track>) {
        val c = controller ?: return
        if (list.isEmpty()) return
        play(list.shuffled(), 0)
        c.shuffleModeEnabled = false
    }

    fun playNext(t: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) { play(listOf(t), 0); return }
        c.addMediaItem(c.currentMediaItemIndex + 1, item(t))
        queue.add((c.currentMediaItemIndex + 1).coerceAtMost(queue.size), t)
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNext() }
    fun previous() {
        controller?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPrevious() }
    }
    fun seek(ms: Long) { controller?.seekTo(ms); position = ms }
    fun jumpTo(index: Int) { controller?.seekToDefaultPosition(index) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
}

/** Background playback + media notification. Stream URLs are resolved lazily when playback starts. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val urlCache = ConcurrentHashMap<String, Pair<String, Long>>()

    override fun onCreate() {
        super.onCreate()
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(HttpDownloader.UA)
            .setAllowCrossProtocolRedirects(true)
        val upstream = DefaultDataSource.Factory(this, http)
        val resolving = ResolvingDataSource.Factory(upstream) { spec -> resolve(spec) }
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolving))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        session = MediaSession.Builder(this, player).build()
    }

    private fun resolve(spec: DataSpec): DataSpec {
        val s = spec.uri.toString()
        if (!s.contains("youtube.com/watch")) return spec
        val id = Track.idOf(s)
        Store.downloadFile(id)?.let { return spec.withUri(Uri.fromFile(it)) }
        val now = System.currentTimeMillis()
        urlCache[id]?.let { (u, at) -> if (now - at < 4 * 3600_000L) return spec.withUri(Uri.parse(u)) }
        try {
            val u = Yt.streamUrl(id, Store.quality)
            urlCache[id] = u to now
            return spec.withUri(Uri.parse(u))
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
