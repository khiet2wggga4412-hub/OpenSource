package dev.liquid.clickgui.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ModuleCategory(val displayName: String) {
    Combat("Combat"),
    Movement("Movement"),
    Player("Player"),
    Visual("Visual"),
    Misc("Misc"),
}

enum class ModuleEffectState {
    CatalogOnly,
    Ready,
    Active,
    Unsupported,
    Error,
}

@Stable
class ModuleUiModel(
    val id: String,
    val name: String,
    val englishName: String = name,
    val description: String,
    val category: ModuleCategory,
    val iconRes: Int = 0,
    enabled: Boolean = false,
    val settings: List<SettingUiModel> = emptyList(),
    shortcutKeyCode: Int = 0,
    quickOverlayVisible: Boolean = false,
    effectAvailable: Boolean = false,
    effectBackend: String = "catalog_only",
) {
    var enabled by mutableStateOf(enabled)
    var shortcutKeyCode by mutableIntStateOf(shortcutKeyCode)
    var quickOverlayVisible by mutableStateOf(quickOverlayVisible)
    var effectAvailable by mutableStateOf(effectAvailable)
    var effectBackend by mutableStateOf(effectBackend)
    var effectState by mutableStateOf(
        if (effectAvailable) ModuleEffectState.Ready else ModuleEffectState.CatalogOnly,
    )
    var effectErrorMessage by mutableStateOf("")

    fun updateEffectStatus(
        backend: String,
        state: ModuleEffectState,
        errorMessage: String = "",
    ) {
        effectBackend = backend
        effectState = state
        effectAvailable = state != ModuleEffectState.CatalogOnly &&
            state != ModuleEffectState.Unsupported
        effectErrorMessage = errorMessage
    }
}

sealed interface SettingUiModel {
    val id: String
    val label: String
}

@Stable
class BoolSetting(
    override val id: String,
    override val label: String,
    value: Boolean,
) : SettingUiModel {
    var value by mutableStateOf(value)
}

@Stable
class FloatSetting(
    override val id: String,
    override val label: String,
    value: Float,
    val min: Float,
    val max: Float,
    val unit: String = "",
) : SettingUiModel {
    var value by mutableFloatStateOf(value)
}

@Stable
class IntSetting(
    override val id: String,
    override val label: String,
    value: Int,
    val min: Int,
    val max: Int,
    val unit: String = "",
) : SettingUiModel {
    var value by mutableIntStateOf(value)
}

@Stable
class EnumSetting(
    override val id: String,
    override val label: String,
    val options: List<String>,
    selectedIndex: Int,
) : SettingUiModel {
    var selectedIndex by mutableIntStateOf(
        if (options.isEmpty()) 0 else selectedIndex.coerceIn(options.indices),
    )
}

@Stable
class ActionSetting(
    override val id: String,
    override val label: String,
) : SettingUiModel
