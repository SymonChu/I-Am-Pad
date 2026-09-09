# impad pro — Zygisk 模块

impad pro 的免框架模式：通过 Magisk/KernelSU 的 Zygisk 注入，实现 11 个宿主的平板登录伪装，无需 LSPosed。

## 原理

- native 注入器（Rust）在 `postAppSpecialize` 把模块 APK + DEX 复制进目标进程，加载 `ZygiskEntry`
- `ZygiskEntry` 判定进程是否在 11 宿主白名单内，是则用 `EmbeddedXposedInterface`（libxposed 接口实现）驱动 `HookEntrance`，复用 impad 全部 hook 逻辑
- native 层全量注入，目标筛选由 Kotlin 白名单把关（注入进程读不到 root 的 /data/adb，故不做 WebUI 开关）

## 构建

```bash
# 1. 编译 native（双 ABI），需 rustup + NDK 30 交叉工具链
cd impad-zygisk/native
cargo build --release --target aarch64-linux-android
cargo build --release --target armv7-linux-androideabi

# 2. 打包模块 ZIP（payload 用 assemble 出的 APK）
cd ..
./build-module.sh /path/to/app-release.apk
# 产出 release/impad_zygisk-<vc>-<ver>.zip，直接刷入 Magisk/KernelSU
```

## 安装

Magisk 需启用 Zygisk（或 KernelSU）。刷入 ZIP 后重启，11 个宿主自动生效。

## License

GPL-3.0。
