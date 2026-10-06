package com.example.md3clickgui.music

import android.content.Context
import android.content.SharedPreferences

object NeteaseSession {

    var cookie: String = ""

    private var prefs: SharedPreferences? = null
    private var nmtid: String = ""

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("netease_music_session", Context.MODE_PRIVATE)
        nmtid = prefs?.getString("nmtid", "").orEmpty()
    }

    fun nmtidToken(): String {
        if (nmtid.isBlank()) {
            nmtid = buildString(32) {
                repeat(32) { append("0123456789abcdef"[kotlin.random.Random.nextInt(16)]) }
            }
            prefs?.edit()?.putString("nmtid", nmtid)?.apply()
        }
        return nmtid
    }
}
