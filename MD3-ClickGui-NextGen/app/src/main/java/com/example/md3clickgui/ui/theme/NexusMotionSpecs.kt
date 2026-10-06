package com.example.md3clickgui.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Motion specs for value types the Material 3 motion scheme does not cover.
 *
 * `MaterialTheme.motionScheme.*Spec<T>()` is generic over the animated value, but each of its tokens
 * is a spring that Material 3 parameterises identically regardless of type — the type parameter only
 * has to match what the animation wrapper asks for. `animateColorAsState` wants
 * `AnimationSpec<Color>` and `animateDpAsState` wants `AnimationSpec<Dp>`, so those two call sites
 * rebuild the same spring for their type instead of taking the `Float` instance.
 *
 * The damping and stiffness are the Material 3 values as compiled into
 * `androidx.compose.material3.tokens.StandardMotionTokens` (which is `@RestrictTo` and therefore not
 * callable from app code):
 *
 * | token           | damping ratio | stiffness |
 * |-----------------|---------------|-----------|
 * | default spatial | 0.9           | 700       |
 * | fast spatial    | 0.9           | 1400      |
 * | default effects | 0.9           | 1600      |
 * | fast effects    | 0.9           | 3800      |
 *
 * `MotionScheme.standard()` builds exactly these; see [NexusMotion] for the full token table.
 */

/** M3 default spatial, for colour changes that carry position meaning (a track filling up). */
fun <T> m3DefaultSpatialSpring(): AnimationSpec<T> =
    spring(dampingRatio = StandardSpatialDamping, stiffness = StandardSpatialStiffness)

/** M3 fast spatial, for tap-sized movement. */
fun <T> m3FastSpatialSpring(): AnimationSpec<T> =
    spring(dampingRatio = StandardSpatialDamping, stiffness = FastSpatialStiffness)

/** M3 default effects, for colour and opacity. */
fun <T> m3DefaultEffectsSpring(): AnimationSpec<T> =
    spring(dampingRatio = StandardSpatialDamping, stiffness = StandardEffectsStiffness)

/** M3 fast effects, for colour changes that answer a tap. */
fun <T> m3FastEffectsSpring(): AnimationSpec<T> =
    spring(dampingRatio = StandardSpatialDamping, stiffness = FastEffectsStiffness)

/** Convenience aliases so a call site can name the value type it animates. */
fun colorSpring(fast: Boolean = true): AnimationSpec<Color> =
    if (fast) m3FastEffectsSpring() else m3DefaultEffectsSpring()

fun dpSpring(): AnimationSpec<Dp> = m3DefaultSpatialSpring()

private const val StandardSpatialDamping = Spring.DampingRatioNoBouncy
private const val StandardSpatialStiffness = 700f
private const val FastSpatialStiffness = 1400f
private const val StandardEffectsStiffness = 1600f
private const val FastEffectsStiffness = 3800f
