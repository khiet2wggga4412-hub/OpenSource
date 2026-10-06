package com.example.md3clickgui.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val coverUrl: String = ""
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val trackCount: Int,
    val playCount: Long = 0L
)

object NeteaseMusicApi {

    internal const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private const val EAPI_KEY = "e82ckenh8dichen8"
    private const val BASE = "https://interfacepc.music.163.com"
    private const val EAPI_UA = "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"
    private const val WEAPI_BASE = "https://music.163.com"

    private const val NONCE = "0CoJUm6Qyw8W8jud"
    private const val IV = "0102030405060708"
    private const val RSA_PUBLIC_KEY = "010001"
    private const val RSA_MODULUS = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b7" +
        "25152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c9387" +
        "0114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d5" +
        "46b8e289dc6935b3ece0462db0a22b8e7"

    private val random = Random()
    private val hexUpper = "0123456789ABCDEF"
    private val hexLower = "0123456789abcdef"

    suspend fun search(keyword: String): List<Song> = withContext(Dispatchers.IO) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) return@withContext emptyList()
        val payload = JSONObject()
            .put("s", normalized)
            .put("type", 1)
            .put("limit", 50)
            .put("offset", 0)
            .put("total", true)
        val json = eapiPost("/api/search/get", payload) ?: return@withContext emptyList()
        val songs = json.optJSONObject("result")?.optJSONArray("songs") ?: return@withContext emptyList()
        val result = mutableListOf<Song>()
        for (i in 0 until songs.length()) {
            val item = songs.optJSONObject(i) ?: continue

            if (item.optInt("fee", 0) > 0) continue
            parseSong(item)?.let { result.add(it) }
        }
        result
    }

    suspend fun songUrl(id: Long): String? = withContext(Dispatchers.IO) {

        val endpoint = "https://api.injahow.cn/meting/?server=netease&type=url&id=$id"
        var connection: HttpURLConnection? = null
        try {
            connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
            if (code in 300..399 && location != null && location.startsWith("http")) {
                return@withContext location.replace("http://", "https://")
            }
            null
        } catch (t: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun playlistSongs(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val data = JSONObject().put("id", playlistId).put("n", 100000).put("s", 8)
        val json = weapiPost("/weapi/v3/playlist/detail", data) ?: return@withContext emptyList()
        parseSongArray(json.optJSONObject("playlist")?.optJSONArray("tracks"))
    }

    suspend fun highqualityPlaylists(limit: Int = 30): List<PlaylistSummary> = withContext(Dispatchers.IO) {
        val data = JSONObject().put("cat", "全部").put("limit", limit).put("lasttime", 0).put("total", true)
        val json = weapiPost("/weapi/playlist/highquality/list", data) ?: return@withContext emptyList()
        mapPlaylists(json.optJSONArray("playlists"))
    }

    private fun parseSongArray(array: JSONArray?): List<Song> {
        if (array == null) return emptyList()
        val result = mutableListOf<Song>()
        for (i in 0 until array.length()) parseSong(array.optJSONObject(i))?.let { result.add(it) }
        return result
    }

    private fun mapPlaylists(array: JSONArray?): List<PlaylistSummary> {
        if (array == null) return emptyList()
        val result = mutableListOf<PlaylistSummary>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optLong("id", -1L)
            if (id <= 0) continue
            val cover = item.optString("picUrl", "").ifBlank { item.optString("coverImgUrl", "") }
            result.add(
                PlaylistSummary(
                    id = id,
                    name = item.optString("name", "Unknown").ifBlank { "Unknown" },
                    coverUrl = cover.replace("http://", "https://"),
                    trackCount = item.optInt("trackCount", 0),
                    playCount = item.optLong("playCount", 0L)
                )
            )
        }
        return result
    }

    private fun weapiPost(uri: String, obj: JSONObject): JSONObject? {
        val (params, encSecKey) = weapi(obj)
        val cookie = buildCookieWithSession()
        val url = WEAPI_BASE + uri
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Cookie", cookie)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Referer", "https://music.163.com/")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            val form = "params=" + URLEncoder.encode(params, "UTF-8") +
                "&encSecKey=" + URLEncoder.encode(encSecKey, "UTF-8")
            connection.outputStream.use { os -> os.write(form.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) {
                null
            } else {
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                JSONObject(body)
            }
        } catch (t: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun weapi(obj: JSONObject): Pair<String, String> {
        val text = obj.toString()
        val secretKey = randomSecretKey()
        val params = aesCbcBase64(aesCbcBase64(text, NONCE), secretKey)
        val encSecKey = rsaEncSecKey(secretKey)
        return params to encSecKey
    }

    private fun randomSecretKey(): String = buildString(16) {
        for (i in 0 until 16) append(hexLower[random.nextInt(16)])
    }

    private fun aesCbcBase64(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(IV.toByteArray(Charsets.UTF_8))
        )
        return Base64.getEncoder().encodeToString(cipher.doFinal(text.toByteArray(Charsets.UTF_8)))
    }

    private fun rsaEncSecKey(secretKey: String): String {
        val text = BigInteger(1, secretKey.reversed().toByteArray(Charsets.UTF_8))
        val exponent = BigInteger(RSA_PUBLIC_KEY, 16)
        val modulus = BigInteger(RSA_MODULUS, 16)
        return text.modPow(exponent, modulus).toString(16).padStart(256, '0')
    }

    private fun buildCookieWithSession(): String {
        val base = buildCookie()
        val session = NeteaseSession.cookie
        return if (session.isBlank()) base else "$base; $session"
    }

    private fun parseSong(obj: JSONObject?): Song? {
        obj ?: return null
        val id = obj.optLong("id", -1L)
        if (id <= 0) return null
        val title = obj.optString("name", "Unknown").ifBlank { "Unknown" }
        val artist = artistName(obj)
        val duration = when {
            obj.has("duration") -> obj.optLong("duration", 0L)
            obj.has("dt") -> obj.optLong("dt", 0L)
            else -> 0L
        }
        val coverUrl = (obj.optJSONObject("al") ?: obj.optJSONObject("album"))
            ?.optString("picUrl", "").orEmpty()
        return Song(
            id = id, title = title, artist = artist, durationMs = duration,
            coverUrl = coverUrl.replace("http://", "https://")
        )
    }

    private fun artistName(obj: JSONObject): String {
        join(obj.optJSONArray("ar"))?.let { return it }
        join(obj.optJSONArray("artists"))?.let { return it }
        val albumArtist = (obj.optJSONObject("al") ?: obj.optJSONObject("album"))
            ?.optJSONObject("artist")
        return albumArtist?.optString("name", "").orEmpty()
    }

    private fun join(array: JSONArray?): String? {
        if (array == null || array.length() == 0) return null
        val names = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val name = array.optJSONObject(i)?.optString("name", "").orEmpty()
            if (name.isNotEmpty()) names.add(name)
        }
        return if (names.isEmpty()) null else names.joinToString(" / ")
    }

    private fun eapiPost(uri: String, obj: JSONObject): JSONObject? {
        obj.put("e_r", false)
        val params = eapi(uri, obj)
        val url = BASE + "/eapi/" + uri.substring(5)
        val cookie = buildCookie()
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("User-Agent", EAPI_UA)
            connection.setRequestProperty("Cookie", cookie)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Referer", "https://music.163.com/")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.outputStream.use { os ->
                os.write(("params=$params").toByteArray(Charsets.UTF_8))
            }
            if (connection.responseCode !in 200..299) {
                null
            } else {
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                JSONObject(body)
            }
        } catch (t: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun eapi(uri: String, obj: JSONObject): String {
        val text = obj.toString()
        val digest = md5Hex("nobody${uri}use${text}md5forencrypt")
        val raw = "${uri}-36cd479b6b5-${text}-36cd479b6b5-${digest}"
        return aesEcbHex(raw, EAPI_KEY)
    }

    private fun aesEcbHex(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
        val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        return buildString(encrypted.size * 2) {
            for (b in encrypted) {
                val v = b.toInt() and 0xFF
                append(hexUpper[v ushr 4]).append(hexUpper[v and 0x0F])
            }
        }
    }

    private fun md5Hex(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            for (b in digest) {
                val v = b.toInt() and 0xFF
                append(hexLower[v ushr 4]).append(hexLower[v and 0x0F])
            }
        }
    }

    private fun buildCookie(): String {
        val now = System.currentTimeMillis()
        val deviceId = buildString {
            repeat(52) { append(hexUpper[random.nextInt(16)]) }
        }
        val requestId = "${now}_${random.nextInt(1000).toString().padStart(4, '0')}"
        val fields = linkedMapOf(
            "NMTID" to NeteaseSession.nmtidToken(),
            "osver" to "Microsoft-Windows-10-Professional-build-19045-64bit",
            "deviceId" to deviceId,
            "os" to "pc",
            "appver" to "3.1.17.204416",
            "versioncode" to "140",
            "mobilename" to "",
            "buildver" to (now / 1000).toString(),
            "resolution" to "1920x1080",
            "__csrf" to "",
            "channel" to "netease",
            "requestId" to requestId
        )
        return fields.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }
}
