package com.example.md3clickgui.ui.modules

import com.example.md3clickgui.ui.model.GuiModule

/**
 * A module owns its display metadata and settings outside of the screen.
 *
 * To add a card: create an object implementing this interface, then register it in
 * [allModuleDefinitions]. Nothing else needs to change — state, save/restore, and
 * rendering are automatic.
 */
interface ModuleDefinition {
    /** Which section this card belongs to; must be one of [SectionIds]. */
    val sectionId: String
    val model: GuiModule
}
