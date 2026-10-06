package com.example.md3clickgui.ui.modules.misc

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

object LanguageModule : ModuleDefinition {
    private const val ID = "misc.language"

    override val sectionId = SectionIds.MISC
    override val model = GuiModule(
        id = ID,
        name = "Language",
        icon = Icons.Default.Language,
        binding = ModuleBinding.Language,
        settings = listOf(
            ModuleSetting.Choice("Language", listOf("English", "简体中文"))
        )
    )
}
