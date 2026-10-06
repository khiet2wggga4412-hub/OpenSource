package com.example.md3clickgui.music

import android.content.Context
import android.content.SharedPreferences

/**
 * Anonymous request identity for the NetEase endpoints, plus the (always empty) cookie the weapi
 * signer reads from.
 *
 * There is deliberately no account support here. The login endpoints (the qrcode and cellphone
 * login calls) are refused for third-party clients by the service's risk checks, so a sign-in never
 * completes. Every feature that needs a session (favorites, daily recommendations, personal FM,
 * liking a track) was removed together with the login UI; what remains are the endpoints that
 * answer anonymously.
 *
 * What is kept is the client fingerprint: NMTID is expected on modern requests and is persisted,
 * so the same value is reused instead of a fresh one per request (which looks likelier to be
 * flagged as automation).
 */
object NeteaseSession {

    /**
     * Cookie header for the weapi POST. Always empty in this build; kept as the single source of
     * truth so a future session source has one place to write to.
     */
    var cookie: String = ""

    private var prefs: SharedPreferences? = null
    private var nmtid: String = ""

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("netease_music_session", Context.MODE_PRIVATE)
        nmtid = prefs?.getString("nmtid", "").orEmpty()
    }

    /** NMTID client fingerprint, generated once and reused afterwards. */
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
