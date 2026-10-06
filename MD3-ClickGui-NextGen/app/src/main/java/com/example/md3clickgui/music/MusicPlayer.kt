package com.example.md3clickgui.music

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object MusicPlayer {

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private lateinit var appContext: Context
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var player: MediaPlayer? = null
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)

    private var playJob: Job? = null
    private var playToken = 0

    private var resumeOnFocusGain = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {

                pauseForFocusLoss()
                abandonAudioFocus()
                releaseWakeLock()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {

                resumeOnFocusGain = isPlaying
                pauseForFocusLoss()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {

            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    startPlayback()
                }
            }
        }
    }

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun play(song: Song) {
        if (!::appContext.isInitialized) return
        if (song.id == currentSong?.id && player != null) {
            toggle()
            return
        }
        val token = stopAndRelease()
        currentSong = song
        durationMs = song.durationMs
        positionMs = 0L
        isBuffering = true
        error = null
        RecentStore.add(song)
        startKeepAlive()

        playJob = scope.launch {

            var ownsKeepAlive = true
            try {
                val url = NeteaseMusicApi.songUrl(song.id)
                ensureActive()
                if (token != playToken) return@launch
                if (url == null) {
                    isBuffering = false
                    error = "获取播放地址失败"
                    return@launch
                }
                val next = MediaPlayer()
                next.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                next.setOnPreparedListener { mp ->
                    isBuffering = false
                    durationMs = if (mp.duration > 0) mp.duration.toLong() else song.durationMs
                    runCatching { mp.start() }
                        .onSuccess { isPlaying = true }
                        .onFailure { error = it.message ?: "Failed to start playback" }
                }
                next.setOnCompletionListener { mp ->
                    isPlaying = false
                    positionMs = (if (mp.duration > 0) mp.duration else durationMs).toLong()
                    releaseWakeLock()
                }
                next.setOnErrorListener { _, what, extra ->
                    isBuffering = false
                    isPlaying = false
                    error = "Playback error ($what/$extra)"
                    ownsKeepAlive = false
                    stopKeepAlive()
                    true
                }
                if (token != playToken) {
                    next.release()
                    return@launch
                }
                player = next
                try {
                    next.setDataSource(
                        appContext,
                        Uri.parse(url),
                        mapOf(
                            "User-Agent" to NeteaseMusicApi.USER_AGENT,
                            "Referer" to "https://music.163.com/"
                        )
                    )
                    next.prepareAsync()

                    withTimeoutOrNull(timeMillis = PrepareTimeoutMs) {
                        while (player === next && isBuffering) delay(250)
                    }
                    if (token == playToken && isBuffering) {
                        isBuffering = false
                        error = "Stream timed out"
                        ownsKeepAlive = false
                        stopKeepAlive()
                        stopAndRelease()
                    }
                } catch (t: Exception) {
                    isBuffering = false
                    error = t.message ?: "Failed to load stream"
                    ownsKeepAlive = false
                    stopKeepAlive()
                    stopAndRelease()
                }
            } finally {

                if (ownsKeepAlive && token != playToken && player == null) stopKeepAlive()
            }
        }
    }

    private fun startPlayback() {
        val p = player ?: return
        runCatching { p.start() }
            .onSuccess {
                isPlaying = true
                requestAudioFocus()
                acquireWakeLock()
            }
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) {
            runCatching { p.pause() }
            isPlaying = false
            releaseWakeLock()
            abandonAudioFocus()
        } else {
            startPlayback()
        }
    }

    fun seekTo(ms: Long) {
        val p = player ?: return
        val target = ms.coerceIn(0L, durationMs.coerceAtLeast(0L))
        runCatching { p.seekTo(target.toInt()) }
        positionMs = target
    }

    fun syncPosition() {
        val p = player ?: return
        if (p.isPlaying) {
            positionMs = p.currentPosition.coerceAtLeast(0).toLong()
            if (p.duration > 0) durationMs = p.duration.toLong()
        }
    }

    fun release() {
        stopAndRelease()
        stopKeepAlive()
    }

    private fun stopAndRelease(): Int {
        playToken++
        playJob?.cancel()
        playJob = null
        val p = player
        player = null
        if (p != null) {
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        isPlaying = false
        isBuffering = false
        return playToken
    }

    private fun pauseForFocusLoss() {
        val p = player ?: return
        if (p.isPlaying) {
            runCatching { p.pause() }
            isPlaying = false
        }
        releaseWakeLock()
    }

    private fun startKeepAlive() {
        if (!::appContext.isInitialized) return
        try {
            appContext.startForegroundService(
                Intent(appContext, MusicPlaybackService::class.java)
                    .setAction(MusicPlaybackService.ACTION_PLAY)
            )
        } catch (_: Exception) {

        }
        requestAudioFocus()
        acquireWakeLock()
    }

    private fun stopKeepAlive() {
        if (!::appContext.isInitialized) return
        releaseWakeLock()
        abandonAudioFocus()
        runCatching { appContext.stopService(Intent(appContext, MusicPlaybackService::class.java)) }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
            .also { focusRequest = it }
        am.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        focusRequest?.let { am.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun acquireWakeLock() {
        if (!::appContext.isInitialized) return
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "md3clickgui:music")
            wakeLock?.setReferenceCounted(false)
        }
        wakeLock?.takeIf { !it.isHeld }?.acquire()
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    private const val PrepareTimeoutMs = 20_000L
}
