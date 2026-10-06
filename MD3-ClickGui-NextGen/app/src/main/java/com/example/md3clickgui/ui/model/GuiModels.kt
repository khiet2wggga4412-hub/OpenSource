package com.example.md3clickgui.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
sealed interface ModuleSetting {
    data class Toggle(val label: String, val defaultValue: Boolean = false) : ModuleSetting
    data class Slider(
        val label: String,
        val defaultValue: Float,
        val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
        val suffix: String = "%",

        val steps: Int = 0,
        val decimalPlaces: Int = 0
    ) : ModuleSetting
    data class Choice(val label: String, val options: List<String>, val defaultIndex: Int = 0) : ModuleSetting
    data class ColorPicker(val label: String, val defaultColorHex: String = "#176B60") : ModuleSetting

    data object Shortcut : ModuleSetting

    data object Keybind : ModuleSetting
}

@Immutable
data class GuiModule(
    val name: String,
    val icon: ImageVector,
    val settings: List<ModuleSetting> = emptyList(),

    val id: String = name,

    val binding: ModuleBinding = ModuleBinding.Standard
)

enum class ModuleBinding {
    Standard,
    DarkMode,
    DynamicColor,
    Theme,
    Language,
    ShortcutButton,

    Content
}

enum class ShortcutStyle { Icon, Text, IconText }

@Immutable
data class GuiSection(
    val name: String,
    val icon: ImageVector,
    val modules: List<GuiModule> = emptyList(),

    val id: String = name
)
