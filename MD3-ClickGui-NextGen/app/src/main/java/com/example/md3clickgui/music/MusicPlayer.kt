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

/**
 * Process-wide singleton player. Keeping it out of the Compose lifecycle lets playback survive the
 * Music panel closing, and a foreground service (started while audio is active) keeps the app alive
 * in the background / with the screen off. UI observes its Compose state directly.
 */
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

    /** The in-flight resolve/prepare; cancelled per play() so stale requests stop early. */
    private var playJob: Job? = null
    private var playToken = 0

    /** True only while playback is paused by a transient focus loss and may resume on regain. */
    private var resumeOnFocusGain = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss: another app owns audio now. Stop holding focus so it can play.
                pauseForFocusLoss()
                abandonAudioFocus()
                releaseWakeLock()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Keep the focus request: AUDIOFOCUS_GAIN arrives when the interruption ends.
                resumeOnFocusGain = isPlaying
                pauseForFocusLoss()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Music content: ducking is handled by the system mixer, nothing to do here.
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
            // startKeepAlive() runs after stopAndRelease(): everything it takes must be undone
            // here, or a failed resolve leaves a foreground service, a wake lock and audio focus held.
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
                    // No callback ever arrives for a dead stream; fail instead of buffering forever.
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
                // A superseded request must not leave the foreground service running.
                if (ownsKeepAlive && token != playToken && player == null) stopKeepAlive()
            }
        }
    }

    /** Resumes the prepared player without re-resolving the stream. */
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

    /** Reads live position/duration from the underlying player; called on a periodic tick. */
    fun syncPosition() {
        val p = player ?: return
        if (p.isPlaying) {
            positionMs = p.currentPosition.coerceAtLeast(0).toLong()
            if (p.duration > 0) durationMs = p.duration.toLong()
        }
    }

    /**
     * Tears the whole player down. Deliberately does **not** cancel [scope]: the scope belongs to
     * this process-wide singleton, so cancelling it here (the notification Stop action calls this)
     * would silently kill every later [play] — the launched coroutine never runs and playback stays
     * buffering forever.
     */
    fun release() {
        stopAndRelease()
        stopKeepAlive()
    }

    /** Cancels the in-flight resolve/prepare and returns the new generation token. */
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
            // Foreground start is refused while the app is backgrounded on Android 12+;
            // playback itself still works, so this is not fatal.
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

    /** One request object for the process: re-requesting the same focus is idempotent. */
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