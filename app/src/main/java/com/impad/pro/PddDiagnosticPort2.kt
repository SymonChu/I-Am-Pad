package com.impad.pro

import android.util.Log
import io.github.libxposed.api.XposedModule
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

/**
 * TabletHook(rroro2239) 移植的拼多多登录诊断钩子（2/2）：
 * 凭证写入记录 + login_token_expired 失效事件。自包含建 bridge。
 */
internal fun XposedModule.hookPddCredentialWrite(classLoader: ClassLoader) {
    runCatching {
        System.loadLibrary("dexkit")
        DexKitBridge.create(classLoader, true).use { bridge ->
            val matches = bridge.findMethod {
                matcher {
                    usingEqStrings("access_token", "uid", "uin", "login_time")
                }
            }.filter { m ->
                Modifier.isStatic(m.modifiers) &&
                    m.returnTypeName == "void" &&
                    m.paramTypeNames == listOf(
                        String::class.java.name,
                        String::class.java.name,
                        String::class.java.name,
                        String::class.java.name,
                        "boolean"
                    )
            }
            require(matches.size == 1) { "凭证写入方法 匹配数=${matches.size}" }
            val method = matches.first().getMethodInstance(classLoader)
            hook(method).intercept { chain ->
                val result = chain.proceed()
                val fields = (0..3).joinToString(",") { i -> chain.args.getOrNull(i) as? String ?: "" }
                val complete = (0..2).all { !(chain.args.getOrNull(it) as? String).isNullOrEmpty() }
                log(Log.INFO, TAG, "Pdd 凭证写入: [$fields] 完整=$complete")
                result
            }
            log(Log.INFO, TAG, "TabletHook: 拼多多凭证写入记录已 hook ${method.declaringClass.name}")
        }
    }.onFailure {
        log(Log.WARN, TAG, "TabletHook: 拼多多凭证移植失败(忽略): ${it.message}")
    }
}

internal fun XposedModule.hookPddTokenExpired(classLoader: ClassLoader) {
    runCatching {
        System.loadLibrary("dexkit")
        DexKitBridge.create(classLoader, true).use { bridge ->
            val matches = bridge.findMethod {
                matcher {
                    usingEqStrings("login_token_expired")
                }
            }.filter { m ->
                !Modifier.isStatic(m.modifiers) &&
                    m.returnTypeName == "void" &&
                    m.paramTypeNames.size == 1
            }.sortedBy { it.methodSign }
            require(matches.isNotEmpty()) { "token 失效事件方法为空" }
            val methods = matches.map { it.getMethodInstance(classLoader) }
            methods.forEachIndexed { index, method ->
                hook(method).intercept { chain ->
                    val event = chain.args.getOrNull(0)
                    val eventName = runCatching {
                        event?.javaClass?.getField("name")?.get(event) as? String
                    }.getOrNull()
                    if (eventName == "login_token_expired") {
                        log(Log.WARN, TAG, "Pdd 收到 login_token_expired 事件")
                    }
                    chain.proceed()
                }
                log(
                    Log.INFO, TAG,
                    "TabletHook: 拼多多 token 事件已 hook[$index] ${method.declaringClass.name}"
                )
            }
        }
    }.onFailure {
        log(Log.WARN, TAG, "TabletHook: 拼多多 token 事件移植失败(忽略): ${it.message}")
    }
}

private const val TAG = "ImpPad.TabletHook"
