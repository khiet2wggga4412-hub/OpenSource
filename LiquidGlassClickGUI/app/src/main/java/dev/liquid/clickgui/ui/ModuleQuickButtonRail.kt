package dev.liquid.clickgui.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liquid.clickgui.model.ModuleUiModel

private val QuickButtonBlue = Color(0xFF078BFF)
private val QuickButtonInk = Color(0xFF075EA8)

@Composable
fun ModuleQuickFloatingButton(
    module: ModuleUiModel,
    controller: ClickGuiController,
    sizeDp: Int,
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonSizeDp = FloatingButtonConfig.coerceSize(sizeDp)
    val displayName = if (controller.uiLanguage == UiLanguage.Chinese) {
        module.name
    } else {
        module.englishName
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (module.enabled) {
            QuickButtonBlue
        } else {
            Color.White.copy(alpha = 0.66f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "quickFloatingBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (module.enabled) Color.White else QuickButtonInk,
        animationSpec = tween(durationMillis = 180),
        label = "quickFloatingContent",
    )
    val borderColor by animateColorAsState(
        targetValue = if (module.enabled) {
            Color.White.copy(alpha = 0.72f)
        } else {
            QuickButtonBlue.copy(alpha = 0.46f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "quickFloatingBorder",
    )
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.94f
            module.enabled -> 1.025f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 560f),
        label = "quickFloatingScale",
    )
    Box(
        modifier = modifier
            .height(buttonSizeDp.dp)
            .widthIn(min = buttonSizeDp.dp)
            .wrapContentWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 180))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 7.dp,
                shape = CircleShape,
                ambientColor = QuickButtonBlue.copy(alpha = 0.18f),
                spotColor = QuickButtonBlue.copy(alpha = 0.24f),
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { controller.setEnabled(module, !module.enabled) },
            )
            .pointerInput(module.id, onDrag) {
                detectDragGesturesAfterLongPress { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
            .padding(horizontal = (buttonSizeDp * 0.34f).coerceAtLeast(11f).dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = displayName,
            transitionSpec = {
                fadeIn(tween(130)) togetherWith fadeOut(tween(90))
            },
            label = "quickFloatingLanguage",
        ) { name ->
            BasicText(
                text = name,
                style = TextStyle(
                    color = contentColor,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (buttonSizeDp * 0.30f).coerceIn(10.5f, 15f).sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
        }
    }
}
