package dev.liquid.clickgui.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PersistentHudOverlay(
    controller: ClickGuiController,
    modifier: Modifier = Modifier,
) {
    val arrayListState = controller.arrayListHudState()
    val activeModuleNames = controller.activeModuleNamesForHud()
    var injectionVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(INJECTION_INTRO_START_DELAY_MS)
        injectionVisible = true
        delay(INJECTION_INTRO_VISIBLE_DURATION_MS)
        injectionVisible = false
    }

    val offsetX by animateDpAsState(
        targetValue = arrayListState.layout.offsetX.dp,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "arrayListOffsetX",
    )
    val offsetY by animateDpAsState(
        targetValue = arrayListState.layout.offsetY.dp,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "arrayListOffsetY",
    )
    val listAlignment = when (arrayListState.layout.position) {
        1 -> Alignment.TopStart
        2 -> Alignment.BottomEnd
        3 -> Alignment.BottomStart
        else -> Alignment.TopEnd
    }
    val listPadding = when (arrayListState.layout.position) {
        1 -> Modifier.padding(start = offsetX, top = offsetY)
        2 -> Modifier.padding(end = offsetX, bottom = offsetY)
        3 -> Modifier.padding(start = offsetX, bottom = offsetY)
        else -> Modifier.padding(end = offsetX, top = offsetY)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ModuleSwitchDynamicIsland(
            coordinator = controller.moduleSwitchNotices,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                )
                .padding(top = 6.dp, start = 16.dp, end = 16.dp),
        )

        ArrayListHud(
            state = arrayListState,
            moduleNames = activeModuleNames,
            modifier = Modifier
                .align(listAlignment)
                .then(listPadding),
        )

        InjectionIntro(
            visible = injectionVisible,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ArrayListHud(
    state: ArrayListHudState,
    moduleNames: List<String>,
    modifier: Modifier = Modifier,
) {
    val names = remember(moduleNames, state.layout.sortMode) {
        val comparator = when (state.layout.sortMode) {
            1 -> compareBy<String> { it.length }.thenBy { it }
            2 -> compareBy<String> { it.lowercase() }
            3 -> compareByDescending<String> { it.lowercase() }
            else -> compareByDescending<String> { it.length }.thenBy { it }
        }
        moduleNames.sortedWith(comparator)
    }
    val alignRight = state.layout.position == 0 || state.layout.position == 2
    val animationDuration = state.animation.durationMillis
    val enter = arrayListEnterTransition(state.animation.style, animationDuration, alignRight)
    val exit = arrayListExitTransition(state.animation.style, animationDuration, alignRight)

    val colorMotion = rememberInfiniteTransition(label = "arrayListColorMotion")
    val colorPhase by colorMotion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (5_000f / state.colors.speed).roundToInt().coerceAtLeast(250),
                easing = LinearEasing,
            ),
        ),
        label = "arrayListColorPhase",
    )

    AnimatedVisibility(
        visible = state.enabled && names.isNotEmpty(),
        modifier = modifier,
        enter = enter,
        exit = exit,
    ) {
        Column(
            modifier = Modifier
                .then(if (state.layout.uniformWidth) Modifier.width(IntrinsicSize.Max) else Modifier)
                .animateContentSize(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 500f),
                ),
            verticalArrangement = Arrangement.spacedBy(state.layout.itemSpacing.dp),
            horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start,
        ) {
            names.forEachIndexed { index, moduleName ->
                ArrayListItem(
                    name = decoratedArrayListName(moduleName, index, state),
                    index = index,
                    count = names.size,
                    phase = colorPhase,
                    alignRight = alignRight,
                    state = state,
                )
            }
        }
    }
}

@Composable
private fun ArrayListItem(
    name: String,
    index: Int,
    count: Int,
    phase: Float,
    alignRight: Boolean,
    state: ArrayListHudState,
) {
    val accent = arrayListColor(state.colors, index, count, phase)
        .copy(alpha = state.text.opacity)
    val backgroundColor = arrayListBackgroundColor(state.background, state.layout.displayMode)
    val shapeRadius = when (state.layout.displayMode) {
        1 -> 2f
        else -> state.layout.cornerRadius
    }
    val shape = RoundedCornerShape(shapeRadius.dp)
    val hasBackground = state.layout.displayMode != 0 && state.background.opacity > 0f
    val isGlass = state.layout.displayMode == 3
    val fontSize = state.text.fontSize
    val itemHeight = fontSize + state.layout.paddingVertical * 2f + 3f
    val textShadow = if (state.text.shadowEnabled) {
        Shadow(
            color = if (state.text.blackShadow) {
                Color.Black.copy(alpha = state.text.shadowOpacity)
            } else {
                accent.copy(alpha = state.text.shadowOpacity)
            },
            offset = Offset(state.text.shadowOffsetX, state.text.shadowOffsetY),
            blurRadius = state.text.shadowBlur,
        )
    } else {
        null
    }

    var rowModifier: Modifier = Modifier.animateContentSize(
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 560f),
    )
    if (state.layout.uniformWidth) rowModifier = rowModifier.fillMaxWidth()
    if (hasBackground && state.background.glow && state.background.glowStrength > 0f) {
        rowModifier = rowModifier.shadow(
            elevation = (2f + state.background.glowStrength * 10f).dp,
            shape = shape,
            ambientColor = accent.copy(alpha = state.background.glowStrength * 0.42f),
            spotColor = accent.copy(alpha = state.background.glowStrength * 0.34f),
        )
    }
    if (hasBackground) rowModifier = rowModifier.background(backgroundColor, shape)
    if (isGlass) {
        rowModifier = rowModifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.18f),
            shape = shape,
        )
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.line.mode == 1) {
            ArrayListLine(accent, itemHeight, state.line)
        }
        if (state.layout.uniformWidth && alignRight) Spacer(Modifier.weight(1f))
        BasicText(
            text = name,
            modifier = Modifier.padding(
                horizontal = state.layout.paddingHorizontal.dp,
                vertical = state.layout.paddingVertical.dp,
            ),
            style = TextStyle(
                color = accent,
                fontFamily = FontFamily.SansSerif,
                fontWeight = arrayListFontWeight(state.text.fontWeight),
                fontSize = fontSize.sp,
                letterSpacing = state.text.letterSpacing.sp,
                shadow = textShadow,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        if (state.layout.uniformWidth && !alignRight) Spacer(Modifier.weight(1f))
        if (state.line.mode == 2) {
            ArrayListLine(accent, itemHeight, state.line)
        }
    }
}

@Composable
private fun ArrayListLine(
    accent: Color,
    itemHeight: Float,
    line: ArrayListLineState,
) {
    val lineColor = accent.copy(alpha = line.opacity)
    var modifier: Modifier = Modifier
        .width(line.width.dp)
        .height(itemHeight.dp)
    if (line.glow && line.glowStrength > 0f) {
        modifier = modifier.shadow(
            elevation = (line.glowStrength * 8f).dp,
            shape = RoundedCornerShape(50),
            ambientColor = lineColor,
            spotColor = lineColor,
        )
    }
    Box(modifier.background(lineColor))
}

private fun decoratedArrayListName(
    source: String,
    index: Int,
    state: ArrayListHudState,
): String {
    var name = if (state.text.uppercase) source.uppercase() else source
    name = when (state.layout.textDecoration) {
        1 -> "• $name"
        2 -> "[$name]"
        3 -> "— $name"
        else -> name
    }
    return if (state.layout.showIndex) "${index + 1}. $name" else name
}

private fun arrayListEnterTransition(style: Int, duration: Int, alignRight: Boolean): EnterTransition =
    when (style) {
        0 -> EnterTransition.None
        1 -> fadeIn(tween(duration))
        3 -> fadeIn(tween(duration)) + scaleIn(tween(duration), initialScale = 0.92f)
        else -> fadeIn(tween(duration)) + slideInHorizontally(
            animationSpec = tween(duration, easing = FastOutSlowInEasing),
            initialOffsetX = { if (alignRight) it / 2 else -it / 2 },
        )
    }

private fun arrayListExitTransition(style: Int, duration: Int, alignRight: Boolean): ExitTransition =
    when (style) {
        0 -> ExitTransition.None
        1 -> fadeOut(tween(duration))
        3 -> fadeOut(tween(duration)) + scaleOut(tween(duration), targetScale = 0.94f)
        else -> fadeOut(tween(duration)) + slideOutHorizontally(
            animationSpec = tween(duration, easing = FastOutSlowInEasing),
            targetOffsetX = { if (alignRight) it / 2 else -it / 2 },
        )
    }

private fun arrayListFontWeight(mode: Int): FontWeight = when (mode) {
    0 -> FontWeight.Light
    1 -> FontWeight.Normal
    3 -> FontWeight.SemiBold
    4 -> FontWeight.Bold
    else -> FontWeight.Medium
}

private fun arrayListBackgroundColor(
    background: ArrayListBackgroundState,
    displayMode: Int,
): Color {
    val base = when (background.colorMode) {
        1 -> Color.Black
        2 -> Color(0xFF59616A)
        3 -> Color.White
        4 -> Color(0xFF102C48)
        5 -> Color(
            red = background.red / 255f,
            green = background.green / 255f,
            blue = background.blue / 255f,
        )
        else -> Color(0xFF121820)
    }
    val glassAdjusted = if (displayMode == 3) lerp(base, Color.White, 0.18f) else base
    return glassAdjusted.copy(alpha = background.opacity)
}

private fun arrayListColor(
    colors: ArrayListColorState,
    index: Int,
    count: Int,
    phase: Float,
): Color {
    val progress = if (count <= 1) 0.5f else index.toFloat() / (count - 1).toFloat()
    val direction = if (colors.reverse) -1f else 1f
    val primary = primaryArrayListColor(colors)
    val secondary = secondaryArrayListColor(colors)
    val itemPhase = direction * phase + progress * colors.phaseSpacing
    val wave = ((sin(itemPhase * PI.toFloat() * 2f) + 1f) / 2f).coerceIn(0f, 1f)
    val raw = when (colors.mode) {
        0 -> primary
        1 -> Color.White
        2 -> lerp(Color(0xFF55B4FF), Color.White, progress)
        3 -> lerp(primary, secondary, if (colors.reverse) 1f - progress else progress)
        4 -> Color.hsv(
            hue = positiveModulo(itemPhase * 360f, 360f),
            saturation = 1f,
            value = 1f,
        )
        5 -> lerp(primary, secondary, wave * colors.breathStrength)
        6 -> lerp(primary, secondary, (wave * 0.55f + progress * 0.45f).coerceIn(0f, 1f))
        7 -> lerp(primary, secondary, wave)
        else -> primary
    }
    return adjustArrayListColor(raw, colors.saturation, colors.brightness)
}

private fun primaryArrayListColor(colors: ArrayListColorState): Color = when (colors.primaryMode) {
    1 -> Color(0xFF25D8FF)
    2 -> Color(0xFF43E38D)
    3 -> Color(0xFF9C72FF)
    4 -> Color(0xFFFFA63D)
    5 -> Color(0xFFFF68B0)
    6 -> Color(colors.primaryRed / 255f, colors.primaryGreen / 255f, colors.primaryBlue / 255f)
    else -> Color(0xFF30A4FF)
}

private fun secondaryArrayListColor(colors: ArrayListColorState): Color = when (colors.secondaryMode) {
    1 -> Color(0xFF39E3F2)
    2 -> Color(0xFF4C8DFF)
    3 -> Color(0xFFAA78FF)
    4 -> Color(0xFFFF71B8)
    5 -> Color(0xFFFFA347)
    6 -> Color(colors.secondaryRed / 255f, colors.secondaryGreen / 255f, colors.secondaryBlue / 255f)
    else -> Color.White
}

private fun adjustArrayListColor(color: Color, saturation: Float, brightness: Float): Color {
    val gray = color.red * 0.2126f + color.green * 0.7152f + color.blue * 0.0722f
    return Color(
        red = (gray + (color.red - gray) * saturation).coerceIn(0f, 1f) * brightness,
        green = (gray + (color.green - gray) * saturation).coerceIn(0f, 1f) * brightness,
        blue = (gray + (color.blue - gray) * saturation).coerceIn(0f, 1f) * brightness,
        alpha = color.alpha,
    )
}

private fun positiveModulo(value: Float, modulus: Float): Float = ((value % modulus) + modulus) % modulus
