package com.example.md3clickgui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.md3clickgui.music.MusicPlayer
import com.example.md3clickgui.music.NeteaseSession
import com.example.md3clickgui.music.RecentStore
import androidx.activity.compose.setContent
import com.example.md3clickgui.ui.screens.NexusClickGui
import com.example.md3clickgui.ui.state.rememberClickGuiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicPlayer.init(applicationContext)
        NeteaseSession.init(applicationContext)
        RecentStore.init(applicationContext)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        setContent {
            val state = rememberClickGuiState()
            NexusClickGui(state)
        }
    }
}
