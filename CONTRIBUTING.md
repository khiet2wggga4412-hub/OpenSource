# 贡献指南

感谢你参与 LiquidGlassClickGUI。

## 开发约定

- 使用 JDK 21 和项目自带的 Gradle Wrapper。
- Kotlin 代码遵循官方代码风格和现有包结构。
- 保持项目为纯 UI 工程，不加入游戏功能后端、内存操作或 native hook。
- 修改第三方衍生组件时，同步检查 `THIRD_PARTY_NOTICES.md`。

## 提交前检查

```powershell
.\gradlew.bat clean :app:assembleDebug
```

请勿提交 `local.properties`、IDE 缓存、Gradle 缓存、签名文件、APK 或 `build` 目录。
