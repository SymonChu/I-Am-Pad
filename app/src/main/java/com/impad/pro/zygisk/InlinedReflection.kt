package com.impad.pro.zygisk

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Member

// ── Primitive / boxed type aliases (inlined from WeKite utils/reflection/Classes.kt) ──
// Used by ArtHookBridge's DexMaker code-gen where the Kotlin keyword is shadowed
// by a need to reference the java Class object of that primitive.

inline val int: Class<Int> get() = Int::class.javaPrimitiveType!!
inline val bool: Class<Boolean> get() = Boolean::class.javaPrimitiveType!!
inline val byte: Class<Byte> get() = Byte::class.javaPrimitiveType!!
inline val short: Class<Short> get() = Short::class.javaPrimitiveType!!
inline val long: Class<Long> get() = Long::class.javaPrimitiveType!!
inline val float: Class<Float> get() = Float::class.javaPrimitiveType!!
inline val double: Class<Double> get() = Double::class.javaPrimitiveType!!
inline val char: Class<Char> get() = Char::class.javaPrimitiveType!!
inline val void: Class<Void> get() = Void.TYPE

inline val BInt: Class<Int> get() = Int::class.javaObjectType
inline val BBool: Class<Boolean> get() = Boolean::class.javaObjectType
inline val BByte: Class<Byte> get() = Byte::class.javaObjectType
inline val BShort: Class<Short> get() = Short::class.javaObjectType
inline val BLong: Class<Long> get() = Long::class.javaObjectType
inline val BFloat: Class<Float> get() = Float::class.javaObjectType
inline val BDouble: Class<Double> get() = Double::class.javaObjectType
inline val BChar: Class<Char> get() = Char::class.javaObjectType
inline val BString: Class<String> get() = String::class.java

// ── reflekt.utils.inline (dev.ujhhgtg.reflekt) ────────────────────────────

inline val Member.isStatic: Boolean get() = java.lang.reflect.Modifier.isStatic(modifiers)

fun <T : AccessibleObject> T.makeAccessibleCompat(): T {
    @Suppress("DEPRECATION")
    if (!isAccessible) {
        runCatching { isAccessible = true }
    }
    return this
}