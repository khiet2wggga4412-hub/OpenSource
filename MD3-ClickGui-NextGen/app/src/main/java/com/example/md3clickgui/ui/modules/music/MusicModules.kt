package com.example.md3clickgui.ui.modules.music

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

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
