package dev.liquid.clickgui.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class GlassPalette(
    val ink: Color = Color(0xFF101820),
    val secondaryInk: Color = Color(0xFF52616B),
    val accent: Color = Color(0xFF078BFF),
    val active: Color = Color(0xFF27C764),
    val warning: Color = Color(0xFFFFB020),
    val danger: Color = Color(0xFFFF5268),
    val scrim: Color = Color(0x30001828),
    // 纯白根背景上使用轻微蓝灰玻璃色，保持层级而不重复绘制整张白底。
    val glassSurface: Color = Color(0x78EAF2F8),
    val glassSurfaceStrong: Color = Color(0xEDF8FBFD),
    val hairline: Color = Color(0x33101820),
)

object GlassTypography {
    val Brand = TextStyle(
        color = Color(0xFF101820),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 19.sp,
        letterSpacing = 0.sp,
    )
    val Title = TextStyle(
        color = Color(0xFF101820),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
    )
    val Body = TextStyle(
        color = Color(0xFF101820),
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
    )
    val Label = TextStyle(
        color = Color(0xFF101820),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.sp,
    )
}

val LocalGlassPalette = staticCompositionLocalOf { GlassPalette() }
