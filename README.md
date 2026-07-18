# LiquidGlassClickGUI

一个面向 Android 12+ 的纯 Kotlin / Jetpack Compose ClickGUI 界面工程，使用 Kyant0 Backdrop 实现液态玻璃视觉效果。

> [!IMPORTANT]
> 当前仓库只实现界面、交互状态与宿主界面挂载，不包含游戏功能后端。

## 功能概览

- 液态玻璃背景、侧边分类导航和模块卡片
- Toggle、Slider、ComboBox 与 Dialog 动画
- 长按打开模块设置页
- 主悬浮按钮、功能快捷按钮与灵动岛式状态提示
- Compose Array List 与持久 HUD 覆盖层
- LOX / Xposed 入口，可将 Compose View 挂载到 `com.netease.x19` 的 Activity

## 工程边界

本项目不包含：

- C / C++、JNI、NDK 或 CMake 构建
- Minecraft / Bedrock SDK
- ShadowHook 或其他 native inline hook
- 内存读写、数据包处理或游戏功能实现

模块开关和设置只更新进程内的 Compose UI 状态，不会改变游戏行为。

## 环境要求

- JDK 21（项目编译出的 JVM 字节码目标为 Java 17）
- Android Studio 或命令行 Android SDK
- Android SDK Platform 37（`compileSdk = 37`）
- 最低 Android 12（API 31）

## 构建

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux：

```bash
./gradlew :app:assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 项目结构

```text
LiquidGlassClickGUI/
├─ .github/workflows/       GitHub Actions 构建检查
├─ app/
│  ├─ libs/                 Xposed API（仅编译期使用）
│  └─ src/
│     ├─ debug/             Debug 专用入口或工具
│     └─ main/
│        ├─ assets/         Xposed 模块入口声明
│        ├─ java/
│        │  ├─ com.kyant…   基于官方示例修改的玻璃组件
│        │  └─ dev.liquid.clickgui/
│        │     ├─ data/     UI 目录与目标应用契约
│        │     ├─ glass/    液态玻璃导航和弹窗
│        │     ├─ model/    界面模型
│        │     ├─ overlay/  独立覆盖层
│        │     ├─ ui/       Compose 页面与交互
│        │     └─ xposed/   宿主识别与界面挂载
│        └─ res/            Android 资源
├─ docs/                    开发文档
└─ gradle/                  Gradle Wrapper
```

详细的界面扩展方式见 [开发指南](docs/DEVELOPMENT_GUIDE_ZH.md)。

## 第三方代码与许可

第三方来源、修改说明和许可信息见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。相关 Apache License 2.0 全文见 [KYANT0-APACHE-2.0.txt](KYANT0-APACHE-2.0.txt)。

项目自身的开源许可证尚未指定。在公开发布前，请根据你的授权意图添加项目级 `LICENSE`；在此之前，默认不授予第三方复制、修改或分发项目自有代码的权利。
