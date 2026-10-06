package dev.liquid.clickgui.xposed

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.liquid.clickgui.data.TargetAppContract
import java.util.concurrent.CopyOnWriteArrayList

object HookTarget {
    const val PACKAGE_NAME = TargetAppContract.PACKAGE_NAME
}

fun interface HookFeature {
    fun install(loadPackageParam: XC_LoadPackage.LoadPackageParam)
}

object HookRegistry {
    private val features = CopyOnWriteArrayList<HookFeature>()

    @JvmStatic
    fun register(feature: HookFeature) {
        features.addIfAbsent(feature)
    }

    internal fun installAll(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        features.forEach { feature ->
            runCatching { feature.install(loadPackageParam) }
                .onFailure { XposedBridge.log("LiquidClickGUI feature failed: ${it.message}") }
        }
    }
}
