package dev.liquid.clickgui.data

import dev.liquid.clickgui.model.ActionSetting
import dev.liquid.clickgui.model.BoolSetting
import dev.liquid.clickgui.model.EnumSetting
import dev.liquid.clickgui.model.FloatSetting
import dev.liquid.clickgui.model.IntSetting
import dev.liquid.clickgui.model.ModuleEffectState
import dev.liquid.clickgui.model.ModuleUiModel

class UiModuleGateway {
    val status: String
        get() = "Compose UI only; native backend and SDK removed"

    fun loadCatalog(): List<ModuleUiModel> = iconPackCatalog()
        .filterNot { it.name in RemovedPackFeatures }

    fun setModuleEnabled(module: ModuleUiModel, enabled: Boolean): Boolean {
        module.enabled = enabled
        module.updateEffectStatus(
            backend = UI_ONLY_BACKEND,
            state = if (enabled) ModuleEffectState.Active else ModuleEffectState.Ready,
        )
        return true
    }

    fun setBool(module: ModuleUiModel, setting: BoolSetting, value: Boolean): Boolean {
        setting.value = value
        return true
    }

    fun setFloat(module: ModuleUiModel, setting: FloatSetting, value: Float): Boolean {
        setting.value = value.coerceIn(setting.min, setting.max)
        return true
    }

    fun setInt(module: ModuleUiModel, setting: IntSetting, value: Int): Boolean {
        setting.value = value.coerceIn(setting.min, setting.max)
        return true
    }

    fun setEnum(module: ModuleUiModel, setting: EnumSetting, index: Int): Boolean {
        if (setting.options.isEmpty()) return false
        setting.selectedIndex = index.coerceIn(setting.options.indices)
        return true
    }

    fun invokeAction(module: ModuleUiModel, setting: ActionSetting): Boolean = true

    fun setShortcut(module: ModuleUiModel, keyCode: Int): Boolean {
        module.shortcutKeyCode = keyCode.coerceAtLeast(0)
        return true
    }

    private companion object {
        const val UI_ONLY_BACKEND = "compose_ui_only"
    }
}

private val RemovedPackFeatures = setOf(
    "广角png",
    "百米大刀",
    "超级击退",
    "长臂猿",
    "自动疾跑",
    "船体分离",
    "锁背",
    "锁定传送",
    "模糊效果",
    "按键绑定",
    "方块距离",
    "防踢",
    "关闭攻击冷却",
    "管理员权限",
    "激流杀戮",
    "人物旋转",
    "伙伴蛋获取",
    "音乐演奏",
    "自杀光环",
)
