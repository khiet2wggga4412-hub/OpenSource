package com.example.md3clickgui.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.example.md3clickgui.ui.theme.NexusMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.md3clickgui.ui.language.uiText
import com.example.md3clickgui.ui.model.FloatingButtonStyle
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.state.ClickGuiState
import com.example.md3clickgui.ui.state.QuickFloatingButtonPosition
import com.example.md3clickgui.ui.theme.NexusDimensions
import com.example.md3clickgui.ui.theme.NexusSpacing
import kotlin.math.roundToInt
import com.example.md3clickgui.ui.theme.colorSpring

/** Renders enabled module shortcuts above the collapsed window state. */
@Composable
fun QuickFloatingButtonLayer(
    modules: List<GuiModule>,
    state: ClickGuiState,
    modifier: Modifier = Modifier
) {
    val visibleModules = modules.filter { state.isQuickFloatingButtonEnabled(it) }
    val isVisible = state.canInteractWithModules() && !state.isWindowOpen && visibleModules.isNotEmpty()
    val shortcutAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        // Effects token: this animates alpha, not position.
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "quickButtonLayerAlpha"
    )

    // Keep the positioning layer alive through the entire fade. Removing it at
    // the visibility boundary causes a one-frame remeasure and visible jitter.
    BoxWithConstraints(
        modifier = modifier.graphicsLayer { alpha = shortcutAlpha }
    ) {
            val density = LocalDensity.current
            val touchTargetPx = with(density) { NexusDimensions.quickFloatingButtonTouchTarget.toPx() }
            val travelWidth = (constraints.maxWidth - touchTargetPx).coerceAtLeast(0f)
            val travelHeight = (constraints.maxHeight - touchTargetPx).coerceAtLeast(0f)
            val stackStepPx = with(density) {
                (NexusDimensions.quickFloatingButtonTouchTarget + NexusSpacing.small).toPx()
            }

            visibleModules.forEachIndexed { index, module ->
                val defaultPosition = QuickFloatingButtonPosition(
                    xFraction = 1f,
                    yFraction = if (travelHeight > 0f) {
                        (1f - ((index + 1) * stackStepPx / travelHeight)).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                )
                val position = state.quickFloatingButtonPosition(module.id) ?: defaultPosition
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (position.xFraction * travelWidth).roundToInt(),
                                (position.yFraction * travelHeight).roundToInt()
                            )
                        }
                        .align(Alignment.TopStart)
                ) {
                        QuickFloatingButton(
                            module = module,
                            state = state,
                            enabled = isVisible,
                            onClick = { state.toggle(module) },
                        onDrag = { delta ->
                            state.moveQuickFloatingButton(
                                moduleId = module.id,
                                deltaX = delta.x,
                                deltaY = delta.y,
                                travelWidth = travelWidth,
                                travelHeight = travelHeight,
                                defaultPosition = defaultPosition
                            )
                        }
                    )
                }
            }
    }
}

/** Project name: the label the collapsed-window button carries once it shows text. */
private const val MaterialLabel = "Material"

@Composable
private fun QuickFloatingButton(
    module: GuiModule,
    state: ClickGuiState,
    enabled: Boolean,
    onClick: () -> Unit,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isChecked = state.isChecked(module)
    val style = state.floatingButtonStyle()
    val buttonSize = state.floatingButtonSize().dp
    val cornerRadius = state.floatingButtonCornerRadius().dp
    val glyphSize = buttonSize * 0.625f
    val labelFontSize = (buttonSize.value * 0.375f).sp
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = labelFontSize,
        lineHeight = labelFontSize * 1.34f
    )
    val containerColor by animateColorAsState(
        targetValue = if (isChecked) colors.primary else colors.primaryContainer,
        animationSpec = colorSpring(),
        label = "quickButtonContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isChecked) colors.onPrimary else colors.onPrimaryContainer,
        animationSpec = colorSpring(),
        label = "quickButtonContentColor"
    )
    Box(
        modifier = Modifier
            .defaultMinSize(
                minWidth = NexusDimensions.quickFloatingButtonTouchTarget,
                minHeight = NexusDimensions.quickFloatingButtonTouchTarget
            )
            .pointerInput(module.id, enabled) {
                if (enabled) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                        }
                    )
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Toggle ${module.name}",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = if (style == FloatingButtonStyle.Icon) {
                Modifier.size(buttonSize)
            } else {
                Modifier.defaultMinSize(minWidth = buttonSize)
            },
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            when (style) {
                FloatingButtonStyle.Icon -> Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = "Toggle ${module.name}",
                        modifier = Modifier.size(glyphSize)
                    )
                }
                FloatingButtonStyle.Text -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = buttonSize * 0.375f, vertical = buttonSize * 0.25f)
                ) {
                    Text(
                        uiText(state.languageIndex, module.name),
                        style = labelStyle,
                        maxLines = 1
                    )
                }
                FloatingButtonStyle.IconText -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(buttonSize * 0.25f),
                    modifier = Modifier.padding(horizontal = buttonSize * 0.375f, vertical = buttonSize * 0.25f)
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = "Toggle ${module.name}",
                        modifier = Modifier.size(glyphSize)
                    )
                    Text(
                        uiText(state.languageIndex, module.name),
                        style = labelStyle,
                        maxLines = 1
                    )
                }
            }
        }
    }
}


/** The collapsed-window control, positioned independently from the scaled panel. */
@Composable
fun OpenPanelFloatingButton(
    state: ClickGuiState,
    travelWidth: Float,
    travelHeight: Float,
    modifier: Modifier = Modifier
) {
    val position = state.openPanelButtonPosition()
    val style = state.floatingButtonStyle()
    val buttonSize = state.floatingButtonSize().dp
    val cornerRadius = state.floatingButtonCornerRadius().dp
    val glyphSize = buttonSize * 0.625f
    val labelFontSize = (buttonSize.value * 0.375f).sp
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = labelFontSize,
        lineHeight = labelFontSize * 1.34f
    )
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (position.xFraction * travelWidth - travelWidth / 2f).roundToInt(),
                    (position.yFraction * travelHeight - travelHeight / 2f).roundToInt()
                )
            }
            .defaultMinSize(
                minWidth = NexusDimensions.quickFloatingButtonTouchTarget,
                minHeight = NexusDimensions.quickFloatingButtonTouchTarget
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.moveOpenPanelButton(
                            deltaX = dragAmount.x,
                            deltaY = dragAmount.y,
                            travelWidth = travelWidth,
                            travelHeight = travelHeight
                        )
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = MaterialLabel,
                onClick = state::openWindow
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = if (style == FloatingButtonStyle.Icon) {
                Modifier.size(buttonSize)
            } else {
                Modifier.defaultMinSize(minWidth = buttonSize)
            },
            shape = RoundedCornerShape(cornerRadius),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            when (style) {
                FloatingButtonStyle.Icon -> Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = MaterialLabel,
                        modifier = Modifier.size(glyphSize)
                    )
                }
                FloatingButtonStyle.Text -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = buttonSize * 0.375f, vertical = buttonSize * 0.25f)
                ) {
                    Text(
                        uiText(state.languageIndex, MaterialLabel),
                        style = labelStyle,
                        maxLines = 1
                    )
                }
                FloatingButtonStyle.IconText -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(buttonSize * 0.25f),
                    modifier = Modifier.padding(horizontal = buttonSize * 0.375f, vertical = buttonSize * 0.25f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = MaterialLabel,
                        modifier = Modifier.size(glyphSize)
                    )
                    Text(
                        uiText(state.languageIndex, MaterialLabel),
                        style = labelStyle,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
