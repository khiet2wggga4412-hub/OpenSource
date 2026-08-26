# My-Git Android UI Projects

本仓库用于集中维护 Android ClickGUI、HUD 与 Xposed 界面实验项目。当前 `main` 分支包含两个彼此独立的 Gradle 工程，构建时请进入对应目录或使用对应 Wrapper。

## 项目目录

| 项目 | 路径 | 技术栈 | 说明 |
| --- | --- | --- | --- |
| LiquidGlassClickGUI | 仓库根目录 | Kotlin、Jetpack Compose | 原有液态玻璃 ClickGUI 界面工程 |
| LiquidPE | [`LiquidPE/`](LiquidPE/) | Java、Android View、Xposed API 82 | 水影风格 ClickGUI、HUD、配置、音乐与材质界面模块 |

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

根目录工程保留原有 Kotlin / Jetpack Compose 液态玻璃界面实现及相关文档。

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug
```

详细开发资料见 [`docs/`](docs/) 和 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。

## 仓库结构

```text
My-Git/
├─ app/                    LiquidGlassClickGUI Android 模块
├─ docs/                   LiquidGlassClickGUI 文档
├─ LiquidPE/               LiquidPE 独立 Gradle 工程
│  ├─ app/src/             Android/Xposed 源码与资源
│  ├─ gradle/              Gradle Wrapper 与版本目录
│  ├─ RELEASE.md           LiquidPE 发布与校验信息
│  └─ VERIFICATION.txt     构建、签名及回滚记录
├─ gradle/                 根目录工程 Gradle Wrapper
└─ README.md               项目索引
```

## 说明

仓库中的界面模块用于 UI 开发、兼容性测试和技术研究。模块行为取决于使用者配置的运行环境与作用域。请勿提交签名密钥、账号凭据、设备日志或其他敏感文件。
