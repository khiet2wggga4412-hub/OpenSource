package com.example.md3clickgui.ui.modules.visual

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.ModuleDefinition
import com.example.md3clickgui.ui.modules.SectionIds

/**
 * Arraylist: lists every enabled module as a compact Material 3 stack of rows, drawn over the
 * workspace by [com.example.md3clickgui.ui.components.ArrayListHudLayer].
 *
 * Entries are derived from the module state rather than stored, so the list always matches what is
 * actually enabled. Rows are ordered by label length, longest first — the classic arraylist shape.
 * Only display options live here.
 */
object ArrayListModule : ModuleDefinition {
    private const val ID = "visual.arraylist"

    /** Choice order must match the indices read by the HUD layer. */
    val positionOptions = listOf("Top right", "Top left", "Bottom right", "Bottom left")

    /** Setting order is referenced by index in the HUD layer; keep it in sync. */
    override val sectionId = SectionIds.VISUAL
    override val model = GuiModule(
        id = ID,
        name = "ArrayList",
        icon = Icons.AutoMirrored.Filled.ViewList,
        settings = listOf(
            ModuleSetting.Choice("Position", positionOptions),
            ModuleSetting.Slider(
                "Text size",
                defaultValue = 13f,
                // Down to 1sp: the list is a HUD, and a very small label is a legitimate look for
                // it. Everything in the row (icon, line height, padding) scales with this value.
                valueRange = 1f..20f,
                suffix = " sp",
                decimalPlaces = 0
            ),
            ModuleSetting.Toggle("Show icons", defaultValue = true),
            ModuleSetting.Toggle("Background", defaultValue = true),
            ModuleSetting.Slider(
                "Background opacity",
                defaultValue = 0.72f,
                valueRange = 0.2f..1f,
                suffix = "%",
                decimalPlaces = 0
            )
        )
    )
}
