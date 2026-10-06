package com.example.md3clickgui.ui.modules.visual

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.TrackChanges
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

object EspModule : ModuleDefinition {
    private const val ID = "visual.esp"

    override val sectionId = SectionIds.VISUAL
    override val model = GuiModule(
        id = ID,
        name = "ESP",
        icon = Icons.Default.FilterCenterFocus,
        settings = listOf(
            ModuleSetting.Choice("Mode", listOf("Box", "Outline", "Glow")),
            ModuleSetting.Slider("Range", defaultValue = 32f, valueRange = 8f..64f, suffix = " m"),
            ModuleSetting.Toggle("Show health", defaultValue = true),
            ModuleSetting.Toggle("Show distance", defaultValue = true),
            ModuleSetting.Toggle("Only players", defaultValue = true)
        )
    )
}

object TracersModule : ModuleDefinition {
    private const val ID = "visual.tracers"

    override val sectionId = SectionIds.VISUAL
    override val model = GuiModule(
        id = ID,
        name = "Tracers",
        icon = Icons.Default.TrackChanges,
        settings = listOf(
            ModuleSetting.Slider("Width", defaultValue = 1.2f, valueRange = 0.5f..3f, suffix = " px", decimalPlaces = 1),
            ModuleSetting.Choice("Origin", listOf("Crosshair", "Feet", "Top")),
            ModuleSetting.Toggle("Depth test")
        )
    )
}

object CameraModule : ModuleDefinition {
    private const val ID = "visual.camera"

    override val sectionId = SectionIds.VISUAL
    override val model = GuiModule(
        id = ID,
        name = "Camera",
        icon = Icons.Default.BlurOn,
        settings = listOf(
            ModuleSetting.Slider("Distance", defaultValue = 4f, valueRange = 2f..12f, suffix = " m", decimalPlaces = 1),
            ModuleSetting.Toggle("No clip"),
            ModuleSetting.Toggle("Follow rotation", defaultValue = true)
        )
    )
}
