package dev.liquid.clickgui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import dev.liquid.clickgui.model.ModuleCategory
import dev.liquid.clickgui.ui.GlassTypography
import dev.liquid.clickgui.ui.LocalGlassPalette
import dev.liquid.clickgui.ui.PackResourceIcon

data class SideNavigationItem(
    val category: ModuleCategory,
    val icon: ImageVector,
    val label: String = category.displayName,
    val iconRes: Int = 0,
)

/**
 * 基于 Kyant0 LiquidBottomTabs 的绘制结构改造：玻璃底座和选中透镜仍是两层 Backdrop，
 * 只把水平位移改为纵向位移。此文件是经过修改的衍生实现。
 */
@Composable
fun LiquidSideNavigation(
    items: List<SideNavigationItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGlassPalette.current
    val navBackdrop = rememberLayerBackdrop()
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelected by rememberUpdatedState(onSelected)
    var draggingSelector by remember { mutableStateOf(false) }
    var draggedSelectorY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        val density = LocalDensity.current
        val verticalPaddingPx = with(density) { 4.dp.toPx() }
        val contentHeightPx = constraints.maxHeight - verticalPaddingPx * 2f
        val itemHeightPx = contentHeightPx / items.size
        val itemHeight = with(density) { itemHeightPx.toDp() }
        val selectorY by animateFloatAsState(
            targetValue = if (draggingSelector) draggedSelectorY else selectedIndex * itemHeightPx,
            animationSpec = if (draggingSelector) {
                snap()
            } else {
                spring(dampingRatio = 0.78f, stiffness = 520f)
            },
            label = "sideSelectorY",
        )
        val selectorScale by animateFloatAsState(
            targetValue = if (draggingSelector) 1.12f else 1f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f),
            label = "sideSelectorDragScale",
        )

        Column(
            modifier = Modifier
                .layerBackdrop(navBackdrop)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(26.dp) },
                    effects = {
                        vibrancy()
                        blur(10.dp.toPx())
                        lens(20.dp.toPx(), 22.dp.toPx())
                    },
                    onDrawSurface = { drawRect(palette.glassSurfaceStrong.copy(alpha = 0.48f)) },
                )
                .fillMaxHeight()
                .fillMaxWidth()
                .pointerInput(items.size, itemHeightPx) {
                    var dragSelectedIndex = currentSelectedIndex

                    fun updateDrag(y: Float) {
                        draggedSelectorY = (y - verticalPaddingPx - itemHeightPx / 2f)
                            .coerceIn(0f, contentHeightPx - itemHeightPx)
                        val index = ((y - verticalPaddingPx) / itemHeightPx)
                            .toInt()
                            .coerceIn(items.indices)
                        if (index != dragSelectedIndex) {
                            dragSelectedIndex = index
                            currentOnSelected(index)
                        }
                    }
                    detectDragGesturesAfterLongPress(
                        onDragStart = { position ->
                            dragSelectedIndex = currentSelectedIndex
                            draggingSelector = true
                            updateDrag(position.y)
                        },
                        onDragEnd = { draggingSelector = false },
                        onDragCancel = { draggingSelector = false },
                    ) { change, _ ->
                        change.consume()
                        updateDrag(change.position.y)
                    }
                }
                .padding(vertical = 4.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val contentColor by animateColorAsState(
                    targetValue = if (selected) palette.accent else palette.secondaryInk,
                    animationSpec = spring(stiffness = 520f),
                    label = "navContentColor",
                )
                val contentScale by animateFloatAsState(
                    targetValue = if (selected) 1.06f else 1f,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
                    label = "navContentScale",
                )

                Column(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clip(RoundedRectangle(22.dp))
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                        ) { onSelected(index) }
                        .semantics { role = Role.Tab }
                        .graphicsLayer {
                            scaleX = contentScale
                            scaleY = contentScale
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        if (item.iconRes != 0) {
                            PackResourceIcon(
                                resourceId = item.iconRes,
                                contentDescription = item.label,
                                color = contentColor,
                                modifier = Modifier.size(26.dp),
                            )
                        } else {
                            Image(
                                painter = rememberVectorPainter(item.icon),
                                contentDescription = item.label,
                                colorFilter = ColorFilter.tint(contentColor),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        text = item.label,
                        style = GlassTypography.Label.copy(color = contentColor),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 4.dp)
                .graphicsLayer {
                    translationY = selectorY
                    scaleX = selectorScale
                    scaleY = selectorScale
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, navBackdrop),
                    shape = { RoundedRectangle(22.dp) },
                    effects = {
                        lens(
                            refractionHeight = 10.dp.toPx(),
                            refractionAmount = 14.dp.toPx(),
                            chromaticAberration = true,
                        )
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(alpha = 0.55f) },
                    innerShadow = { InnerShadow(radius = 7.dp, alpha = 0.75f) },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.07f))
                    },
                )
                .height(itemHeight)
                .fillMaxWidth(),
        )
    }
}
