package com.example.md3clickgui.ui.theme

/**
 * Motion is taken from Material 3's own motion scheme, with no project-local curves or durations.
 *
 * The scheme is installed once in [NexusTheme] via `MotionScheme.standard()` and read at each call
 * site through `MaterialTheme.motionScheme`. Material 3 defines motion as spring physics rather
 * than easing curves; the token values behind `standard()` are (from
 * `androidx.compose.material3.tokens.StandardMotionTokens`):
 *
 * | token            | damping ratio | stiffness |
 * |------------------|---------------|-----------|
 * | default spatial  | 0.9           | 700       |
 * | fast spatial     | 0.9           | 1400      |
 * | slow spatial     | 0.9           | 300       |
 * | default effects  | 0.9           | 1600      |
 * | fast effects     | 0.9           | 3800      |
 * | slow effects     | 0.9           | 300       |
 *
 * Choosing a spec:
 * - **spatial** — anything that moves, resizes or changes position.
 * - **effects** — colour and opacity changes.
 * - **fast** — small, near, frequent: a tap response, a short distance.
 * - **default** — the normal case: a panel, a section, a screen-level transition.
 * - **slow** — large distances the user should be able to follow.
 *
 * The spec methods are generic (`<T>`), so each call site pulls the exact type it needs — a slide
 * takes `defaultSpatialSpec<IntOffset>()`, a fade takes `defaultEffectsSpec<Float>()`. There is no
 * shared constant to keep in sync, which is why this file has no values in it.
 */
object NexusMotion
