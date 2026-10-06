package dev.liquid.clickgui.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.liquid.clickgui.model.ActionSetting
import dev.liquid.clickgui.model.BoolSetting
import dev.liquid.clickgui.model.EnumSetting
import dev.liquid.clickgui.model.FloatSetting
import dev.liquid.clickgui.model.IntSetting
import dev.liquid.clickgui.model.ModuleCategory
import dev.liquid.clickgui.model.ModuleEffectState
import dev.liquid.clickgui.model.ModuleUiModel
import dev.liquid.clickgui.data.UiModuleGateway

enum class UiLanguage { Chinese, English }

@Stable
class ClickGuiController(
    private val gateway: UiModuleGateway = UiModuleGateway(),
    private val configStore: ClickGuiConfigStore? = null,
) {
    val modules = mutableStateListOf<ModuleUiModel>()
    val moduleSwitchNotices = ModuleSwitchNoticeCoordinator()
    var selectedCategory by mutableStateOf(ModuleCategory.Combat)
    var selectedModule by mutableStateOf<ModuleUiModel?>(null)
    var showAboutDialog by mutableStateOf(false)
    var pendingShortcutModule by mutableStateOf<ModuleUiModel?>(null)
    var uiLanguage by mutableStateOf(configStore?.uiLanguage ?: UiLanguage.Chinese)
        private set
    var autoSaveEnabled by mutableStateOf(configStore?.autoSaveEnabled ?: true)
        private set
    val floatingButtonConfig = FloatingButtonConfig(
        configStore?.floatingButtonSizeDp ?: FloatingButtonConfig.DEFAULT_SIZE_DP,
    )

    val bridgeStatus: String get() = gateway.status

    private val dynamicIslandNoticeSetting = BoolSetting(
        id = DYNAMIC_ISLAND_SETTING_ID,
        label = "Module switch notifications",
        value = true,
    )

    private val dynamicIslandNoticeModule = ModuleUiModel(
        id = DYNAMIC_ISLAND_NOTICE_ID,
        name = "灵动岛提示",
        englishName = "Dynamic Island Alerts",
        description = "Top pill notification after a confirmed module switch",
        category = ModuleCategory.Visual,
        enabled = true,
        settings = listOf(dynamicIslandNoticeSetting),
        effectAvailable = true,
        effectBackend = COMPOSE_UI_BACKEND,
    ).apply {
        updateEffectStatus(
            backend = COMPOSE_UI_BACKEND,
            state = ModuleEffectState.Active,
        )
    }

    init {
        modules += gateway.loadCatalog()
        if (modules.none { it.id == DYNAMIC_ISLAND_NOTICE_ID }) {
            modules += dynamicIslandNoticeModule
        }

        bindLocalModule(ARRAY_LIST_ID, COMPOSE_OVERLAY_BACKEND)

        ClickGuiRenderer.selectBackgroundMode(
            configStore?.backgroundMode ?: ClickGuiRenderer.backgroundMode,
        )
        configStore?.restoreModules(modules)
        synchronizeRestoredModuleStates()
    }

    fun modulesFor(category: ModuleCategory): List<ModuleUiModel> =
        modules.filter { it.category == category }

    fun quickOverlayModules(): List<ModuleUiModel> =
        modules.filter { it.quickOverlayVisible }

    fun setEnabled(module: ModuleUiModel, enabled: Boolean) {
        val previousValue = module.enabled
        val succeeded = when {
            module.id == DYNAMIC_ISLAND_NOTICE_ID ->
                setDynamicIslandNoticeEnabled(module, enabled)
            module.id in LOCAL_VISUAL_MODULE_IDS ->
                setLocalVisualEnabled(module, enabled)
            else -> gateway.setModuleEnabled(module, enabled)
        }
        if (!succeeded || previousValue == enabled) return

        ModuleSwitchSoundPlayer.play(enabled)

        val mayShowNotice = dynamicIslandNoticeModule.enabled ||
            module.id == DYNAMIC_ISLAND_NOTICE_ID
        if (mayShowNotice) {
            moduleSwitchNotices.publish(module.name, enabled)
        }
        persistIfEnabled()
    }

    fun setQuickOverlayVisible(module: ModuleUiModel, visible: Boolean) {
        module.quickOverlayVisible = visible
        persistIfEnabled()
    }

    fun toggleQuickOverlay(module: ModuleUiModel) {
        setQuickOverlayVisible(module, !module.quickOverlayVisible)
    }

    fun setBool(module: ModuleUiModel, setting: BoolSetting, value: Boolean) {
        when {
            module.id == DYNAMIC_ISLAND_NOTICE_ID &&
                setting.id == DYNAMIC_ISLAND_SETTING_ID -> setEnabled(module, value)
            module.id in LOCAL_VISUAL_MODULE_IDS -> {
                setting.value = value
                refreshLocalModuleState(module)
            }
            else -> gateway.setBool(module, setting, value)
        }
        persistIfEnabled()
    }

    fun setFloat(module: ModuleUiModel, setting: FloatSetting, value: Float) {
        if (module.id in LOCAL_VISUAL_MODULE_IDS) {
            setting.value = value.coerceIn(setting.min, setting.max)
            refreshLocalModuleState(module)
        } else {
            gateway.setFloat(module, setting, value)
        }
        persistIfEnabled()
    }

    fun setInt(module: ModuleUiModel, setting: IntSetting, value: Int) {
        if (module.id in LOCAL_VISUAL_MODULE_IDS) {
            setting.value = value.coerceIn(setting.min, setting.max)
            refreshLocalModuleState(module)
        } else {
            gateway.setInt(module, setting, value)
        }
        persistIfEnabled()
    }

    fun setEnum(module: ModuleUiModel, setting: EnumSetting, index: Int) {
        if (module.id in LOCAL_VISUAL_MODULE_IDS) {
            if (setting.options.isNotEmpty()) {
                setting.selectedIndex = index.coerceIn(setting.options.indices)
                refreshLocalModuleState(module)
            }
        } else {
            gateway.setEnum(module, setting, index)
        }
        persistIfEnabled()
    }

    fun invokeAction(module: ModuleUiModel, setting: ActionSetting) {
        gateway.invokeAction(module, setting)
    }

    fun beginShortcutCapture(module: ModuleUiModel) {
        pendingShortcutModule = module
    }

    fun commitShortcut(keyCode: Int) {
        val module = pendingShortcutModule ?: return
        val normalizedKeyCode = keyCode.coerceAtLeast(0)
        modules.filter { it !== module && it.shortcutKeyCode == normalizedKeyCode }
            .forEach { it.shortcutKeyCode = 0 }
        if (isUiOwnedModule(module)) {
            module.shortcutKeyCode = normalizedKeyCode
        } else {
            gateway.setShortcut(module, normalizedKeyCode)
        }
        pendingShortcutModule = null
        persistIfEnabled()
    }

    fun clearShortcut(module: ModuleUiModel) {
        if (isUiOwnedModule(module)) {
            module.shortcutKeyCode = 0
        } else {
            gateway.setShortcut(module, 0)
        }
        pendingShortcutModule = null
        persistIfEnabled()
    }

    fun updateUiLanguage(language: UiLanguage) {
        if (uiLanguage == language) return
        uiLanguage = language
        persistIfEnabled()
    }

    fun toggleUiLanguage() {
        updateUiLanguage(
            if (uiLanguage == UiLanguage.Chinese) UiLanguage.English else UiLanguage.Chinese,
        )
    }

    fun updateAutoSaveEnabled(enabled: Boolean) {
        if (autoSaveEnabled == enabled) return
        autoSaveEnabled = enabled
        configStore?.setAutoSaveEnabled(enabled)
        if (enabled) saveConfigurationNow()
    }

    fun setFloatingButtonSize(sizeDp: Int) {
        floatingButtonConfig.setSize(sizeDp)
        persistIfEnabled()
    }

    fun setBackgroundMode(mode: ClickGuiBackgroundMode) {
        ClickGuiRenderer.selectBackgroundMode(mode)
        persistIfEnabled()
    }

    fun handleShortcutKey(keyCode: Int): Boolean {
        if (keyCode <= 0) return false
        if (pendingShortcutModule != null) {
            commitShortcut(keyCode)
            return true
        }
        val module = modules.firstOrNull { it.shortcutKeyCode == keyCode } ?: return false
        setEnabled(module, !module.enabled)
        return true
    }

    fun arrayListHudState(): ArrayListHudState {
        val module = modules.firstOrNull { it.id == ARRAY_LIST_ID }
            ?: return ArrayListHudState.Disabled
        return ArrayListHudState(
            enabled = module.enabled,
            layout = ArrayListLayoutState(
                displayMode = module.enumValue("display_mode", 3),
                position = module.enumValue("position"),
                sortMode = module.enumValue("sort_mode"),
                offsetX = module.intValue("offset_x", 10).coerceAtLeast(0),
                offsetY = module.intValue("offset_y", 70).coerceAtLeast(0),
                uniformWidth = module.boolValue("uniform_width"),
                showIndex = module.boolValue("show_index"),
                textDecoration = module.enumValue("text_decoration"),
                paddingHorizontal = module.floatValue("padding_horizontal", 6f).coerceIn(0f, 24f),
                paddingVertical = module.floatValue("padding_vertical", 2f).coerceIn(0f, 14f),
                itemSpacing = module.floatValue("item_spacing", 2f).coerceIn(0f, 16f),
                cornerRadius = module.floatValue("corner_radius", 7f).coerceIn(0f, 24f),
            ),
            text = ArrayListTextState(
                fontSize = module.floatValue("font_size", 14f).coerceIn(8f, 36f),
                fontWeight = module.enumValue("font_weight", 2),
                letterSpacing = module.floatValue("letter_spacing", 0f).coerceIn(-0.5f, 3f),
                opacity = module.floatValue("text_opacity", 1f).coerceIn(0.05f, 1f),
                uppercase = module.boolValue("uppercase"),
                shadowEnabled = module.boolValue("text_shadow", true),
                blackShadow = module.enumValue("shadow_mode", 1) == 1,
                shadowOpacity = module.floatValue("shadow_opacity", 0.72f).coerceIn(0f, 1f),
                shadowBlur = module.floatValue("shadow_blur", 3f).coerceIn(0f, 14f),
                shadowOffsetX = module.floatValue("shadow_offset_x", 1f).coerceIn(-5f, 5f),
                shadowOffsetY = module.floatValue("shadow_offset_y", 1.2f).coerceIn(-5f, 5f),
            ),
            background = ArrayListBackgroundState(
                colorMode = module.enumValue("background_color"),
                opacity = module.floatValue("background_opacity", 0.46f).coerceIn(0f, 1f),
                red = module.intValue("background_red", 18).coerceIn(0, 255),
                green = module.intValue("background_green", 24).coerceIn(0, 255),
                blue = module.intValue("background_blue", 32).coerceIn(0, 255),
                glow = module.boolValue("background_glow", true),
                glowStrength = module.floatValue("background_glow_strength", 0.34f).coerceIn(0f, 1f),
            ),
            colors = ArrayListColorState(
                mode = module.enumValue("color", 2),
                primaryMode = module.enumValue("primary_color"),
                primaryRed = module.intValue("primary_red", 48).coerceIn(0, 255),
                primaryGreen = module.intValue("primary_green", 164).coerceIn(0, 255),
                primaryBlue = module.intValue("primary_blue", 255).coerceIn(0, 255),
                secondaryMode = module.enumValue("secondary_color"),
                secondaryRed = module.intValue("secondary_red", 255).coerceIn(0, 255),
                secondaryGreen = module.intValue("secondary_green", 255).coerceIn(0, 255),
                secondaryBlue = module.intValue("secondary_blue", 255).coerceIn(0, 255),
                speed = module.floatValue("color_speed", 1f).coerceIn(0.1f, 4f),
                saturation = module.floatValue("color_saturation", 0.78f).coerceIn(0f, 1f),
                brightness = module.floatValue("color_brightness", 1f).coerceIn(0.25f, 1f),
                phaseSpacing = module.floatValue("phase_spacing", 0.28f).coerceIn(0f, 1f),
                reverse = module.boolValue("reverse_color"),
                breathStrength = module.floatValue("breath_strength", 0.52f).coerceIn(0f, 1f),
            ),
            line = ArrayListLineState(
                mode = module.enumValue("line_mode", 2),
                width = module.floatValue("line_width", 1.5f).coerceIn(0.5f, 8f),
                opacity = module.floatValue("line_opacity", 0.9f).coerceIn(0f, 1f),
                glow = module.boolValue("line_glow", true),
                glowStrength = module.floatValue("line_glow_strength", 0.45f).coerceIn(0f, 1f),
            ),
            animation = ArrayListAnimationState(
                style = module.enumValue("animation_style", 2),
                durationMillis = module.intValue("animation_duration", 240).coerceIn(80, 800),
            ),
        )
    }

    fun activeModuleNamesForHud(): List<String> = modules
        .asSequence()
        .filter { module ->
            module.id != ARRAY_LIST_ID &&
                module.enabled &&
                module.effectState == ModuleEffectState.Active
        }
        .map { module ->
            if (uiLanguage == UiLanguage.Chinese) module.name else module.englishName
        }
        .toList()

    private fun bindLocalModule(moduleId: String, backend: String) {
        val module = modules.firstOrNull { it.id == moduleId } ?: return
        module.enabled = false
        module.updateEffectStatus(
            backend = backend,
            state = ModuleEffectState.Ready,
        )
    }

    private fun setLocalVisualEnabled(module: ModuleUiModel, enabled: Boolean): Boolean {
        module.enabled = enabled
        module.updateEffectStatus(
            backend = module.effectBackend,
            state = if (enabled) ModuleEffectState.Active else ModuleEffectState.Ready,
        )
        return true
    }

    private fun refreshLocalModuleState(module: ModuleUiModel) {
        module.updateEffectStatus(
            backend = module.effectBackend,
            state = if (module.enabled) ModuleEffectState.Active else ModuleEffectState.Ready,
        )
    }

    private fun setDynamicIslandNoticeEnabled(
        module: ModuleUiModel,
        enabled: Boolean,
    ): Boolean {
        module.enabled = enabled
        dynamicIslandNoticeSetting.value = enabled
        module.updateEffectStatus(
            backend = COMPOSE_UI_BACKEND,
            state = if (enabled) ModuleEffectState.Active else ModuleEffectState.Ready,
        )
        return true
    }

    private fun synchronizeRestoredModuleStates() {
        modules.forEach { module ->
            when {
                module.id == DYNAMIC_ISLAND_NOTICE_ID -> {
                    dynamicIslandNoticeSetting.value = module.enabled
                    module.updateEffectStatus(
                        backend = COMPOSE_UI_BACKEND,
                        state = if (module.enabled) ModuleEffectState.Active else ModuleEffectState.Ready,
                    )
                }
                module.id in LOCAL_VISUAL_MODULE_IDS -> module.updateEffectStatus(
                    backend = COMPOSE_OVERLAY_BACKEND,
                    state = if (module.enabled) ModuleEffectState.Active else ModuleEffectState.Ready,
                )
                else -> module.updateEffectStatus(
                    backend = module.effectBackend,
                    state = if (module.enabled) ModuleEffectState.Active else ModuleEffectState.Ready,
                )
            }
        }
    }

    private fun persistIfEnabled() {
        if (autoSaveEnabled) saveConfigurationNow()
    }

    private fun saveConfigurationNow() {
        configStore?.save(
            language = uiLanguage,
            floatingButtonSizeDp = floatingButtonConfig.buttonSizeDp,
            backgroundMode = ClickGuiRenderer.backgroundMode,
            modules = modules,
        )
    }

    private fun isUiOwnedModule(module: ModuleUiModel): Boolean =
        module.id == DYNAMIC_ISLAND_NOTICE_ID || module.id in LOCAL_VISUAL_MODULE_IDS

    private companion object {
        const val ARRAY_LIST_ID = "array_list"
        const val DYNAMIC_ISLAND_NOTICE_ID = "dynamic_island_notice"
        const val DYNAMIC_ISLAND_SETTING_ID = "module_switch_notifications"
        const val COMPOSE_UI_BACKEND = "compose_ui"
        const val COMPOSE_OVERLAY_BACKEND = "compose_overlay"
        val LOCAL_VISUAL_MODULE_IDS = setOf(ARRAY_LIST_ID)
    }
}

private fun ModuleUiModel.boolValue(id: String, default: Boolean = false): Boolean =
    settings.filterIsInstance<BoolSetting>().firstOrNull { it.id == id }?.value ?: default

private fun ModuleUiModel.floatValue(id: String, default: Float): Float =
    settings.filterIsInstance<FloatSetting>().firstOrNull { it.id == id }?.value ?: default

private fun ModuleUiModel.intValue(id: String, default: Int): Int =
    settings.filterIsInstance<IntSetting>().firstOrNull { it.id == id }?.value ?: default

private fun ModuleUiModel.enumValue(id: String, default: Int = 0): Int =
    settings.filterIsInstance<EnumSetting>().firstOrNull { it.id == id }?.selectedIndex ?: default

data class ArrayListHudState(
    val enabled: Boolean,
    val layout: ArrayListLayoutState,
    val text: ArrayListTextState,
    val background: ArrayListBackgroundState,
    val colors: ArrayListColorState,
    val line: ArrayListLineState,
    val animation: ArrayListAnimationState,
) {
    companion object {
        val Disabled = ArrayListHudState(
            enabled = false,
            layout = ArrayListLayoutState(),
            text = ArrayListTextState(),
            background = ArrayListBackgroundState(),
            colors = ArrayListColorState(),
            line = ArrayListLineState(),
            animation = ArrayListAnimationState(),
        )
    }
}

data class ArrayListLayoutState(
    val displayMode: Int = 3,
    val position: Int = 0,
    val sortMode: Int = 0,
    val offsetX: Int = 10,
    val offsetY: Int = 70,
    val uniformWidth: Boolean = false,
    val showIndex: Boolean = false,
    val textDecoration: Int = 0,
    val paddingHorizontal: Float = 6f,
    val paddingVertical: Float = 2f,
    val itemSpacing: Float = 2f,
    val cornerRadius: Float = 7f,
)

data class ArrayListTextState(
    val fontSize: Float = 14f,
    val fontWeight: Int = 2,
    val letterSpacing: Float = 0f,
    val opacity: Float = 1f,
    val uppercase: Boolean = false,
    val shadowEnabled: Boolean = true,
    val blackShadow: Boolean = true,
    val shadowOpacity: Float = 0.72f,
    val shadowBlur: Float = 3f,
    val shadowOffsetX: Float = 1f,
    val shadowOffsetY: Float = 1.2f,
)

data class ArrayListBackgroundState(
    val colorMode: Int = 0,
    val opacity: Float = 0.46f,
    val red: Int = 18,
    val green: Int = 24,
    val blue: Int = 32,
    val glow: Boolean = true,
    val glowStrength: Float = 0.34f,
)

data class ArrayListColorState(
    val mode: Int = 2,
    val primaryMode: Int = 0,
    val primaryRed: Int = 48,
    val primaryGreen: Int = 164,
    val primaryBlue: Int = 255,
    val secondaryMode: Int = 0,
    val secondaryRed: Int = 255,
    val secondaryGreen: Int = 255,
    val secondaryBlue: Int = 255,
    val speed: Float = 1f,
    val saturation: Float = 0.78f,
    val brightness: Float = 1f,
    val phaseSpacing: Float = 0.28f,
    val reverse: Boolean = false,
    val breathStrength: Float = 0.52f,
)

data class ArrayListLineState(
    val mode: Int = 2,
    val width: Float = 1.5f,
    val opacity: Float = 0.9f,
    val glow: Boolean = true,
    val glowStrength: Float = 0.45f,
)

data class ArrayListAnimationState(
    val style: Int = 2,
    val durationMillis: Int = 240,
)
