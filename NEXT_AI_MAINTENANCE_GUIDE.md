# AADisplay 后续 AI 维护与上游同步指导手册

> 更新日期：2026-11-16（Asia/Taipei）
>
> 交接对象：负责维护 azip52/AADisplay 的 AI。
>
> 编写前已核对的 master：d6e879d057ae48d78c8275eed77a503f4c713288。
>
> 本文件记录产品约束、代码现状、同步边界和验证证据。开始新任务前，完整阅读本文件及 AGENTS.md，并重新核对远程 master、源码、提交记录和 Actions。用户最新要求优先；文档与代码存在差异时，应说明差异，不能把快照当成实时状态。

## 1. 项目身份与维护目标

- 目标仓库：[azip52/AADisplay](https://github.com/azip52/AADisplay)。
- 原作者与原项目：[Nitsuya/AADisplay](https://github.com/Nitsuya/AADisplay)。
- 兼容性主要参考：[Stashboy/AAPwned](https://github.com/Stashboy/AAPwned)。上游已从 Stashboy/AADisplay 更名；本仓库没有跟随其应用更名和包名迁移。
- 原生 AA 操作栏、焦点和按键的重要参考：[ankittand0n/AADisplay](https://github.com/ankittand0n/AADisplay)。
- 曾比较 [mohdafix/AADisplay](https://github.com/mohdafix/AADisplay)，没有采用其纵向侧栏方案。
- 本定制版本保留 **Powered by Codex** 和原项目致谢。

维护目标：选择性吸收新版 Android Auto 的兼容、显示生命周期、输入、配置读取与工程稳定性改进，同时保留本仓库的产品行为。不能直接合并整个 Stashboy 分支，也不能只凭提交标题判断是否适合移植。

## 2. 当前版本、分支和授权

| 项目 | 已核对状态 |
|---|---|
| 应用名称 | AADisplay |
| applicationId / namespace | io.github.nitsuya.aa.display |
| versionName | 0.23#17.8-r8 |
| versionCode | 3014 |
| compileSdk / targetSdk | 36 / 36 |
| minSdk | 31（Android 12） |
| 分支 | 仅保留 master |
| 构建 | GitHub Actions，JDK 17，同时产出 Debug 与 Release/R8 APK |
| 分发 | Actions artifacts；不单独维护 GitHub Releases |
| 版本说明 | CHANGELOG.md；不保留独立 RELEASE_NOTES_* 文件 |

用户于 2026-11-02 明确授权：**后续新代码直接提交到 master，无需另建分支或 PR，也无需为常规提交再次请求授权。** 此约定已写入根目录 AGENTS.md。

执行时：

1. 先核对远程 master，检查工作区；不得覆盖用户尚未提交的修改。
2. 按用户任务完成实现与适当检查，直接提交到 master。
3. 使用普通快进推送，或带预期旧 SHA 的 ref 更新；不强制覆盖远程历史。
4. 推送后等待 Actions 结束，报告提交、构建结果和产物。
5. 不新增工作分支。原 PR #1 已合并，codex/aa178-display-compat 已删除。
6. 不把本次分支清理授权扩大为自动删除以后他人新建且包含未合入工作的分支。
7. 创建 Release、改变签名或更改 GitHub 设置，应按具体任务与已有授权执行，不从普通代码维护推导出无限范围授权。

纯文档整理不递增 APK 版本号；功能版本发布同步更新 versionName、versionCode、README 与 CHANGELOG。

## 3. 上游基线与已完成的定制

### 3.1 完整基线与部分移植必须分开

- 完整继承的 Stashboy 基线：`c770d65c1338245dbd6ce1349d1a5243f802cfc1`。
- 基线标题：release: v0.23.6 Android Auto 17.2 display hardening。
- 此后从 `6a2817d4e7eb1e75cc658820ea863056ac4cb419` 手工移植三项改动，形成定制提交 `51cb254`：
  1. AA 17.8 Auto Open 改用车载 SDK 启动路径。
  2. 自适应虚拟屏密度。
  3. 移除虚拟屏 PRESENTATION 标志。
- **没有整体同步到 6a2817d4，更没有整体同步到上游最新 main。** 后续审查仍须从完整基线比较，并排除已部分移植的代码片段，不能把上述三项重复移植。

### 3.2 本仓库提交追溯

| 提交 | 内容 |
|---|---|
| a68bc47 | 定制启动流程与实体按键，恢复自由配置 |
| eab7dfe | 加固真实包名任务追踪、旋钮和旧状态迁移等 |
| 0a23703 | 恢复原生 AA 导航与普通返回键注入 |
| 95189f1 | 恢复独立 Auto Open、中文设置与状态配色 |
| 78643a4 | 完善兼容性说明和项目致谢 |
| 05c1499 | 简化兼容性文档 |
| 51cb254 | 选择性移植 AA 17.8 启动和显示三项改动 |
| b3eb97b | 修复 CI 安装旧 Android SDK tools 包失败 |
| d6e879d | 记录用户授权的直接提交 master 约定 |

前六个定制提交相对完整基线涉及 24 个文件，新增 511 行、删除 1626 行；这包含代码、布局和文档，仅供历史追溯，不是后续任务的变更量。

### 3.3 已查看的上游提交及纳入边界

截至本文件更新时，远程 Stashboy/AAPwned 的 main 为 `2cc6fa1258ef27fcaa8df3b62fbd7dbdf18412c7`。以下表格不是“全部已同步”的清单：

| 上游提交 | 内容与本仓库状态 |
|---|---|
| 6a2817d4 / v0.23.11 | 仅纳入上述三项启动/显示代码；其他改动未整体合入 |
| fd97e806 / v0.23.12 | 麦克风槽位覆盖为 AADisplay Home；未纳入 |
| 6c116c5c / v0.23.13 | 会话关闭开关、销毁后返回 AA 启动器、悬浮控制器交互等；未纳入 |
| 117d5167 / v0.23.14 | AAPwned 更名及全项目包名迁移；未纳入 |
| 4d5c992d | 上游更名迁移文档；未纳入 |
| 7256dce8 / v0.23.15 | 修复上游更名漏改 xposed_init，并重新加入 Waze 控制；未纳入 |
| cde71802 / v0.23.16 | Waze 拦截机制精简与相关性能优化；未纳入 |
| 2cc6fa12 / v0.23.17 | 标题为默认关闭 CloseLauncherDashboard、恢复原生 AA Home → Dashboard；已发现但未进行完整差异审查或移植 |

上游的 17.3～17.6 候选版本记录被汇总在 6a2817d4 中，不能按版本标题推断存在多次新的 hook 修复。是否适用应检查实际差异。

未纳入的交互变化不等同于未来永久禁止：例如“虚拟屏销毁后退出失效画面”可单独评估为稳定性修复，但不能连带改变关闭按钮或其他产品语义。

## 4. 必须保留的产品行为

### 4.1 任意包名或显式组件启动

主页“默认启动包名”允许自由输入：

```text
com.example.app
com.example.app/.MainActivity
```

- 仅填包名：由 system_server 的 PackageManager 解析可启动 Activity。
- 显式组件：解析明确的 Activity。
- 设置页只做语法校验，不因 Android 包可见性限制导致查不到包就拒绝保存。
- 解析失败记录日志，不自动回退手机桌面，不恢复 Launcher 下拉列表或“必须安装第二桌面”等限制。
- 打开车机 AADisplay 后，虚拟屏显示用户配置的应用。

AaVirtualDisplayAdapter 必须区分原始输入 mLauncherSpec、解析组件 mLauncherComponent、真实包名 mLauncherPackageName 和任务 ID mLauncherPackageTaskId。组件字符串不能当作包名用于任务查询、进程保护或 force-stop。

内部 startLauncher() 重新打开配置应用；它与 AA 自己的 Home/启动器按钮是不同操作。配置应用任务被移除时清理记录，不自动重启手机桌面；最近任务与第二任务切换不恢复旧 HomePackage 特殊排除。已移除的 Home/Back 后主动清理 PiP 任务逻辑，不应未经审查直接带回。

### 4.2 原生 AA 操作栏与焦点

- CloseLauncherDashboard 默认 false，设置保存时继续写入 false。
- MainActivity.disableLegacyCustomAaRail() 迁移旧配置为 false。
- AaUiHook 仅在旧配置明确为 true 时安装遗留的 layout/facet bar/click hooks。
- 正常配置保留 AA 自己的栏布局选择和焦点图，不无条件覆盖 canonical vertical rail。
- 未纳入麦克风槽位覆盖 Home 按钮。

遗留侧栏源码仍在仓库中；存在代码不代表正常配置会启用。彻底删除前应核查调用链与兼容用途。

### 4.3 实体键与旋钮

- DPAD_CENTER / Enter：向虚拟屏注入 DPAD_CENTER。
- 上下左右及 SYSTEM_NAVIGATION_*：交给 super.onKeyDown()，允许焦点移动到 AA 原生栏。
- Back：向虚拟屏注入普通 KeyEvent，source 为 SOURCE_KEYBOARD。
- 不恢复 FLAG_FROM_SYSTEM | FLAG_VIRTUAL_HARD_KEY；该组合曾在用户车机上导致返回键失效。
- 旋钮接收 ACTION_SCROLL，优先 AXIS_SCROLL，兼容 AXIS_VSCROLL。
- 正值 → DPAD_DOWN；负值 → DPAD_UP；根据幅度生成步数，每次最多 20 步。
- 用户接受长按表现为连续短按，不必额外引入复杂状态机。
- 实体输入的历史实车结论仅针对用户自己的车机。不同 OEM 方向不一致时，优先考虑可配置反转，不统一颠倒默认方向。

### 4.4 Auto Open：当前已改用车载 SDK

- 主页开关在默认启动包名上方，默认关闭。
- hookAutoOpen() 仍 hook LayoutInfo 构造，延迟约 1 秒，10 秒去抖。
- 启动流程为 Car.createCar() → connect() → getGmsCarFirstPartyManager() → startCarAaDisplay() → CarFirstPartyManager.startCarActivity()，Intent 指向模块自己的 AaActivityService。
- 连接回调中启动失败会记录日志、重置去抖时间；完成后尝试 disconnect()。
- Auto Open 独立于遗留侧栏开关及其视图资源，不依赖 Waze、Maps 或手机 Launcher。
- **不再调用 CarSystemUiControllerService.a(Intent)**。旧手册中的该调用是历史实现，不能恢复。
- 进入 AADisplay 后，再由虚拟屏流程启动用户配置应用。

仍需注意：LayoutInfo 的字符串/构造形状解析可能随 AA 变化；AACarUtil 获取 SDK 管理器时仍反射访问包装对象的 a 字段。更换启动路径不代表完全消除了所有内部 API 风险。

### 4.5 当前显示参数行为

- 使用 TextureView 实际显示区域的像素宽高请求虚拟屏。
- VirtualDisplayDpi > 50 时使用手动配置；否则按短边计算：
  `max(1, 短边像素 × 160 / 600)`，整数除法。
- 例：720×480 → 128 dpi；1920×720 → 192 dpi；1920×1080 → 288 dpi。
- 当前主页没有手动密度输入项；底层配置键仍存在。
- AndroidAutoDpi 与 VirtualDisplayDpi 是不同配置，分别涉及 AA 自身与虚拟屏。
- 创建虚拟屏已移除 PRESENTATION，保留 PUBLIC、SECURE、OWN_CONTENT_ONLY 等其他标志。
- CoreManagerService 保留会话显示参数锁定：新会话或横竖方向变化时重新确定，同一会话重连通常沿用锁定的宽高和密度。
- 本段是已合入代码的行为说明，不构成新增设置或重构任务。

### 4.6 自定义销毁秒数与 Shell

主页直接显示：任意非负整数秒销毁延迟（默认 180，0 为立即）、创建虚拟屏前执行、销毁虚拟屏后执行；命令支持多行 Root Shell。

释放顺序必须保持：显示资源和窗口释放 → 显示 ID/密度等状态重置 → destroyVirtualDisplayAfter() → 最后解绑 ShellManager。名称为 After 的命令必须发生在实际释放之后。

销毁时保护模块自身、配置应用的真实包名和手机主显示的前台应用，不能将显式组件误作包名。

### 4.7 中文界面、配置与旧状态迁移

主页与提示保持中文；激活/警告/错误状态图标和文字使用日夜主题高对比颜色；GitHub 菜单指向 azip52/AADisplay。

旧 Waze/Google Maps 禁用功能已移除：包括设置、管理器、专用 WazeHook、入口与 Waze 作用域。MainActivity 中保留旧状态恢复代码，必要时重新启用曾被禁用的组件、重置全局标志并移除旧偏好。这是兼容迁移，不是恢复禁用功能；不要按关键词机械删除。

## 5. 上游吸收与排除规则

适合逐项审查吸收：

- AA 新版本类、方法、资源和构造函数解析。
- TextureView / Surface 绑定、重连、尺寸变化、VirtualDisplay 创建防重与回收。
- Binder / PackageManager 异常保护、IME、触控、配置可读性。
- 无产品语义变化的线程安全、性能、日志和空值保护。
- 有必要且能验证的构建工具与依赖更新。

未经用户明确改变需求，不引入：

- Waze/Google Maps 禁用或强制修改它们组件状态的功能。
- 手机 Launcher 查询限定、自动回退或仅允许桌面列表选取。
- 强制纵向栏、替换原生底栏或麦克风入口。
- 固定销毁档位、删除创建前/销毁后命令、将设置移入二级页面。
- 把 Auto Open 与侧栏或导航应用控制绑定。
- AAPwned 品牌、包名迁移，或与用户未选择的新交互捆绑移植。

## 6. 关键源码地图

下列 Java/Kotlin 路径均位于 aa-display/src/main/java/io/github/nitsuya/aa/display/：

| 路径（相对上述目录） | 重点 |
|---|---|
| ui/aa/AaDisplayActivity.java | 实体键、方向焦点、旋钮 |
| ui/aa/AaVirtualDisplayAdapter.kt | 启动目标、任务追踪、输入、显示标志、销毁顺序 |
| ui/aa/fragment/AaMainFragment.kt | TextureView、Surface、显示区域和自动密度 |
| xposed/CoreManagerService.kt | 显示参数锁定、创建防重、会话管理 |
| xposed/hook/aa/AaUiHook.kt | Auto Open、旧侧栏门控、圆角与解析 |
| xposed/hook/aa/AaDpiHook.kt | AA 自身显示密度 hook |
| util/AACarUtil.kt | 车载 SDK 管理器与启动工具 |
| ui/main/MainActivity.kt | 中文主页、校验、保存、旧状态迁移 |
| ui/window/DisplayWindow.kt | 控制窗口与销毁倒计时 |
| util/AADisplayConfig.kt | 配置键、默认值 |
| util/SharedPreferencesAccess.kt | Hook 可读配置 |
| service/ShellManagerService.kt | Root Shell 执行 |
| xposed/XposedInit.kt 与 xposed/hook/aa/ | 作用进程、hook 加载 |

另需按任务阅读：activity_main.xml、strings.xml、values/colors.xml、values-night/colors.xml、AndroidManifest.xml、values/arrays.xml、aa-display/build.gradle.kts、.github/workflows/build.yml、README.md、CHANGELOG.md、AGENTS.md。

当前 xposed_scope：

```text
android
com.google.android.projection.gearhead
com.autonavi.amapauto
com.ss.squarehome2
```

后两个包来自历史实现，无调用链和实车证据时不要武断删除。

## 7. 每次维护的流程

1. 完整阅读本手册、AGENTS.md、README、CHANGELOG；核对 master 最新 SHA、分支列表、工作区和 Actions。
2. 获取上游最新 main，记录比较范围。Stashboy 远程地址使用 https://github.com/Stashboy/AAPwned.git；已有 remote 直接 fetch 或更新 URL，不重复添加。
3. 从完整基线 c770d65 比较；同时结合部分移植清单和当前代码去重。以后只有确认某提交全部适用改动已处理，才能相应推进完整审查基线。
4. 逐提交分类：直接吸收、手工移植、排除或暂缓。记录来源 SHA、解决的问题、涉及文件、产品行为影响与理由。
5. 按用户当前任务实施。用户只要求审查时先汇报；已经授权实现时继续完成，不为日常实现和直接提交 master 反复请求确认。
6. 小批量修改并完成适当检查，不混入无关上游功能。
7. 直接提交 master，等待 Actions，报告证据与未验证范围。更新部分移植记录，避免下次重复处理。

基本只读比较：

```powershell
git fetch origin --prune
git fetch stashboy --tags
git log --oneline c770d65..stashboy/main
git diff --stat c770d65...stashboy/main
git diff c770d65...stashboy/main -- aa-display/src .github aa-display/build.gradle.kts
```

Windows 环境若 Git 默认 schannel 报 SEC_E_NO_CREDENTIALS，可对当次命令使用 `-c http.sslBackend=openssl`；确有需要时再使用本机已配置代理。不要关闭 TLS 校验，也不要将机器代理地址写入项目配置。Git 写操作凭据不可用时，可使用已连接的 GitHub 工具，不输出凭据。

## 8. 检查、构建与产物

静态检查：

```powershell
git diff --check
rg -n "Waze|GoogleMaps|DisableWaze|DisableGoogle|queryIntentActivities|FLAG_VIRTUAL_HARD_KEY|sys_ui_layout_canonical_vertical_rail" aa-display/src
```

每个命中结合上下文判断。重点核对：真实包名追踪与保护、资源释放顺序、方向/返回/确认/旋钮路由、独立 Auto Open、旧侧栏门控、配置可读性、主页默认值、失败隔离和显示重连。

当前 CI：

- actions/checkout@v4、actions/setup-java@v4、android-actions/setup-android@v3、actions/upload-artifact@v4。
- JDK 17；API 36；build-tools 35.0.0。
- setup-android 显式设置 `packages: platform-tools`；不要去掉，否则 action 默认请求的旧 tools 包曾导致 SDK 准备失败。
- 命令：`./gradlew :aa-display:assembleDebug :aa-display:assembleRelease --stacktrace`。
- artifact 名称：`aa-display-debug`、`aa-display-release`。
- APK 名称：aa-display-0.23-17.8-r8.apk（Debug 与 Release 在各自产物中）。
- 下载页：[主分支工作流](https://github.com/azip52/AADisplay/actions/workflows/build.yml?query=branch%3Amaster)。

本地无可用 JDK/SDK 时如实说明，并使用 Actions 完成编译验证。文档变更会触发现有工作流；不为一次文档整理擅改触发规则。

签名按当前 build.gradle.kts 判断：KEY_ANDROID 存在时选择 release keystore，否则回退 Debug 签名。历史曾未配置稳定签名，不能未经检查就断言当前 Secret 状态。不同构建能否覆盖安装应核对实际签名；不要上传私钥。非阻断 action/Node 弃用警告按需处理，不盲目升级全部构建工具。

## 9. 已有验证证据与边界

| 范围 | 证据或状态 |
|---|---|
| 17.2 历史产品行为 | 原交接记录为用户车机已验证；实体输入结论仅适用于该车机 |
| r7 构建 | [30700606271](https://github.com/azip52/AADisplay/actions/runs/30700606271) 成功 |
| r8 主分支构建 | [41400925466](https://github.com/azip52/AADisplay/actions/runs/41400925466) 成功，head 为 d6e879d；Debug 与 Release 均上传 |
| r8 代码检查 | diff --check、三个源码变更及定制约束复查；五组密度数值样例核对，不等同于运行测试 |
| AA 17.8 用户车机回归 | 目前没有完整通过记录，不能标为已实车验证 |
| 上游 DHU/设备记录 | 仅为上游证据，不能替代本仓库在用户车机上的测试 |

2026-11-16 编写时 master 最新构建仍为上表 r8 构建；本手册自身提交若触发新工作流，应以新的实际结果补充交付汇报。不要把取消的文档工作流直接判断为代码构建故障。

## 10. 实车回归清单

记录 AA 精确版本、手机 Android/API、LSPosed、车机型号、模块版本和提交。启用作用域并重启必要进程；设置可能被 Xposed/AA 缓存。

- 启动：Auto Open 开/关；普通包名、显式组件；语法正确但不存在的目标只记录失败、不回退桌面。
- 显示：连接、断开、快速重连、亮灭屏、尺寸或方向变化；无黑屏、重复显示与崩溃。
- 导航：AA 原生栏和焦点可达；返回作用于虚拟屏应用；确认等同 DPAD_CENTER；旋钮双向和步数正常。
- 销毁：0 秒和自定义秒数；用无害标记日志验证创建前/销毁后命令顺序；不误杀配置应用和主屏前台应用。
- 配置：重启后保存有效；日夜激活状态可读；模块/系统加载版本显示准确。
- 显示参数：实际生效宽高和密度符合当前配置；完整销毁后重建再比较不同配置，防止会话锁定影响结论。
- 文档：APK、README、CHANGELOG 与验证结论一致。

保存必要的 logcat、截图或短视频、Actions URL、测试矩阵及未验证项。没有设备时明确写“待实车验证”，不虚构测试。

## 11. 后续风险与维护注意

1. LayoutInfo 解析仍与 Auto Open 安装有关；考虑按实际需要改进解析和失败隔离。
2. 车载 SDK 管理器获取仍有反射字段依赖，需要随 AA 更新复查。
3. 旧侧栏与旧地图状态迁移源码有明确门控或兼容用途，不能机械删除。
4. OEM 实体键/旋钮差异不能用其他设备的推测代替用户实车结论。
5. 配置可见性与进程缓存会影响保存、生效判断。
6. 稳定签名和 action 运行时需持续维护，但只有当前证据支持时才改动。
7. 上游更名后大批路径变化可能掩盖真正的功能差异；本仓库继续使用原包名。
8. 2cc6fa12 等尚未完整审查的提交，不因标题看似符合需求就直接合入。

## 12. 交付汇报与下个 AI 的启动提示

每轮维护说明：来源提交及具体移植片段、仍未纳入项、修改文件和行为、本地/CI/实车分别完成了什么、master 提交 SHA、Actions 链接、artifact 名称与剩余验证项。

可直接交给下一位 AI：

```text
请先逐字完整阅读根目录 NEXT_AI_MAINTENANCE_GUIDE.md 和 AGENTS.md，再核对远程 master、README、CHANGELOG、源码、最新 Actions 与上游提交。

目标是维护 azip52/AADisplay 的定制行为，选择性吸收 Stashboy/AAPwned 的兼容与稳定性改进。完整上游基线仍为 c770d65；6a2817d4 只移植了 AA 17.8 车载 SDK 自动打开、自适应虚拟屏密度和移除 PRESENTATION 三项，不能把它当成整体同步完成。

保留任意包名/显式组件启动、原生 AA 导航、现有实体输入、独立 Auto Open、自定义销毁秒数、创建前/销毁后 Shell 与中文主页。不要引入地图应用禁用、Launcher 限定或回退、强制侧栏、麦克风替换、固定延迟或上游包名迁移。

根据用户当前请求决定仅审查还是实施。已经授权的实现直接提交 master，不另建分支或 PR，不反复请求常规推送授权；核对远程 SHA，不强制覆盖历史。逐提交检查、记录部分移植边界，完成适当检查并等待 Actions。明确区分编译通过和实车通过。
```

---

维护原则：**跟进 Stashboy 的兼容能力，同时守住用户已经确定的产品行为。**
