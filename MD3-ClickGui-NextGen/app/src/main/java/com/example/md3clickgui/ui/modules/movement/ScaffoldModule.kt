package com.example.md3clickgui.ui.modules.movement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

object ScaffoldModule : ModuleDefinition {
    private const val ID = "movement.scaffold"

    override val sectionId = SectionIds.MOVEMENT
    override val model = GuiModule(
        id = ID,
        name = "Scaffold",
        icon = Icons.Default.GridView,
        settings = listOf(
            ModuleSetting.Toggle("Keep Sprint", defaultValue = true),
            ModuleSetting.Slider(
                "Extension length",
                defaultValue = 3f,
                valueRange = 1f..6f,
                suffix = " blocks",
                steps = 4
            ),
            ModuleSetting.Toggle("Rotate", defaultValue = true),
            ModuleSetting.Choice("Rotation mode", listOf("Normal", "Smooth", "Silent")),
            ModuleSetting.Toggle("Swing", defaultValue = true),
            ModuleSetting.Toggle("Silent scaffold"),
            ModuleSetting.Toggle("Auto rescue"),
            ModuleSetting.Toggle("Render", defaultValue = true),
            ModuleSetting.Toggle("Enable timer"),
            ModuleSetting.Slider(
                "Timer speed",
                defaultValue = 1f,
                valueRange = 1f..5f,
                suffix = "x",
                decimalPlaces = 2
            )
        )
    )
}
