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

/** 
 * Paint the rows and one continuous indicator in their common parent, including the gaps.
 * 
 * 优化: 支持可打断动画,降低操作延迟
 * - 当用户快速切换选项时,立即从当前位置开始新动画
 * - 始终保持 400ms 完整动画时长,确保动画质感一致
 */
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

    /**
     * The id the indicator is actually showing.
     *
     * When the selection moves to a row that is not part of the sliding group — the rail's anchored
     * Music and Config — the indicator keeps showing this id's row, so it visibly stays on the card it
     * was already on rather than sliding away or disappearing.
     */
    private var shownId by mutableStateOf<String?>(null)

    /**
     * The rect the indicator should be drawn at, for the selected row or for the row it last showed.
     *
     * Returns null only before any sliding row has been measured.
     */
    fun currentRect(): Rect? {
        val id = shownId ?: return null
        val target = bounds[id] ?: return null
        return origin?.let { lerp(it, target, progress.value) } ?: target
    }

    /**
     * Paints the row washes and the sliding indicator behind the rows.
     *
     * Note the layering consequence: anything drawn after this container — the rail sections that sit
     * outside the scrolling area, for instance — ends up on top, which can clip the indicator.
     */
    fun Modifier.selectionContainer(): Modifier = onGloballyPositioned {
        coordinates = it
        rows.forEach { (id, child) -> updateBounds(id, child) }
    }.drawBehind {
        clipRect {
            val radius = CornerRadius(10.dp.toPx())
            bounds.forEach { (id, rect) ->
                // Only rows measured inside the container get a wash; an anchored row already has its
                // own background and must not be tinted by the selection.
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

    /** Called by the selection effect: the indicator follows the selection only when it can. */
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

    /**
     * Registers a row as a destination the indicator may travel to.
     *
     * @param slidable false for rows the indicator must not track at all — the rail's anchored
     *   sections. Such a row never enters [bounds], so the indicator has no target for it and simply
     *   holds its last position while the selection sits there.
     */
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
    // Colors are inputs, so they belong to the composition rather than to the animation effect.
    SideEffect {
        selection.highlightColor = highlightColor
        selection.rowColor = rowColor
    }
    // Keyed on the selection alone: recomposing (scroll, resize, colour change) must not restart
    // the slide. Every write to the selection's state happens here, never during composition.
    // The spec is read during composition and captured, so the effect body itself needs no theme.
    val slideSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(selectedId) {
        val current = selection.currentRect()
        val targetMeasured = selectedId != null && selection.bounds.containsKey(selectedId)
        // The indicator only follows a selection it can actually be drawn on. Selecting an anchored
        // row leaves shownId alone, so the indicator stays on the card it was already showing.
        selection.track(selectedId, slidable = targetMeasured)
        selection.origin = if (targetMeasured) current else null
        selection.selectedId = selectedId
        // Hold the start rect until the animation finishes; clearing it up-front would make
        // currentRect() fall back to the target and swallow the next interrupt. The reset must
        // stay outside the try: snapTo cancels the holder with CancellationException, which
        // would otherwise be caught before the remaining statements ever run.
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
