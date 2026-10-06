package com.example.md3clickgui.ui.modules.combat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Shield
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

/*
 * Combat cards. Plain [ModuleBinding.Standard] modules: the enable switch in the module list is the
 * module's on/off state, and every setting below it is editable in the details column.
 */

object KillAuraModule : ModuleDefinition {
    private const val ID = "combat.killaura"

    override val sectionId = SectionIds.COMBAT
    override val model = GuiModule(
        id = ID,
        name = "KillAura",
        icon = Icons.Default.GpsFixed,
        settings = listOf(
            ModuleSetting.Slider("Range", defaultValue = 3.2f, valueRange = 2f..6f, suffix = " m", decimalPlaces = 1),
            ModuleSetting.Choice("Target", listOf("Closest", "Lowest health", "Angle")),
            ModuleSetting.Choice("Rotation", listOf("Instant", "Smooth", "Silent")),
            ModuleSetting.Slider("CPS", defaultValue = 12f, valueRange = 5f..20f, suffix = " cps"),
            ModuleSetting.Toggle("Through walls"),
            ModuleSetting.Toggle("Swing", defaultValue = true)
        )
    )
}

object CriticalsModule : ModuleDefinition {
    private const val ID = "combat.criticals"

    override val sectionId = SectionIds.COMBAT
    override val model = GuiModule(
        id = ID,
        name = "Criticals",
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        settings = listOf(
            ModuleSetting.Choice("Mode", listOf("Packet", "Jump", "Mini")),
            ModuleSetting.Slider("Chance", defaultValue = 100f, valueRange = 10f..100f, suffix = "%"),
            ModuleSetting.Toggle("Only while sprinting")
        )
    )
}

object VelocityModule : ModuleDefinition {
    private const val ID = "combat.velocity"

    override val sectionId = SectionIds.COMBAT
    override val model = GuiModule(
        id = ID,
        name = "Velocity",
        icon = Icons.Default.Shield,
        settings = listOf(
            ModuleSetting.Slider("Horizontal", defaultValue = 0f, valueRange = 0f..1f, suffix = "%"),
            ModuleSetting.Slider("Vertical", defaultValue = 0f, valueRange = 0f..1f, suffix = "%"),
            ModuleSetting.Toggle("Knockback only", defaultValue = true),
            ModuleSetting.Toggle("Explosions")
        )
    )
}

object AutoClickerModule : ModuleDefinition {
    private const val ID = "combat.autoclicker"

    override val sectionId = SectionIds.COMBAT
    override val model = GuiModule(
        id = ID,
        name = "AutoClicker",
        icon = Icons.Default.AdsClick,
        settings = listOf(
            ModuleSetting.Slider("CPS", defaultValue = 11f, valueRange = 1f..20f, suffix = " cps", decimalPlaces = 1),
            ModuleSetting.Choice("Button", listOf("Left", "Right", "Both")),
            ModuleSetting.Toggle("Hold only", defaultValue = true),
            ModuleSetting.Toggle("Jitter")
        )
    )
}

object AntiKnockbackModule : ModuleDefinition {
    private const val ID = "combat.antiknockback"

    override val sectionId = SectionIds.COMBAT
    override val model = GuiModule(
        id = ID,
        name = "AntiKnockback",
        icon = Icons.Default.Bolt,
        settings = listOf(
            ModuleSetting.Slider("Strength", defaultValue = 1f, valueRange = 0f..1f, suffix = "%"),
            ModuleSetting.Toggle("Ground only")
        )
    )
}
