# My-Git Android UI Projects

本仓库用于集中维护 Android ClickGUI、HUD 与 Xposed 界面实验项目。当前 `main` 分支包含三个彼此独立的 Gradle 工程，构建时请进入对应目录或使用对应 Wrapper。

## 项目目录

| 项目 | 路径 | 技术栈 | 说明 |
| --- | --- | --- | --- |
| LiquidGlassClickGUI | 仓库根目录 | Kotlin、Jetpack Compose | 原有液态玻璃 ClickGUI 界面工程 |
| LiquidPE | [`LiquidPE/`](LiquidPE/) | Java、Android View、Xposed API 82 | 水影风格 ClickGUI、HUD、配置、音乐与材质界面模块 |
| MD3 Click GUI NextGen | [`MD3-ClickGui-NextGen/`](MD3-ClickGui-NextGen/) | Kotlin、Jetpack Compose、Material 3 | Material 3 Expressive 三栏 ClickGUI、ArrayList HUD 与匿名音乐播放器 |

## LiquidPE

LiquidPE 提供以下界面与模块能力：

- ClickGUI 分类面板、模块列表及完整设置组件
- HUD ArrayList、按键列表和通知界面
- 面板拖拽、独立中心缩放和全局 UI 比例调节
- 中英文全局文本切换与自定义字体
- 配置保存、加载、导出和删除
- Music 与 Materials 顶部分区
- 标准 `assets/xposed_init` 入口和 `com.netease.x19` 默认作用域
- 无桌面启动入口的模块化部署方式

最新源码标签：[`LiquidPE-v1.0-20260826`](https://github.com/khiet2wggga4412-hub/My-Git/tree/LiquidPE-v1.0-20260826)

### 构建 LiquidPE

Windows PowerShell：

```powershell
cd LiquidPE
.\gradlew.bat :app:test :app:assembleRelease --no-daemon
```

macOS / Linux：

```bash
cd LiquidPE
./gradlew :app:test :app:assembleRelease --no-daemon
```

未配置本地签名时会生成未签名 Release APK。发布签名文件、`keystore.properties`、本地 SDK 配置和构建缓存均不会提交到仓库。

## LiquidGlassClickGUI

[`LiquidGlassClickGUI/`](LiquidGlassClickGUI/) 是最早的 Kotlin / Jetpack Compose 液态玻璃 ClickGUI 工程，独立 Gradle 工程，自带 Wrapper。依赖 `io.github.kyant0:backdrop` 与 `shapes`，其许可证见 [`LiquidGlassClickGUI/KYANT0-APACHE-2.0.txt`](LiquidGlassClickGUI/KYANT0-APACHE-2.0.txt)。

Windows PowerShell：

```powershell
cd LiquidGlassClickGUI
.\gradlew.bat :app:assembleDebug
```

需要 JDK 21（工程通过 toolchain 声明）；SDK 路径由 Android Studio 写入 `local.properties`（不提交）。开发资料见 [`LiquidGlassClickGUI/docs/`](LiquidGlassClickGUI/docs/) 与 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。

## MD3 Click GUI NextGen

[`MD3-ClickGui-NextGen/`](MD3-ClickGui-NextGen/) 是一套以 Material 3 Expressive 为基准重写的横屏 ClickGUI，独立 Gradle 工程，自带 Wrapper：

- 三栏工作区：分类栏、模块列表、模块设置，三栏各自独立滚动
- 分类栏可折叠为图标栏，Music / Config 常驻底部锚定，不随其他分类滚动
- 跨分类的中英文模块名搜索
- 28 个模块定义，按分类拆分在 `ui/modules` 下
- ArrayList HUD 浮层，按模块名长度排序，字号、图标、背景与不透明度可调
- 匿名 NetEase 接口的音乐播放（搜索、播放地址、歌单），不含登录
- 前台播放服务，含音频焦点处理与通知栏控制
- 全部动效取自 `MotionScheme.standard()` 弹簧令牌，不使用自定义时长或缓动曲线
- 主题色、明暗模式、悬浮按钮样式与语言均可切换

Windows PowerShell：

```powershell
cd MD3-ClickGui-NextGen
.\gradlew.bat :app:assembleDebug
```

macOS / Linux：

```bash
cd MD3-ClickGui-NextGen
./gradlew :app:assembleDebug
```

需要 JDK 17 或更高版本，并设置 `JAVA_HOME`；SDK 路径由 Android Studio 写入 `local.properties`（不提交）。设计与实现记录见 [`MD3-ClickGui-NextGen/docs/`](MD3-ClickGui-NextGen/docs/)。

## 仓库结构

```text
My-Git/
├─ LiquidGlassClickGUI/    液态玻璃 ClickGUI（Kotlin / Compose）
│  ├─ app/src/             源码与资源
│  ├─ docs/                开发文档
│  └─ gradle/              Gradle Wrapper
├─ LiquidPE/               LiquidPE 独立 Gradle 工程（Java / Xposed）
│  ├─ app/src/             Android/Xposed 源码与资源
│  ├─ gradle/              Gradle Wrapper 与版本目录
│  ├─ RELEASE.md           发布与校验信息
│  └─ VERIFICATION.txt     构建、签名及回滚记录
├─ MD3-ClickGui-NextGen/   MD3 Click GUI NextGen（Kotlin / Compose / Material 3）
│  ├─ app/src/             Compose 源码（ui/、music/）与资源
│  ├─ docs/                优化评审与参考界面研究记录
│  └─ gradle/              Gradle Wrapper
├─ .github/workflows/      CI：三个工程各自独立构建
├─ THIRD_PARTY_NOTICES.md  第三方组件声明
└─ README.md               项目索引
```

三个工程都是自包含的 Gradle 工程，各有独立 Wrapper、`settings.gradle.kts` 与版本配置，互不依赖。仓库根目录不再放置任何工程，只保留索引与仓库级文件。

## 说明

仓库中的界面模块用于 UI 开发、兼容性测试和技术研究。模块行为取决于使用者配置的运行环境与作用域。请勿提交签名密钥、账号凭据、设备日志或其他敏感文件。
