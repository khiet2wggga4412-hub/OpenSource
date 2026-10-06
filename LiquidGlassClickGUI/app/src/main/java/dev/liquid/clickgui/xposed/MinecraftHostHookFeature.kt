package dev.liquid.clickgui.xposed

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 先安装 framework Activity 生命周期 Hook。观察到真实 Minecraft MainActivity 后，
 * 再使用该 Activity 自身的 ClassLoader 安装输入 Hook，避免被 StubApp 的壳 ClassLoader 卡住。
 */
object MinecraftHostHookFeature : HookFeature {
    private const val MAIN_ACTIVITY = "com.mojang.minecraftpe.MainActivity"
    private const val STUB_APPLICATION = "com.netease.android.protect.StubApp"
    private val mode = HostLoadMode.LSPOSED_STUB_APP

    private val lifecycleHooksInstalled = AtomicBoolean(false)
    private val stubHookInstalled = AtomicBoolean(false)
    private val inputHooksInstalled = AtomicBoolean(false)
    private val hostAttached = AtomicBoolean(false)
    private val inputHookFailureReported = AtomicBoolean(false)

    override fun install(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        if (loadPackageParam.packageName != HookTarget.PACKAGE_NAME) return
        if (loadPackageParam.processName != HookTarget.PACKAGE_NAME) return

        installActivityLifecycleHooks()
        installStubApplicationHook(loadPackageParam.classLoader)
    }

    private fun installActivityLifecycleHooks() {
        if (!lifecycleHooksInstalled.compareAndSet(false, true)) return

        runCatching {
            val onCreate = Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)
            val onResume = Activity::class.java.getDeclaredMethod("onResume")
            val onDestroy = Activity::class.java.getDeclaredMethod("onDestroy")
            XposedBridge.hookMethod(onCreate, mainActivityCreatedHook())
            XposedBridge.hookMethod(onResume, mainActivityResumedHook())
            XposedBridge.hookMethod(onDestroy, mainActivityDestroyedHook())
        }.onSuccess {
            XposedBridge.log("LiquidClickGUI framework Activity lifecycle hooks installed")
        }.onFailure { error ->
            lifecycleHooksInstalled.set(false)
            XposedBridge.log("LiquidClickGUI Activity lifecycle hook failed: ${error.message}")
            XposedBridge.log(error)
        }
    }

    private fun installStubApplicationHook(initialClassLoader: ClassLoader) {
        if (!stubHookInstalled.compareAndSet(false, true)) return

        runCatching {
            XposedHelpers.findAndHookMethod(
                STUB_APPLICATION,
                initialClassLoader,
                "attachBaseContext",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args.firstOrNull() as? Context ?: return
                        XposedBridge.log(
                            "LiquidClickGUI StubApp attached; waiting for real MainActivity " +
                                "ClassLoader=${context.classLoader.javaClass.name}",
                        )
                    }
                },
            )
        }.onFailure { error ->
            // 壳实现可能随版本变化；framework Activity 生命周期 Hook 仍可继续工作。
            XposedBridge.log("LiquidClickGUI StubApp hook unavailable: ${error.message}")
            XposedBridge.log(error)
        }
    }

    @Synchronized
    private fun attachToRealHost(activity: Activity): Boolean {
        if (hostAttached.get()) return true

        val mainActivityClass = activity.javaClass
        val classLoader = mainActivityClass.classLoader
            ?: return reportInputHookFailure(
                activity,
                IllegalStateException("Real MainActivity ClassLoader is null"),
            )

        return runCatching {
            installInputHooks(mainActivityClass)
            classLoader
        }.fold(
            onSuccess = { realClassLoader ->
                if (hostAttached.compareAndSet(false, true)) {
                    inputHookFailureReported.set(false)
                    val safeContext = activity.applicationContext ?: activity
                    HostHookBridge.onHostAttached(safeContext, realClassLoader, mode)
                    XposedBridge.log(
                        "LiquidClickGUI real host attached with " +
                            realClassLoader.javaClass.name,
                    )
                }
                true
            },
            onFailure = { error -> reportInputHookFailure(activity, error) },
        )
    }

    private fun installInputHooks(mainActivityClass: Class<*>) {
        if (inputHooksInstalled.get()) return

        val dispatchKeyEvent = findMethodInHierarchy(
            mainActivityClass,
            "dispatchKeyEvent",
            KeyEvent::class.java,
        )
        val dispatchGenericMotionEvent = findMethodInHierarchy(
            mainActivityClass,
            "dispatchGenericMotionEvent",
            MotionEvent::class.java,
        )
        val dispatchTouchEvent = findMethodInHierarchy(
            mainActivityClass,
            "dispatchTouchEvent",
            MotionEvent::class.java,
        )

        XposedBridge.hookMethod(
            dispatchKeyEvent,
            consumingHook { event -> HostHookBridge.onKeyEvent(event as KeyEvent) },
        )
        XposedBridge.hookMethod(
            dispatchGenericMotionEvent,
            consumingHook { event -> HostHookBridge.onGenericMotionEvent(event as MotionEvent) },
        )
        XposedBridge.hookMethod(
            dispatchTouchEvent,
            consumingHook { event -> HostHookBridge.onTouchEvent(event as MotionEvent) },
        )
        inputHooksInstalled.set(true)
        XposedBridge.log("LiquidClickGUI post-shell input hooks installed")
    }

    private fun reportInputHookFailure(activity: Activity, error: Throwable): Boolean {
        if (inputHookFailureReported.compareAndSet(false, true)) {
            val safeContext = activity.applicationContext ?: activity
            HostHookBridge.onHostHookFailed(safeContext, mode, error)
            XposedBridge.log("LiquidClickGUI post-shell input hooks failed: ${error.message}")
            XposedBridge.log(error)
        }
        return false
    }

    private fun mainActivityCreatedHook(): XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val activity = param.thisObject as? Activity ?: return
            if (!isMainActivity(activity) || !attachToRealHost(activity)) return
            XposedBridge.log("LiquidClickGUI MainActivity onCreate observed")
            HostHookBridge.onMainActivityCreated(activity)
        }
    }

    private fun mainActivityResumedHook(): XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val activity = param.thisObject as? Activity ?: return
            if (!isMainActivity(activity) || !attachToRealHost(activity)) return
            XposedBridge.log("LiquidClickGUI MainActivity onResume observed")
            HostHookBridge.onMainActivityResumed(activity)
        }
    }

    private fun mainActivityDestroyedHook(): XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val activity = param.thisObject as? Activity ?: return
            if (isMainActivity(activity)) {
                HostHookBridge.onMainActivityDestroyed(activity)
            }
        }
    }

    private fun findMethodInHierarchy(
        type: Class<*>,
        name: String,
        vararg parameterTypes: Class<*>,
    ): Method {
        var current: Class<*>? = type
        while (current != null) {
            val candidate = current
            runCatching { candidate.getDeclaredMethod(name, *parameterTypes) }
                .getOrNull()
                ?.let { return it }
            current = candidate.superclass
        }
        throw NoSuchMethodException("${type.name}#$name")
    }

    private fun consumingHook(consumer: (Any) -> Boolean): XC_MethodHook =
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val activity = param.thisObject as? Activity ?: return
                if (!isMainActivity(activity)) return
                val event = param.args.firstOrNull() ?: return
                if (consumer(event)) param.result = true
            }
        }

    private fun isMainActivity(activity: Activity): Boolean =
        activity.javaClass.name == MAIN_ACTIVITY
}
