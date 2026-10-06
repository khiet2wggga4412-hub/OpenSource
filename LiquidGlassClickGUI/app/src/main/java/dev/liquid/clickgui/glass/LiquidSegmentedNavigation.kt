package dev.liquid.clickgui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import dev.liquid.clickgui.ui.GlassTypography
import dev.liquid.clickgui.ui.LocalGlassPalette

@Suppress("UNUSED_PARAMETER")
@Composable
fun LiquidSegmentedNavigation(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return

    val palette = LocalGlassPalette.current
    val safeSelectedIndex = selectedIndex.coerceIn(options.indices)
    val trackShape = RoundedCornerShape(17.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(trackShape)
            .background(palette.glassSurfaceStrong.copy(alpha = 0.54f))
            .border(1.dp, Color.White.copy(alpha = 0.30f), trackShape),
    ) {
        val itemWidth = maxWidth / options.size
        val selectorX by animateDpAsState(
            targetValue = itemWidth * safeSelectedIndex,
            animationSpec = spring(dampingRatio = 0.84f, stiffness = 540f),
            label = "segmentedNavigationX",
        )

        Box(
            modifier = Modifier
                .width(itemWidth)
                .fillMaxHeight()
                .graphicsLayer { translationX = selectorX.toPx() }
                .padding(4.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.62f))
                .border(
                    width = 1.dp,
                    color = palette.accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(13.dp),
                ),
        )

        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, option ->
                val selected = index == safeSelectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (selected) palette.accent else palette.secondaryInk,
                    animationSpec = spring(stiffness = 560f),
                    label = "segmentedNavigationText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.RadioButton,
                        ) { onSelected(index) }
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = option,
                        style = GlassTypography.Label.copy(
                            color = textColor,
                            fontSize = if (options.size >= 7) 9.sp else 11.sp,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
