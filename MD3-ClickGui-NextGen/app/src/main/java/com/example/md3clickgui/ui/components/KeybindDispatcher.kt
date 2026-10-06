package com.example.md3clickgui.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.state.ClickGuiState

@Composable
fun Modifier.dispatchKeybinds(state: ClickGuiState, modules: List<GuiModule>): Modifier =
    this
        .focusable()
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val code = event.key.keyCode.toInt()

            val bound = modules.filter { state.keybind(it)?.keyCode == code }
            if (bound.isEmpty()) return@onKeyEvent false
            bound.forEach(state::triggerKeybind)
            true
        }
