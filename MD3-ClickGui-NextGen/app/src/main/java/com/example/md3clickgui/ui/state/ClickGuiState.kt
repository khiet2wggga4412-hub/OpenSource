package com.example.md3clickgui.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.md3clickgui.ui.model.ShortcutStyle
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.theme.NexusThemeSwatchHexes
import java.util.Base64
import kotlin.math.roundToInt
import com.example.md3clickgui.ui.model.ModuleKeybind
import com.example.md3clickgui.ui.model.keyLabel

data class GuiConfig(
    val id: String,
    val name: String,
    val snapshot: ConfigSnapshot
)

data class ConfigSnapshot(
    val darkMode: Boolean = false,
    val dynamicColor: Boolean = false,
    val themeIndex: Int = 0,
    val customThemeHex: String? = null,
    val languageIndex: Int = 0,
    val enabledModules: Map<String, Boolean> = emptyMap(),
    val toggleValues: Map<String, Boolean> = emptyMap(),
    val sliderValues: Map<String, Float> = emptyMap(),
    val choiceValues: Map<String, Int> = emptyMap(),
    val quickShortcutValues: Map<String, Boolean> = emptyMap(),
    val quickShortcutPositions: Map<String, ShortcutPosition> = emptyMap(),
    val openPanelButtonPosition: ShortcutPosition = ShortcutPosition(0.5f, 0.5f),
    val keybinds: Map<String, ModuleKeybind> = emptyMap()
)

/**
 * Single owner of card UI state: selection, panel visibility, per-card enable switches, per-setting values,
 * and the app-level theme bindings. Cards stay stateless, so adding or removing a module
 * requires no state wiring anywhere.
 */
class ClickGuiState(
    initialDarkMode: Boolean = false,
    initialDynamicColor: Boolean = false,
    initialThemeIndex: Int = 0
) {
    var darkMode by mutableStateOf(initialDarkMode)
        private set
    var dynamicColor by mutableStateOf(initialDynamicColor)
        private set
    var languageIndex by mutableIntStateOf(0)
        private set

    var selectedModuleId by mutableStateOf<String?>(null)
        private set
    var isDetailsPanelOpen by mutableStateOf(false)
        private set
    var isWindowOpen by mutableStateOf(true)
        private set
    var isAuthenticated by mutableStateOf(false)
        private set
    var isLoginLoading by mutableStateOf(false)
        private set
    var rememberLogin by mutableStateOf(false)
        private set
    var accountName by mutableStateOf<String?>(null)
        private set
    var accountExpiryText by mutableStateOf<String?>(null)
        private set
    var themeIndex by mutableIntStateOf(initialThemeIndex.coerceIn(0, ThemeSwatchCount - 1))
        private set
    var customThemeHex by mutableStateOf<String?>(null)
        private set
    private val enabledModules = mutableStateMapOf<String, Boolean>()
    private val toggleValues = mutableStateMapOf<String, Boolean>()
    private val sliderValues = mutableStateMapOf<String, Float>()
    private val choiceValues = mutableStateMapOf<String, Int>()
    private val quickShortcutValues = mutableStateMapOf<String, Boolean>()
    private val quickShortcutPositions = mutableStateMapOf<String, ShortcutPosition>()
    private val keybinds = mutableStateMapOf<String, ModuleKeybind>()
    private val configs = mutableStateListOf<GuiConfig>()
    private var nextConfigId by mutableIntStateOf(0)
    private var openPanelButtonPosition by mutableStateOf(ShortcutPosition(0.5f, 0.5f))

    val configurations: List<GuiConfig>
        get() = configs

    fun selectModule(moduleId: String) {
        selectedModuleId = moduleId
        isDetailsPanelOpen = true
    }

    fun closeDetailsPanel() {
        isDetailsPanelOpen = false
    }

    fun openDetailsPanel() {
        if (selectedModuleId != null) isDetailsPanelOpen = true
    }

    fun closeWindow() {
        isWindowOpen = false
    }

    fun openWindow() {
        isWindowOpen = true
    }

    /** Local demo authentication: both fields are required before the loading phase starts. */
    fun submitCredentials(username: String, password: String, remember: Boolean): Boolean {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isEmpty() || password.isBlank()) return false
        isAuthenticated = true
        isLoginLoading = true
        rememberLogin = remember
        accountName = normalizedUsername
        accountExpiryText = DemoAccountExpiryText
        return true
    }

    fun finishLoginLoading() {
        isLoginLoading = false
    }

    fun canInteractWithModules(): Boolean = isAuthenticated && !isLoginLoading

    /** Switch state for any card: bound modules read their app-level flag, standard modules their own entry. */
    fun isChecked(module: GuiModule): Boolean = when (module.binding) {
        ModuleBinding.Standard -> enabledModules[module.id] == true
        ModuleBinding.DarkMode -> darkMode
        ModuleBinding.DynamicColor -> dynamicColor
        ModuleBinding.Theme -> true
        ModuleBinding.Language -> true
        ModuleBinding.ShortcutButton -> true
        ModuleBinding.Content -> true
    }

    fun toggle(module: GuiModule) {
        when (module.binding) {
            ModuleBinding.Standard -> enabledModules[module.id] = !(enabledModules[module.id] == true)
            ModuleBinding.DarkMode -> darkMode = !darkMode
            ModuleBinding.DynamicColor -> applyDynamicColor(!dynamicColor)
            ModuleBinding.Theme -> applyThemeIndex((themeIndex + 1) % ThemeSwatchCount)
            ModuleBinding.Language -> Unit
            ModuleBinding.ShortcutButton -> Unit
            ModuleBinding.Content -> Unit
        }
    }

    fun toggleValue(module: GuiModule, setting: ModuleSetting.Toggle): Boolean = when {
        module.binding == ModuleBinding.Theme && setting.label == "Dark theme" -> darkMode
        module.binding == ModuleBinding.Theme && setting.label == "Dynamic color" -> dynamicColor
        else -> toggleValues[settingKey(module.id, setting.label)] ?: setting.defaultValue
    }

    fun setToggleValue(module: GuiModule, setting: ModuleSetting.Toggle, value: Boolean) {
        when {
            module.binding == ModuleBinding.Theme && setting.label == "Dark theme" -> darkMode = value
            module.binding == ModuleBinding.Theme && setting.label == "Dynamic color" -> applyDynamicColor(value)
            else -> toggleValues[settingKey(module.id, setting.label)] = value
        }
    }

    /** A shortcut is only meaningful for a module that can actually be on or off. */
    fun isQuickShortcutEnabled(module: GuiModule): Boolean =
        module.binding != ModuleBinding.Theme &&
            module.binding != ModuleBinding.Language &&
            module.binding != ModuleBinding.ShortcutButton &&
            module.binding != ModuleBinding.Content &&
            quickShortcutValues[module.id] == true

    fun setQuickShortcutEnabled(module: GuiModule, enabled: Boolean) {
        quickShortcutValues[module.id] = enabled
    }

    /**
     * Peripheral button bound to a module, or null.
     *
     * Every module may carry one, including the ones without an enable switch: pressing the button
     * runs [triggerKeybind], which toggles a switchable module and opens the panel for a content
     * module.
     */
    fun keybind(module: GuiModule): ModuleKeybind? = keybinds[module.id]

    fun bindKey(module: GuiModule, keybind: ModuleKeybind) {
        // One button drives one module: take it away from whoever held it before.
        keybinds.entries.removeAll { it.value.keyCode == keybind.keyCode && it.key != module.id }
        keybinds[module.id] = keybind
    }

    fun clearKeybind(module: GuiModule) {
        keybinds.remove(module.id)
    }

    /**
     * Runs the action bound to a module.
     *
     * Content modules (the music player and its browse panels) have no off state, so the button opens
     * them instead of toggling.
     */
    fun triggerKeybind(module: GuiModule) {
        if (module.binding == ModuleBinding.Content) {
            selectModule(module.id)
            openDetailsPanel()
        } else {
            toggle(module)
        }
    }

    /** Global shortcut style; index matches ShortcutModule's "Style" choice order. */
    fun shortcutStyle(): ShortcutStyle = when (
        choiceValues[settingKey(ShortcutModuleId, ShortcutStyleLabel)] ?: 0
    ) {
        1 -> ShortcutStyle.Text
        2 -> ShortcutStyle.IconText
        else -> ShortcutStyle.Icon
    }

    fun shortcutSize(): Float =
        sliderValues[settingKey(ShortcutModuleId, ShortcutSizeLabel)] ?: DefaultShortcutSize

    fun shortcutCornerRadius(): Float =
        sliderValues[settingKey(ShortcutModuleId, ShortcutCornerRadiusLabel)] ?: DefaultShortcutCornerRadius

    fun quickShortcutPosition(moduleId: String): ShortcutPosition? =
        quickShortcutPositions[moduleId]

    fun moveQuickShortcut(
        moduleId: String,
        deltaX: Float,
        deltaY: Float,
        travelWidth: Float,
        travelHeight: Float,
        defaultPosition: ShortcutPosition
    ) {
        val current = quickShortcutPositions[moduleId] ?: defaultPosition
        quickShortcutPositions[moduleId] = ShortcutPosition(
            xFraction = if (travelWidth > 0f) {
                (current.xFraction + deltaX / travelWidth).coerceIn(0f, 1f)
            } else {
                current.xFraction
            },
            yFraction = if (travelHeight > 0f) {
                (current.yFraction + deltaY / travelHeight).coerceIn(0f, 1f)
            } else {
                current.yFraction
            }
        )
    }

    fun openPanelButtonPosition(): ShortcutPosition = openPanelButtonPosition

    fun moveOpenPanelButton(
        deltaX: Float,
        deltaY: Float,
        travelWidth: Float,
        travelHeight: Float
    ) {
        openPanelButtonPosition = ShortcutPosition(
            xFraction = if (travelWidth > 0f) {
                (openPanelButtonPosition.xFraction + deltaX / travelWidth).coerceIn(0f, 1f)
            } else {
                openPanelButtonPosition.xFraction
            },
            yFraction = if (travelHeight > 0f) {
                (openPanelButtonPosition.yFraction + deltaY / travelHeight).coerceIn(0f, 1f)
            } else {
                openPanelButtonPosition.yFraction
            }
        )
    }

    fun sliderValue(module: GuiModule, setting: ModuleSetting.Slider): Float =
        normalizedSliderValue(setting, sliderValues[settingKey(module.id, setting.label)] ?: setting.defaultValue)

    fun setSliderValue(module: GuiModule, setting: ModuleSetting.Slider, value: Float) {
        sliderValues[settingKey(module.id, setting.label)] = normalizedSliderValue(setting, value)
    }

    private fun normalizedSliderValue(setting: ModuleSetting.Slider, value: Float): Float {
        val start = setting.valueRange.start
        val end = setting.valueRange.endInclusive
        val bounded = (if (value.isFinite()) value else setting.defaultValue).coerceIn(start, end)
        if (setting.steps <= 0 || end <= start) return bounded
        val intervals = setting.steps + 1
        return start + ((bounded - start) / (end - start) * intervals).roundToInt() * (end - start) / intervals
    }

    fun choiceIndex(module: GuiModule, setting: ModuleSetting.Choice): Int = when {
        module.binding == ModuleBinding.Theme -> if (customThemeHex == null) themeIndex else -1
        module.binding == ModuleBinding.Language -> languageIndex
        else -> choiceValues[settingKey(module.id, setting.label)] ?: setting.defaultIndex
    }

    fun setChoiceIndex(module: GuiModule, setting: ModuleSetting.Choice, index: Int) {
        when {
            module.binding == ModuleBinding.Theme -> applyThemeIndex(index)
            module.binding == ModuleBinding.Language -> languageIndex = index.coerceIn(0, LanguageCount - 1)
            else -> choiceValues[settingKey(module.id, setting.label)] = index
        }
    }

    fun colorPickerValue(module: GuiModule, setting: ModuleSetting.ColorPicker): String =
        if (module.binding == ModuleBinding.Theme) {
            customThemeHex ?: NexusThemeSwatchHexes[themeIndex.coerceIn(NexusThemeSwatchHexes.indices)]
        } else {
            setting.defaultColorHex
        }

    fun setColorPickerValue(module: GuiModule, setting: ModuleSetting.ColorPicker, hex: String) {
        if (module.binding == ModuleBinding.Theme) {
            customThemeHex = hex
            dynamicColor = false
        }
    }

    fun createBlankConfig(name: String): Boolean {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return false
        configs += GuiConfig(
            id = "config-${nextConfigId++}",
            name = normalizedName,
            snapshot = ConfigSnapshot()
        )
        return true
    }

    fun saveCurrentToConfig(configId: String) {
        val index = configs.indexOfFirst { it.id == configId }
        if (index >= 0) configs[index] = configs[index].copy(snapshot = captureSnapshot())
    }

    fun loadConfig(configId: String) {
        configs.firstOrNull { it.id == configId }?.snapshot?.let(::applySnapshot)
    }

    fun deleteConfig(configId: String) {
        configs.removeAll { it.id == configId }
    }

    private fun captureSnapshot(): ConfigSnapshot = ConfigSnapshot(
        darkMode = darkMode,
        dynamicColor = dynamicColor,
        themeIndex = themeIndex,
        customThemeHex = customThemeHex,
        languageIndex = languageIndex,
        enabledModules = enabledModules.toMap(),
        toggleValues = toggleValues.toMap(),
        sliderValues = sliderValues.toMap(),
        choiceValues = choiceValues.toMap(),
        quickShortcutValues = quickShortcutValues.toMap(),
        quickShortcutPositions = quickShortcutPositions.toMap(),
        openPanelButtonPosition = openPanelButtonPosition,
        keybinds = keybinds.toMap()
    )

    private fun applySnapshot(snapshot: ConfigSnapshot) {
        darkMode = snapshot.darkMode
        dynamicColor = snapshot.dynamicColor
        themeIndex = snapshot.themeIndex.coerceIn(0, ThemeSwatchCount - 1)
        customThemeHex = snapshot.customThemeHex
        languageIndex = snapshot.languageIndex.coerceIn(0, LanguageCount - 1)
        enabledModules.clear()
        enabledModules.putAll(snapshot.enabledModules)
        toggleValues.clear()
        toggleValues.putAll(snapshot.toggleValues)
        sliderValues.clear()
        sliderValues.putAll(snapshot.sliderValues)
        choiceValues.clear()
        choiceValues.putAll(snapshot.choiceValues)
        quickShortcutValues.clear()
        quickShortcutValues.putAll(snapshot.quickShortcutValues)
        quickShortcutPositions.clear()
        quickShortcutPositions.putAll(snapshot.quickShortcutPositions)
        openPanelButtonPosition = snapshot.openPanelButtonPosition
        keybinds.clear()
        keybinds.putAll(snapshot.keybinds)
    }

    /** Manual theme choices and the system palette are mutually exclusive. */
    private fun applyThemeIndex(index: Int) {
        themeIndex = index.coerceIn(0, ThemeSwatchCount - 1)
        dynamicColor = false
        customThemeHex = null
    }

    /** Enabling dynamic color must be observable even after a custom accent was selected. */
    private fun applyDynamicColor(enabled: Boolean) {
        dynamicColor = enabled
        if (enabled) {
            themeIndex = 0
            customThemeHex = null
        }
    }

    companion object {
        private const val ThemeSwatchCount = 4
        private const val LanguageCount = 2
        private const val DemoAccountExpiryText = "Expires 2026-12-31"
        private const val ShortcutModuleId = "misc.shortcut"
        private const val ShortcutStyleLabel = "Style"
        private const val ShortcutSizeLabel = "Size"
        private const val ShortcutCornerRadiusLabel = "Corner radius"
        private const val DefaultShortcutSize = 32f
        private const val DefaultShortcutCornerRadius = 10f
        private fun settingKey(moduleId: String, label: String) = "$moduleId/$label"

        /** Encodes every map as a "key=value" line list of strings so the whole state is Bundle-safe. */
        val Saver: Saver<ClickGuiState, *> = listSaver(
            save = { state ->
                listOf(
                    if (state.darkMode) "1" else "0",
                    if (state.dynamicColor) "1" else "0",
                    encode(state.enabledModules) { if (it) "1" else "0" },
                    encode(state.toggleValues) { if (it) "1" else "0" },
                    encode(state.sliderValues) { it.toString() },
                    encode(state.choiceValues) { it.toString() },
                    encode(state.quickShortcutValues) { if (it) "1" else "0" },
                    encode(state.quickShortcutPositions) { "${it.xFraction},${it.yFraction}" },
                    state.selectedModuleId.orEmpty(),
                    if (state.isDetailsPanelOpen) "1" else "0",
                    if (state.isWindowOpen) "1" else "0",
                    state.themeIndex.toString(),
                    state.customThemeHex.orEmpty(),
                    if (state.rememberLogin) "1" else "0",
                    "", // Legacy license-key slot kept for state compatibility.
                    if (state.rememberLogin) state.accountName.orEmpty() else "",
                    if (state.rememberLogin) state.accountExpiryText.orEmpty() else "",
                    "${state.openPanelButtonPosition.xFraction},${state.openPanelButtonPosition.yFraction}",
                    state.languageIndex.toString(),
                    encodeConfigs(state.configurations),
                    state.nextConfigId.toString(),
                    // Appended last: the indices above are positional and older saves must keep working.
                    encodeKeybinds(state.keybinds)
                )
            },
            restore = { values ->
                ClickGuiState(
                    initialDarkMode = values[0] == "1",
                    initialDynamicColor = values[1] == "1"
                ).apply {
                    decodeInto(values[2], enabledModules) { it == "1" }
                    if (values.size >= 9) {
                        decodeInto(values[3], toggleValues) { it == "1" }
                        decodeInto(values[4], sliderValues) { it.toFloat() }
                        decodeInto(values[5], choiceValues) { it.toInt() }
                        if (values.size >= 17) {
                            decodeInto(values[6], quickShortcutValues) { it == "1" }
                            decodePositions(values[7], quickShortcutPositions)
                        }
                        val baseIndex = if (values.size >= 17) 8 else 6
                        selectedModuleId = values[baseIndex].takeIf { it.isNotEmpty() }
                        isDetailsPanelOpen = values.getOrNull(baseIndex + 1) == "1"
                        isWindowOpen = values.getOrNull(baseIndex + 2) != "0"
                        themeIndex = values.getOrNull(baseIndex + 3)?.toIntOrNull()?.coerceIn(0, ThemeSwatchCount - 1) ?: 0
                        customThemeHex = values.getOrNull(baseIndex + 4)?.takeIf { it.isNotEmpty() }
                        rememberLogin = values.getOrNull(baseIndex + 5) == "1"
                        isAuthenticated = rememberLogin
                        accountName = values.getOrNull(baseIndex + 7)?.takeIf { it.isNotEmpty() }
                            ?: if (rememberLogin) "Material user" else null
                        accountExpiryText = values.getOrNull(baseIndex + 8)?.takeIf { it.isNotEmpty() }
                            ?: if (rememberLogin) DemoAccountExpiryText else null
                        decodePosition(values.getOrNull(17))?.let { openPanelButtonPosition = it }
                        languageIndex = values.getOrNull(18)?.toIntOrNull()?.coerceIn(0, LanguageCount - 1) ?: 0
                        configs.addAll(decodeConfigs(values.getOrNull(19).orEmpty()))
                        nextConfigId = values.getOrNull(20)?.toIntOrNull() ?: configs.size
                        decodeKeybinds(values.getOrNull(21).orEmpty(), keybinds)
                    } else {
                        // The previous saver stored expanded card ids at index 3.
                        decodeInto(values[4], toggleValues) { it == "1" }
                        decodeInto(values[5], sliderValues) { it.toFloat() }
                        decodeInto(values[6], choiceValues) { it.toInt() }
                    }
                }
            }
        )

        private fun <T> encode(map: Map<String, T>, valueOf: (T) -> String): String =
            map.entries.joinToString("\n") { "${it.key}=${valueOf(it.value)}" }

        private fun <T> decodeInto(encoded: String, into: MutableMap<String, T>, parse: (String) -> T) {
            if (encoded.isEmpty()) return
            encoded.split("\n").forEach { entry ->
                val separator = entry.lastIndexOf('=')
                if (separator > 0) into[entry.substring(0, separator)] = parse(entry.substring(separator + 1))
            }
        }

        private fun decodePositions(encoded: String, into: MutableMap<String, ShortcutPosition>) {
            if (encoded.isEmpty()) return
            encoded.split("\n").forEach { entry ->
                val separator = entry.lastIndexOf('=')
                if (separator <= 0) return@forEach
                val values = entry.substring(separator + 1).split(',')
                val x = values.getOrNull(0)?.toFloatOrNull()
                val y = values.getOrNull(1)?.toFloatOrNull()
                if (x != null && y != null) {
                    into[entry.substring(0, separator)] = ShortcutPosition(
                        xFraction = x.coerceIn(0f, 1f),
                        yFraction = y.coerceIn(0f, 1f)
                    )
                }
            }
        }

        private fun decodePosition(encoded: String?): ShortcutPosition? {
            val values = encoded?.split(',') ?: return null
            val x = values.getOrNull(0)?.toFloatOrNull() ?: return null
            val y = values.getOrNull(1)?.toFloatOrNull() ?: return null
            return ShortcutPosition(
                xFraction = x.coerceIn(0f, 1f),
                yFraction = y.coerceIn(0f, 1f)
            )
        }

        private fun encodeText(value: String): String =
            Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

        private fun decodeText(value: String): String =
            runCatching { String(Base64.getDecoder().decode(value), Charsets.UTF_8) }.getOrDefault("")

        /**
         * `moduleId=keyCode,isGamepad,base64(label)` per line.
         *
         * The label is Base64-encoded so a name containing `,` or `=` cannot split the record.
         */
        private fun encodeKeybinds(map: Map<String, ModuleKeybind>): String =
            map.entries.joinToString("\n") { (moduleId, bind) ->
                "$moduleId=${bind.keyCode},${if (bind.isGamepad) 1 else 0},${encodeText(bind.label)}"
            }

        private fun decodeKeybinds(encoded: String, into: MutableMap<String, ModuleKeybind>) {
            if (encoded.isEmpty()) return
            encoded.split("\n").forEach { entry ->
                val separator = entry.lastIndexOf('=')
                if (separator <= 0) return@forEach
                val parts = entry.substring(separator + 1).split(',')
                val keyCode = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                val isGamepad = parts.getOrNull(1) == "1"
                val label = decodeText(parts.getOrNull(2).orEmpty()).ifEmpty { keyLabel(keyCode) }
                into[entry.substring(0, separator)] = ModuleKeybind(keyCode, isGamepad, label)
            }
        }

        private fun encodeConfigs(configs: List<GuiConfig>): String = configs.joinToString("\n") { config ->
            val snapshot = config.snapshot
            listOf(
                config.id,
                config.name,
                if (snapshot.darkMode) "1" else "0",
                if (snapshot.dynamicColor) "1" else "0",
                snapshot.themeIndex.toString(),
                snapshot.customThemeHex.orEmpty(),
                snapshot.languageIndex.toString(),
                encode(snapshot.enabledModules) { if (it) "1" else "0" },
                encode(snapshot.toggleValues) { if (it) "1" else "0" },
                encode(snapshot.sliderValues) { it.toString() },
                encode(snapshot.choiceValues) { it.toString() },
                encode(snapshot.quickShortcutValues) { if (it) "1" else "0" },
                encode(snapshot.quickShortcutPositions) { "${it.xFraction},${it.yFraction}" },
                "${snapshot.openPanelButtonPosition.xFraction},${snapshot.openPanelButtonPosition.yFraction}",
                encodeKeybinds(snapshot.keybinds)
            ).joinToString("|") { encodeText(it) }
        }

        private fun decodeConfigs(encoded: String): List<GuiConfig> {
            if (encoded.isEmpty()) return emptyList()
            return encoded.split("\n").mapNotNull { line ->
                val fields = line.split("|").map(::decodeText)
                if (fields.size < 14) return@mapNotNull null
                val enabled = mutableMapOf<String, Boolean>()
                val toggles = mutableMapOf<String, Boolean>()
                val sliders = mutableMapOf<String, Float>()
                val choices = mutableMapOf<String, Int>()
                val quickValues = mutableMapOf<String, Boolean>()
                val quickPositions = mutableMapOf<String, ShortcutPosition>()
                val configKeybinds = mutableMapOf<String, ModuleKeybind>()
                decodeInto(fields[7], enabled) { it == "1" }
                decodeInto(fields[8], toggles) { it == "1" }
                decodeInto(fields[9], sliders) { it.toFloat() }
                decodeInto(fields[10], choices) { it.toInt() }
                decodeInto(fields[11], quickValues) { it == "1" }
                decodePositions(fields[12], quickPositions)
                decodeKeybinds(fields.getOrNull(14).orEmpty(), configKeybinds)
                GuiConfig(
                    id = fields[0],
                    name = fields[1],
                    snapshot = ConfigSnapshot(
                        darkMode = fields[2] == "1",
                        dynamicColor = fields[3] == "1",
                        themeIndex = fields[4].toIntOrNull() ?: 0,
                        customThemeHex = fields[5].takeIf { it.isNotEmpty() },
                        languageIndex = fields[6].toIntOrNull() ?: 0,
                        enabledModules = enabled,
                        toggleValues = toggles,
                        sliderValues = sliders,
                        choiceValues = choices,
                        quickShortcutValues = quickValues,
                        quickShortcutPositions = quickPositions,
                        openPanelButtonPosition = decodePosition(fields[13])
                            ?: ShortcutPosition(0.5f, 0.5f),
                        keybinds = configKeybinds
                    )
                )
            }
        }
    }
}

data class ShortcutPosition(
    val xFraction: Float,
    val yFraction: Float
)

@Composable
fun rememberClickGuiState(): ClickGuiState =
    rememberSaveable(saver = ClickGuiState.Saver) { ClickGuiState() }
