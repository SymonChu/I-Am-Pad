@file:Suppress("unused")

package com.impad.pro.zygisk

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.annotation.Keep
import com.impad.pro.bridge.IHookBridge
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Zygisk JVM entry point (impad edition).
 *
 * Called from Rust postAppSpecialize:
 *   ZygiskEntry.init(processName, dataDir, apkPath)
 *
 * Rust side has already: copied module APK + DEX payload into the target app's
 * data dir, loaded them via InMemoryDexClassLoader, and registered all JNI
 * methods (ArtHookBridge natives + this entry's nativeInitialize).
 */
@Keep
object ZygiskEntry {

    private const val TAG = "ImpZygiskEntry"
    private val entryLock = Any()
    private val moduleStarted = AtomicBoolean(false)

    /** Native 层对可注入进程全量注入，这里才是 impad 的「目标允许名单」。 */
    private val supportedRoots: Set<String> = setOf(
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.tencent.tim",
        "com.tencent.wework",
        "com.alibaba.android.rimet",
        "com.airchina.wecompro",
        "com.zwfw.YueZhengYi",
        "cn.powerchina.pact",
        "com.cscec.portal",
        "com.xingin.xhs",
        "com.xunmeng.pinduoduo",
    )

    private fun java.lang.reflect.AccessibleObject.makeAccessibleCompat() {
        if (!isAccessible) {
            @Suppress("DEPRECATION")
            runCatching { isAccessible = true }
        }
    }

    @JvmStatic
    @Keep
    fun init(
        processName: String,
        dataDir: String,
        apkPath: String,
    ) {
        synchronized(entryLock) {
            if (moduleStarted.get()) return

            try {
                Log.i(TAG, "init process=$processName apk=$apkPath dataDir=$dataDir")

                // Native 已全量注入，impad 在这里做目标允许名单把关。
                val targetRoot = processName.substringBefore(':')
                if (targetRoot !in supportedRoots) {
                    Log.i(TAG, "unsupported target $targetRoot, skip")
                    return
                }

                check(nativeInitialize()) { "failed to init ART hook runtime" }

                val bridge: IHookBridge = ArtHookBridge()
                val embedded = EmbeddedXposedInterface(bridge)

                // Capture the host app's final ClassLoader via the factory chain.
                val loadedApk = Class.forName("android.app.LoadedApk")
                val createAppFactory = loadedApk.getDeclaredMethod(
                    "createAppFactory",
                    ApplicationInfo::class.java,
                    ClassLoader::class.java,
                )
                createAppFactory.makeAccessibleCompat()

                bridge.hookMethod(
                    createAppFactory,
                    object : IHookBridge.IMemberHookCallback {
                        override fun beforeHookedMember(param: IHookBridge.IMemberHookParam) = Unit

                        override fun afterHookedMember(param: IHookBridge.IMemberHookParam) {
                            if (param.throwable != null) return
                            val ai = param.args.getOrNull(0) as? ApplicationInfo ?: return
                            if (ai.packageName != targetRoot) return
                            val factory = param.result ?: return
                            installFinalClassLoaderHook(bridge, embedded, factory, targetRoot)
                        }
                    },
                    priority = 10000,
                )

                moduleStarted.set(true)
            } catch (t: Throwable) {
                moduleStarted.set(false)
                Log.e(TAG, "init failed", t)
            }
        }
    }

    private fun installFinalClassLoaderHook(
        bridge: IHookBridge,
        embedded: EmbeddedXposedInterface,
        appComponentFactory: Any,
        targetRoot: String,
    ) {
        try {
            val instantiateClassLoader = appComponentFactory.javaClass.getMethod(
                "instantiateClassLoader",
                ClassLoader::class.java,
                ApplicationInfo::class.java,
            )
            instantiateClassLoader.makeAccessibleCompat()

            bridge.hookMethod(
                instantiateClassLoader,
                object : IHookBridge.IMemberHookCallback {
                    override fun beforeHookedMember(param: IHookBridge.IMemberHookParam) = Unit

                    override fun afterHookedMember(param: IHookBridge.IMemberHookParam) {
                        if (param.throwable != null) return
                        val ai = param.args.getOrNull(1) as? ApplicationInfo ?: return
                        if (ai.packageName != targetRoot) return
                        val hostLoader = param.result as? ClassLoader ?: return
                        startModule(bridge, embedded, ai, hostLoader)
                    }
                },
                priority = 10,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "failed to hook instantiateClassLoader", t)
        }
    }

    private fun startModule(
        bridge: IHookBridge,
        embedded: EmbeddedXposedInterface,
        appInfo: ApplicationInfo,
        hostLoader: ClassLoader,
    ) {
        ModuleActivator.start(bridge, embedded, appInfo, hostLoader)
    }

    @JvmStatic
    private external fun nativeInitialize(): Boolean
}