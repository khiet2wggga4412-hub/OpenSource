package com.example.md3clickgui.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Label
import androidx.compose.material3.IconButton
import androidx.compose.runtime.key
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import android.view.KeyEvent as AndroidKeyCodes
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.state.ClickGuiState
import com.example.md3clickgui.ui.theme.NexusDimensions
import com.example.md3clickgui.ui.theme.NexusCornerShape
import com.example.md3clickgui.ui.theme.NexusIconShape
import com.example.md3clickgui.ui.theme.NexusSpacing
import com.example.md3clickgui.ui.theme.NexusMotion
import com.example.md3clickgui.ui.language.uiText
import kotlinx.coroutines.launch
import java.util.Locale
import android.graphics.Color as AndroidColor
import androidx.compose.material3.TextButton
import com.example.md3clickgui.ui.model.ModuleKeybind
import com.example.md3clickgui.ui.model.isGamepadKey
import com.example.md3clickgui.ui.model.keyLabel
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun ModuleCard(module: GuiModule, state: ClickGuiState, languageIndex: Int = 0, categoryName: String = "", selection: SlidingSelection) {
    val colors = MaterialTheme.colorScheme
    val selected = state.isDetailsPanelOpen && state.selectedModuleId == module.id

    val foreground by animateColorAsState(if (selected) colors.onPrimaryContainer else colors.onSurface, NexusMotion.colorSpec(), label = "moduleTextColor")
    Surface(
        onClick = { state.selectModule(module.id) },
        enabled = state.canInteractWithModules(),
        modifier = Modifier.fillMaxWidth().heightIn(min = NexusDimensions.moduleRow).semantics { this.selected = selected },
        shape = NexusIconShape,
        color = Color.Transparent,
        contentColor = foreground
    ) {
        Row(
            with(selection) { Modifier.fillMaxWidth().selectionRow(module.id) }
                .padding(horizontal = NexusSpacing.small, vertical = NexusSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small)
        ) {
            Column(Modifier.weight(1f)) {
                Text(uiText(languageIndex, module.name), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (categoryName.isNotEmpty()) {
                    Text(uiText(languageIndex, categoryName), style = MaterialTheme.typography.labelSmall,
                        color = foreground.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (module.hasEnableSwitch()) {
                AccessibleSwitch(
                    label = uiText(languageIndex, module.name), checked = state.isChecked(module),
                    enabled = state.canInteractWithModules(), onCheckedChange = { state.toggle(module) }
                )
            }
        }
    }
}

private fun GuiModule.hasEnableSwitch(): Boolean = when (binding) {
    ModuleBinding.Standard, ModuleBinding.DarkMode, ModuleBinding.DynamicColor -> true
    ModuleBinding.Theme, ModuleBinding.Language, ModuleBinding.ShortcutButton, ModuleBinding.Content -> false
}

@Composable
fun ModuleSettingsPanel(
    module: GuiModule,
    state: ClickGuiState,
    modifier: Modifier = Modifier,
    languageIndex: Int = 0,
    categoryName: String = "",
    onClose: () -> Unit = state::closeWindow
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxHeight(), color = colors.surfaceContainerLow, shape = NexusCornerShape) {
        Column(Modifier.fillMaxSize().padding(NexusSpacing.extraSmall)) {
            Surface(shape = NexusIconShape, color = colors.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(start = NexusSpacing.medium, end = NexusSpacing.extraSmall)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = NexusSpacing.extraSmall)) {
                            Text(uiText(languageIndex, module.name), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (categoryName.isNotEmpty()) {
                                Text(uiText(languageIndex, categoryName), style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = uiText(languageIndex, ClosePanelLabel), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(NexusDimensions.rowGap))
            key(module.id) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(NexusDimensions.rowGap)
                ) {
                    module.settings.forEach { setting -> ModuleSettingControl(module, setting, state, languageIndex) }

                    if (module.hasEnableSwitch()) {
                        ModuleSettingControl(module, ModuleSetting.Shortcut, state, languageIndex)
                    }
                    ModuleSettingControl(module, ModuleSetting.Keybind, state, languageIndex)
                }
            }
        }
    }
}

@Composable
private fun KeybindCard(module: GuiModule, state: ClickGuiState, languageIndex: Int) {
    val colors = MaterialTheme.colorScheme
    val bind = state.keybind(module)
    var listening by remember(module.id) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    if (listening) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    SettingRow(uiText(languageIndex, "Keybind")) {
        if (listening) {

            Surface(
                onClick = { listening = false },
                shape = NexusIconShape,
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        val code = event.key.keyCode.toInt()

                        if (code == AndroidKeyCodes.KEYCODE_ESCAPE || code == AndroidKeyCodes.KEYCODE_BACK) {
                            listening = false
                        } else {
                            state.bindKey(module, ModuleKeybind(code, isGamepadKey(code), keyLabel(code)))
                            listening = false
                        }
                        true
                    }
            ) {
                Text(uiText(languageIndex, "Press a button"), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Text(
                text = bind?.label ?: uiText(languageIndex, "None"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (bind == null) colors.onSurfaceVariant else colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(NexusSpacing.small))
            if (bind != null) {
                TextButton(onClick = { state.clearKeybind(module) }) {
                    Text(uiText(languageIndex, "Clear"), style = MaterialTheme.typography.labelMedium)
                }
            }
            TextButton(
                onClick = { listening = true },
                enabled = state.canInteractWithModules()
            ) {
                Text(
                    uiText(languageIndex, if (bind == null) "Bind" else "Change"),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun AccessibleSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(if (checked) 1f else 0f, NexusMotion.feedbackSpec<Float>(), label = "switchThumbSlide")
    val track by animateColorAsState(if (checked) colors.primary else colors.surfaceContainerHighest, NexusMotion.colorSpec(), label = "switchTrack")
    val thumb by animateColorAsState(if (checked) colors.onPrimary else colors.onSurfaceVariant, NexusMotion.colorSpec(), label = "switchThumb")
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    Box(
        modifier = Modifier.size(48.dp)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch,
                    interactionSource = interactions, indication = null, onValueChange = onCheckedChange)
                .focusProperties { canFocus = enabled }
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    stateDescription = if (checked) "On" else "Off"
                },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(width = 40.dp, height = 24.dp)) {
            val opacity = if (enabled) 1f else 0.38f
            drawRoundRect(track.copy(alpha = opacity), cornerRadius = CornerRadius(size.height / 2f))
            drawCircle(thumb.copy(alpha = opacity), radius = (6.dp + 2.dp * progress).toPx(),
                center = Offset(12.dp.toPx() + 16.dp.toPx() * progress, size.height / 2f))
            if (focused) {
                drawRoundRect(colors.primary, topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                    size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                    cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, minHeight: Dp = NexusDimensions.settingRow, control: @Composable RowScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), shape = NexusIconShape, color = colors.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = minHeight)
                .padding(horizontal = NexusSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface,
                modifier = Modifier.weight(0.35f).padding(vertical = NexusSpacing.extraSmall), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.weight(0.65f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End, content = control)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleSettingControl(module: GuiModule, setting: ModuleSetting, state: ClickGuiState, languageIndex: Int) {
    val colors = MaterialTheme.colorScheme
    when (setting) {
        is ModuleSetting.Choice -> SettingRow(uiText(languageIndex, setting.label)) {
            val selectedIndex = state.choiceIndex(module, setting).coerceIn(0, (setting.options.size - 1).coerceAtLeast(0))
            if (module.binding == ModuleBinding.Language) {
                CompactSegmentedControl(options = setting.options, selectedIndex = selectedIndex,
                    onSelect = { state.setChoiceIndex(module, setting, it) },
                    label = uiText(languageIndex, setting.label), modifier = Modifier.widthIn(max = 200.dp).fillMaxWidth())
            } else {
                CompactChoiceMenu(options = setting.options, selectedIndex = selectedIndex,
                    onSelect = { state.setChoiceIndex(module, setting, it) }, languageIndex = languageIndex,
                    modifier = Modifier.widthIn(max = 180.dp).fillMaxWidth())
            }
        }
        is ModuleSetting.ColorPicker -> ThemeColorPickerControl(module, setting, state, languageIndex)
        is ModuleSetting.Slider -> SettingRow(uiText(languageIndex, setting.label), NexusDimensions.sliderRow) {
            val value = state.sliderValue(module, setting)
            val display = formatSliderValue(value, setting, languageIndex)
            val interactions = remember { MutableInteractionSource() }
            Slider(
                value = value, onValueChange = { state.setSliderValue(module, setting, it) },
                valueRange = setting.valueRange, steps = setting.steps,
                interactionSource = interactions,
                thumb = {
                    Label(interactionSource = interactions, label = {
                        Surface(shape = NexusIconShape, color = colors.primary, contentColor = colors.onPrimary) {
                            Text(display, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }) {
                        SliderDefaults.Thumb(interactionSource = interactions,
                            thumbSize = DpSize(4.dp, NexusDimensions.sliderThumbHeight))
                    }
                },
                track = { sliderState -> SliderDefaults.Track(sliderState = sliderState,
                    modifier = Modifier.height(NexusDimensions.sliderTrack),
                    thumbTrackGapSize = 4.dp, trackInsideCornerSize = 2.dp, drawStopIndicator = null) },
                modifier = Modifier.weight(1f).semantics { contentDescription = uiText(languageIndex, setting.label) }
            )
            Text(display, modifier = Modifier.width(56.dp), textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        }
        is ModuleSetting.Toggle -> SettingRow(uiText(languageIndex, setting.label)) {
            AccessibleSwitch(
                label = uiText(languageIndex, setting.label), checked = state.toggleValue(module, setting),
                onCheckedChange = { state.setToggleValue(module, setting, it) }
            )
        }
        is ModuleSetting.Shortcut -> SettingRow(uiText(languageIndex, "Shortcut")) {
            CompactSegmentedControl(
                options = listOf(uiText(languageIndex, "Off"), uiText(languageIndex, "On")),
                selectedIndex = if (state.isQuickShortcutEnabled(module)) 1 else 0,
                onSelect = { state.setQuickShortcutEnabled(module, it == 1) },
                label = uiText(languageIndex, "Shortcut"),
                modifier = Modifier.widthIn(max = 112.dp).fillMaxWidth()
            )
        }
        is ModuleSetting.Keybind -> KeybindCard(module, state, languageIndex)
    }
}

private fun formatSliderValue(value: Float, setting: ModuleSetting.Slider, languageIndex: Int): String {
    val displayedValue = if (setting.suffix == "%") value * 100f else value
    val number = String.format(Locale.US, "%.${setting.decimalPlaces.coerceIn(0, 3)}f", displayedValue)
    return number + uiText(languageIndex, setting.suffix)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeColorPickerControl(module: GuiModule, setting: ModuleSetting.ColorPicker, state: ClickGuiState, languageIndex: Int) {
    val colors = MaterialTheme.colorScheme
    val pickerHex = state.colorPickerValue(module, setting)
    val initialHsv = remember(pickerHex) { hexToHsv(pickerHex) }
    var hue by remember(pickerHex) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(pickerHex) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(pickerHex) { mutableFloatStateOf(initialHsv[2]) }
    var showPalette by remember { mutableStateOf(false) }
    val selectedColor = Color.hsv(hue, saturation, value)
    val selectedHex = hsvToHex(hue, saturation, value)

    SettingRow(uiText(languageIndex, setting.label)) {
        Surface(
            onClick = { showPalette = true },
            modifier = Modifier.widthIn(max = 180.dp).fillMaxWidth().heightIn(min = 36.dp),
            shape = NexusIconShape, color = colors.secondaryContainer
        ) {
            Row(Modifier.padding(horizontal = NexusSpacing.small), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small)) {
                Surface(modifier = Modifier.size(20.dp), shape = NexusIconShape, color = selectedColor) { }
                Text(selectedHex, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSecondaryContainer, maxLines = 1)
                Icon(Icons.Default.ExpandMore, contentDescription = uiText(languageIndex, "Choose accent color"),
                    modifier = Modifier.size(18.dp), tint = colors.onSecondaryContainer)
            }
        }
    }
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    if (showPalette) {
        ModalBottomSheet(
            onDismissRequest = { showPalette = false },
            sheetState = sheetState,
            sheetMaxWidth = 420.dp,
            containerColor = colors.surfaceContainer,
            shape = NexusCornerShape
        ) {
            ThemeColorPickerSheet(
                hue = hue,
                saturation = saturation,
                value = value,
                onSvChange = { newSaturation, newValue ->
                    saturation = newSaturation
                    value = newValue
                    state.setColorPickerValue(module, setting, hsvToHex(hue, saturation, value))
                },
                onHueChange = { newHue ->
                    hue = newHue
                    state.setColorPickerValue(module, setting, hsvToHex(hue, saturation, value))
                },
                onDismiss = { scope.launch { sheetState.hide(); showPalette = false } },
                languageIndex = languageIndex
            )
        }
    }
}

@Composable
private fun ThemeColorPickerSheet(
    hue: Float,
    saturation: Float,
    value: Float,
    onSvChange: (Float, Float) -> Unit,
    onHueChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    languageIndex: Int
) {
    val colors = MaterialTheme.colorScheme
    var svSize by remember { mutableStateOf(IntSize.Zero) }
    var hueSize by remember { mutableStateOf(IntSize.Zero) }
    val selectedColor = Color.hsv(hue, saturation, value)
    val selectedHex = hsvToHex(hue, saturation, value)

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = NexusSpacing.extraLarge, vertical = NexusSpacing.large)) {
        Text(uiText(languageIndex, "Choose accent color"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(Modifier.height(NexusSpacing.large))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .clip(NexusCornerShape)
                    .onSizeChanged { svSize = it }
                    .pointerInput(hue, svSize) {
                        awaitColorGesture { position ->
                            if (svSize.width > 0 && svSize.height > 0) {
                                onSvChange(
                                    (position.x / svSize.width).coerceIn(0f, 1f),
                                    (1f - position.y / svSize.height).coerceIn(0f, 1f)
                                )
                            }
                        }
                    }
            ) {
                val hueColor = Color.hsv(hue, 1f, 1f)
                drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val selector = androidx.compose.ui.geometry.Offset(saturation * size.width, (1f - value) * size.height)
                drawCircle(Color.White, radius = 9f, center = selector)
                drawCircle(Color.Black, radius = 8f, center = selector, style = Stroke(width = 2f))
            }
        }
        Spacer(Modifier.height(NexusSpacing.medium))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(NexusCornerShape)
                .onSizeChanged { hueSize = it }
                .pointerInput(hueSize) {
                    awaitColorGesture { position ->
                        if (hueSize.width > 0) onHueChange((position.x / hueSize.width * 360f).coerceIn(0f, 360f))
                    }
                }
        ) {
            val hueStops = (0..360 step 60).map { Color.hsv(it.toFloat(), 1f, 1f) }
            drawRect(brush = Brush.horizontalGradient(hueStops))
            val selector = androidx.compose.ui.geometry.Offset(hue / 360f * size.width, size.height / 2f)
            drawCircle(Color.White, radius = size.height / 2f, center = selector)
            drawCircle(Color.Black, radius = size.height / 2f - 2f, center = selector, style = Stroke(width = 2f))
        }
        Spacer(Modifier.height(NexusSpacing.large))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(NexusDimensions.colorPreview), shape = NexusCornerShape, color = selectedColor) { }
            Spacer(Modifier.size(NexusSpacing.medium))
            Column(Modifier.weight(1f)) {
                Text(uiText(languageIndex, "Preview"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(selectedHex, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
            }
        }
        Spacer(Modifier.height(NexusSpacing.large))
        FilledTonalButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = NexusCornerShape
        ) { Text(uiText(languageIndex, "Apply color")) }
        Spacer(Modifier.height(NexusSpacing.medium))
    }
}

private fun hexToHsv(hex: String): FloatArray {
    val color = runCatching { AndroidColor.parseColor(hex) }.getOrDefault(AndroidColor.rgb(23, 107, 96))
    return FloatArray(3).also { AndroidColor.colorToHSV(color, it) }
}

private fun hsvToHex(hue: Float, saturation: Float, value: Float): String {
    val color = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    return String.format(Locale.US, "#%06X", color and 0x00FFFFFF)
}

private suspend fun PointerInputScope.awaitColorGesture(onPosition: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var crossedSlop = false
        val slopChange = awaitTouchSlopOrCancellation(down.id) { change, _ ->
            crossedSlop = true
            change.consume()
            onPosition(change.position)
        }
        if (slopChange != null) {
            drag(slopChange.id) { change ->
                onPosition(change.position)
                change.consume()
            }
        } else if (!crossedSlop) {
            onPosition(down.position)
        }
    }
}
