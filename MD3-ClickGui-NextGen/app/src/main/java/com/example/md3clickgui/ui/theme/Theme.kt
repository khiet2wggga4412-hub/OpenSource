package com.example.md3clickgui.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlin.math.pow

/** Default theme colors shown above the custom color drawer. */
val NexusThemeSwatchColors = listOf(
    Color(0xFF176B60),
    Color(0xFF315F90),
    Color(0xFF6750A4),
    Color(0xFF95513C)
)

val NexusThemeSwatchNames = listOf(
    "Nexus", "Ocean", "Violet", "Sunset"
)

/** Hex values matching [NexusThemeSwatchColors], used by the color picker state. */
val NexusThemeSwatchHexes = listOf(
    "#176B60", "#315F90", "#6750A4", "#95513C"
)

private data class NexusPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color
)

private fun mix(base: Color, target: Color, amount: Float): Color = Color(
    base.red + (target.red - base.red) * amount,
    base.green + (target.green - base.green) * amount,
    base.blue + (target.blue - base.blue) * amount,
    1f
)

private fun relativeLuminance(color: Color): Double {
    fun linear(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
}

private fun readableOn(color: Color): Color {
    val darkText = Color(0xFF17201D)
    val backgroundLuminance = relativeLuminance(color)
    fun contrast(foreground: Color): Double {
        val foregroundLuminance = relativeLuminance(foreground)
        val lighter = maxOf(backgroundLuminance, foregroundLuminance)
        val darker = minOf(backgroundLuminance, foregroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }
    return if (contrast(darkText) >= contrast(Color.White)) darkText else Color.White
}

private fun paletteFor(index: Int, darkMode: Boolean, customColorHex: String?): NexusPalette {
    val swatch = customColorHex?.let(::parseHexColor)
        ?: NexusThemeSwatchColors[index.coerceIn(NexusThemeSwatchColors.indices)]
    return if (darkMode) {
        val primary = mix(swatch, Color.White, 0.34f)
        val primaryContainer = mix(swatch, Color.Black, 0.48f)
        val secondary = mix(swatch, Color.White, 0.52f)
        val secondaryContainer = mix(swatch, Color.Black, 0.34f)
        val tertiary = mix(swatch, Color.White, 0.66f)
        val tertiaryContainer = mix(swatch, Color.Black, 0.30f)
        NexusPalette(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = readableOn(primaryContainer),
            inversePrimary = mix(swatch, Color.White, 0.58f),
            secondary = secondary,
            onSecondary = readableOn(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = readableOn(secondaryContainer),
            tertiary = tertiary,
            onTertiary = readableOn(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = readableOn(tertiaryContainer),
            background = mix(swatch, Color.Black, 0.93f),
            onBackground = mix(swatch, Color.White, 0.88f),
            surface = mix(swatch, Color.Black, 0.86f),
            surfaceVariant = mix(swatch, Color.Black, 0.47f),
            onSurface = mix(swatch, Color.White, 0.90f),
            onSurfaceVariant = mix(swatch, Color.White, 0.68f),
            inverseSurface = mix(swatch, Color.White, 0.94f),
            inverseOnSurface = mix(swatch, Color.Black, 0.86f),
            surfaceBright = mix(swatch, Color.Black, 0.76f),
            surfaceDim = mix(swatch, Color.Black, 0.93f),
            surfaceContainerLowest = mix(swatch, Color.Black, 0.97f),
            surfaceContainerLow = mix(swatch, Color.Black, 0.76f),
            surfaceContainer = mix(swatch, Color.Black, 0.72f),
            surfaceContainerHigh = mix(swatch, Color.Black, 0.66f),
            surfaceContainerHighest = mix(swatch, Color.Black, 0.60f),
            outline = mix(swatch, Color.White, 0.50f),
            outlineVariant = mix(swatch, Color.White, 0.30f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        val primary = mix(swatch, Color.Black, 0.18f)
        val primaryContainer = mix(swatch, Color.White, 0.78f)
        val secondary = mix(swatch, Color.Black, 0.34f)
        val secondaryContainer = mix(swatch, Color.White, 0.68f)
        val tertiary = mix(swatch, Color.White, 0.18f)
        val tertiaryContainer = mix(swatch, Color.White, 0.82f)
        NexusPalette(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = readableOn(primaryContainer),
            inversePrimary = mix(swatch, Color.White, 0.42f),
            secondary = secondary,
            onSecondary = readableOn(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = readableOn(secondaryContainer),
            tertiary = tertiary,
            onTertiary = readableOn(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = readableOn(tertiaryContainer),
            background = mix(swatch, Color.White, 0.96f),
            onBackground = mix(swatch, Color.Black, 0.82f),
            surface = mix(swatch, Color.White, 0.98f),
            surfaceVariant = mix(swatch, Color.White, 0.72f),
            onSurface = mix(swatch, Color.Black, 0.82f),
            onSurfaceVariant = mix(swatch, Color.Black, 0.54f),
            inverseSurface = mix(swatch, Color.Black, 0.86f),
            inverseOnSurface = Color.White,
            surfaceBright = Color.White,
            surfaceDim = mix(swatch, Color.White, 0.88f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = mix(swatch, Color.White, 0.94f),
            surfaceContainer = mix(swatch, Color.White, 0.91f),
            surfaceContainerHigh = mix(swatch, Color.White, 0.87f),
            surfaceContainerHighest = mix(swatch, Color.White, 0.83f),
            outline = mix(swatch, Color.Black, 0.38f),
            outlineVariant = mix(swatch, Color.White, 0.56f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}

private fun parseHexColor(hex: String): Color? {
    val value = hex.removePrefix("#").toLongOrNull(16) ?: return null
    if (value > 0xFFFFFF) return null
    return Color(
        ((value shr 16) and 0xFF) / 255f,
        ((value shr 8) and 0xFF) / 255f,
        (value and 0xFF) / 255f,
        1f
    )
}

@Composable
fun NexusTheme(
    darkMode: Boolean = false,
    dynamicColor: Boolean = false,
    themeIndex: Int = 0,
    customColorHex: String? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Manual swatches and the custom picker must win over system dynamic colors.
    // Dynamic color remains available when no manual theme override is active.
    val hasManualTheme = customColorHex != null || themeIndex != 0
    val useDynamic = dynamicColor && !hasManualTheme && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val colors = when {
        useDynamic && darkMode -> remember(context) { dynamicDarkColorScheme(context) }
        useDynamic -> remember(context) { dynamicLightColorScheme(context) }
        else -> remember(darkMode, themeIndex, customColorHex) {
            val palette = paletteFor(themeIndex, darkMode, customColorHex)
            if (darkMode) darkColorScheme(
                primary = palette.primary,
                onPrimary = palette.onPrimary,
                primaryContainer = palette.primaryContainer,
                onPrimaryContainer = palette.onPrimaryContainer,
                inversePrimary = palette.inversePrimary,
                secondary = palette.secondary,
                onSecondary = palette.onSecondary,
                secondaryContainer = palette.secondaryContainer,
                onSecondaryContainer = palette.onSecondaryContainer,
                tertiary = palette.tertiary,
                onTertiary = palette.onTertiary,
                tertiaryContainer = palette.tertiaryContainer,
                onTertiaryContainer = palette.onTertiaryContainer,
                background = palette.background,
                onBackground = palette.onBackground,
                surface = palette.surface,
                surfaceVariant = palette.surfaceVariant,
                onSurface = palette.onSurface,
                onSurfaceVariant = palette.onSurfaceVariant,
                inverseSurface = palette.inverseSurface,
                inverseOnSurface = palette.inverseOnSurface,
                error = palette.error,
                onError = palette.onError,
                errorContainer = palette.errorContainer,
                onErrorContainer = palette.onErrorContainer,
                outline = palette.outline,
                outlineVariant = palette.outlineVariant,
                surfaceBright = palette.surfaceBright,
                surfaceDim = palette.surfaceDim,
                surfaceContainerLowest = palette.surfaceContainerLowest,
                surfaceContainerLow = palette.surfaceContainerLow,
                surfaceContainer = palette.surfaceContainer,
                surfaceContainerHigh = palette.surfaceContainerHigh,
                surfaceContainerHighest = palette.surfaceContainerHighest
            ) else lightColorScheme(
                primary = palette.primary,
                onPrimary = palette.onPrimary,
                primaryContainer = palette.primaryContainer,
                onPrimaryContainer = palette.onPrimaryContainer,
                inversePrimary = palette.inversePrimary,
                secondary = palette.secondary,
                onSecondary = palette.onSecondary,
                secondaryContainer = palette.secondaryContainer,
                onSecondaryContainer = palette.onSecondaryContainer,
                tertiary = palette.tertiary,
                onTertiary = palette.onTertiary,
                tertiaryContainer = palette.tertiaryContainer,
                onTertiaryContainer = palette.onTertiaryContainer,
                background = palette.background,
                onBackground = palette.onBackground,
                surface = palette.surface,
                surfaceVariant = palette.surfaceVariant,
                onSurface = palette.onSurface,
                onSurfaceVariant = palette.onSurfaceVariant,
                inverseSurface = palette.inverseSurface,
                inverseOnSurface = palette.inverseOnSurface,
                error = palette.error,
                onError = palette.onError,
                errorContainer = palette.errorContainer,
                onErrorContainer = palette.onErrorContainer,
                outline = palette.outline,
                outlineVariant = palette.outlineVariant,
                surfaceBright = palette.surfaceBright,
                surfaceDim = palette.surfaceDim,
                surfaceContainerLowest = palette.surfaceContainerLowest,
                surfaceContainerLow = palette.surfaceContainerLow,
                surfaceContainer = palette.surfaceContainer,
                surfaceContainerHigh = palette.surfaceContainerHigh,
                surfaceContainerHighest = palette.surfaceContainerHighest
            )
        }
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        // Standard motion, not expressive: the expressive scheme drives M3 components with
        // spring-physics specs, so indicators and sliders carried their own bounce regardless of
        // NexusMotion. Everything now shares the one non-overshooting ease.
        motionScheme = MotionScheme.standard(),
    ) {
        CompositionLocalProvider(LocalRippleConfiguration provides null, content = content)
    }
}
