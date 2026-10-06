package dev.liquid.clickgui.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

internal object ModuleSwitchSoundPlayer {
    private const val SAMPLE_RATE = 44_100
    private const val DURATION_SECONDS = 0.24f
    private val handler = Handler(Looper.getMainLooper())
    private var activeTrack: AudioTrack? = null

    fun play(enabled: Boolean) {
        handler.post {
            runCatching {
                activeTrack?.runCatching {
                    stop()
                    release()
                }

                val samples = synthesizeChime(enabled)
                val pcm = ByteBuffer
                    .allocate(samples.size * 2)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .apply { samples.forEach(::putShort) }
                    .array()
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.setVolume(0.34f)
                activeTrack = track
                track.play()

                handler.postDelayed(
                    {
                        if (activeTrack === track) activeTrack = null
                        track.runCatching {
                            stop()
                            release()
                        }
                    },
                    360L,
                )
            }
        }
    }

    private fun synthesizeChime(enabled: Boolean): ShortArray {
        val count = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val firstFrequency = if (enabled) 880f else 740f
        val secondFrequency = if (enabled) 1_318.5f else 554.4f
        return ShortArray(count) { sampleIndex ->
            val time = sampleIndex.toFloat() / SAMPLE_RATE
            val first = note(time, 0f, firstFrequency, 0.15f)
            val second = note(time, 0.055f, secondFrequency, 0.17f)
            val value = ((first * 0.62f + second * 0.48f) * Short.MAX_VALUE)
                .coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
            value.toInt().toShort()
        }
    }

    private fun note(time: Float, start: Float, frequency: Float, duration: Float): Float {
        val localTime = time - start
        if (localTime !in 0f..duration) return 0f
        val attack = (localTime / 0.008f).coerceIn(0f, 1f)
        val release = ((duration - localTime) / 0.045f).coerceIn(0f, 1f)
        val decay = exp(-localTime * 10.5f)
        return sin(2.0 * PI * frequency * localTime).toFloat() * attack * release * decay * 0.24f
    }
}
