package com.example.md3clickgui.ui.modules.music

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

/**
 * NetEase Cloud Music player card; rendered by a dedicated panel, not the generic settings UI.
 * Bound to [ModuleBinding.Content] so the module list shows no enable switch for it.
 */
object MusicModule : ModuleDefinition {
    override val sectionId = SectionIds.MUSIC
    override val model = GuiModule(
        id = "music.player",
        name = "Music",
        icon = Icons.Default.MusicNote,
        binding = ModuleBinding.Content
    )
}