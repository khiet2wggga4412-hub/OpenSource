package com.example.md3clickgui.ui.modules.world

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Waves
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

object NukerModule : ModuleDefinition {
    private const val ID = "world.nuker"

    override val sectionId = SectionIds.WORLD
    override val model = GuiModule(
        id = ID,
        name = "Nuker",
        icon = Icons.Default.AutoFixHigh,
        settings = listOf(
            ModuleSetting.Slider("Range", defaultValue = 4.5f, valueRange = 2f..6f, suffix = " m", decimalPlaces = 1),
            ModuleSetting.Choice("Shape", listOf("Flat", "Cube", "Sphere")),
            ModuleSetting.Choice("Sort", listOf("Nearest", "Hardest", "Random")),
            ModuleSetting.Toggle("Only exposed", defaultValue = true)
        )
    )
}

object AutoBuildModule : ModuleDefinition {
    private const val ID = "world.autobuild"

    override val sectionId = SectionIds.WORLD
    override val model = GuiModule(
        id = ID,
        name = "AutoBuild",
        icon = Icons.Default.GridView,
        settings = listOf(
            ModuleSetting.Slider("Extend", defaultValue = 3f, valueRange = 1f..6f, suffix = " blocks", steps = 4),
            ModuleSetting.Slider("Place delay", defaultValue = 60f, valueRange = 0f..200f, suffix = " ms"),
            ModuleSetting.Toggle("Rotate", defaultValue = true),
            ModuleSetting.Toggle("Silent", defaultValue = true)
        )
    )
}

object FastPlaceModule : ModuleDefinition {
    private const val ID = "world.fastplace"

    override val sectionId = SectionIds.WORLD
    override val model = GuiModule(
        id = ID,
        name = "FastPlace",
        icon = Icons.Default.FlashOn,
        settings = listOf(
            ModuleSetting.Slider("Delay", defaultValue = 0f, valueRange = 0f..4f, suffix = " ticks"),
            ModuleSetting.Toggle("Only blocks", defaultValue = true)
        )
    )
}

object LiquidsModule : ModuleDefinition {
    private const val ID = "world.liquids"

    override val sectionId = SectionIds.WORLD
    override val model = GuiModule(
        id = ID,
        name = "Liquids",
        icon = Icons.Default.Waves,
        settings = listOf(
            ModuleSetting.Toggle("Water", defaultValue = true),
            ModuleSetting.Toggle("Lava", defaultValue = true),
            ModuleSetting.Slider("Speed", defaultValue = 1.5f, valueRange = 1f..3f, suffix = "x", decimalPlaces = 1)
        )
    )
}
