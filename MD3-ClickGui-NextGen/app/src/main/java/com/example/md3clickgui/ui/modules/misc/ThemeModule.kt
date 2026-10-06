package com.example.md3clickgui.ui.modules.misc

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.theme.NexusThemeSwatchNames
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

/** Global color palette selector exposed as a regular Misc module. */
object ThemeModule : ModuleDefinition {
    private const val ID = "misc.theme"

    override val sectionId = SectionIds.MISC
    override val model = GuiModule(
        id = ID,
        name = "Theme",
        icon = Icons.Default.Palette,
        binding = ModuleBinding.Theme,
        settings = listOf(
            ModuleSetting.Choice("Theme color", NexusThemeSwatchNames),
            ModuleSetting.ColorPicker("Accent color"),
            ModuleSetting.Toggle("Dark theme"),
            ModuleSetting.Toggle("Dynamic color")
        )
    )
}
