package dev.liquid.clickgui.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ModuleEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        if (loadPackageParam.packageName != HookTarget.PACKAGE_NAME) return
        if (loadPackageParam.processName != HookTarget.PACKAGE_NAME) return

        XposedBridge.log("LiquidClickGUI attached to ${HookTarget.PACKAGE_NAME}")
        HostHookBridge.register(InjectedHostRuntime)
        registerFeatures()
        HookRegistry.installAll(loadPackageParam)
    }

    private fun registerFeatures() {
        HookRegistry.register(MinecraftHostHookFeature)
    }
}
