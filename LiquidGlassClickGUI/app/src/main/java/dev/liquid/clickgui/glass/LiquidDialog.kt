package dev.liquid.clickgui.glass

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import dev.liquid.clickgui.model.ActionSetting
import dev.liquid.clickgui.model.BoolSetting
import dev.liquid.clickgui.model.EnumSetting
import dev.liquid.clickgui.model.FloatSetting
import dev.liquid.clickgui.model.IntSetting
import dev.liquid.clickgui.model.ModuleUiModel
import dev.liquid.clickgui.model.SettingUiModel
import dev.liquid.clickgui.ui.ClickGuiController
import dev.liquid.clickgui.ui.ClickGuiBackgroundMode
import dev.liquid.clickgui.ui.GlassTypography
import dev.liquid.clickgui.ui.LocalGlassPalette
import dev.liquid.clickgui.ui.UiLanguage
import kotlin.math.roundToInt

/** 与 Kyant0 catalog DialogContent 相同的 blur + lens + colorControls 组合。 */
@Composable
fun LiquidModuleDialog(
    visible: Boolean,
    module: ModuleUiModel?,
    controller: ClickGuiController,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    backgroundMode: ClickGuiBackgroundMode,
) {
    val palette = LocalGlassPalette.current
    val language = controller.uiLanguage
    var renderedModule by remember { mutableStateOf(module) }
    LaunchedEffect(module) {
        if (module != null) renderedModule = module
    }
    AnimatedVisibility(
        visible = visible && module != null,
        enter = fadeIn(tween(180)) +
            scaleIn(tween(260), initialScale = 0.90f) +
            slideInVertically(tween(260)) { it / 10 },
        exit = fadeOut(tween(210)) +
            scaleOut(tween(280), targetScale = 0.88f) +
            slideOutVertically(tween(280)) { it / 8 },
    ) {
        val shownModule = module ?: renderedModule ?: return@AnimatedVisibility
        val dialogLayer = rememberLayerBackdrop()
        val controlBackdrop = rememberCombinedBackdrop(backdrop, dialogLayer)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .height((maxHeight * 0.76f).coerceAtMost(620.dp))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(dialogLayer)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(30.dp) },
                            effects = {
                                colorControls(brightness = 0.08f, saturation = 1.15f)
                                blur(10.dp.toPx())
                                lens(22.dp.toPx(), 42.dp.toPx(), depthEffect = true)
                            },
                            highlight = { Highlight.Plain },
                            onDrawSurface = {
                                drawRect(
                                    palette.glassSurfaceStrong.copy(
                                        alpha = if (backgroundMode == ClickGuiBackgroundMode.GaussianBlur) {
                                            0.68f
                                        } else 0.88f,
                                    ),
                                )
                            },
                        ),
                )
                Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 22.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            if (language == UiLanguage.Chinese) {
                                shownModule.name
                            } else {
                                shownModule.englishName
                            },
                            style = GlassTypography.Title,
                        )
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            shownModule.description,
                            style = GlassTypography.Body.copy(color = palette.secondaryInk),
                            maxLines = 2,
                        )
                    }
                    LiquidToggle(
                        selected = { shownModule.enabled },
                        onSelect = { controller.setEnabled(shownModule, it) },
                        backdrop = controlBackdrop,
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "floating_quick_button") {
                        FloatingQuickButtonControl(shownModule, controller, controlBackdrop, language)
                    }
                    item(key = "shortcut") {
                        ShortcutControl(shownModule, controller, controlBackdrop, language)
                    }
                    if (shownModule.settings.isEmpty()) {
                        item(key = "empty") {
                            BasicText(
                                "No configurable settings",
                                style = GlassTypography.Body.copy(color = palette.secondaryInk),
                                modifier = Modifier.padding(vertical = 28.dp),
                            )
                        }
                    } else {
                        items(shownModule.settings, key = { it.id }) { setting ->
                            SettingControl(shownModule, setting, controller, controlBackdrop, language)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    LiquidButton(
                        onClick = onDismiss,
                        backdrop = controlBackdrop,
                        tint = palette.accent,
                        surfaceColor = palette.glassSurface,
                    ) {
                        BasicText(
                            if (language == UiLanguage.Chinese) "完成" else "Done",
                            style = GlassTypography.Body.copy(color = palette.ink),
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun FloatingQuickButtonControl(
    module: ModuleUiModel,
    controller: ClickGuiController,
    backdrop: Backdrop,
    language: UiLanguage,
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                if (language == UiLanguage.Chinese) "悬浮快捷按钮" else "Floating shortcut",
                style = GlassTypography.Body,
            )
            BasicText(
                if (language == UiLanguage.Chinese) {
                    if (module.quickOverlayVisible) "收起面板后显示" else "未创建"
                } else if (module.quickOverlayVisible) "Visible after collapsing" else "Not created",
                style = GlassTypography.Label.copy(color = palette.secondaryInk),
            )
        }
        LiquidToggle(
            selected = { module.quickOverlayVisible },
            onSelect = { controller.setQuickOverlayVisible(module, it) },
            backdrop = backdrop,
        )
    }
}

@Composable
private fun ShortcutControl(
    module: ModuleUiModel,
    controller: ClickGuiController,
    backdrop: Backdrop,
    language: UiLanguage,
) {
    val palette = LocalGlassPalette.current
    val capturing = controller.pendingShortcutModule === module
    val keyLabel = when {
        capturing -> if (language == UiLanguage.Chinese) "请按任意键..." else "Press any key..."
        module.shortcutKeyCode == KeyEvent.KEYCODE_UNKNOWN ->
            if (language == UiLanguage.Chinese) "未绑定" else "Unbound"
        else -> KeyEvent.keyCodeToString(module.shortcutKeyCode).removePrefix("KEYCODE_")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                if (language == UiLanguage.Chinese) "功能快捷键" else "Shortcut key",
                style = GlassTypography.Body,
            )
            BasicText(
                keyLabel,
                style = GlassTypography.Label.copy(
                    color = if (capturing) palette.ink else palette.secondaryInk,
                ),
            )
        }
        if (module.shortcutKeyCode != KeyEvent.KEYCODE_UNKNOWN && !capturing) {
            LiquidButton(
                onClick = { controller.clearShortcut(module) },
                backdrop = backdrop,
                surfaceColor = palette.glassSurface,
            ) {
                BasicText(if (language == UiLanguage.Chinese) "清除" else "Clear", style = GlassTypography.Label)
            }
        }
        LiquidButton(
            onClick = { controller.beginShortcutCapture(module) },
            backdrop = backdrop,
            tint = Color.White,
            surfaceColor = palette.glassSurface,
        ) {
            BasicText(
                if (language == UiLanguage.Chinese) {
                    if (capturing) "等待" else "绑定"
                } else if (capturing) "Waiting" else "Bind",
                style = GlassTypography.Label,
            )
        }
    }
}

@Composable
private fun SettingControl(
    module: ModuleUiModel,
    setting: SettingUiModel,
    controller: ClickGuiController,
    backdrop: Backdrop,
    language: UiLanguage,
) {
    val palette = LocalGlassPalette.current
    when (setting) {
        is BoolSetting -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                localizedSettingText(setting.label, language),
                style = GlassTypography.Body,
                modifier = Modifier.weight(1f),
            )
            LiquidToggle(
                selected = { setting.value },
                onSelect = { controller.setBool(module, setting, it) },
                backdrop = backdrop,
            )
        }

        is FloatSetting -> Column(Modifier.fillMaxWidth()) {
            SettingHeader(localizedSettingText(setting.label, language), "%.2f%s".format(setting.value, setting.unit))
            LiquidSlider(
                value = { setting.value },
                onValueChange = { controller.setFloat(module, setting, it) },
                valueRange = setting.min..setting.max,
                visibilityThreshold = (setting.max - setting.min) / 1000f,
                backdrop = backdrop,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }

        is IntSetting -> Column(Modifier.fillMaxWidth()) {
            SettingHeader(localizedSettingText(setting.label, language), "${setting.value}${setting.unit}")
            LiquidSlider(
                value = { setting.value.toFloat() },
                onValueChange = { controller.setInt(module, setting, it.roundToInt()) },
                valueRange = setting.min.toFloat()..setting.max.toFloat(),
                visibilityThreshold = 0.5f,
                backdrop = backdrop,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }

        is EnumSetting -> Column(Modifier.fillMaxWidth()) {
            SettingHeader(
                localizedSettingText(setting.label, language),
                localizedSettingText(setting.options[setting.selectedIndex], language),
            )
            LiquidSegmentedNavigation(
                options = setting.options.map { localizedSettingText(it, language) },
                selectedIndex = setting.selectedIndex,
                onSelected = { controller.setEnum(module, setting, it) },
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        is ActionSetting -> LiquidButton(
            onClick = { controller.invokeAction(module, setting) },
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            surfaceColor = palette.glassSurface,
        ) {
            BasicText(
                localizedSettingText(setting.label, language),
                style = GlassTypography.Body.copy(color = palette.accent),
            )
        }
    }
}

@Composable
private fun SettingHeader(label: String, value: String) {
    val palette = LocalGlassPalette.current
    Row(modifier = Modifier.fillMaxWidth()) {
        BasicText(label, style = GlassTypography.Body, modifier = Modifier.weight(1f))
        BasicText(value, style = GlassTypography.Label.copy(color = palette.accent))
    }
}

private fun localizedSettingText(text: String, language: UiLanguage): String {
    if (language == UiLanguage.Chinese) return text
    return SettingEnglish[text] ?: text
}

private val SettingEnglish = mapOf(
    "显示模式" to "Display mode",
    "显示位置" to "Position",
    "排序方式" to "Sorting",
    "出现动画" to "Entry animation",
    "动画时长" to "Animation duration",
    "水平偏移" to "Horizontal offset",
    "垂直偏移" to "Vertical offset",
    "统一背景宽度" to "Uniform background width",
    "显示序号" to "Show index",
    "文字装饰" to "Text decoration",
    "文字大小" to "Text size",
    "文字粗细" to "Font weight",
    "文字间距" to "Letter spacing",
    "文字透明度" to "Text opacity",
    "英文大写" to "Uppercase English",
    "水平内边距" to "Horizontal padding",
    "垂直内边距" to "Vertical padding",
    "条目间距" to "Item spacing",
    "背景圆角" to "Corner radius",
    "背景颜色" to "Background color",
    "背景透明度" to "Background opacity",
    "背景红色" to "Background red",
    "背景绿色" to "Background green",
    "背景蓝色" to "Background blue",
    "背景发光" to "Background glow",
    "背景发光强度" to "Background glow strength",
    "文字颜色模式" to "Text color mode",
    "主颜色" to "Primary color",
    "主色红色" to "Primary red",
    "主色绿色" to "Primary green",
    "主色蓝色" to "Primary blue",
    "副颜色" to "Secondary color",
    "副色红色" to "Secondary red",
    "副色绿色" to "Secondary green",
    "副色蓝色" to "Secondary blue",
    "颜色动画速度" to "Color animation speed",
    "颜色饱和度" to "Color saturation",
    "颜色亮度" to "Color brightness",
    "颜色相位间距" to "Color phase spacing",
    "颜色反向" to "Reverse colors",
    "呼吸强度" to "Breathing strength",
    "文字阴影" to "Text shadow",
    "阴影模式" to "Shadow mode",
    "阴影透明度" to "Shadow opacity",
    "阴影模糊" to "Shadow blur",
    "阴影水平偏移" to "Shadow X offset",
    "阴影垂直偏移" to "Shadow Y offset",
    "线条位置" to "Line position",
    "线条宽度" to "Line width",
    "线条透明度" to "Line opacity",
    "线条发光" to "Line glow",
    "线条发光强度" to "Line glow strength",
    "极简" to "Minimal",
    "条带" to "Strip",
    "卡片" to "Cards",
    "玻璃" to "Glass",
    "右上" to "Top right",
    "左上" to "Top left",
    "右下" to "Bottom right",
    "左下" to "Bottom left",
    "长度降序" to "Length descending",
    "长度升序" to "Length ascending",
    "名称升序" to "Name ascending",
    "名称降序" to "Name descending",
    "淡入" to "Fade",
    "滑入" to "Slide",
    "缩放" to "Scale",
    "圆点" to "Dot",
    "方括号" to "Brackets",
    "短横线" to "Dash",
    "细" to "Light",
    "常规" to "Regular",
    "中等" to "Medium",
    "半粗" to "Semi-bold",
    "粗体" to "Bold",
    "深黑" to "Dark",
    "纯黑" to "Black",
    "灰色" to "Gray",
    "白色" to "White",
    "深蓝" to "Navy",
    "自定义" to "Custom",
    "蓝色" to "Blue",
    "青色" to "Cyan",
    "绿色" to "Green",
    "紫色" to "Purple",
    "橙色" to "Orange",
    "粉色" to "Pink",
    "纯白" to "White",
    "蓝白" to "Blue-white",
    "双色渐变" to "Gradient",
    "彩虹" to "Rainbow",
    "呼吸" to "Breathing",
    "氛围" to "Ambient",
    "海浪" to "Wave",
    "彩色" to "Colored",
    "黑色" to "Black",
    "无" to "None",
    "左侧" to "Left",
    "右侧" to "Right",
    "模式" to "Mode",
    "水平速度" to "Horizontal speed",
    "垂直速度" to "Vertical speed",
    "脉冲间隔" to "Pulse interval",
    "防悬空停留" to "Anti kick",
    "作用范围" to "Range",
    "视野角度" to "Field of view",
    "操作延迟" to "Action delay",
    "目标优先级" to "Target priority",
    "墙体检测" to "Wall check",
    "仅玩家" to "Players only",
    "单次操作数" to "Actions per cycle",
    "筛选顺序" to "Filter order",
    "保留快捷栏" to "Keep hotbar",
    "完成后关闭" to "Close when done",
    "移动模式" to "Movement mode",
    "速度倍率" to "Speed multiplier",
    "加速平滑度" to "Acceleration smoothing",
    "仅移动时" to "Only while moving",
    "潜行时暂停" to "Pause while sneaking",
    "显示距离" to "Render distance",
    "透明度" to "Opacity",
    "线条粗细" to "Line thickness",
    "颜色模式" to "Color mode",
    "穿墙显示" to "Through walls",
    "最大距离" to "Maximum distance",
    "执行延迟" to "Execution delay",
    "目标方式" to "Target method",
    "执行前确认" to "Confirm before execution",
    "方块延迟" to "Block delay",
    "处理顺序" to "Processing order",
    "使用白名单" to "Use whitelist",
    "附近有人时暂停" to "Pause near players",
    "发送间隔" to "Send interval",
    "重复次数" to "Repeat count",
    "随机后缀" to "Random suffix",
    "打开界面时暂停" to "Pause while menus are open",
    "显示比例" to "Display scale",
    "过渡时长" to "Transition duration",
    "渲染质量" to "Render quality",
    "关闭时恢复" to "Restore when disabled",
    "强度" to "Strength",
    "平滑度" to "Smoothing",
    "启用条件" to "Activation condition",
    "场景切换时关闭" to "Disable on scene change",
    "显示大小" to "Display size",
    "显示动画" to "Animations",
    "工作模式" to "Working mode",
    "安全模式" to "Safe mode",
    "仅在游戏内运行" to "Only in game",
    "离开世界时关闭" to "Disable when leaving world",
    "自动" to "Automatic",
    "按键" to "Key bind",
    "持续" to "Continuous",
    "始终" to "Always",
    "移动时" to "While moving",
    "按键时" to "While key is held",
    "玩家" to "Players",
    "生物" to "Mobs",
    "全部" to "All",
    "距离" to "Distance",
    "生命" to "Health",
    "护甲" to "Armor",
    "视角" to "View angle",
)
