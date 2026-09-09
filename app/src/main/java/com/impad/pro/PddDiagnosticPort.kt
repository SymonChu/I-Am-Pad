package com.impad.pro

import android.util.Log
import io.github.libxposed.api.XposedModule
import org.json.JSONObject
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

/**
 * TabletHook(rroro2239) 移植的拼多多登录诊断钩子（1/2）：
 * 登录请求提交记录 + 二维码换票。自包含建 bridge，runCatching 隔离。
 */
internal fun XposedModule.hookPddLoginRequest(classLoader: ClassLoader) {
    runCatching {
        System.loadLibrary("dexkit")
        DexKitBridge.create(classLoader, true).use { bridge ->
            val matches = bridge.findMethod {
                matcher {
                    usingEqStrings("login_scene", "refer_page_sn", "app_id")
                }
            }.filter { m ->
                !Modifier.isStatic(m.modifiers) &&
                    m.returnTypeName == "void" &&
                    m.paramTypeNames == listOf(JSONObject::class.java.name, String::class.java.name)
            }
            require(matches.size == 1) { "登录请求提交方法 匹配数=${matches.size}" }
            val method = matches.first().getMethodInstance(classLoader)
            hook(method).intercept { chain ->
                val payload = chain.args.getOrNull(0) as? JSONObject
                val url = chain.args.getOrNull(1) as? String
                log(
                    Log.INFO, TAG,
                    "Pdd 登录请求: url=$url payloadKeys=${payload?.keys()?.asSequence()?.toList()}"
                )
                chain.proceed()
            }
            log(Log.INFO, TAG, "TabletHook: 拼多多登录请求记录已 hook ${method.declaringClass.name}")
        }
    }.onFailure {
        log(Log.WARN, TAG, "TabletHook: 拼多多登录请求移植失败(忽略): ${it.message}")
    }
}

internal fun XposedModule.hookPddTicketExchange(classLoader: ClassLoader) {
    runCatching {
        System.loadLibrary("dexkit")
        DexKitBridge.create(classLoader, true).use { bridge ->
            val matches = bridge.findMethod {
                matcher {
                    usingEqStrings("ticket", "secret_key", "/api/sigerus/auth/ticket")
                }
            }.filter { m ->
                Modifier.isPublic(m.modifiers) &&
                    !Modifier.isStatic(m.modifiers) &&
                    m.returnTypeName == "void" &&
                    m.paramTypeNames == listOf(String::class.java.name, String::class.java.name)
            }
            require(matches.size == 1) { "二维码换票方法 匹配数=${matches.size}" }
            val method = matches.first().getMethodInstance(classLoader)
            hook(method).intercept { chain ->
                log(Log.INFO, TAG, "Pdd 二维码换票: ticket=${chain.args.getOrNull(0)}")
                chain.proceed()
            }
            log(Log.INFO, TAG, "TabletHook: 拼多多换票记录已 hook ${method.declaringClass.name}")
        }
    }.onFailure {
        log(Log.WARN, TAG, "TabletHook: 拼多多换票移植失败(忽略): ${it.message}")
    }
}

private const val TAG = "ImpPad.TabletHook"
