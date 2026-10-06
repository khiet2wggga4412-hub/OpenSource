package com.example.md3clickgui.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Foreground service that keeps the process alive while music is streaming. It owns the persistent
 * notification (with play/pause and stop actions); the actual [MediaPlayer] lives in [MusicPlayer].
 */
class MusicPlaybackService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastPosted: String? = null

    /** The notification embeds live state, so the actions would go stale without a refresh. */
    private val refreshNotification = object : Runnable {
        override fun run() {
            postNotification()
            handler.postDelayed(this, NotificationRefreshMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music playback",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                MusicPlayer.release()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> MusicPlayer.toggle()
        }
        // startForeground must precede any notify() on this id, and it also picks up a state
        // change that happened before the service was started.
        startForeground(NOTIFICATION_ID, buildNotification())
        lastPosted = notificationFingerprint()
        handler.removeCallbacks(refreshNotification)
        handler.postDelayed(refreshNotification, NotificationRefreshMs)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshNotification)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun postNotification() {
        val fingerprint = notificationFingerprint()
        if (fingerprint == lastPosted) return
        lastPosted = fingerprint
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    /** Cheap comparison key; the notification is only rebuilt when playback state actually moves. */
    private fun notificationFingerprint(): String {
        val song = MusicPlayer.currentSong
        return "${song?.id}|${MusicPlayer.isPlaying}|${MusicPlayer.error}"
    }

    private fun buildNotification(): Notification {
        val song = MusicPlayer.currentSong
        val playing = MusicPlayer.isPlaying
        val title = song?.title ?: "Material Music"
        val text = when {
            song == null -> "Idle"
            playing -> song.artist.ifBlank { "Playing" }
            else -> "Paused"
        }
        val togglePending = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPending = PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, toggleIcon),
                    if (playing) "Pause" else "Play",
                    togglePending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    "Stop",
                    stopPending
                ).build()
            )
            .build()
    }

    companion object {
        const val ACTION_PLAY = "com.example.md3clickgui.music.PLAY"
        const val ACTION_TOGGLE = "com.example.md3clickgui.music.TOGGLE"
        const val ACTION_STOP = "com.example.md3clickgui.music.STOP"
        private const val CHANNEL_ID = "music_playback"
        private const val NOTIFICATION_ID = 1001
        private const val NotificationRefreshMs = 1_000L
    }
}