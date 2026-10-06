# 纯 UI 后续开发指南

## 工程边界

本工程只负责 Compose UI。LOX / Xposed 代码仅用于识别目标 Activity、挂载界面和转发输入事件，不提供游戏功能。

不要在当前工程中加入 JNI、CMake、NDK、SDK 地址表、内存访问或数据包处理。需要功能后端时，应建立独立工程并定义清晰的进程间接口。

## 主要源码

- `data/UiModuleGateway.kt`：演示模块目录及设置
- `model/ModuleModels.kt`：模块和设置模型
- `ui/ClickGuiScreen.kt`：主界面布局与模块卡片
- `ui/ClickGuiController.kt`：纯 UI 状态和交互
- `glass/`：液态玻璃导航和 Dialog
- `ui/LiquidFloatingButton.kt`：主悬浮按钮
- `ui/ModuleQuickButtonRail.kt`：独立功能快捷按钮
- `ui/ModuleSwitchDynamicIsland.kt`：开关提示
- `xposed/HostComposeOverlay.kt`：将 Compose View 挂载到宿主

以上路径均相对于 `app/src/main/java/dev/liquid/clickgui/`。

## 添加界面模块

在 `UiModuleGateway.referenceCatalog()` 中添加 `ModuleUiModel`。支持的设置类型包括：

- `BoolSetting`：Toggle
- `FloatSetting`：浮点 Slider
- `IntSetting`：整数 Slider
- `EnumSetting`：ComboBox
- `ActionSetting`：命令按钮

所有设置只改变进程内 UI 状态。不要使用 `READY`、`APPLIED` 等文案暗示游戏后端已经生效。

## 提交前验证

执行 Debug 构建：

```powershell
.\gradlew.bat clean :app:assembleDebug
```

并检查以下内容：

1. APK 中不存在 `libliquidbridge.so`、Minecraft SDK、ShadowHook 或项目自有 `.so`。
2. `local.properties`、签名文件、APK 和构建目录没有进入 Git 暂存区。
3. 第三方示例代码的来源和修改说明仍与 `THIRD_PARTY_NOTICES.md` 一致。

