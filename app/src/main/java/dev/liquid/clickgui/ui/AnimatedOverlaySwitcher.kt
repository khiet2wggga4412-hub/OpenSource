package dev.liquid.clickgui.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Coordinates the full panel and collapsed floating button without clipping exit animations. */
@Composable
fun AnimatedOverlaySwitcher(
    expanded: Boolean,
    onExpandedLayoutRequired: () -> Unit,
    onCollapsedLayoutRequired: () -> Unit,
    modifier: Modifier = Modifier,
    playInjectionIntro: Boolean = false,
    expandedContent: @Composable () -> Unit,
    collapsedContent: @Composable () -> Unit,
) {
    var panelVisible by remember { mutableStateOf(expanded && !playInjectionIntro) }
    var floatingButtonVisible by remember { mutableStateOf(!expanded && !playInjectionIntro) }
    var introFinished by remember { mutableStateOf(!playInjectionIntro) }
    var lastExpanded by remember { mutableStateOf(expanded) }

    LaunchedEffect(playInjectionIntro) {
        if (!playInjectionIntro) return@LaunchedEffect
        delay(INJECTION_PANEL_START_DELAY_MS)
        introFinished = true
    }

    LaunchedEffect(expanded, introFinished) {
        if (!introFinished) return@LaunchedEffect
        if (lastExpanded == expanded) {
            panelVisible = expanded
            floatingButtonVisible = !expanded
            return@LaunchedEffect
        }
        lastExpanded = expanded

        if (expanded) {
            floatingButtonVisible = false
            delay(FLOATING_BUTTON_LAYOUT_SWAP_DELAY_MS)
            onExpandedLayoutRequired()
            panelVisible = true
        } else {
            panelVisible = false
            delay(PANEL_LAYOUT_SWAP_DELAY_MS)
            onCollapsedLayoutRequired()
            floatingButtonVisible = true
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = panelVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(PANEL_ENTER_DURATION_MS, easing = FastOutSlowInEasing)) +
                scaleIn(
                    initialScale = 0.86f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 430f),
                ) +
                slideInVertically(
                    initialOffsetY = { height -> height / 18 },
                    animationSpec = tween(PANEL_ENTER_DURATION_MS, easing = FastOutSlowInEasing),
                ),
            exit = fadeOut(tween(PANEL_EXIT_DURATION_MS)) +
                scaleOut(
                    targetScale = 0.94f,
                    animationSpec = tween(PANEL_EXIT_DURATION_MS),
                ) +
                slideOutVertically(
                    targetOffsetY = { height -> height / 28 },
                    animationSpec = tween(PANEL_EXIT_DURATION_MS),
                ),
        ) {
            expandedContent()
        }

        AnimatedVisibility(
            visible = floatingButtonVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(FLOATING_BUTTON_ENTER_DURATION_MS)) +
                scaleIn(
                    initialScale = 0.72f,
                    animationSpec = tween(FLOATING_BUTTON_ENTER_DURATION_MS),
                ),
            exit = fadeOut(tween(FLOATING_BUTTON_EXIT_DURATION_MS)) +
                scaleOut(
                    targetScale = 0.78f,
                    animationSpec = tween(FLOATING_BUTTON_EXIT_DURATION_MS),
                ),
        ) {
            collapsedContent()
        }
    }
}

/** A restrained, non-interactive status card shown after the host loading layer settles. */
@Composable
internal fun InjectionIntro(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (!visible) {
            ready = false
            return@LaunchedEffect
        }
        ready = false
        delay(INJECTION_STATUS_CHANGE_MS)
        ready = true
    }

    val statusColor by animateColorAsState(
        targetValue = if (ready) Color(0xFF168CFF) else Color(0xFF8DA7BD),
        animationSpec = tween(240),
        label = "injectionStatusColor",
    )
    val statusScale by animateFloatAsState(
        targetValue = if (ready) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 520f),
        label = "injectionStatusScale",
    )
    val progress by animateFloatAsState(
        targetValue = if (ready) 1f else 0.18f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "injectionProgress",
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(220)) + scaleIn(tween(280), initialScale = 0.97f),
        exit = fadeOut(tween(260)) + scaleOut(tween(260), targetScale = 0.985f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(368.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color(0xFF168CFF).copy(alpha = 0.14f),
                        spotColor = Color.Black.copy(alpha = 0.18f),
                    )
                    .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(30.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF168CFF).copy(alpha = 0.20f),
                        shape = RoundedCornerShape(30.dp),
                    )
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .graphicsLayer {
                                scaleX = statusScale
                                scaleY = statusScale
                            }
                            .background(statusColor.copy(alpha = 0.10f), CircleShape)
                            .border(1.dp, statusColor.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = if (ready) "✓" else "L",
                            style = TextStyle(
                                color = statusColor,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (ready) 19.sp else 18.sp,
                                letterSpacing = 0.4.sp,
                            ),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        BasicText(
                            text = "LIQUID CLICKGUI",
                            style = TextStyle(
                                color = Color(0xFF18232E),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                letterSpacing = 0.6.sp,
                            ),
                        )
                        BasicText(
                            text = if (ready) "Injection complete" else "Preparing interface",
                            modifier = Modifier.padding(top = 4.dp),
                            style = TextStyle(
                                color = Color(0xFF607181),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                letterSpacing = 0.1.sp,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF18232E).copy(alpha = 0.08f), CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.dp)
                            .background(statusColor, CircleShape),
                    )
                }
            }
        }
    }
}

private const val PANEL_ENTER_DURATION_MS = 280
private const val PANEL_EXIT_DURATION_MS = 220
private const val FLOATING_BUTTON_ENTER_DURATION_MS = 190
private const val FLOATING_BUTTON_EXIT_DURATION_MS = 130
internal const val INJECTION_INTRO_START_DELAY_MS = 25_000L
internal const val INJECTION_INTRO_VISIBLE_DURATION_MS = 2_200L
private const val INJECTION_PANEL_START_DELAY_MS =
    INJECTION_INTRO_START_DELAY_MS + INJECTION_INTRO_VISIBLE_DURATION_MS + 360L
private const val INJECTION_STATUS_CHANGE_MS = 480L
private const val PANEL_LAYOUT_SWAP_DELAY_MS = 240L
private const val FLOATING_BUTTON_LAYOUT_SWAP_DELAY_MS = 150L
