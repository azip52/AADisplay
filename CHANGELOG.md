# 更新记录

## 0.23#17.8-r8（2026-11-02，待实车验证）

- 从 Stashboy/AAPwned 的 `6a2817d4` 手工移植三项改动：AA 17.8 Auto Open 启动路径、自适应虚拟屏 DPI、移除 Presentation 标志。
- Auto Open 改用车载 SDK 的 `CarFirstPartyManager.startCarActivity`，移除对旧 `CarSystemUiControllerService.a(Intent)` 的依赖；保留默认关闭、独立于侧栏、约 1 秒延迟、10 秒去抖及失败后重试。
- 未手动指定有效 DPI（配置值不大于 50）时，按虚拟屏短边计算 `短边像素 × 160 / 600`，目标为 sw600 大屏布局；大于 50 的手动值继续优先。应用字号和布局可能变化。
- 虚拟屏不再登记为 Presentation 展示屏，保留其他显示标志。
- 修复 CI SDK 准备失败：显式安装 platform-tools，避免请求已不可用的旧 tools 包。
- 保留任意包名/组件启动、原生 AA 操作栏、实体键与旋钮、自定义销毁秒数、创建前和销毁后 Shell 命令及中文设置。
- 本轮仅移植上述代码片段：完整上游基线仍为 `c770d65`，未整体同步到 `6a2817d4`；未引入麦克风替换 Home、强制侧栏、Waze/Maps 控制或更名。
- 验证范围：编译验证不能替代实车验证；AA 17.8 自动打开、媒体应用退出全屏、显示重连与实体输入均待用户车机验证。

## 0.23#17.2-r7

- 恢复“自动打开”开关，并使其不依赖自定义纵向侧栏。
- 主界面设置和激活状态中文化。
- 修复激活状态卡片文字与图标颜色对比度。
- 重写 README，补充项目来源、致谢、Actions 下载方式及 Powered by Codex。
- 删除独立的 `RELEASE_NOTES` 文件。

## 0.23#17.2-r6

- 恢复 Nitsuya 风格的返回键事件注入，修复部分车机返回键无效。
- 默认使用 Android Auto 原生操作栏与焦点导航。
- 方向键交由 Android Auto 处理，可将焦点移动到原生底部操作栏。
- 确认键保持映射为 `DPAD_CENTER`。

## 0.23#17.2-r5

- 以 Stashboy 的新版 Android Auto 适配为基础建立定制版本。
- 移除 Waze、Google Maps、手机桌面 Launcher 限定等新增功能。
- 默认启动目标支持任意应用包名或明确的 Activity 组件。
- 恢复任意秒数的虚拟屏销毁延迟。
- 恢复创建虚拟屏前、销毁虚拟屏后的 Root Shell 命令。
- 修复显式组件任务追踪、虚拟屏销毁顺序和旧系统状态迁移。
- GitHub Actions 同时构建 Debug 与 Release APK。
