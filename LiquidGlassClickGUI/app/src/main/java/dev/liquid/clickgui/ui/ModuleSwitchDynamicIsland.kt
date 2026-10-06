package dev.liquid.clickgui.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import dev.liquid.clickgui.BuildConfig
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class ModuleSwitchNotice(
    val sequence: Long,
    val moduleName: String,
    val enabled: Boolean,
)

@Stable
class ModuleSwitchNoticeCoordinator {
    var notices by mutableStateOf<List<ModuleSwitchNotice>>(emptyList())
        private set

    val currentNotice: ModuleSwitchNotice?
        get() = notices.lastOrNull()

    private var sequence = 0L

    fun publish(moduleName: String, enabled: Boolean) {
        notices = listOf(ModuleSwitchNotice(++sequence, moduleName, enabled))
    }

    fun dismiss(sequence: Long) {
        notices = notices.filterNot { it.sequence == sequence }
    }
}

private sealed interface IslandContent {
    val key: Long

    data object Idle : IslandContent {
        override val key = 0L
    }

    data class Notice(val value: ModuleSwitchNotice) : IslandContent {
        override val key = value.sequence
    }
}

@Composable
fun ModuleSwitchDynamicIsland(
    coordinator: ModuleSwitchNoticeCoordinator,
    modifier: Modifier = Modifier,
    isFullScreenHost: Boolean = true,
) {
    val notice = coordinator.currentNotice
    val content: IslandContent = notice?.let(IslandContent::Notice) ?: IslandContent.Idle

    LaunchedEffect(notice?.sequence) {
        val active = notice ?: return@LaunchedEffect
        delay(if (active.enabled) RUNNING_NOTICE_MILLIS else COMPLETE_NOTICE_MILLIS)
        coordinator.dismiss(active.sequence)
    }

    val darkTheme = isSystemInDarkTheme()
    val backdrop = rememberCanvasBackdrop(
        if (darkTheme) DarkIslandBackdropPainter else LightIslandBackdropPainter,
    )
    val baseAlpha = if (isFullScreenHost) 0.40f else 0.75f
    val topColor by animateColorAsState(
        if (darkTheme) Color(0xFF34383D).copy(alpha = baseAlpha)
        else Color(0xFFF4F6F8).copy(alpha = baseAlpha),
        tween(260),
        label = "islandTopColor",
    )
    val bottomColor by animateColorAsState(
        if (darkTheme) Color(0xFF1D2024).copy(alpha = baseAlpha)
        else Color(0xFFDDE1E5).copy(alpha = baseAlpha),
        tween(260),
        label = "islandBottomColor",
    )
    val edgeLight = rememberInfiniteTransition(label = "islandEdgeLight")
    val edgePhase by edgeLight.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_800, easing = LinearEasing)),
        label = "islandEdgePhase",
    )

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .widthIn(min = 190.dp, max = 620.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(28.dp) },
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = if (darkTheme) 0.015f else 0.035f,
                            contrast = 1.06f,
                            saturation = 0.82f,
                        )
                        blur(14.dp.toPx())
                        lens(
                            refractionHeight = 8.dp.toPx(),
                            refractionAmount = 12.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = false,
                        )
                    },
                    highlight = {
                        Highlight(
                            width = 0.45.dp,
                            blurRadius = 1.4.dp,
                            alpha = if (darkTheme) 0.46f else 0.32f,
                            style = com.kyant.backdrop.highlight.HighlightStyle.Ambient,
                        )
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 4.dp,
                            offset = DpOffset(0.dp, 1.dp),
                            color = Color.White.copy(alpha = if (darkTheme) 0.13f else 0.24f),
                            alpha = 0.58f,
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 13.dp,
                            offset = DpOffset(0.dp, 4.dp),
                            color = Color.Black.copy(alpha = 0.18f),
                        )
                    },
                    onDrawSurface = {
                        drawRect(Brush.verticalGradient(listOf(topColor, bottomColor)))
                    },
                )
                .animateContentSize(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 430f),
                )
                .drawWithContent {
                    drawContent()
                    val inset = 1.25.dp.toPx()
                    val borderSize = Size(
                        width = (size.width - inset * 2f).coerceAtLeast(0f),
                        height = (size.height - inset * 2f).coerceAtLeast(0f),
                    )
                    val corner = CornerRadius(borderSize.height / 2f, borderSize.height / 2f)
                    val angle = edgePhase * (Math.PI * 2.0)
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val reach = size.width.coerceAtLeast(size.height) * 0.72f
                    val edgeBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF238BFF),
                            Color(0xFF8FD2FF),
                            Color.White,
                            Color(0xFFB7E4FF),
                            Color(0xFF238BFF),
                        ),
                        start = center - direction * reach,
                        end = center + direction * reach,
                    )
                    drawRoundRect(
                        brush = edgeBrush,
                        topLeft = Offset(inset, inset),
                        size = borderSize,
                        cornerRadius = corner,
                        style = Stroke(width = 5.dp.toPx()),
                        alpha = 0.36f,
                    )
                    drawRoundRect(
                        brush = edgeBrush,
                        topLeft = Offset(inset, inset),
                        size = borderSize,
                        cornerRadius = corner,
                        style = Stroke(width = 1.35.dp.toPx()),
                    )
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = content,
                transitionSpec = {
                    (slideInVertically(tween(260, easing = FastOutSlowInEasing)) { -it } +
                        fadeIn(tween(190)))
                        .togetherWith(
                            slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it } +
                                fadeOut(tween(160)),
                        )
                },
                contentKey = { it.key },
                label = "islandVerticalMessage",
            ) { item ->
                IslandMessage(item = item, darkTheme = darkTheme)
            }
        }
    }
}

@Composable
private fun IslandMessage(item: IslandContent, darkTheme: Boolean) {
    val textColor = if (darkTheme) Color(0xFFF5F7FA) else Color(0xFF15191E)
    val secondary = if (darkTheme) Color(0xFFABB3BC) else Color(0xFF555E68)
    val notice = (item as? IslandContent.Notice)?.value
    val accent = when {
        notice == null -> Color(0xFF9BCBFF)
        notice.enabled -> Color(0xFF83BAE8)
        else -> Color(0xFFB8C0C8)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIcon(
            icon = when {
                notice == null -> Icons.Rounded.Tune
                notice.enabled -> Icons.Rounded.Check
                else -> Icons.Rounded.Close
            },
            contentDescription = null,
            color = accent,
            modifier = Modifier.size(17.dp),
        )
        BasicText(
            text = notice?.moduleName ?: "Glass Client",
            style = TextStyle(
                color = textColor,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            ),
            maxLines = 1,
            softWrap = false,
        )
        BasicText(
            text = when {
                notice == null -> "v${BuildConfig.VERSION_NAME}"
                notice.enabled -> "ON"
                else -> "OFF"
            },
            style = TextStyle(
                color = if (notice == null) secondary else accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}

private val DarkIslandBackdropPainter: DrawScope.() -> Unit = {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF3A3E43), Color(0xFF202328), Color(0xFF15171A)),
        ),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.055f),
        radius = size.minDimension * 0.86f,
        center = Offset(size.width * 0.22f, 0f),
    )
}

private val LightIslandBackdropPainter: DrawScope.() -> Unit = {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFFF8FAFC), Color(0xFFE5E9ED), Color(0xFFD6DADE)),
        ),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.34f),
        radius = size.minDimension,
        center = Offset(size.width * 0.20f, 0f),
    )
}

private const val RUNNING_NOTICE_MILLIS = 5_200L
private const val COMPLETE_NOTICE_MILLIS = 3_600L
