#!/usr/bin/env bash
# impad pro — Zygisk 模块打包脚本（NAS 本地执行）
#
# 输入：
#   - native 已编译产物: impad-zygisk/native/target/<abi>/release/libimpad_zygisk.so
#   - payload APK:        <apk-release>（assemble 出的 app.apk，含 Zygisk 嵌入 Kotlin 代码）
#   - template/          模块模板（customize.sh 等占位符）
#
# 输出：
#   impad-zygisk/release/<name>-<version>.zip （可直接安装到 Magisk/KernelSU）

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ZDIR="$ROOT/impad-zygisk"
NATIVE="$ZDIR/native"
TEMPLATE="$ZDIR/template"

SONAME=impad_zygisk
MODULE_ID=impad_zygisk
MODULE_NAME="impad pro Zygisk"
AUTHOR="SymonChu"
DESCRIPTION="impad pro — Zygisk mode. 平板登录伪装 for 11 宿主（微信/QQ/TIM/企微/钉钉/小红书/拼多多/国航之翼/粤政易/中电建/中建）。"

# 版本：取 git 提交数作 versionCode，versionName 用固定值
VERSION_CODE=$(git -C "$ROOT" rev-list --count HEAD 2>/dev/null || echo 0)
VERSION_NAME=1.0.0

# 从已编译 native 找 .so
SO_ARM64="$ZDIR/native/target/aarch64-linux-android/release/libimpad_zygisk.so"
SO_ARM="$ZDIR/native/target/armv7-linux-androideabi/release/libimpad_zygisk.so"

# payload APK（第一个参数可覆盖）
PAYLOAD_APK="${1:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"

work="$ZDIR/output/module"
rm -rf "$work"
mkdir -p "$work"

echo "[1/6] 拷贝 template -> $work"
cp -r "$TEMPLATE"/. "$work"/

echo "[2/6] 解析 module.prop 占位符"
sed -i "s/\${moduleId}/$MODULE_ID/g" "$work/module.prop"
sed -i "s/\${moduleName}/$MODULE_NAME/g" "$work/module.prop"
sed -i "s/\${versionName}/$VERSION_NAME/g" "$work/module.prop"
sed -i "s/\${versionCode}/$VERSION_CODE/g" "$work/module.prop"

echo "[3/6] 替换脚本占位符 (@DEBUG@/@SONAME@/@SUPPORTED_ABIS@)"
for f in customize.sh post-fs-data.sh service.sh; do
  sed -i "s/@DEBUG@/false/g; s/@SONAME@/$SONAME/g; s/@SUPPORTED_ABIS@/arm arm64/g" "$work/$f"
done

echo "[4/6] 复制 native .so"
mkdir -p "$work/lib/arm64-v8a" "$work/lib/armeabi-v7a"
cp "$SO_ARM64" "$work/lib/arm64-v8a/lib$SONAME.so"
[ -f "$SO_ARM" ] && cp "$SO_ARM" "$work/lib/armeabi-v7a/lib$SONAME.so"

echo "[5/6] 复制 payload APK"
mkdir -p "$work/payload"
cp "$PAYLOAD_APK" "$work/payload/impad.apk"

echo "[6/6] 打包 ZIP"
REL="$ZDIR/release"
mkdir -p "$REL"
ZIP="$REL/impad_zygisk-$VERSION_CODE-$VERSION_NAME.zip"
rm -f "$ZIP"
(
  cd "$work"
  zip -rq9 "$ZIP" .
)
echo "产出: $ZIP"