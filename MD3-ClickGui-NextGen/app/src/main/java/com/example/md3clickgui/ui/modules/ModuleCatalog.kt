package com.example.md3clickgui.ui.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.md3clickgui.ui.model.GuiSection
import com.example.md3clickgui.ui.modules.combat.AntiKnockbackModule
import com.example.md3clickgui.ui.modules.combat.AutoClickerModule
import com.example.md3clickgui.ui.modules.combat.CriticalsModule
import com.example.md3clickgui.ui.modules.combat.KillAuraModule
import com.example.md3clickgui.ui.modules.combat.VelocityModule
import com.example.md3clickgui.ui.modules.misc.FloatingButtonModule
import com.example.md3clickgui.ui.modules.misc.LanguageModule
import com.example.md3clickgui.ui.modules.misc.ThemeModule
import com.example.md3clickgui.ui.modules.movement.FlyModule
import com.example.md3clickgui.ui.modules.movement.NoFallModule
import com.example.md3clickgui.ui.modules.movement.ScaffoldModule
import com.example.md3clickgui.ui.modules.movement.SpeedModule
import com.example.md3clickgui.ui.modules.movement.SprintModule
import com.example.md3clickgui.ui.modules.music.FeaturedModule
import com.example.md3clickgui.ui.modules.music.MusicModule
import com.example.md3clickgui.ui.modules.music.RecentModule
import com.example.md3clickgui.ui.modules.player.AutoEatModule
import com.example.md3clickgui.ui.modules.player.AutoToolModule
import com.example.md3clickgui.ui.modules.player.AutoTotemModule
import com.example.md3clickgui.ui.modules.player.ChestStealerModule
import com.example.md3clickgui.ui.modules.visual.ArrayListModule
import com.example.md3clickgui.ui.modules.visual.CameraModule
import com.example.md3clickgui.ui.modules.visual.EspModule
import com.example.md3clickgui.ui.modules.visual.TracersModule
import com.example.md3clickgui.ui.modules.world.AutoBuildModule
import com.example.md3clickgui.ui.modules.world.FastPlaceModule
import com.example.md3clickgui.ui.modules.world.LiquidsModule
import com.example.md3clickgui.ui.modules.world.NukerModule

/** Stable section identifiers referenced by module definitions. */
object SectionIds {
    const val AI = "ai"
    const val COMBAT = "combat"
    const val MOVEMENT = "movement"
    const val WORLD = "world"
    const val PLAYER = "player"
    const val VISUAL = "visual"
    const val MISC = "misc"
    const val MUSIC = "music"
    const val CONFIG = "config"
}

private data class SectionTemplate(
    val id: String,
    val name: String,
    val icon: ImageVector
)

/** Ordered section shell; modules attach via [ModuleDefinition.sectionId]. */
private val sectionTemplates = listOf(
    SectionTemplate(SectionIds.AI, "AI", Icons.Default.AutoFixHigh),
    SectionTemplate(SectionIds.COMBAT, "Combat", Icons.Default.Bolt),
    SectionTemplate(SectionIds.MOVEMENT, "Movement", Icons.Default.Explore),
    SectionTemplate(SectionIds.WORLD, "World", Icons.Default.Language),
    SectionTemplate(SectionIds.PLAYER, "Player", Icons.Default.Person),
    SectionTemplate(SectionIds.VISUAL, "Visual", Icons.Default.Visibility),
    SectionTemplate(SectionIds.MISC, "Misc", Icons.Default.AutoAwesome),
    SectionTemplate(SectionIds.MUSIC, "Music", Icons.Default.MusicNote),
    SectionTemplate(SectionIds.CONFIG, "Config", Icons.Default.Folder)
)

/**
 * The single registration point for module cards.
 *
 * Add a card: create a `ModuleDefinition` object and append it here.
 * Remove a card: delete its entry (and its file).
 * Card order within a section follows this list.
 */
val allModuleDefinitions: List<ModuleDefinition> = listOf(
    // Combat
    KillAuraModule,
    CriticalsModule,
    VelocityModule,
    AutoClickerModule,
    AntiKnockbackModule,
    // Movement
    SprintModule,
    ScaffoldModule,
    FlyModule,
    SpeedModule,
    NoFallModule,
    // World
    NukerModule,
    AutoBuildModule,
    FastPlaceModule,
    LiquidsModule,
    // Player
    AutoTotemModule,
    AutoEatModule,
    ChestStealerModule,
    AutoToolModule,
    // Visual
    ArrayListModule,
    EspModule,
    TracersModule,
    CameraModule,
    // Misc
    ThemeModule,
    LanguageModule,
    FloatingButtonModule,
    // Music
    MusicModule,
    FeaturedModule,
    RecentModule
)

private fun buildSections(): List<GuiSection> {
    val knownIds = sectionTemplates.map { it.id }.toSet()
    allModuleDefinitions.forEach { definition ->
        require(definition.sectionId in knownIds) {
            "Module '${definition.model.id}' references unknown section '${definition.sectionId}'"
        }
    }
    return sectionTemplates.map { template ->
        GuiSection(
            id = template.id,
            name = template.name,
            icon = template.icon,
            modules = allModuleDefinitions
                .filter { it.sectionId == template.id }
                .map { it.model }
        )
    }
}

/** Ordered category list consumed by navigation and the module content screen. */
val guiSections: List<GuiSection> = buildSections()
