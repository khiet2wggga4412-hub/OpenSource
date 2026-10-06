package dev.liquid.clickgui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.liquid.clickgui.ui.ClickGuiController
import dev.liquid.clickgui.ui.LiquidClickGuiScreen
import dev.liquid.clickgui.ui.PersistentHudOverlay

class UiPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val controller = remember { ClickGuiController() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEFF3F6)),
            ) {
                LiquidClickGuiScreen(
                    controller = controller,
                    modifier = Modifier.fillMaxSize(),
                    onCollapse = {},
                    onExit = {},
                )
                PersistentHudOverlay(
                    controller = controller,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
