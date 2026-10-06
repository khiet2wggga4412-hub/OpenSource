package com.example.md3clickgui.music

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local, on-device "recently played" history. Kept deliberately independent of the NetEase account
 * so it always works, even when logged out. MusicPlayer reports each played track here.
 */
object RecentStore {
    private const val MAX = 200
    private const val KEY = "recent_songs"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("netease_recent", Context.MODE_PRIVATE)
    }

    fun add(song: Song) {
        val editor = prefs ?: return
        val list = songs().toMutableList()
        list.removeAll { it.id == song.id }
        list.add(0, song)
        while (list.size > MAX) list.removeAt(list.lastIndex)
        val array = JSONArray()
        for (s in list) {
            array.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("artist", s.artist)
                    .put("durationMs", s.durationMs)
                    .put("coverUrl", s.coverUrl)
            )
        }
        editor.edit().putString(KEY, array.toString()).apply()
    }

    fun songs(): List<Song> {
        val json = prefs?.getString(KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val result = mutableListOf<Song>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            result.add(
                Song(
                    id = item.optLong("id", -1L),
                    title = item.optString("title", "Unknown").ifBlank { "Unknown" },
                    artist = item.optString("artist", ""),
                    durationMs = item.optLong("durationMs", 0L),
                    coverUrl = item.optString("coverUrl", "")
                )
            )
        }
        return result
    }
}