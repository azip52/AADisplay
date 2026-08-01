# AADisplay

[![Build AADisplay](https://github.com/azip52/AADisplay/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/azip52/AADisplay/actions/workflows/build.yml?query=branch%3Amaster)
[![Powered by Codex](https://img.shields.io/badge/Powered%20by-Codex-111827?logo=openai&logoColor=white)](https://openai.com/codex/)
![Xposed Module](https://img.shields.io/badge/Xposed-Module-blue)
![Android SDK min 31](https://img.shields.io/badge/Android%20SDK-%3E%3D%2031-brightgreen?logo=android)
![Android SDK target 36](https://img.shields.io/badge/Android%20SDK-target%2036-brightgreen?logo=android)

> **Powered by Codex** — 本定制版本的分析、开发、复查、文档与自动构建配置由 Codex 协助完成。

AADisplay 是一个通过虚拟屏幕在 Android Auto 中显示手机应用的 Xposed 模块。本仓库 fork 自 [Stashboy/AADisplay](https://github.com/Stashboy/AADisplay)，保留其对新版 Android Auto、显示生命周期、Surface、输入及 LSPosed 配置读取等方面的适配，同时恢复并调整了部分上游行为，包括任意应用包名/Activity 组件启动、自定义虚拟屏销毁延迟，以及创建和销毁虚拟屏前后的 Root Shell 命令，并参考了 [ankittand0n/AADisplay](https://github.com/ankittand0n/AADisplay) 的原生 Android Auto 操作栏与实体按键焦点处理方式。

## 版本与适配情况

- 当前模块版本：`0.23#17.2-r7`
- 当前已适配并实车测试的 Android Auto 版本：`17.2`
- 最低 Android 系统版本：Android 12（API 31）

## 主要功能

- 在 Android Auto 中创建并显示手机虚拟屏。
- 可填写任意可启动应用包名，例如 `com.example.app`。
- 支持明确指定 Activity，例如 `com.example.app/.MainActivity`。
- 可选择连接 Android Auto 后自动打开 AADisplay。
- 使用 Android Auto 原生底部操作栏及焦点导航。
- 支持实体返回键、方向键、确认键和旋钮输入（仅在我自己的车机上测试）。
- 可设置 Android Auto 断开后的虚拟屏销毁延迟。
- 可在创建虚拟屏前、销毁虚拟屏后执行自定义 Root Shell 命令。
- 保留新版 Android Auto 兼容、虚拟屏生命周期及输入稳定性修复。

## 使用条件

- Android 12 或更高版本（最低 API 31）。
- 已取得 Root 权限。
- 已安装并启用 LSPosed 或兼容的 Xposed 框架。
- 已安装 Android Auto。

## 安装与配置

1. 下载并安装 APK。
2. 在 LSPosed 中启用 AADisplay 模块，作用域至少选择：
   - 系统框架（`android`）
   - Android Auto（`com.google.android.projection.gearhead`）
3. 打开手机端 AADisplay，设置自动打开、默认启动包名、销毁延迟及可选 Shell 命令。
4. 保存设置后重启手机，使系统框架与 Android Auto 重新加载模块和配置。
5. 在车机的 Android Auto 应用列表中打开 AADisplay；如果启用了“自动打开”，AA 界面初始化后会自动进入 AADisplay（即启动你填写的应用包名）。

## 下载

本仓库通过 GitHub Actions 自动构建 APK，不单独维护 GitHub Releases。

前往 [Build AADisplay 工作流](https://github.com/azip52/AADisplay/actions/workflows/build.yml?query=branch%3Amaster)，打开 `master` 分支最近一次成功的构建，在页面底部的 **Artifacts** 中下载：

- `aa-display-release`：Release/R8 构建，推荐日常测试。
- `aa-display-debug`：Debug 构建，适合排查问题。

GitHub Actions 构建产物可能需要登录 GitHub 才能下载，并会按照 GitHub 的保留策略自动过期。仓库未配置 `KEY_ANDROID` 签名密钥时，构建会使用临时 Debug 签名；不同构建之间若无法覆盖安装，请先卸载旧测试版。

## 本地构建

需要 JDK 17 和 Android SDK：

```bash
./gradlew :aa-display:assembleDebug
./gradlew :aa-display:assembleRelease
```

APK 输出目录：

```text
aa-display/build/outputs/apk/
```

## 致谢

- 感谢最早的作者 [Nitsuya](https://github.com/Nitsuya) 创建 [Nitsuya/AADisplay](https://github.com/Nitsuya/AADisplay)。
- 感谢 [Stashboy/AADisplay](https://github.com/Stashboy/AADisplay) 对新版 Android Auto 与显示稳定性的持续适配。
- 感谢 [ankittand0n/AADisplay](https://github.com/ankittand0n/AADisplay) 的相关实现，为原生操作栏和实体按键行为提供参考。

## 许可

本项目沿用上游许可，详情请参阅 [LICENSE](LICENSE)。
