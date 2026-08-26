package com.liquid.org.xposed;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import dalvik.system.DexClassLoader;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed entry point for the authorized UI test target.
 * The hook loads LiquidPE's UI and observes the target's UniFix lifecycle.
 */
public final class HookInit implements IXposedHookLoadPackage {
    private static final String TAG = "LiquidPE-Hook";
    private static final String TARGET_PACKAGE = "com.netease.x19";
    private static final String MAIN_ACTIVITY = "com.mojang.minecraftpe.MainActivity";
    private static final String UNIFIX_BASE = "com.netease.ntunisdk.unifix.UniFixBase";
    private static final String MODULE_PACKAGE = "com.liquid.org";
    private static final String OVERLAY_VIEW = "com.liquid.org.ui.overlay.LiquidBounceOverlayView";
    private static final Map<Activity, View> ATTACHED_VIEWS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null || !TARGET_PACKAGE.equals(lpparam.packageName)) return;
        if (!INSTALLED.compareAndSet(false, true)) return;

        try {
            hookUniFix(lpparam.classLoader);
            hookTargetActivityLifecycle(lpparam.classLoader);
            Log.i(TAG, "Hook installed for " + TARGET_PACKAGE);
        } catch (Throwable error) {
            INSTALLED.set(false);
            Log.e(TAG, "Failed to install target Activity hook", error);
        }
    }

    private static void hookTargetActivityLifecycle(ClassLoader targetLoader) {
        boolean directInstalled = false;
        try {
            XposedHelpers.findAndHookMethod(MAIN_ACTIVITY, targetLoader, "onCreate",
                    Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            attachIfTargetActivity(param.thisObject);
                        }
                    });
            Log.i(TAG, "Direct target Activity hook installed");
            directInstalled = true;
        } catch (Throwable unavailable) {
            Log.i(TAG, "Named target Activity hook unavailable; using lifecycle fallback");
        }

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        attachIfTargetActivity(param.thisObject);
                    }
                });
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                attachIfTargetActivity(param.thisObject);
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (Boolean.TRUE.equals(param.args[0])) {
                            attachIfTargetActivity(param.thisObject);
                        }
                    }
                });
        Log.i(TAG, "Base Activity lifecycle hooks installed (direct=" + directInstalled + ")");
    }

    private static void attachIfTargetActivity(Object instance) {
        if (!(instance instanceof Activity)) return;
        Activity activity = (Activity) instance;
        if (!TARGET_PACKAGE.equals(activity.getPackageName())) return;
        String className = activity.getClass().getName();
        if (!isTargetActivityClass(className)) return;
        attachLiquidPEUi(activity);
    }

    private static boolean isTargetActivityClass(String className) {
        if (className == null) return false;
        // The target ships several launcher variants (MainActivityDefault/DynTest*).
        return MAIN_ACTIVITY.equals(className)
                || className.startsWith("com.mojang.minecraftpe.MainActivity")
                || className.startsWith("com.netease.minecraftpe.MainActivity");
    }

    private static void hookUniFix(ClassLoader classLoader) {
        try {
            Class<?> uniFixType = classLoader.loadClass(UNIFIX_BASE);
            XposedHelpers.findAndHookMethod(uniFixType, "a", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Log.i(TAG, "UniFixBase.a observed");
                }
            });
            Log.i(TAG, "UniFix hook installed");
        } catch (Throwable error) {
            Log.w(TAG, "UniFix class or method unavailable", error);
        }
    }

    private static void attachLiquidPEUi(Activity activity) {
        if (activity == null) return;
        if (ATTACHED_VIEWS.containsKey(activity)) return;
        try {
            Context moduleContext = activity.createPackageContext(
                    MODULE_PACKAGE, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            String apkPath = moduleContext.getApplicationInfo().sourceDir;
            File optimizedDir = activity.getDir("liquidpe_hook_dex", Context.MODE_PRIVATE);
            DexClassLoader loader = new DexClassLoader(
                    apkPath, optimizedDir.getAbsolutePath(),
                    moduleContext.getApplicationInfo().nativeLibraryDir,
                    activity.getClassLoader());
            Class<?> viewType = Class.forName(OVERLAY_VIEW, true, loader);
            // Keep target lifecycle/application state while resolving the UI's R values from the module APK.
            Context overlayContext = new OverlayContext(activity, moduleContext);
            View view = (View) viewType.getConstructor(Context.class).newInstance(overlayContext);
            viewType.getMethod("setTransparentBase", boolean.class).invoke(view, true);
            viewType.getMethod("setHudVisible", boolean.class).invoke(view, true);
            viewType.getMethod("setClickGuiVisibleImmediately", boolean.class).invoke(view, true);
            activity.addContentView(view, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ATTACHED_VIEWS.put(activity, view);
            Log.i(TAG, "LiquidPE UI loaded through DexClassLoader");
        } catch (Throwable error) {
            Log.e(TAG, "Unable to load LiquidPE UI", error);
        }
    }

    private static final class OverlayContext extends ContextWrapper {
        private final Context moduleContext;

        OverlayContext(Context targetContext, Context moduleContext) {
            super(targetContext);
            this.moduleContext = moduleContext;
        }

        @Override
        public android.content.res.Resources getResources() {
            return moduleContext.getResources();
        }

        @Override
        public android.content.res.AssetManager getAssets() {
            return moduleContext.getAssets();
        }

        @Override
        public String getPackageName() {
            return moduleContext.getPackageName();
        }
    }
}
