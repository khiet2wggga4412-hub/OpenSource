package com.example.md3clickgui.ui.modules.visual

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

object ArrayListModule : ModuleDefinition {
    private const val ID = "visual.arraylist"

    val positionOptions = listOf("Top right", "Top left", "Bottom right", "Bottom left")

    override val sectionId = SectionIds.VISUAL
    override val model = GuiModule(
        id = ID,
        name = "ArrayList",
        icon = Icons.AutoMirrored.Filled.ViewList,
        settings = listOf(
            ModuleSetting.Choice("Position", positionOptions),
            ModuleSetting.Slider(
                "Text size",
                defaultValue = 13f,

                valueRange = 1f..20f,
                suffix = " sp",
                decimalPlaces = 0
            ),
            ModuleSetting.Toggle("Show icons", defaultValue = true),
            ModuleSetting.Toggle("Background", defaultValue = true),
            ModuleSetting.Slider(
                "Background opacity",
                defaultValue = 0.72f,
                valueRange = 0.2f..1f,
                suffix = "%",
                decimalPlaces = 0
            )
        )
    )
}
