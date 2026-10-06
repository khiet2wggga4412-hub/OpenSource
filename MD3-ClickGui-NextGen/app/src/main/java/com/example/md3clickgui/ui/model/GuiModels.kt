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
        /** Number of interior stops; zero keeps continuous dragging. */
        val steps: Int = 0,
        val decimalPlaces: Int = 0
    ) : ModuleSetting
    data class Choice(val label: String, val options: List<String>, val defaultIndex: Int = 0) : ModuleSetting
    data class ColorPicker(val label: String, val defaultColorHex: String = "#176B60") : ModuleSetting
}

@Immutable
data class GuiModule(
    val name: String,
    val icon: ImageVector,
    val settings: List<ModuleSetting> = emptyList(),
    /** Stable identity used by state holders; display names may change. */
    val id: String = name,
    /** Optional binding for modules that control app-level state. */
    val binding: ModuleBinding = ModuleBinding.Standard
)

enum class ModuleBinding {
    Standard,
    DarkMode,
    DynamicColor,
    Theme,
    Language,
    FloatingButton,

    /**
     * Content a card opens rather than a feature that is on or off (the music player and its browse
     * panels). Such a card shows no enable switch: tapping it opens the panel, and there is no
     * meaningful disabled state to switch to.
     */
    Content
}

/** Visual variants for per-module floating shortcuts; ordinal matches the Misc module's Style choice order. */
enum class FloatingButtonStyle { Icon, Text, IconText }

@Immutable
data class GuiSection(
    val name: String,
    val icon: ImageVector,
    val modules: List<GuiModule> = emptyList(),
    /** Stable identity referenced by [com.example.md3clickgui.ui.modules.SectionIds]. */
    val id: String = name
)
