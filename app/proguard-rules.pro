-dontwarn java.lang.reflect.AnnotatedType
# 包名从 com.houvven.impad 改为 com.impad.pro 后，旧 keep 规则失效，
# R8 会把只被 java_init.list 按名字引用的入口类当不可达代码裁掉（v1.1.2 及之前所有 release 均损坏）
-keep class com.impad.pro.** { *; }
-dontobfuscate
