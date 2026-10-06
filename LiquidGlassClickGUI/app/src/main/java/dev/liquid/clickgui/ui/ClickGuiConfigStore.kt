package dev.liquid.clickgui.ui

import android.content.Context
import dev.liquid.clickgui.model.BoolSetting
import dev.liquid.clickgui.model.EnumSetting
import dev.liquid.clickgui.model.FloatSetting
import dev.liquid.clickgui.model.IntSetting
import dev.liquid.clickgui.model.ModuleUiModel

/** SharedPreferences-backed storage for the UI-only ClickGUI configuration. */
class ClickGuiConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val autoSaveEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTO_SAVE, true)

    val uiLanguage: UiLanguage
        get() = runCatching {
            UiLanguage.valueOf(
                preferences.getString(KEY_LANGUAGE, UiLanguage.Chinese.name)
                    ?: UiLanguage.Chinese.name,
            )
        }.getOrDefault(UiLanguage.Chinese)

    val floatingButtonSizeDp: Int
        get() = FloatingButtonConfig.coerceSize(
            preferences.getInt(KEY_FLOATING_BUTTON_SIZE, FloatingButtonConfig.DEFAULT_SIZE_DP),
        )

    val backgroundMode: ClickGuiBackgroundMode
        get() = runCatching {
            ClickGuiBackgroundMode.valueOf(
                preferences.getString(KEY_BACKGROUND_MODE, ClickGuiBackgroundMode.SolidWhite.name)
                    ?: ClickGuiBackgroundMode.SolidWhite.name,
            )
        }.getOrDefault(ClickGuiBackgroundMode.SolidWhite)

    fun restoreModules(modules: List<ModuleUiModel>) {
        modules.forEach { module ->
            val enabledKey = moduleKey(module.id, "enabled")
            if (preferences.contains(enabledKey)) {
                module.enabled = preferences.getBoolean(enabledKey, module.enabled)
            }
            val shortcutKey = moduleKey(module.id, "shortcut")
            if (preferences.contains(shortcutKey)) {
                module.shortcutKeyCode = preferences.getInt(shortcutKey, 0).coerceAtLeast(0)
            }
            val quickOverlayKey = moduleKey(module.id, "quick_overlay")
            if (preferences.contains(quickOverlayKey)) {
                module.quickOverlayVisible = preferences.getBoolean(
                    quickOverlayKey,
                    module.quickOverlayVisible,
                )
            }
            module.settings.forEach settingLoop@{ setting ->
                val key = settingKey(module.id, setting.id)
                if (!preferences.contains(key)) return@settingLoop
                when (setting) {
                    is BoolSetting -> setting.value = preferences.getBoolean(key, setting.value)
                    is FloatSetting -> setting.value = preferences.getFloat(key, setting.value)
                        .coerceIn(setting.min, setting.max)
                    is IntSetting -> setting.value = preferences.getInt(key, setting.value)
                        .coerceIn(setting.min, setting.max)
                    is EnumSetting -> if (setting.options.isNotEmpty()) {
                        setting.selectedIndex = preferences.getInt(key, setting.selectedIndex)
                            .coerceIn(setting.options.indices)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_SAVE, enabled).apply()
    }

    fun save(
        language: UiLanguage,
        floatingButtonSizeDp: Int,
        backgroundMode: ClickGuiBackgroundMode,
        modules: List<ModuleUiModel>,
    ) {
        preferences.edit().apply {
            putBoolean(KEY_AUTO_SAVE, true)
            putString(KEY_LANGUAGE, language.name)
            putInt(KEY_FLOATING_BUTTON_SIZE, FloatingButtonConfig.coerceSize(floatingButtonSizeDp))
            putString(KEY_BACKGROUND_MODE, backgroundMode.name)
            modules.forEach { module ->
                putBoolean(moduleKey(module.id, "enabled"), module.enabled)
                putInt(moduleKey(module.id, "shortcut"), module.shortcutKeyCode.coerceAtLeast(0))
                putBoolean(moduleKey(module.id, "quick_overlay"), module.quickOverlayVisible)
                module.settings.forEach { setting ->
                    val key = settingKey(module.id, setting.id)
                    when (setting) {
                        is BoolSetting -> putBoolean(key, setting.value)
                        is FloatSetting -> putFloat(key, setting.value)
                        is IntSetting -> putInt(key, setting.value)
                        is EnumSetting -> putInt(key, setting.selectedIndex)
                        else -> Unit
                    }
                }
            }
        }.apply()
    }

    private fun moduleKey(moduleId: String, field: String): String = "module.$moduleId.$field"

    private fun settingKey(moduleId: String, settingId: String): String =
        "module.$moduleId.setting.$settingId"

    private companion object {
        const val PREFERENCES_NAME = "liquid_click_gui_config"
        const val KEY_AUTO_SAVE = "ui.auto_save"
        const val KEY_LANGUAGE = "ui.language"
        const val KEY_FLOATING_BUTTON_SIZE = "ui.floating_button_size"
        const val KEY_BACKGROUND_MODE = "ui.background_mode"
    }
}
