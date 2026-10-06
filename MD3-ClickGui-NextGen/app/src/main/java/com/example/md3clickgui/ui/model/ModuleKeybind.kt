package com.example.md3clickgui.ui.model

import android.view.KeyEvent
import androidx.compose.runtime.Immutable

/**
 * A physical button bound to a module.
 *
 * Only real input devices produce these: the Compose key pipeline reports a keyboard or gamepad
 * button as a [KeyEvent] with a stable code, so [keyCode] plus [isGamepad] is enough to identify one
 * and [label] is what the settings row shows.
 *
 * Mouse buttons are deliberately not modelled. Compose routes a mouse button through the pointer
 * pipeline (`PointerEvent.buttons`), not the key pipeline, so identifying button 4 and 5 would mean
 * re-implementing click handling for every control in the panel. Keyboard and gamepad cover the
 * buttons an emulator maps to a game's controls, which is what this panel is used with.
 */
@Immutable
data class ModuleKeybind(
    val keyCode: Int,
    val isGamepad: Boolean = false,
    /** Human-readable name resolved once at capture time, e.g. "G" or "Button A". */
    val label: String
)

/** Android key codes that represent a gamepad button rather than a keyboard key. */
private val GamepadKeyRange = KeyEvent.KEYCODE_BUTTON_A..KeyEvent.KEYCODE_BUTTON_MODE

fun isGamepadKey(keyCode: Int): Boolean = keyCode in GamepadKeyRange

/** Names shown in the settings row. Falls back to the numeric code for anything unmapped. */
fun keyLabel(keyCode: Int): String = when (keyCode) {
    KeyEvent.KEYCODE_A -> "A"
    KeyEvent.KEYCODE_B -> "B"
    KeyEvent.KEYCODE_C -> "C"
    KeyEvent.KEYCODE_D -> "D"
    KeyEvent.KEYCODE_E -> "E"
    KeyEvent.KEYCODE_F -> "F"
    KeyEvent.KEYCODE_G -> "G"
    KeyEvent.KEYCODE_H -> "H"
    KeyEvent.KEYCODE_I -> "I"
    KeyEvent.KEYCODE_J -> "J"
    KeyEvent.KEYCODE_K -> "K"
    KeyEvent.KEYCODE_L -> "L"
    KeyEvent.KEYCODE_M -> "M"
    KeyEvent.KEYCODE_N -> "N"
    KeyEvent.KEYCODE_O -> "O"
    KeyEvent.KEYCODE_P -> "P"
    KeyEvent.KEYCODE_Q -> "Q"
    KeyEvent.KEYCODE_R -> "R"
    KeyEvent.KEYCODE_S -> "S"
    KeyEvent.KEYCODE_T -> "T"
    KeyEvent.KEYCODE_U -> "U"
    KeyEvent.KEYCODE_V -> "V"
    KeyEvent.KEYCODE_W -> "W"
    KeyEvent.KEYCODE_X -> "X"
    KeyEvent.KEYCODE_Y -> "Y"
    KeyEvent.KEYCODE_Z -> "Z"
    KeyEvent.KEYCODE_0 -> "0"
    KeyEvent.KEYCODE_1 -> "1"
    KeyEvent.KEYCODE_2 -> "2"
    KeyEvent.KEYCODE_3 -> "3"
    KeyEvent.KEYCODE_4 -> "4"
    KeyEvent.KEYCODE_5 -> "5"
    KeyEvent.KEYCODE_6 -> "6"
    KeyEvent.KEYCODE_7 -> "7"
    KeyEvent.KEYCODE_8 -> "8"
    KeyEvent.KEYCODE_9 -> "9"
    KeyEvent.KEYCODE_SPACE -> "Space"
    KeyEvent.KEYCODE_ENTER -> "Enter"
    KeyEvent.KEYCODE_TAB -> "Tab"
    KeyEvent.KEYCODE_ESCAPE -> "Esc"
    KeyEvent.KEYCODE_DEL -> "Backspace"
    KeyEvent.KEYCODE_MINUS -> "-"
    KeyEvent.KEYCODE_EQUALS -> "="
    KeyEvent.KEYCODE_LEFT_BRACKET -> "["
    KeyEvent.KEYCODE_RIGHT_BRACKET -> "]"
    KeyEvent.KEYCODE_BACKSLASH -> "\\"
    KeyEvent.KEYCODE_SEMICOLON -> ";"
    KeyEvent.KEYCODE_APOSTROPHE -> "'"
    KeyEvent.KEYCODE_COMMA -> ","
    KeyEvent.KEYCODE_PERIOD -> "."
    KeyEvent.KEYCODE_SLASH -> "/"
    KeyEvent.KEYCODE_GRAVE -> "`"
    KeyEvent.KEYCODE_DPAD_UP -> "D-pad Up"
    KeyEvent.KEYCODE_DPAD_DOWN -> "D-pad Down"
    KeyEvent.KEYCODE_DPAD_LEFT -> "D-pad Left"
    KeyEvent.KEYCODE_DPAD_RIGHT -> "D-pad Right"
    KeyEvent.KEYCODE_DPAD_CENTER -> "D-pad Center"
    KeyEvent.KEYCODE_SHIFT_LEFT -> "L Shift"
    KeyEvent.KEYCODE_SHIFT_RIGHT -> "R Shift"
    KeyEvent.KEYCODE_CTRL_LEFT -> "L Ctrl"
    KeyEvent.KEYCODE_CTRL_RIGHT -> "R Ctrl"
    KeyEvent.KEYCODE_ALT_LEFT -> "L Alt"
    KeyEvent.KEYCODE_ALT_RIGHT -> "R Alt"
    KeyEvent.KEYCODE_BUTTON_A -> "Button A"
    KeyEvent.KEYCODE_BUTTON_B -> "Button B"
    KeyEvent.KEYCODE_BUTTON_C -> "Button C"
    KeyEvent.KEYCODE_BUTTON_X -> "Button X"
    KeyEvent.KEYCODE_BUTTON_Y -> "Button Y"
    KeyEvent.KEYCODE_BUTTON_Z -> "Button Z"
    KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
    KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
    KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
    KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
    KeyEvent.KEYCODE_BUTTON_THUMBL -> "L Stick"
    KeyEvent.KEYCODE_BUTTON_THUMBR -> "R Stick"
    KeyEvent.KEYCODE_BUTTON_START -> "Start"
    KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
    KeyEvent.KEYCODE_BUTTON_MODE -> "Guide"
    else -> if (keyCode >= KeyEvent.KEYCODE_F1 && keyCode <= KeyEvent.KEYCODE_F12) {
        "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
    } else {
        "Key $keyCode"
    }
}
