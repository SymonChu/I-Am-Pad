# impad pro

> 在手机上实现平板登录的 Xposed 模块。基于 [I-Am-Pad](https://github.com/Houvven/I-Am-Pad)（libxposed API）二次开发，将手机伪装成平板设备，绕过「仅限平板端登录」的限制。

利用 Xposed / LSPosed 框架或 Magisk / KernelSU 的 Zygisk 注入，把手机伪装成平板设备，支持多种常见应用的平板模式登录。

## 双模式

| 模式 | 产物 | 环境要求 | 说明 |
|---|---|---|---|
| **LSPosed 模式** | `impad.pro-<version>-release.apk` | 已 root，安装 LSPosed 框架 | 在作用域内勾选目标应用 |
| **Zygisk 模式** | `impad.pro-zygisk-<version>.zip` | Magisk（启用 Zygisk）/ KernelSU | 免 LSPosed 框架，native 全量注入，刷入即对所有 11 个宿主生效 |

## 支持应用（11 个）

| 应用 | 包名 | 实现方式 |
|---|---|---|
| 微信 | `com.tencent.mm` | hook `CgiCheckLoginAsPad` + `isFoldableDevice` |
| QQ | `com.tencent.mobileqq` | 改 `Build.MODEL` + `ro.build.characteristics` 平板属性 |
| TIM | `com.tencent.tim` | 复用 QQ 同款平板伪装 |
| 企业微信 | `com.tencent.wework` | hook `isAndroidPad*` 返回平板 |
| 钉钉 | `com.alibaba.android.rimet` | hook `isMultipleLoginFoldable` |
| 小红书 | `com.xingin.xhs` | `getDeviceType=pad` + 精确型号 |
| 拼多多 | `com.xunmeng.pinduoduo` | hook 平板判定方法（移植 TabletHook PddHook）|
| 国航之翼 | `com.airchina.wecompro` | 企微定制 `WwUtil.isPadJudge` |
| 粤政易 | `com.zwfw.YueZhengYi` | 企微定制 `WwUtil.isPadJudge` |
| 中电建 | `cn.powerchina.pact` | 企微定制 `WwUtil.isPadJudge` |
| 中建 | `com.cscec.portal` | 企微定制 `WwUtil.isPadJudge` |

> 拼多多的平板判定与登录诊断钩子移植自 [TabletHook](https://github.com/roro2239/TabletHook)（见致谢）；微信 / QQ 保留 impad 上游的稳定实现。

## 安装使用

### LSPosed 模式
1. 手机已 root 并安装 LSPosed 框架
2. 安装签名 APK，在 LSPosed 中启用本模块
3. 勾选需要模拟的应用作用域
4. 重启对应应用生效

### Zygisk 模式
1. 已安装 Magisk（**设置中启用 Zygisk**）或 KernelSU
2. 刷入 `impad.pro-zygisk-<version>.zip`
3. 重启后对所有 11 个宿主生效（native 全量注入，作用域由模块内置白名单把关）

## 构建

```bash
# LSPosed APK
./gradlew :app:assembleRelease

# Zygisk 模块 ZIP
cd impad-zygisk && ./build-module.sh <signed.apk路径>
```

也可直接在 GitHub Actions 中构建（tag 触发自动发布 Release）。

## 发布

打 tag 推送即可触发 CI 自动构建并发布：

```bash
git tag v1.0.3 && git push origin v1.0.3
```

CI 全绿后自动创建 Release，只上传**签名 APK**（`impad.pro-<version>-release.apk`，可直接安装）与 Zygisk 模块 ZIP。

## 说明

- 模块基于 libxposed API，各应用的平板实现会随应用版本更新而变化
- 本项目仅供学习交流，请遵守相关应用的用户协议
- 若某应用版本更新导致失效，欢迎提交 issue 或 PR

## 致谢

本项目离不开以下两个开源项目的启发与代码，在此致以诚挚感谢：

- **[I-Am-Pad](https://github.com/Houvven/I-Am-Pad)**（原 `com.houvven.impad`）— 本项目基于其平板登录伪装核心 fork 开发，微信/QQ/钉钉/企业微信/小红书等宿主实现均源自上游
- **[TabletHook](https://github.com/roro2239/TabletHook)** — 拼多多的平板判定与诊断钩子移植自该项目

## License

本项目采用 [GPL-3.0](LICENSE)。请在遵守许可协议的前提下使用与分发。