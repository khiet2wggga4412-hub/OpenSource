package com.example.md3clickgui.ui.modules.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LockOpen
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

/* Player cards. Same shape as the other standard modules. */

object AutoTotemModule : ModuleDefinition {
    private const val ID = "player.autototem"

    override val sectionId = SectionIds.PLAYER
    override val model = GuiModule(
        id = ID,
        name = "AutoTotem",
        icon = Icons.Default.HealthAndSafety,
        settings = listOf(
            ModuleSetting.Slider("Health threshold", defaultValue = 12f, valueRange = 2f..20f, suffix = " hp"),
            ModuleSetting.Toggle("Offhand swap", defaultValue = true),
            ModuleSetting.Toggle("Prefer enchanted")
        )
    )
}

object AutoEatModule : ModuleDefinition {
    private const val ID = "player.autoeat"

    override val sectionId = SectionIds.PLAYER
    override val model = GuiModule(
        id = ID,
        name = "AutoEat",
        icon = Icons.Default.LocalDining,
        settings = listOf(
            ModuleSetting.Slider("Hunger threshold", defaultValue = 14f, valueRange = 2f..20f, suffix = " hp"),
            ModuleSetting.Choice("Preferred", listOf("Best food", "Golden apple", "Any")),
            ModuleSetting.Toggle("Swap back", defaultValue = true)
        )
    )
}

object ChestStealerModule : ModuleDefinition {
    private const val ID = "player.cheststealer"

    override val sectionId = SectionIds.PLAYER
    override val model = GuiModule(
        id = ID,
        name = "ChestStealer",
        icon = Icons.Default.Inventory2,
        settings = listOf(
            ModuleSetting.Slider("Delay", defaultValue = 80f, valueRange = 0f..300f, suffix = " ms"),
            ModuleSetting.Toggle("Auto close", defaultValue = true),
            ModuleSetting.Toggle("Ignore trash", defaultValue = true)
        )
    )
}

object AutoToolModule : ModuleDefinition {
    private const val ID = "player.autotool"

    override val sectionId = SectionIds.PLAYER
    override val model = GuiModule(
        id = ID,
        name = "AutoTool",
        icon = Icons.Default.LockOpen,
        settings = listOf(
            ModuleSetting.Toggle("Keep durability", defaultValue = true),
            ModuleSetting.Toggle("Swap back", defaultValue = true)
        )
    )
}
