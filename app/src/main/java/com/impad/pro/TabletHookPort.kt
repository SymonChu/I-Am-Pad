package com.impad.pro

import android.util.Log
import io.github.libxposed.api.XposedModule
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

/**
 * TabletHook(rroro2239) 移植的微信增强钩子：
 * 新版微信(8.0.77+)用 inTabletEnv 判定平板环境，比旧的折叠屏关键词更通用。
 * 自包含：内部创建 DexKitBridge，runCatching 隔离，失败不影响原 hook。
 */
internal fun XposedModule.hookWeChatTabletEnv(classLoader: ClassLoader) {
    runCatching {
        System.loadLibrary("dexkit")
        DexKitBridge.create(classLoader, true).use { bridge ->
            val matches = bridge.findMethod {
                matcher {
                    usingStrings("inTabletEnv")
                }
            }.filter { m ->
                Modifier.isPublic(m.modifiers) &&
                    Modifier.isStatic(m.modifiers) &&
                    m.returnTypeName == "boolean"
            }
            require(matches.size == 1) { "inTabletEnv 匹配数=${matches.size}" }
            val method = matches.first().getMethodInstance(classLoader)
            hookToReturn(method, true)
            log(
                Log.INFO,
                TAG,
                "TabletHook: 微信 inTabletEnv 已 hook ${method.declaringClass.name}.${method.name}"
            )
        }
    }.onFailure {
        log(Log.WARN, TAG, "TabletHook: 微信 inTabletEnv 移植失败(忽略): ${it.message}")
    }
}

private const val TAG = "ImpPad.TabletHook"
