package dev.liquid.clickgui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton

@Composable
fun LiquidFloatingButton(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = FloatingButtonConfig.DEFAULT_SIZE_DP,
    onDrag: (Offset) -> Unit = {},
) {
    val palette = GlassPalette()
    val buttonSizeDp = FloatingButtonConfig.coerceSize(sizeDp)
    val mainButtonSize = (buttonSizeDp + 10).dp
    CompositionLocalProvider(LocalGlassPalette provides palette) {
        val backdrop = rememberLayerBackdrop()
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(onDrag) {
                    detectDragGesturesAfterLongPress { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                }
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            ) {
                Box(Modifier.fillMaxSize().background(Color.White))
            }
            LiquidButton(
                onClick = onExpand,
                backdrop = backdrop,
                modifier = Modifier.size(mainButtonSize),
                tint = palette.accent,
                surfaceColor = palette.glassSurface,
                buttonHeight = mainButtonSize,
                horizontalPadding = 0.dp,
            ) {
                GlassIcon(
                    icon = Icons.Rounded.Tune,
                    contentDescription = "Open ClickGUI",
                    color = palette.ink,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}
