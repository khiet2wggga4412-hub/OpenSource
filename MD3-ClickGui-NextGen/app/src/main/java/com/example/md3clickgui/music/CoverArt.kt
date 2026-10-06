package com.example.md3clickgui.music

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.net.HttpURLConnection
import java.net.URL

object CoverArt {

    fun loadCover(url: String): Bitmap? {
        if (url.isBlank()) return null
        val target = url.replace("http://", "https://")
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(target).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout = 12_000
            conn.setRequestProperty("User-Agent", NeteaseMusicApi.USER_AGENT)
            if (conn.responseCode !in 200..299) return null
            val bytes = conn.inputStream.use { it.readBytes() }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (t: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun dominantColor(bitmap: Bitmap): Int {
        val targetW = 40
        val targetH = (targetW * bitmap.height / bitmap.width.toFloat()).coerceAtLeast(1f).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, false)
        var rSum = 0.0
        var gSum = 0.0
        var bSum = 0.0
        var weightTotal = 0.0
        for (y in 0 until targetH) {
            for (x in 0 until targetW) {
                val c = scaled.getPixel(x, y)
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                val saturation = if (max == 0) 0.0 else (max - min) / max.toDouble()
                if (luminance < 0.10 || luminance > 0.90 || saturation < 0.15) continue
                val weight = saturation * (max / 255.0)
                rSum += r * weight
                gSum += g * weight
                bSum += b * weight
                weightTotal += weight
            }
        }
        if (scaled !== bitmap) scaled.recycle()
        if (weightTotal <= 0.0) return 0
        return Color.rgb(
            (rSum / weightTotal).toInt().coerceIn(0, 255),
            (gSum / weightTotal).toInt().coerceIn(0, 255),
            (bSum / weightTotal).toInt().coerceIn(0, 255)
        )
    }
}
