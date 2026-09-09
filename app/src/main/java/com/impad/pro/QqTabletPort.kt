package com.impad.pro

import android.content.Context
import android.util.Log
import io.github.libxposed.api.XposedModule
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * TabletHook(rroro2239) 移植的 QQ TABLET 枚举方案：
 * hook 设备类型 Getter 返回 TABLET 枚举，让 QQ 走平板客户端通道。
 * 原版用 DexKit 2.2.0 的 invokes 统计引用数；这里用 2.1.0 API 等价改写：
 * 直接找「返回枚举 + 单 Context 参数 + static」的 Getter 方法。
 * runCatching 隔离，失败不影响原 simulateTabletModel 方案。
 */
internal fun XposedModule.hookQqTabletEnum(
    bridge: DexKitBridge,
    classLoader: ClassLoader,
) {
    runCatching {
        val getterCandidates = bridge.findMethod {
            matcher {
                modifiers(Modifier.STATIC)
                paramTypes(Context::class.java)
                usingStrings("AppSetting_params", "AppSetting_params_pad")
            }
        }
        require(getterCandidates.isNotEmpty()) { "设备类型 Getter 候选为空" }

        // 找到返回类型是枚举的那个 Getter（QQ 设备类型 Getter 返回枚举）
        var tabletMethod: Method? = null
        var tabletValue: Enum<*>? = null
        for (candidate in getterCandidates) {
            val enumClass = runCatching {
                Class.forName(candidate.returnTypeName, false, classLoader)
            }.getOrNull() ?: continue
            if (!enumClass.isEnum) continue
            val constants = enumClass.enumConstants
            val tab = constants?.firstOrNull { it is Enum<*> && it.name == "TABLET" }
            if (tab != null) {
                tabletMethod = candidate.getMethodInstance(classLoader)
                @Suppress("UNCHECKED_CAST")
                tabletValue = tab as Enum<*>
                break
            }
        }
        requireNotNull(tabletMethod) { "未找到 QQ TABLET 枚举 Getter" }
        requireNotNull(tabletValue) { "QQ 枚举无 TABLET 值" }

        hook(tabletMethod).intercept { tabletValue }
        log(
            Log.INFO,
            TAG,
            "TabletHook: QQ TABLET 枚举已 hook ${tabletMethod.declaringClass.name} -> ${tabletValue.javaClass.name}.TABLET"
        )
    }.onFailure {
        log(Log.WARN, TAG, "TabletHook: QQ TABLET 移植失败(忽略): ${it.message}")
    }
}

private const val TAG = "ImpPad.TabletHook"
