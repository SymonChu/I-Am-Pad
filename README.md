# impad pro

在手机上实现平板登录（Xposed 模块，基于 libxposed API）。

利用 Xposed/LSPosed 框架，将手机伪装成平板进行登录，支持多种常见应用的平板模式。

## 支持应用

| 应用 | 包名 | 说明 |
|---|---|---|
| 微信 | `com.tencent.mm` | 平板登录 |
| QQ | `com.tencent.mobileqq` | 平板模式 |
| TIM | `com.tencent.tim` | 平板模式 |
| 企业微信 | `com.tencent.wework` | 平板登录 |
| 钉钉 | `com.alibaba.android.rimet` | 平板登录 |
| 小红书 | `com.xingin.xhs` | 平板识别 |
| 拼多多 | `com.xunmeng.pinduoduo` | 平板判定 |
| 国航之翼 | `com.airchina.wecompro` | 企微定制 |
| 粤政易 | `com.zwfw.YueZhengYi` | 企微定制 |
| 中电建 | `cn.powerchina.pact` | 企微定制 |
| 中建 | `com.cscec.portal` | 企微定制 |

## 安装使用

1. 使用 LSPosed 配套环境（模块基于 Xposed API）
2. 安装 APK 并勾选需要模拟的应用作用域
3. 重启对应应用生效

## 说明
- 模块基于 libxposed API，适用于 LSPosed 环境
- 各应用的平板登录实现可能随应用版本更新而变化
- 本项目为学习交流用途，请遵守相关应用的用户协议

## License
GPL-3.0