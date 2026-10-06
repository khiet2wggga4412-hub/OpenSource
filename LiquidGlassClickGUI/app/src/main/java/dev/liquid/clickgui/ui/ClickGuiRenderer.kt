package dev.liquid.clickgui.ui

import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.lang.ref.WeakReference

enum class ClickGuiBackgroundMode {

    SolidWhite,

    GaussianBlur,
}

@Stable
object ClickGuiRenderer {
    var backgroundMode by mutableStateOf(ClickGuiBackgroundMode.SolidWhite)
        private set

    var gaussianBlurRadiusDp by mutableFloatStateOf(DEFAULT_BLUR_RADIUS_DP)
        private set

    private var blurredHostContent = WeakReference<View>(null)

    fun useSolidWhite() {
        backgroundMode = ClickGuiBackgroundMode.SolidWhite
    }

    fun useGaussianBlur(radiusDp: Float = DEFAULT_BLUR_RADIUS_DP) {
        gaussianBlurRadiusDp = radiusDp.coerceIn(MIN_BLUR_RADIUS_DP, MAX_BLUR_RADIUS_DP)
        backgroundMode = ClickGuiBackgroundMode.GaussianBlur
    }

    fun selectBackgroundMode(mode: ClickGuiBackgroundMode) {
        backgroundMode = mode
    }

    internal fun updateHostEffect(activity: Activity, overlayExpanded: Boolean) {
        val content = activity.findViewById<View>(android.R.id.content) ?: return
        val previous = blurredHostContent.get()
        if (previous !== null && previous !== content) {
            previous.setRenderEffect(null)
        }

        val shouldBlur = overlayExpanded && backgroundMode == ClickGuiBackgroundMode.GaussianBlur
        if (shouldBlur) {
            val density = activity.resources.displayMetrics.density
            val radiusPx = (gaussianBlurRadiusDp * density).coerceAtLeast(1f)
            content.setRenderEffect(
                RenderEffect.createBlurEffect(
                    radiusPx,
                    radiusPx,
                    Shader.TileMode.CLAMP,
                ),
            )
            blurredHostContent = WeakReference(content)
        } else {
            content.setRenderEffect(null)
            blurredHostContent.clear()
        }
    }

    internal fun clearHostEffect() {
        blurredHostContent.get()?.setRenderEffect(null)
        blurredHostContent.clear()
    }

    private const val DEFAULT_BLUR_RADIUS_DP = 24f
    private const val MIN_BLUR_RADIUS_DP = 4f
    private const val MAX_BLUR_RADIUS_DP = 48f
}
