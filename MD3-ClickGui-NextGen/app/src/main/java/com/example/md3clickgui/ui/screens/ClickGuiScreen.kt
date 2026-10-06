package com.example.md3clickgui.ui.screens

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.example.md3clickgui.ui.components.dispatchKeybinds
import com.example.md3clickgui.ui.components.ArrayListHudLayer
import com.example.md3clickgui.ui.components.ClickGuiWindow
import com.example.md3clickgui.ui.components.OpenPanelButton
import com.example.md3clickgui.ui.components.ShortcutLayer
import com.example.md3clickgui.ui.model.GuiSection
import com.example.md3clickgui.ui.modules.guiSections
import com.example.md3clickgui.ui.state.ClickGuiState
import com.example.md3clickgui.ui.state.rememberClickGuiState
import com.example.md3clickgui.ui.theme.NexusDimensions
import com.example.md3clickgui.ui.theme.NexusCornerShape
import com.example.md3clickgui.ui.theme.NexusSpacing
import com.example.md3clickgui.ui.theme.NexusTheme
import com.example.md3clickgui.ui.theme.NexusMotion
import androidx.compose.ui.unit.IntOffset

@Composable
fun ClickGuiScreen(state: ClickGuiState, sections: List<GuiSection> = guiSections) {
    val colors = MaterialTheme.colorScheme

    val panelVisibilitySpec = NexusMotion.enterSpec<Float>()
    val panelExitSpec = NexusMotion.exitSpec<Float>()
    val windowSlideSpec = NexusMotion.enterSpec<Float>()
    val windowStateHolder = rememberSaveableStateHolder()
    val modules = remember(sections) { sections.flatMap { it.modules } }
    var panelWidthRatio by rememberSaveable { mutableFloatStateOf(DefaultPanelWidthRatio) }
    var panelHeightRatio by rememberSaveable { mutableFloatStateOf(DefaultPanelHeightRatio) }
    Box(
        modifier = Modifier.fillMaxSize().background(colors.background)
            .dispatchKeybinds(state, modules),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val touchTargetPx = with(LocalDensity.current) {
                NexusDimensions.quickShortcutTouchTarget.toPx()
            }
            val shortcutTravelWidth = (maxWidth.value * LocalDensity.current.density - touchTargetPx).coerceAtLeast(0f)
            val shortcutTravelHeight = (maxHeight.value * LocalDensity.current.density - touchTargetPx).coerceAtLeast(0f)

            val density = LocalDensity.current
            val viewportWidth = maxWidth
            val viewportHeight = maxHeight
            val basePanelWidth = viewportWidth * DefaultPanelWidthRatio
            val basePanelHeight = viewportHeight * DefaultPanelHeightRatio
            val windowTravelPx = with(density) { (basePanelHeight * 0.16f).roundToPx() }
            val panelWidthPx = with(density) { viewportWidth.toPx() }
            val panelHeightPx = with(density) { viewportHeight.toPx() }
            val uniformPanelScale = minOf(
                panelWidthRatio / DefaultPanelWidthRatio,
                panelHeightRatio / DefaultPanelHeightRatio
            )

            AnimatedVisibility(
                visible = state.isWindowOpen,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(animationSpec = panelVisibilitySpec) + scaleIn(initialScale = 0.97f, animationSpec = panelVisibilitySpec) +
                    slideInVertically(NexusMotion.enterSpec<IntOffset>()) { windowTravelPx },
                exit = fadeOut(animationSpec = panelExitSpec) + scaleOut(targetScale = 0.97f, animationSpec = panelVisibilitySpec) +
                    slideOutVertically(NexusMotion.exitSpec<IntOffset>()) { windowTravelPx },
                label = "clickGuiWindowVisibility"
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(basePanelWidth)
                            .height(basePanelHeight)
                            .scale(uniformPanelScale)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = NexusCornerShape,
                                color = colors.surface,
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp
                            ) {
                                windowStateHolder.SaveableStateProvider("workspace") {
                                    ClickGuiWindow(state = state, sections = sections)
                                }
                            }
                        }
                        Canvas(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .offset(x = 8.dp, y = 8.dp)
                                .padding(NexusSpacing.small)
                                .pointerInput(viewportWidth, viewportHeight) {
                                    detectDragGesturesAfterLongPress(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (panelWidthPx > 0f) {
                                                panelWidthRatio = (panelWidthRatio + dragAmount.x / panelWidthPx).coerceIn(0.58f, 0.98f)
                                            }
                                            if (panelHeightPx > 0f) {
                                                panelHeightRatio = (panelHeightRatio + dragAmount.y / panelHeightPx).coerceIn(0.58f, 0.94f)
                                            }
                                        }
                                    )
                                }
                        ) {
                            drawArc(
                                color = colors.primary,
                                startAngle = 0f,
                                sweepAngle = 90f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !state.isWindowOpen,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(animationSpec = panelVisibilitySpec),
                exit = fadeOut(animationSpec = panelExitSpec),
                label = "openPanelButtonVisibility"
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    OpenPanelButton(
                        state = state,
                        travelWidth = shortcutTravelWidth,
                        travelHeight = shortcutTravelHeight
                    )
                }
            }
        }
        ShortcutLayer(
            modules = modules,
            state = state,
            modifier = Modifier.fillMaxSize()
        )

        ArrayListHudLayer(
            state = state,
            sections = sections,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun NexusClickGui(state: ClickGuiState) {
    NexusTheme(
        darkMode = state.darkMode,
        dynamicColor = state.dynamicColor,
        themeIndex = state.themeIndex,
        customColorHex = state.customThemeHex
    ) {
        ClickGuiScreen(state)
    }
}

private const val DefaultPanelWidthRatio = 0.93f
private const val DefaultPanelHeightRatio = 0.86f

@Preview(
    name = "Material Click GUI",
    showBackground = true,
    backgroundColor = 0xFFF7FAF9,
    widthDp = 1280,
    heightDp = 720
)
@Composable
private fun ClickGuiScreenPreview() {
    val state = rememberClickGuiState()
    NexusClickGui(state)
}
