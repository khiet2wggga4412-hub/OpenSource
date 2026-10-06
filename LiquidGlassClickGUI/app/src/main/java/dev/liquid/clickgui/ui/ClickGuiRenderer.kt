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

/** ClickGUI 根背景的渲染模式。 */
enum class ClickGuiBackgroundMode {
    /** 默认模式：GUI 根层只绘制一次纯白背景。 */
    SolidWhite,

    /** Android 12+：模糊宿主 Activity 的内容层，再覆盖半透明白色玻璃层。 */
    GaussianBlur,
}

/**
 * ClickGUI 的统一渲染配置入口。
 *
 * 外部可调用 [useSolidWhite] 或 [useGaussianBlur] 切换背景。宿主注入层只会模糊
 * android.R.id.content，不会把后挂载到 DecorView 的 Compose GUI 一起模糊。
 */
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

    /** 由宿主悬浮层在主面板展开状态变化或渲染配置变化时调用。 */
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

    /** detach、收起或异常退出时必须调用，防止游戏画面残留模糊。 */
    internal fun clearHostEffect() {
        blurredHostContent.get()?.setRenderEffect(null)
        blurredHostContent.clear()
    }

    private const val DEFAULT_BLUR_RADIUS_DP = 24f
    private const val MIN_BLUR_RADIUS_DP = 4f
    private const val MAX_BLUR_RADIUS_DP = 48f
}
