package com.strofy.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val thumb: String,
    val duration: Long
) {
    val url: String get() = "https://www.youtube.com/watch?v=$id"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("artist", artist)
        .put("thumb", thumb).put("dur", duration)

    companion object {
        fun fromJson(o: JSONObject) = Track(
            o.getString("id"), o.optString("title"), o.optString("artist"),
            o.optString("thumb"), o.optLong("dur")
        )

        fun idOf(url: String): String =
            Regex("[?&]v=([\\w-]{6,})").find(url)?.groupValues?.get(1)
                ?: Regex("youtu\\.be/([\\w-]{6,})").find(url)?.groupValues?.get(1)
                ?: url
    }
}

data class Playlist(val name: String, val tracks: List<Track>)

fun fmt(sec: Long): String {
    if (sec <= 0) return ""
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Persistent app state (settings, library, history). */
object Store {
    lateinit var ctx: Context
    private lateinit var sp: SharedPreferences

    // ----- settings -----
    var themeMode by mutableIntStateOf(2); private set       // 0 system, 1 light, 2 dark
    var accent by mutableIntStateOf(0); private set
    var quality by mutableIntStateOf(0); private set         // 0 high, 1 medium, 2 low
    var showQuick by mutableStateOf(true); private set
    var keepSearch by mutableStateOf(true); private set
    var keepHistory by mutableStateOf(true); private set
    var lyricsSize by mutableIntStateOf(22); private set

    // ----- library -----
    val liked = mutableStateListOf<Track>()
    val history = mutableStateListOf<Track>()
    val downloads = mutableStateListOf<Track>()
    val recent = mutableStateListOf<String>()
    val playlists = mutableStateListOf<Playlist>()
    private val plays = HashMap<String, Int>()

    fun init(c: Context) {
        ctx = c.applicationContext
        sp = ctx.getSharedPreferences("strofy", Context.MODE_PRIVATE)
        themeMode = sp.getInt("theme", 2)
        accent = sp.getInt("accent", 0)
        quality = sp.getInt("quality", 0)
        showQuick = sp.getBoolean("showQuick", true)
        keepSearch = sp.getBoolean("keepSearch", true)
        keepHistory = sp.getBoolean("keepHistory", true)
        lyricsSize = sp.getInt("lyricsSize", 22)
        liked.addAll(readTracks("liked"))
        history.addAll(readTracks("history"))
        downloads.addAll(readTracks("downloads").filter { downloadFile(it.id) != null })
        sp.getString("recent", null)?.let { s ->
            val a = JSONArray(s); for (i in 0 until a.length()) recent.add(a.getString(i))
        }
        sp.getString("playlists", null)?.let { s ->
            val a = JSONArray(s)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                playlists.add(Playlist(o.getString("name"), tracksOf(o.getJSONArray("tracks"))))
            }
        }
        sp.getString("plays", null)?.let { s ->
            val o = JSONObject(s); o.keys().forEach { k -> plays[k] = o.getInt(k) }
        }
    }

    // ----- settings setters -----
    fun setTheme(v: Int) { themeMode = v; sp.edit().putInt("theme", v).apply() }
    fun setAccent(v: Int) { accent = v; sp.edit().putInt("accent", v).apply() }
    fun setQuality(v: Int) { quality = v; sp.edit().putInt("quality", v).apply() }
    fun setShowQuick(v: Boolean) { showQuick = v; sp.edit().putBoolean("showQuick", v).apply() }
    fun setKeepSearch(v: Boolean) { keepSearch = v; sp.edit().putBoolean("keepSearch", v).apply() }
    fun setKeepHistory(v: Boolean) { keepHistory = v; sp.edit().putBoolean("keepHistory", v).apply() }
    fun setLyricsSize(v: Int) { lyricsSize = v; sp.edit().putInt("lyricsSize", v).apply() }

    // ----- likes -----
    fun isLiked(t: Track) = liked.any { it.id == t.id }
    fun toggleLike(t: Track) {
        if (isLiked(t)) liked.removeAll { it.id == t.id } else liked.add(0, t)
        saveTracks("liked", liked)
    }

    // ----- history -----
    fun recordPlay(t: Track) {
        if (!keepHistory) return
        history.removeAll { it.id == t.id }
        history.add(0, t)
        while (history.size > 200) history.removeAt(history.lastIndex)
        plays[t.id] = (plays[t.id] ?: 0) + 1
        saveTracks("history", history)
        sp.edit().putString("plays", JSONObject(plays as Map<*, *>).toString()).apply()
    }

    fun top100(): List<Track> =
        history.sortedByDescending { plays[it.id] ?: 0 }.take(100)

    fun clearHistory() {
        history.clear(); plays.clear()
        sp.edit().remove("history").remove("plays").apply()
    }

    fun addRecent(q: String) {
        if (!keepSearch || q.isBlank()) return
        recent.remove(q); recent.add(0, q)
        while (recent.size > 15) recent.removeAt(recent.lastIndex)
        sp.edit().putString("recent", JSONArray(recent.toList()).toString()).apply()
    }

    fun clearRecent() { recent.clear(); sp.edit().remove("recent").apply() }

    // ----- playlists -----
    fun createPlaylist(name: String) { playlists.add(Playlist(name, emptyList())); savePlaylists() }
    fun addToPlaylist(index: Int, t: Track) {
        val p = playlists.getOrNull(index) ?: return
        if (p.tracks.any { it.id == t.id }) return
        playlists[index] = p.copy(tracks = p.tracks + t)
        savePlaylists()
    }
    fun removeFromPlaylist(index: Int, t: Track) {
        val p = playlists.getOrNull(index) ?: return
        playlists[index] = p.copy(tracks = p.tracks.filter { it.id != t.id })
        savePlaylists()
    }
    fun deletePlaylist(index: Int) { if (index in playlists.indices) { playlists.removeAt(index); savePlaylists() } }

    private fun savePlaylists() {
        val arr = JSONArray()
        playlists.forEach { p ->
            arr.put(JSONObject().put("name", p.name).put("tracks", JSONArray(p.tracks.map { it.toJson() })))
        }
        sp.edit().putString("playlists", arr.toString()).apply()
    }

    // ----- downloads -----
    private val dir: File get() = File(ctx.filesDir, "downloads").apply { mkdirs() }
    fun fileFor(id: String) = File(dir, "$id.audio")
    fun downloadFile(id: String): File? = fileFor(id).takeIf { it.exists() && it.length() > 0 }
    fun isDownloaded(id: String) = downloads.any { it.id == id }
    fun addDownload(t: Track) {
        if (!isDownloaded(t.id)) downloads.add(0, t)
        saveTracks("downloads", downloads)
    }
    fun removeDownload(t: Track) {
        downloads.removeAll { it.id == t.id }
        fileFor(t.id).delete()
        saveTracks("downloads", downloads)
    }
    fun clearDownloads() {
        downloads.toList().forEach { fileFor(it.id).delete() }
        downloads.clear(); saveTracks("downloads", downloads)
    }

    // ----- json helpers -----
    private fun saveTracks(key: String, list: List<Track>) {
        sp.edit().putString(key, JSONArray(list.map { it.toJson() }).toString()).apply()
    }
    private fun readTracks(key: String): List<Track> =
        sp.getString(key, null)?.let { tracksOf(JSONArray(it)) } ?: emptyList()
    private fun tracksOf(a: JSONArray): List<Track> =
        (0 until a.length()).mapNotNull { runCatching { Track.fromJson(a.getJSONObject(it)) }.getOrNull() }
}
