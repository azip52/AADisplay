# 更新记录

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
