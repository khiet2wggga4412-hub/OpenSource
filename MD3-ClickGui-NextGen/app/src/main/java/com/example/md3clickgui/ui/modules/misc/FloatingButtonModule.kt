package com.example.md3clickgui.ui.modules.misc

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

/** Global style and size for the per-module floating shortcuts, exposed in Misc. */
object FloatingButtonModule : ModuleDefinition {
    private const val ID = "misc.floatingbutton"

    override val sectionId = SectionIds.MISC
    override val model = GuiModule(
        id = ID,
        name = "Floating button",
        icon = Icons.Default.AutoAwesome,
        binding = ModuleBinding.FloatingButton,
        settings = listOf(
            ModuleSetting.Choice("Style", listOf("Icon", "Text", "Icon + text")),
            ModuleSetting.Slider("Size", defaultValue = 32f, valueRange = 24f..48f, suffix = "dp", decimalPlaces = 0),
            ModuleSetting.Slider("Corner radius", defaultValue = 10f, valueRange = 0f..24f, suffix = "dp", decimalPlaces = 0)
        )
    )
}