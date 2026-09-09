package com.impad.pro.zygisk

import android.content.pm.ApplicationInfo
import android.content.SharedPreferences
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.Keep
import com.impad.pro.bridge.IHookBridge
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Constructor

/**
 * libxposed 101 [XposedInterface] implementation backed by the Zygisk [ArtHookBridge].
 * Installed onto impad's `HookEntrance` via `attachFramework(...)` so every
 * `hook(exec).intercept { … }` from the module routes onto the ART bridge.
 */
@Keep
class EmbeddedXposedInterface(
    private val bridge: IHookBridge,
) : XposedInterface {

    override fun getApiVersion(): Int = XposedInterface.API_101

    override fun getFrameworkName(): String = "Zygisk"

    override fun getFrameworkVersion(): String = "v1"

    override fun getFrameworkVersionCode(): Long = 1

    override fun getFrameworkProperties(): Long = 0

    override fun hook(executable: Executable): XposedInterface.HookBuilder =
        EmbeddedHookBuilder(bridge, executable)

    override fun hookClassInitializer(clazz: Class<*>): XposedInterface.HookBuilder {
        val clinit: Method = clazz.getDeclaredMethod("<clinit>")
        return EmbeddedHookBuilder(bridge, clinit)
    }

    override fun deoptimize(executable: Executable): Boolean = bridge.deoptimize(executable)

    override fun getInvoker(method: Method): XposedInterface.Invoker<*, Method> =
        EmbeddedMethodInvoker(bridge, method)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getInvoker(constructor: Constructor<T>): XposedInterface.CtorInvoker<T> =
        EmbeddedCtorInvoker(bridge, constructor) as XposedInterface.CtorInvoker<T>

    override fun log(p0: Int, p1: String?, p2: String) {
        Log.println(p0, p1 ?: getFrameworkName(), p2)
    }

    override fun log(p0: Int, p1: String?, p2: String, p3: Throwable?) {
        val text = p2 + (if (p3 == null) "" else "\n" + p3.stackTraceToString())
        Log.println(p0, p1 ?: getFrameworkName(), text)
    }

    override fun getModuleApplicationInfo(): ApplicationInfo =
        error("embedded: no module application info")

    override fun getRemotePreferences(p0: String): SharedPreferences {
        throw UnsupportedOperationException("embedded: no remote preferences")
    }

    override fun listRemoteFiles(): Array<String> = emptyArray()

    override fun openRemoteFile(p0: String): ParcelFileDescriptor {
        throw java.io.FileNotFoundException("embedded: no remote file $p0")
    }
}

// ───── HookBuilder ───────────────────────────────────────────────────────────

private class EmbeddedHookBuilder(
    private val bridge: IHookBridge,
    private val executable: Executable,
) : XposedInterface.HookBuilder {

    private var priority = 0
    private var exceptionMode: XposedInterface.ExceptionMode? = null

    override fun setPriority(priority: Int): XposedInterface.HookBuilder {
        this.priority = priority
        return this
    }

    override fun setExceptionMode(error: XposedInterface.ExceptionMode): XposedInterface.HookBuilder {
        this.exceptionMode = error
        return this
    }

    override fun intercept(hooker: XposedInterface.Hooker): XposedInterface.HookHandle {
        val handle = bridge.hookMethod(
            executable,
            object : IHookBridge.IMemberHookCallback {
                override fun beforeHookedMember(param: IHookBridge.IMemberHookParam) {
                    try {
                        val chain = EmbeddedChain(bridge, executable, param)
                        val result: Any? = hooker.intercept(chain)
                        param.result = result
                        param.extra = chain
                    } catch (t: Throwable) {
                        param.throwable = t
                    }
                }

                override fun afterHookedMember(param: IHookBridge.IMemberHookParam) = Unit
            },
            priority,
        )
        return EmbeddedHookHandle(bridge, executable, handle)
    }
}

// ───── Chain ─────────────────────────────────────────────────────────────────

private class EmbeddedChain(
    private val bridge: IHookBridge,
    private val executable: Executable,
    private val param: IHookBridge.IMemberHookParam,
) : XposedInterface.Chain {

    override fun getExecutable(): Executable = executable

    override fun getThisObject(): Any = param.thisObject as Any

    @Suppress("UNCHECKED_CAST")
    override fun getArgs(): List<Any> = param.args.asList() as List<Any>

    override fun getArg(i: Int): Any = param.args[i] as Any

    override fun proceed(): Any {
        param.result = invokeOriginal(param.args)
        return param.result as Any
    }

    override fun proceed(p0: Array<Any>): Any {
        @Suppress("UNCHECKED_CAST")
        param.result = invokeOriginal(p0 as Array<Any?>)
        return param.result as Any
    }

    override fun proceedWith(p0: Any): Any {
        param.result = invokeWithReceiver(p0, param.args)
        return param.result as Any
    }

    override fun proceedWith(p0: Any, p1: Array<Any>): Any {
        @Suppress("UNCHECKED_CAST")
        param.result = invokeWithReceiver(p0, p1 as Array<Any?>)
        return param.result as Any
    }

    private fun invokeOriginal(args: Array<Any?>): Any? {
        val m = executable as? Method
            ?: error("embedded chain only supports Method hooks")
        return bridge.invokeOriginalMethod(m, param.thisObject, args)
    }

    private fun invokeWithReceiver(thisObject: Any?, args: Array<Any?>): Any? {
        val m = executable as? Method
            ?: error("embedded chain only supports Method hooks")
        return bridge.invokeOriginalMethod(m, thisObject, args)
    }
}

// ───── HookHandle + Invokers ─────────────────────────────────────────────────

private class EmbeddedHookHandle(
    private val bridge: IHookBridge,
    private val executable: Executable,
    private val handle: IHookBridge.MemberUnhookHandle,
) : XposedInterface.HookHandle {

    override fun getExecutable(): Executable = executable

    override fun unhook() {
        handle.unhook()
    }
}

private class EmbeddedMethodInvoker(
    private val bridge: IHookBridge,
    private val method: Method,
) : XposedInterface.Invoker<EmbeddedMethodInvoker, Method> {

    override fun setType(p0: XposedInterface.Invoker.Type): EmbeddedMethodInvoker = this

    override fun invoke(p0: Any, vararg p1: Any): Any {
        @Suppress("UNCHECKED_CAST")
        val arr = p1 as Array<Any?>
        return bridge.invokeOriginalMethod(method, p0, arr) as Any
    }

    override fun invokeSpecial(p0: Any, vararg p1: Any): Any {
        @Suppress("UNCHECKED_CAST")
        val arr = p1 as Array<Any?>
        return bridge.invokeOriginalMethod(method, p0, arr) as Any
    }
}

private class EmbeddedCtorInvoker(
    private val bridge: IHookBridge,
    private val constructor: Constructor<*>,
) : XposedInterface.CtorInvoker<Any> {

    override fun setType(p0: XposedInterface.Invoker.Type): XposedInterface.CtorInvoker<Any> = this

    override fun invoke(p0: Any, vararg p1: Any): Any {
        @Suppress("UNCHECKED_CAST")
        val arr = p1 as Array<Any?>
        bridge.invokeOriginalConstructor(constructor as Constructor<Any?>, p0, arr)
        return null as Any
    }

    override fun invokeSpecial(p0: Any, vararg p1: Any): Any {
        @Suppress("UNCHECKED_CAST")
        val arr = p1 as Array<Any?>
        bridge.invokeOriginalConstructor(constructor as Constructor<Any?>, p0, arr)
        return null as Any
    }

    @Suppress("UNCHECKED_CAST")
    override fun newInstance(vararg p0: Any): Any {
        return bridge.newInstanceOrigin(constructor as Constructor<Any?>, *p0)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <U : Any> newInstanceSpecial(p0: Class<U>, vararg p1: Any): U {
        return p0.cast(bridge.newInstanceOrigin(constructor as Constructor<Any?>, *p1))
    }
}