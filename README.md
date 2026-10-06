# OpenSource

Android ClickGUI / HUD 实验项目集合。三个相互独立的 Gradle 工程，各带 Wrapper，互不依赖。

| 项目 | 技术栈 |
| --- | --- |
| [`LiquidGlassClickGUI/`](LiquidGlassClickGUI/) | Kotlin、Jetpack Compose、液态玻璃 |
| [`LiquidPE/`](LiquidPE/) | Java、Android View、Xposed API 82 |
| [`MD3-ClickGui-NextGen/`](MD3-ClickGui-NextGen/) | Kotlin、Jetpack Compose、Material 3 |

## 构建

进入对应目录，使用各自的 Wrapper：

```bash
cd LiquidGlassClickGUI  && ./gradlew :app:assembleDebug
cd LiquidPE             && ./gradlew :app:assembleDebug
cd MD3-ClickGui-NextGen && ./gradlew :app:assembleDebug
```

需要 JDK 17+（LiquidGlassClickGUI 需 JDK 21）。SDK 路径由 Android Studio 写入 `local.properties`，不提交。

## 说明

第三方组件声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。CI 会为每个工程独立构建 debug APK。
