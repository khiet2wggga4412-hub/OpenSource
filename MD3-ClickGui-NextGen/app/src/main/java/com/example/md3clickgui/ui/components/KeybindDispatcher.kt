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

/**
 * Dispatches peripheral-button presses to the modules bound to them.
 *
 * Scope: this runs while the app owns the focused window. It cannot see input destined for another
 * app, which is a platform boundary — intercepting a game's keys would need an Xposed hook or an
 * accessibility service, and this project has neither.
 *
 * The host is made focusable so it can take part in the focus path, but it never *requests* focus:
 * asking for it at composition claimed focus from the login text fields and stopped the keyboard
 * from opening there. A key event that no focused child consumes still bubbles up to this host, which
 * is all the dispatcher needs.
 *
 * Auto-repeat is not filtered: Compose exposes no repeat flag on its key event (the underlying
 * `nativeKeyEvent` is restricted to the library), so holding a button down fires repeatedly, which is
 * the usual behaviour for a ClickGUI keybind.
 */
@Composable
fun Modifier.dispatchKeybinds(state: ClickGuiState, modules: List<GuiModule>): Modifier =
    this
        .focusable()
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val code = event.key.keyCode.toInt()
            // Several modules may share a button; all of them react.
            val bound = modules.filter { state.keybind(it)?.keyCode == code }
            if (bound.isEmpty()) return@onKeyEvent false
            bound.forEach(state::triggerKeybind)
            true
        }
