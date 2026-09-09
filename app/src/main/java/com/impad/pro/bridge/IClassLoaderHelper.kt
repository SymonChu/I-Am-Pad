package com.impad.pro.bridge

interface IClassLoaderHelper {

    fun createEmptyInMemoryMultiDexClassLoader(parent: ClassLoader): ClassLoader

    fun injectDexToClassLoader(classLoader: ClassLoader, dexBytes: ByteArray, dexName: String?)
}
