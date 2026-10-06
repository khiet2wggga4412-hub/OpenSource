package com.example.md3clickgui.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

object NexusMotion {

    const val DurationMillis = 300

    private val EnterEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    private val ExitEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> enterSpec(): FiniteAnimationSpec<T> = tween(DurationMillis, easing = EnterEasing)

    fun <T> exitSpec(): FiniteAnimationSpec<T> = tween(DurationMillis, easing = ExitEasing)

    fun <T> feedbackSpec(): FiniteAnimationSpec<T> = enterSpec()

    fun colorSpec(): AnimationSpec<Color> = tween(DurationMillis, easing = EnterEasing)

    fun dpSpec(): FiniteAnimationSpec<Dp> = tween(DurationMillis, easing = EnterEasing)
}
