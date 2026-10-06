package com.example.md3clickgui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.md3clickgui.ui.language.uiText
import com.example.md3clickgui.ui.theme.NexusMotion
import kotlin.math.roundToInt

/** An anchored menu that reveals its rows without moving the settings underneath. */
@Composable
internal fun CompactChoiceMenu(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    languageIndex: Int = 0
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val onSelectLatest by rememberUpdatedState(onSelect)
    var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
    var popupMounted by remember { mutableStateOf(false) }
    val visibility = remember { MutableTransitionState(false) }
    val anchorInteractions = remember { MutableInteractionSource() }
    val scrollState = rememberScrollState()
    val focusRequesters = remember(options.size) { List(options.size) { FocusRequester() } }
    var focusedIndex by remember { mutableStateOf(0) }

    fun openMenu() {
        if (options.isNotEmpty() && !visibility.targetState) {
            focusedIndex = selectedIndex.coerceIn(options.indices)
            popupMounted = true
            visibility.targetState = true
        }
    }

    fun closeMenu(selection: Int? = null) {
        if (visibility.targetState) {
            visibility.targetState = false
            if (selection != null && selection in options.indices) onSelectLatest(selection)
        }
    }

    // Commit the value on the click; keep only the visual exit alive until it finishes.
    LaunchedEffect(popupMounted, visibility.isIdle, visibility.currentState, visibility.targetState) {
        if (popupMounted && visibility.isIdle && !visibility.currentState && !visibility.targetState) {
            popupMounted = false
        }
    }

    Box(modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        anchorBounds = IntRect(bounds.left.roundToInt(), bounds.top.roundToInt(),
            bounds.right.roundToInt(), bounds.bottom.roundToInt())
    }) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(36.dp).clip(shape)
                .clickable(enabled = options.isNotEmpty(), role = Role.Button,
                    interactionSource = anchorInteractions, indication = null, onClick = ::openMenu)
                .semantics {
                    stateDescription = uiText(languageIndex, options.getOrNull(selectedIndex).orEmpty())
                },
            shape = shape,
            color = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer
        ) {
            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(uiText(languageIndex, options.getOrNull(selectedIndex).orEmpty()),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        if (popupMounted && anchorBounds.width > 0 && options.isNotEmpty()) {
            val marginPx = with(density) { 8.dp.roundToPx() }
            val gapPx = with(density) { 4.dp.roundToPx() }
            val desiredHeightPx = with(density) { (36.dp * options.size + 8.dp).roundToPx() }
            val below = (windowSize.height - marginPx - anchorBounds.bottom - gapPx).coerceAtLeast(0)
            val above = (anchorBounds.top - marginPx - gapPx).coerceAtLeast(0)
            val opensAbove = desiredHeightPx > below && above > below
            val availableHeightPx = if (opensAbove) above else below
            val maximumHeight = with(density) { availableHeightPx.toDp() }.coerceAtMost(288.dp)
            val menuWidth = with(density) {
                anchorBounds.width.coerceAtMost((windowSize.width - marginPx * 2).coerceAtLeast(1)).toDp()
            }
            val positionProvider = remember(opensAbove, marginPx, gapPx) {
                ChoiceMenuPositionProvider(opensAbove, marginPx, gapPx)
            }

            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { closeMenu() },
                properties = PopupProperties(focusable = visibility.targetState, dismissOnBackPress = true,
                    dismissOnClickOutside = true)
            ) {
                AnimatedVisibility(
                    visibleState = visibility,
                    enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>(), expandFrom = if (opensAbove) Alignment.Bottom else Alignment.Top) +
                        fadeIn(MaterialTheme.motionScheme.defaultSpatialSpec<Float>()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>(), shrinkTowards = if (opensAbove) Alignment.Bottom else Alignment.Top) +
                        fadeOut(MaterialTheme.motionScheme.defaultSpatialSpec<Float>())
                ) {
                    Surface(
                        modifier = Modifier.width(menuWidth).heightIn(max = maximumHeight)
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) false
                                else when (event.key) {
                                    Key.Escape -> { closeMenu(); true }
                                    Key.DirectionDown, Key.DirectionUp -> {
                                        if (visibility.targetState) {
                                            val direction = if (event.key == Key.DirectionDown) 1 else -1
                                            focusedIndex = (focusedIndex + direction + options.size) % options.size
                                            focusRequesters[focusedIndex].requestFocus()
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            },
                        shape = shape, color = colors.surfaceContainerHigh,
                        tonalElevation = 0.dp, shadowElevation = 4.dp
                    ) {
                        Column(
                            Modifier.verticalScroll(scrollState).padding(4.dp).selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            options.forEachIndexed { index, option ->
                                val interactions = remember { MutableInteractionSource() }
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .focusRequester(focusRequesters[index])
                                        .selectable(selected = index == selectedIndex,
                                            enabled = visibility.targetState, role = Role.RadioButton,
                                            interactionSource = interactions, indication = null,
                                            onClick = { closeMenu(index) }),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (index == selectedIndex) colors.secondaryContainer else colors.surfaceContainerHigh,
                                    contentColor = if (index == selectedIndex) colors.onSecondaryContainer else colors.onSurface
                                ) {
                                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(uiText(languageIndex, option), modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (index == selectedIndex) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    LaunchedEffect(Unit) {
                        val index = selectedIndex.coerceIn(options.indices)
                        scrollState.scrollTo(with(density) { (36.dp * index).roundToPx() })
                        focusRequesters[index].requestFocus()
                    }
                }
            }
        }
    }
}

private class ChoiceMenuPositionProvider(
    private val opensAbove: Boolean,
    private val margin: Int,
    private val gap: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val desiredX = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left
            else anchorBounds.right - popupContentSize.width
        val desiredY = if (opensAbove) anchorBounds.top - gap - popupContentSize.height
            else anchorBounds.bottom + gap
        val maxX = (windowSize.width - margin - popupContentSize.width).coerceAtLeast(margin)
        val maxY = (windowSize.height - margin - popupContentSize.height).coerceAtLeast(margin)
        return IntOffset(desiredX.coerceIn(margin, maxX), desiredY.coerceIn(margin, maxY))
    }
}
