package dev.liquid.clickgui.xposed

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import de.robv.android.xposed.XposedBridge

enum class HostLoadMode(val displayName: String) {
    LSPOSED_STUB_APP("LOX / Xposed（StubApp 壳后）"),
}

interface HostHookCallbacks {
    fun onHostAttached(
        context: Context,
        classLoader: ClassLoader,
        mode: HostLoadMode,
    ) = Unit

    fun onHostHookFailed(context: Context, mode: HostLoadMode, error: Throwable) = Unit
    fun onMainActivityCreated(activity: Activity) = Unit
    fun onMainActivityResumed(activity: Activity) = Unit
    fun onMainActivityDestroyed(activity: Activity) = Unit
    fun onKeyEvent(event: KeyEvent): Boolean = false
    fun onGenericMotionEvent(event: MotionEvent): Boolean = false
    fun onTouchEvent(event: MotionEvent): Boolean = false
}

/**
 * Hook 与具体功能之间的稳定边界。回调运行在目标进程，默认实现不会拦截输入。
 */
object HostHookBridge {
    private object NoOpCallbacks : HostHookCallbacks

    @Volatile
    private var callbacks: HostHookCallbacks = NoOpCallbacks

    @JvmStatic
    fun register(callbacks: HostHookCallbacks) {
        this.callbacks = callbacks
    }

    @JvmStatic
    fun clear() {
        callbacks = NoOpCallbacks
    }

    internal fun onHostAttached(
        context: Context,
        classLoader: ClassLoader,
        mode: HostLoadMode,
    ) {
        dispatch { callbacks.onHostAttached(context, classLoader, mode) }
    }

    internal fun onHostHookFailed(context: Context, mode: HostLoadMode, error: Throwable) {
        dispatch { callbacks.onHostHookFailed(context, mode, error) }
    }

    internal fun onMainActivityCreated(activity: Activity) {
        dispatch { callbacks.onMainActivityCreated(activity) }
    }

    internal fun onMainActivityResumed(activity: Activity) {
        dispatch { callbacks.onMainActivityResumed(activity) }
    }

    internal fun onMainActivityDestroyed(activity: Activity) {
        dispatch { callbacks.onMainActivityDestroyed(activity) }
    }

    internal fun onKeyEvent(event: KeyEvent): Boolean =
        runCatching { callbacks.onKeyEvent(event) }.getOrDefault(false)

    internal fun onGenericMotionEvent(event: MotionEvent): Boolean =
        runCatching { callbacks.onGenericMotionEvent(event) }.getOrDefault(false)

    internal fun onTouchEvent(event: MotionEvent): Boolean =
        runCatching { callbacks.onTouchEvent(event) }.getOrDefault(false)

    private inline fun dispatch(block: () -> Unit) {
        runCatching(block).onFailure {
            XposedBridge.log("LiquidClickGUI host callback failed: ${it.message}")
            XposedBridge.log(it)
        }
    }
}
