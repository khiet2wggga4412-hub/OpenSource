package dev.liquid.clickgui.xposed

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal object InjectedHostRuntime : HostHookCallbacks {
    private val hostReady = AtomicBoolean(false)
    private val loading = AtomicBoolean(false)

    @Volatile
    private var hostClassLoader: ClassLoader? = null

    @Volatile
    private var loadMode = HostLoadMode.LSPOSED_STUB_APP

    override fun onHostAttached(
        context: Context,
        classLoader: ClassLoader,
        mode: HostLoadMode,
    ) {
        hostClassLoader = classLoader
        loadMode = mode
        hostReady.set(true)
        XposedBridge.log("LiquidClickGUI real host ClassLoader ready")
    }

    override fun onHostHookFailed(context: Context, mode: HostLoadMode, error: Throwable) {
        hostReady.set(false)
        HostLoadReporter.failure(context, mode, error)
    }

    override fun onMainActivityResumed(activity: Activity) {
        if (!hostReady.get()) return
        if (HostComposeOverlay.isAttachedTo(activity)) return
        if (!loading.compareAndSet(false, true)) return

        scheduleOverlayAttach(activity, attempt = 0)
    }

    private fun scheduleOverlayAttach(activity: Activity, attempt: Int) {
        val decorView = activity.window.decorView
        decorView.post {
            if (activity.isFinishing || activity.isDestroyed) {
                loading.set(false)
                return@post
            }

            if (decorView.windowToken == null) {
                if (attempt < MAX_WINDOW_TOKEN_ATTEMPTS) {
                    decorView.postDelayed(
                        { scheduleOverlayAttach(activity, attempt + 1) },
                        WINDOW_TOKEN_RETRY_DELAY_MS,
                    )
                } else {
                    loading.set(false)
                    HostLoadReporter.failure(
                        activity,
                        loadMode,
                        IllegalStateException("MainActivity window token was not attached"),
                    )
                }
                return@post
            }

            runCatching { attachOverlay(activity) }
                .onFailure { error ->
                    loading.set(false)
                    HostLoadReporter.failure(activity, loadMode, error)
                }
        }
    }

    private fun attachOverlay(activity: Activity) {
        val classLoader = checkNotNull(hostClassLoader) {
            "Real host ClassLoader is not available"
        }
        val resolvedActivity = Class.forName(
            activity.javaClass.name,
            false,
            classLoader,
        )
        check(resolvedActivity.isInstance(activity)) {
            "MainActivity was not resolved by the post-shell ClassLoader"
        }
        HostComposeOverlay.attach(activity) { result ->
            loading.set(false)
            result
                .onSuccess { HostLoadReporter.success(activity, loadMode) }
                .onFailure { error -> HostLoadReporter.failure(activity, loadMode, error) }
        }
    }

    override fun onMainActivityDestroyed(activity: Activity) {
        HostComposeOverlay.detach(activity)
        loading.set(false)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = HostComposeOverlay.onKeyEvent(event)

    override fun onTouchEvent(event: MotionEvent): Boolean = HostComposeOverlay.onTouchEvent(event)

    private const val MAX_WINDOW_TOKEN_ATTEMPTS = 30
    private const val WINDOW_TOKEN_RETRY_DELAY_MS = 50L
}

private object HostLoadReporter {
    private val lastMessage = AtomicReference<String?>(null)

    fun success(context: Context, mode: HostLoadMode) {
        show(context, "加载模式：${mode.displayName}\nUI：成功\n后端：已移除（仅界面）")
    }

    fun failure(context: Context, mode: HostLoadMode, error: Throwable) {
        XposedBridge.log(error)
        val reason = (error.message ?: error.javaClass.simpleName)
            .replace('\n', ' ')
            .take(96)
        show(context, "加载模式：${mode.displayName}\nUI：失败\n原因：$reason")
    }

    private fun show(context: Context, message: String) {
        XposedBridge.log("LiquidClickGUI ${message.replace('\n', ' ')}")
        if (lastMessage.getAndSet(message) == message) return
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext ?: context, message, Toast.LENGTH_LONG).show()
        }
    }
}
