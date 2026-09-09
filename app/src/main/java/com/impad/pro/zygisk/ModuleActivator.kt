package com.impad.pro.zygisk

import android.content.pm.ApplicationInfo
import android.util.Log
import com.impad.pro.bridge.IHookBridge
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Proxy

/**
 * Bootstraps impad's [com.impad.pro.HookEntrance] (an XposedModule) in Zygisk
 * mode by attaching a synthetic [io.github.libxposed.api.XposedInterface]
 * (provided by [EmbeddedXposedInterface]) and driving its lifecycle:
 *
 *   1. instantiate `HookEntrance` from the module ClassLoader
 *   2. `entrance.attachFramework(embedded, failRunnable)`
 *   3. synthesize a `PackageReadyParam` (via a JDK Proxy) that reports the host
 *      app's packageName + classLoader
 *   4. `entrance.onPackageReady(ready)`
 *
 * After step 4 the module's `onPackageReady` routes via its normal hook builders,
 * which now land on the embedded ART bridge.
 */
object ModuleActivator {

    private const val TAG = "ModuleActivator"
    private const val HOOK_ENTRANCE = "com.impad.pro.HookEntrance"

    @JvmStatic
    fun start(
        bridge: IHookBridge,
        embedded: EmbeddedXposedInterface,
        appInfo: ApplicationInfo,
        hostLoader: ClassLoader,
    ): Boolean {
        return try {
            // HookEntrance must be loaded from the module's own DEX, not the host.
            val moduleLoader = ModuleActivator::class.java.classLoader
                ?: error("no module ClassLoader")

            Log.i(TAG, "loading $HOOK_ENTRANCE with ${moduleLoader.javaClass.name}")
            val entranceClass = moduleLoader.loadClass(HOOK_ENTRANCE)
            val entrance = entranceClass.getDeclaredConstructor().newInstance() as XposedModuleInterface

            val wrapper = entrance as io.github.libxposed.api.XposedInterfaceWrapper
            wrapper.attachFramework(embedded)

            val readyParam = synthesizePackageReadyParam(appInfo, hostLoader, moduleLoader)
            entrance.onPackageReady(readyParam)

            Log.i(TAG, "impad module started for ${appInfo.packageName}")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "failed to activate module", t)
            false
        }
    }

    private fun synthesizePackageReadyParam(
        appInfo: ApplicationInfo,
        hostLoader: ClassLoader,
        ifaceLoader: ClassLoader,
    ): XposedModuleInterface.PackageReadyParam {
        val pkg = appInfo.packageName.orEmpty()
        return Proxy.newProxyInstance(
            ifaceLoader,
            arrayOf(XposedModuleInterface.PackageReadyParam::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "toString" -> "ImpPadPackageReadyParam{$pkg}"
                "hashCode" -> pkg.hashCode()
                "equals" -> (args?.firstOrNull() === proxy)
                "getPackageName" -> pkg
                "getApplicationInfo" -> appInfo
                "getClassLoader", "getDefaultClassLoader" -> hostLoader
                "isFirstPackage" -> java.lang.Boolean.TRUE
                "getAppComponentFactory" -> appInfo.appComponentFactory
                else -> null
            }
        } as XposedModuleInterface.PackageReadyParam
    }
}