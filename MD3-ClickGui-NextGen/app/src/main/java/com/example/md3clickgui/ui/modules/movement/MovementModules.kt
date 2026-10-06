package com.example.md3clickgui.ui.modules.movement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.RocketLaunch
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

/* Movement cards. Same shape as the Combat ones: enable switch, then settings. */

object SprintModule : ModuleDefinition {
    private const val ID = "movement.sprint"

    override val sectionId = SectionIds.MOVEMENT
    override val model = GuiModule(
        id = ID,
        name = "Sprint",
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        settings = listOf(
            ModuleSetting.Toggle("Keep sprint", defaultValue = true),
            ModuleSetting.Toggle("Avoid slowing", defaultValue = true),
            ModuleSetting.Toggle("Only on ground", defaultValue = true)
        )
    )
}

object FlyModule : ModuleDefinition {
    private const val ID = "movement.fly"

    override val sectionId = SectionIds.MOVEMENT
    override val model = GuiModule(
        id = ID,
        name = "Fly",
        icon = Icons.Default.FlightTakeoff,
        settings = listOf(
            ModuleSetting.Choice("Mode", listOf("Vanilla", "Glide", "Packet")),
            ModuleSetting.Slider("Speed", defaultValue = 1f, valueRange = 0.5f..4f, suffix = "x", decimalPlaces = 2),
            ModuleSetting.Toggle("Anti kick", defaultValue = true),
            ModuleSetting.Toggle("Auto disable on ground")
        )
    )
}

object SpeedModule : ModuleDefinition {
    private const val ID = "movement.speed"

    override val sectionId = SectionIds.MOVEMENT
    override val model = GuiModule(
        id = ID,
        name = "Speed",
        icon = Icons.Default.RocketLaunch,
        settings = listOf(
            ModuleSetting.Choice("Mode", listOf("NCP", "Vanilla", "Timer")),
            ModuleSetting.Slider("Multiplier", defaultValue = 1.4f, valueRange = 1f..3f, suffix = "x", decimalPlaces = 2),
            ModuleSetting.Toggle("Only while sprinting", defaultValue = true)
        )
    )
}

object NoFallModule : ModuleDefinition {
    private const val ID = "movement.nofall"

    override val sectionId = SectionIds.MOVEMENT
    override val model = GuiModule(
        id = ID,
        name = "NoFall",
        icon = Icons.Default.Air,
        settings = listOf(
            ModuleSetting.Choice("Mode", listOf("Packet", "Spoof", "Void")),
            ModuleSetting.Slider("Min height", defaultValue = 3f, valueRange = 2f..12f, suffix = " blocks")
        )
    )
}
