package com.example.md3clickgui.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Motion for the whole app: one duration, two Material 3 easing curves.
 *
 * The duration is [DurationMillis] everywhere — there are no per-animation timings to keep in sync.
 *
 * ## Why curves rather than the motion scheme's springs
 *
 * `MotionScheme.standard()` describes motion as springs, and `MaterialTheme.motionScheme` still
 * exposes them. They are deliberately **not** used here: a spring starts from zero velocity, so a
 * toggle or a colour change visibly "spools up" before it reads as a response, which is the lag this
 * project wanted gone. A tween on a decelerating curve starts moving on the first frame.
 *
 * The curve values are the Material 3 tokens from
 * `androidx.compose.material3.tokens.MotionTokens`, so the timing still follows the platform
 * guidelines:
 *
 * | curve                      | value                | used for                         |
 * |----------------------------|----------------------|----------------------------------|
 * | `EasingStandardDecelerate` | `(0.0, 0.0, 0.0, 1)` | entering, and every tap response |
 * | `EasingStandard`           | `(0.2, 0.0, 0.0, 1)` | leaving                          |
 *
 * ## Choosing a spec
 *
 * - [enterSpec] — appearing, growing, sliding in, and any tap response that should read instantly.
 * - [exitSpec] — disappearing, shrinking, sliding out.
 * - [feedbackSpec] — alias of [enterSpec] for tap responses; named so the intent is visible.
 * - [colorSpec] / [dpSpec] — the same timing for `animateColorAsState` and `animateDpAsState`.
 */
object NexusMotion {

    /** The single duration every animation uses. */
    const val DurationMillis = 300

    private val EnterEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    private val ExitEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Enters and tap responses: starts immediately, settles onto the target. */
    fun <T> enterSpec(): FiniteAnimationSpec<T> = tween(DurationMillis, easing = EnterEasing)

    /** Exits: standard easing, so something leaving does not look slower than it arriving. */
    fun <T> exitSpec(): FiniteAnimationSpec<T> = tween(DurationMillis, easing = ExitEasing)

    /** A tap response. Same timing as [enterSpec]; the name records the intent at the call site. */
    fun <T> feedbackSpec(): FiniteAnimationSpec<T> = enterSpec()

    /** Colour is not a `Number`, so `animateColorAsState` needs its own spec instance. */
    fun colorSpec(): AnimationSpec<Color> = tween(DurationMillis, easing = EnterEasing)

    /** `animateDpAsState` likewise needs an explicit `Dp` instance. */
    fun dpSpec(): FiniteAnimationSpec<Dp> = tween(DurationMillis, easing = EnterEasing)
}
