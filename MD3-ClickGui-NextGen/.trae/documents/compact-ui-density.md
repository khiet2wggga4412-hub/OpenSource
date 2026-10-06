# 紧凑化 UI 改造方案

## Context

当前 ClickGui 三栏布局(左导航 / 中模块列表 / 右设置面板)结构与 Athena 截图一致,但整体密度偏松:workspace 外边距 24dp、栏间距 16dp、列表项 padding 16dp、列表行间距 12dp、圆角 20dp,视觉上比 Athena 截图"散"。用户希望向 Athena 的"紧凑而不凌乱"看齐,同时保留小图标、加分类模块数徽章、圆角一并收紧。

本次改造只调密度与一个新元素(数量徽章),不动数据模型、不动交互逻辑、不动 NexusSpacing 节奏本身,只在组件层选择更小的档位 + 收紧两个圆角变量。

## 改动清单

### 1. 圆角变量收紧 — `app/src/main/java/com/example/md3clickgui/ui/theme/UiTokens.kt`
- `NexusCornerRadius = 20.dp` → `14.dp`
- `NexusIconCornerRadius = 12.dp` → `10.dp`

这两处是全局唯一圆角源,改完所有 `NexusCornerShape` / `NexusIconShape` 引用自动跟随(卡片、面板、CategoryRail、ClickGuiScreen centerMask、TopBar 头像 tile 等)。

### 2. 模块卡片密度收紧 — `app/src/main/java/com/example/md3clickgui/ui/components/ModuleCard.kt`
`ModuleCard` 函数内:
- 卡片内 Row padding:`NexusSpacing.large`(16dp)→ `horizontal = NexusSpacing.medium(12dp), vertical = NexusSpacing.small(8dp)`
- 图标 tile:`Modifier.size(NexusDimensions.iconTile)`(40dp)→ 新增 `NexusDimensions.moduleListIconTile = 28.dp` 并引用(避免影响 TopBar 40dp 头像)
- 图标本身:`Modifier.size(21.dp)` → `Modifier.size(16.dp)`
- 图标与文字间距:`Spacer(Modifier.size(NexusSpacing.medium))`(12dp)→ `NexusSpacing.small`(8dp)

`ModuleSettingsPanel` 函数内:
- 外边距:`.padding(NexusSpacing.large)`(16dp)→ `.padding(NexusSpacing.medium)`(12dp)
- 设置项间距 `Arrangement.spacedBy(NexusSpacing.small)`(8dp)保留(设置项需可区分,不再压)

### 3. workspace 与栏间距收紧 — `app/src/main/java/com/example/md3clickgui/ui/components/ClickGuiWindow.kt`
- workspace 外边距(第 152 行):`.padding(NexusSpacing.extraLarge)`(24dp)→ `.padding(NexusSpacing.large)`(16dp)
- 顶层 Row 栏间距(第 156 行):`spacedBy(NexusSpacing.large)`(16dp)→ `spacedBy(NexusSpacing.small)`(8dp)
- 内层 Row 栏间距(第 189 行):同上 → `spacedBy(NexusSpacing.small)`(8dp)
- 模块列表 LazyColumn 间距(第 220 行):`spacedBy(NexusSpacing.medium)`(12dp)→ `spacedBy(NexusSpacing.small)`(8dp)
- 列表底部 padding(第 219 行):`PaddingValues(bottom = NexusSpacing.extraLarge)`(24dp)→ `PaddingValues(bottom = NexusSpacing.large)`(16dp)

### 4. 新增模块图标 tile 尺寸 — `app/src/main/java/com/example/md3clickgui/ui/theme/UiTokens.kt`
在 `NexusDimensions` 内新增:
```kotlin
val moduleListIconTile = 28.dp
```
专供模块列表卡片使用,不污染 `iconTile`(TopBar 头像仍用 40dp)。

### 5. 左侧导航加模块数量徽章 — `app/src/main/java/com/example/md3clickgui/ui/components/CategoryRail.kt`
在展开态 label Box 内,把当前 `Text` 包成 `Row { Text(weight 1f, maxLines=1) + 徽章 }`:
- 徽章数据:`item.modules.size`(GuiSection 已有 modules 字段)
- 徽章样式:`Surface(shape = NexusIconShape, color = colors.surfaceContainerHighest)` 内放 `Text("$count", style = labelMedium, color = onSurfaceVariant)`
- 徽章 alpha/translation 跟随现有 `labelAlpha` / `labelOffset` 动画(放在同一个 graphicsLayer Box 内,自然继承)
- 折叠态自动隐藏(因 labelAlpha=0)

## 不改的部分
- `NexusSpacing` 数值本身(4/8/12/16/24 节奏不变,只在组件层选更小档位)
- 模块模型 `GuiModule` / `GuiSection`(无新数据字段)
- 交互逻辑、动画时序、状态持有
- Athena 截图里的"绑定键 NONE"和"行内分类标签"——当前项目无按键绑定系统,中间列表已知分类,这两项是冗余信息,不加

## 验证

```
.\gradle-local.ps1 :app:assembleDebug --offline --no-daemon
```

构建成功后安装 APK,目测:
1. 中间模块列表行明显变矮,图标变小但仍可识别,行间距收紧
2. 左右栏间距与外边距收紧,整体面板更"满"
3. 圆角变小(14dp),视觉更利落
4. 左侧导航展开时分类名后出现数字徽章,折叠时隐藏
5. 设置面板 padding 收紧,设置项间距仍可区分
6. TopBar 头像 tile 不受影响(仍 40dp)
