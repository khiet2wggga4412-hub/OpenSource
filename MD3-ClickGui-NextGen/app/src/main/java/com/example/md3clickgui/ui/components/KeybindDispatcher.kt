package com.example.md3clickgui.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
 * The host is made focusable and asked for focus once, because a key event only reaches a composable
 * inside the focus path. `onKeyEvent` is used rather than `onPreviewKeyEvent` so an open dialog or a
 * focused button still gets first refusal on the press.
 *
 * Auto-repeat is not filtered: Compose exposes no repeat flag on its key event (the underlying
 * `nativeKeyEvent` is restricted to the library), so holding a button down fires repeatedly, which is
 * the usual behaviour for a ClickGUI keybind.
 */
@Composable
fun Modifier.dispatchKeybinds(state: ClickGuiState, modules: List<GuiModule>): Modifier {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
    return this
        .focusRequester(focusRequester)
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
}
