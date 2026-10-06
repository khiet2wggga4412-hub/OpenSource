package com.example.md3clickgui.ui.modules.music

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

/*
 * NetEase Cloud Music sidebar features, one card per feature. Each is rendered by a dedicated
 * panel (see MusicFeaturePanels.kt); defining objects here only carry display metadata.
 *
 * Only account-free features are listed. The account login and the panels that depend on a session
 * (Favorites, Recommend, Roam) were removed: their endpoints need a logged-in NetEase session,
 * which a third-party client cannot obtain because the login flow fails the service's risk checks.
 *
 * Both cards are [ModuleBinding.Content]: they open a panel, they are not features with an off
 * state, so the module list shows no enable switch for them.
 */

object FeaturedModule : ModuleDefinition {
    override val sectionId = SectionIds.MUSIC
    override val model = GuiModule(
        id = "music.featured",
        name = "Featured",
        icon = Icons.Default.Star,
        binding = ModuleBinding.Content
    )
}

object RecentModule : ModuleDefinition {
    override val sectionId = SectionIds.MUSIC
    override val model = GuiModule(
        id = "music.recent",
        name = "Recent",
        icon = Icons.Default.History,
        binding = ModuleBinding.Content
    )
}
