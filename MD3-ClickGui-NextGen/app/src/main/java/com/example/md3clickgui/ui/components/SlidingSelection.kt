package com.example.md3clickgui.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.md3clickgui.ui.theme.NexusMotion
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme

internal class SlidingSelection {
    private var coordinates: LayoutCoordinates? = null
    private val rows = mutableMapOf<String, LayoutCoordinates>()
    val bounds = mutableStateMapOf<String, Rect>()
    private val visibleBounds = mutableStateMapOf<String, Rect>()
    val progress = Animatable(1f)
    var origin by mutableStateOf<Rect?>(null)
    var selectedId by mutableStateOf<String?>(null)
    var highlightColor by mutableStateOf(Color.Transparent)
    var rowColor by mutableStateOf(Color.Transparent)

    private var shownId by mutableStateOf<String?>(null)

    fun currentRect(): Rect? {
        val id = shownId ?: return null
        val target = bounds[id] ?: return null
        return origin?.let { lerp(it, target, progress.value) } ?: target
    }

    fun Modifier.selectionContainer(): Modifier = onGloballyPositioned {
        coordinates = it
        rows.forEach { (id, child) -> updateBounds(id, child) }
    }.drawBehind {
        clipRect {
            val radius = CornerRadius(10.dp.toPx())
            bounds.forEach { (id, rect) ->

                visibleBounds[id]?.let { visible ->
                    if (visible.width > 0 && visible.height > 0) {
                        clipRect(visible.left, visible.top, visible.right, visible.bottom) {
                            drawRoundRect(rowColor, rect.topLeft, rect.size, radius)
                        }
                    }
                }
            }
            currentRect()?.let { rect -> drawRoundRect(highlightColor, rect.topLeft, rect.size, radius) }
        }
    }

    fun track(id: String?, slidable: Boolean) {
        if (slidable) shownId = id
    }

    private fun updateBounds(id: String, child: LayoutCoordinates) {
        val parent = coordinates ?: return
        if (parent.isAttached && child.isAttached) {
            bounds[id] = Rect(parent.localPositionOf(child, Offset.Zero),
                Size(child.size.width.toFloat(), child.size.height.toFloat()))
            visibleBounds[id] = parent.localBoundingBoxOf(child, clipBounds = true)
        }
    }

    @Composable
    fun Modifier.selectionRow(id: String, slidable: Boolean = true): Modifier {
        DisposableEffect(this@SlidingSelection, id) {
            onDispose { rows.remove(id); bounds.remove(id); visibleBounds.remove(id) }
        }
        if (!slidable) return this
        return onGloballyPositioned { child -> rows[id] = child; updateBounds(id, child) }
    }
}

@Composable
internal fun rememberSlidingSelection(selectedId: String?, highlightColor: Color, rowColor: Color): SlidingSelection {
    val selection = remember { SlidingSelection() }

    SideEffect {
        selection.highlightColor = highlightColor
        selection.rowColor = rowColor
    }

    val slideSpec = NexusMotion.feedbackSpec<Float>()
    LaunchedEffect(selectedId) {
        val current = selection.currentRect()
        val targetMeasured = selectedId != null && selection.bounds.containsKey(selectedId)

        selection.track(selectedId, slidable = targetMeasured)
        selection.origin = if (targetMeasured) current else null
        selection.selectedId = selectedId

        try {
            if (targetMeasured && current != null) {
                selection.progress.snapTo(0f)
                selection.progress.animateTo(
                    targetValue = 1f,
                    animationSpec = slideSpec
                )
            } else {
                selection.progress.snapTo(1f)
            }
        } finally {
            selection.origin = null
        }
    }
    return selection
}
