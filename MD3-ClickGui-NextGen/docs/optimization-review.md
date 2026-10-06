# 优化清单（2026-09 审查，2026-10-06 更新）

审查范围：`app/src/main/java` 全部 64 个 Kotlin 文件（14,941 行）+ 构建配置。
方法：逐文件阅读、全仓 grep 交叉验证调用点、用现成 APK 与 release 构建量化体积、Gradle 9.7.1 + JDK21 + android-37 实测编译。
未做：真机/模拟器运行时 profiling（本机无可用模拟器），因此"每帧"类结论来自代码路径推导，已逐条标注证据位置。

> **2026-10-06 更新一：灵动岛（`com.mchack.island` 全部代码 + 宿主接线）已按需求整体移除。**
> `app/src/main/res/drawable-nodpi` 下 16 个演示 PNG 一并删除。
> 删除前的完整副本在 `E:\MD3-ClickGui-NextGen-archive\island-removed-20261006-114224`。
> 下文 A2/A3/A4/C2/C5 与"岛"相关的条目均**已随移除而消失**，保留在此仅作记录。
>
> **2026-10-06 更新二：music 的账号登录链路已整体移除**（第三方客户端过不了网易的风控校验，登录永远无法完成）。
> 删掉：`MusicLoginPanel`（扫码 / 手机号+密码 / 验证码三种方式）、`NeteaseSession` 的账号态（profile / cookie 持久化 / logout / csrf）、
> API 的登录端点（`qrKey` / `qrCreate` / `qrCheck` / `cellphoneLogin` / `sendCaptcha` / `cellphoneCaptchaLogin` / `userAccount` / `md5Password`）
> 与需登录端点（`recommendSongs` / `personalFm` / `likedSongs` / `likeSong` / `personalizedPlaylists`），
> 以及依赖会话的三个面板（Favorites / Recommend / Roam）、`LoginRequired` 占位与相应 20 条中文文案。
> 保留：音乐播放器、搜索、播放地址解析、精品歌单（Featured）、歌单详情、最近播放（Recent，本地存储）、匿名请求指纹（NMTID）。
> 备份在 `E:\MD3-ClickGui-NextGen-archive\music-login-removed-20261006-114952`。
>
> 当前规模：**31 个 Kotlin 文件 / 5,333 行**；APK debug **18.81 MB**、release **12.61 MB**。

## 结论速览

| # | 问题 | 证据 | 收益 | 代价 |
|---|---|---|---|---|
| A1 | `material-icons-extended` 拖入 10,655 个图标类 | debug APK `classes.dex` 42.8 MB；实测该类前缀出现 22,813 次；实际只用 30 个图标 | debug 包与构建时间大幅下降 | 低（换 30 个图标） |
| A2 | ~~岛上每秒强制整棵子树重组 + 第二个 60 Hz 帧回调~~ | 已随移除消失 | — | — |
| A3 | ~~岛上动画期每帧新建 6 个 `Path` + 1 个 `Paint`~~ | 已随移除消失 | — | — |
| A4 | ~~岛 47% 代码不可达~~ | 已整体移除 | 已完成 | — |
| B1 | `MusicPlayer.release()` 取消了唯一协程作用域 → 通知栏 Stop 后音乐永久失效 | `MusicPlayer.kt` | 已修 | 已完成 |
| B2 | 封面无缓存、全分辨率解码，重播重下重解 | `CoverArt.kt:17-35`、`MusicModulePanel.kt:136-148` | 每首歌省 1.6–4 MB 位图与一次网络往返 | 中 |
| B3 | 歌单用 `Column + verticalScroll` 全量组合（最多 10 万条） | `MusicFeaturePanels.kt`、`NeteaseMusicApi.kt:239-243` | 消除大歌单卡死/OOM | 低 |
| B4 | 解析 URL 失败时前台服务、wake lock、音频焦点全部不释放 | `MusicPlayer.kt` | 已修 | 已完成 |
| C1 | 定位路径全部失效，构建不可复现 | 原 `gradle-local.ps1` 三个路径均不存在 | 已修，见文末 | 已完成 |
| C2 | ~~模块图标不显示：`GlyphBitmaps.warm()` 无调用者~~ | 已随移除消失 | — | — |
| C3 | 中文模式下登录页/配置页仍是英文 | `ClickGuiWindow.kt`；键已存在于 `UiStrings.kt` | 已修 | 已完成 |
| C4 | `rememberSlidingSelection` 在组合期写状态 + 动画先 snap 回 0 | `SlidingSelection.kt` | 已修 | 已完成 |
| C5 | ~~`state`/`Panel` 双主题体系~~ | 已随移除消失 | — | — |
| C6 | `NexusTheme` 每次重组重算 34 色板（含 pow 亮度） | `Theme.kt:97-198` | 已修 | 已完成 |
| C7 | 快速拖动悬浮按钮时整个图层重组 + `BoxWithConstraints` 重新测量 | `QuickFloatingButton.kt:55,65-93` | 拖动更顺 | 中 |
| C8 | 构建并行被关闭；Gradle wrapper 无法下载；非 git 仓库 | `gradle.properties`、wrapper、无 `.git` | 已修（git 仍未纳入） | 已完成 |
| C9 | Kotlin 源告警若干（deprecated 图标、`quadraticBezierTo`） | 见"构建告警" | 图标类告警已清 | 已完成 |

## 构建告警（实测）

- `MusicFeaturePanels.kt:134,747`、`MusicModules.kt:19` 的废弃图标**已修**。
- ~~`island/cards/*` 的 `quadraticBezierTo`~~ 已随移除消失。
- Gradle 10 兼容：`Using a Project object as a dependency notation has been deprecated`——来自 AGP 9.2.1，非本项目脚本，仅供知悉。

## 已确认的良好实践

- 无 `GlobalScope`、无 `runBlocking`、无主线程网络调用；`!!` 仅 4 处且都有保护。
- 三个单例都只持有 `applicationContext`，无 Context 泄漏。
- 主题/语言/配置的快照与 `Saver` 序列化完整，`rememberSaveableStateHolder` 正确保住了窗口关闭重开时的导航状态。

## Android Studio 编译问题排查（2026-10-06）

现象：项目在 AS 里无法编译。已用命令行在同一台机器、同一套工具链上逐项排除：

| 检查项 | 结果 |
|---|---|
| `gradlew.bat :app:assembleDebug`（用 IDE 的 JBR 25） | 成功 |
| `clean` + `assembleDebug`（IDE JBR） | 成功，10 秒 |
| `--no-daemon --no-build-cache` 冷构建 | 成功，7 秒 |
| SDK 完整性（`D:\AndroidSDK`） | android-37.0 / build-tools 36.0.0 / platform-tools / licenses 齐全 |
| IDE 的 SDK 设置（`android.sdk.path.xml`） | `D:\AndroidSDK`，与 `local.properties` 一致 |
| Gradle 发行版 | 原先 `~/.gradle/wrapper/dists` 为空（wrapper 下载失败）；现已装好 9.7.1 |
| 守护进程日志 | 无任何失败记录，只有成功构建 |
| IDE `idea.log` | 只有网络类异常（`plugins.jetbrains.com` SSL 断开），与编译无关 |

### 找到并修掉的三处不一致

1. **Kotlin 字节码目标未固定**。`app/build.gradle.kts` 只声明了 `compileOptions = 17`，Kotlin 的 `jvmTarget` 则跟随运行 Gradle 的 JDK。本机 AS 自带 JBR 是 **25**，于是 Kotlin 目标随之为 25，而 Java 是 17，IDE 的 `.idea/compiler.xml` 又写着 21——三处不一致。已加 `kotlin { compilerOptions { jvmTarget = JVM_17 } }`；冷构建已验证 Kotlin 与 Java 产物都是 `major=61`（Java 17）。
2. **Gradle 发行版缺失**。wrapper 要从 `services.gradle.org` 下载，该域名在本机经常超时；失败后 `wrapper/dists` 一直是空的，AS 一打开就同步失败。现已下载并安装到 `~/.gradle/wrapper/dists/gradle-9.7.1-bin/…`（含 `.ok` 标记）。
3. **构建并行被关闭**（`workers.max=2`、`parallel=false`），已改为 4 / true，并启用 `org.gradle.caching` 与 `org.gradle.configuration-cache`。

另外清掉了 IDE 侧两个可能残留的编译缓存（不触碰项目文件）：
`%LOCALAPPDATA%\Google\AndroidStudio2026.2.1\compile-server` 与 `\compiler`。

### AS 侧操作步骤（按顺序）

1. 关闭 Android Studio（它正在运行时会重写自己的配置）。
2. 打开项目后执行 **File → Sync Project with Gradle Files**；日志里应出现 `Configuration cache entry stored`。
3. 若仍失败：**Build → Clean Project**，再 **Build → Rebuild Project**。
4. 若还失败：**File → Invalidate Caches… → Invalidate and Restart**；必要时删掉项目下的 `.gradle` 与 `build` 目录再同步。
5. 确认 **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** 选的是 `jbr-21`/`jbr`（即 `D:\AndroidStudio\jbr`），不要指向不存在的 `E:\DevEnv`。

### 需要实机确认
- `gradle-local.ps1` 里的三条路径（`E:\DevEnv\Gradle\.gradle`、`.trae-cn` 下的 JDK17、`E:\DevEnv\Android\Sdk`）在本机都不存在，该脚本不可用；请用 `gradle-here.ps1` 或直接 `gradlew.bat`。
- 项目未纳入 git，本次改动没有版本回退点。

## 灵动岛遮挡点击的修复（2026-10-06）

> 该 bug 所在的整块代码随后已被移除（见文首说明），本节保留作记录。

现象：开启灵动岛后，屏幕顶部正中有一片区域点不动。

原因：[IslandStage.kt](app/src/main/java/com/mchack/island/island/IslandStage.kt#L160-L168) 的命中层是一整块 **520dp × 242dp** 的舞台节点，`stageGestures` 的 `pointerInput` 挂在整块上。`pointerInput` 一旦建立协程就会消费该区域内**所有**指针事件，而岛体胶囊实际只有约 33dp 高，其余约 209dp 是空白。ClickGUI 面板占屏 93%×86% 且居中，顶部正中这块正好被压住——于是面板上部出现一片死区。

命中测试 `hitTest` 本身写对了（非命中返回 `HIT_NONE`），缺的只是**放行**：

- [Gestures.kt](app/src/main/java/com/mchack/island/island/Gestures.kt) 新增 `islandDown()` 闸门：`awaitEachGesture` 内先判断按压是否落在岛体/小球上，落在空白处直接返回、不消费事件，下层照常收到。
- 原先嵌在 `awaitEachGesture` 内部的 `awaitPointerEventScope { … }` 已移除（`awaitEachGesture` 本身就在该作用域内，嵌套会被 Kotlin 拒绝）；内部那处 `hitTest(down.position)` 也补上了 `/ density` 换算——几何量（`w0`/`h0`/`ballD0`）全是 dp，原来按物理像素比较是错的。
- 已确认这是岛内**唯一**的全尺寸事件消费者：`Panel.kt` 里的 `pointerInput` 都挂在具体的行/按钮上。

`clean` + `assembleDebug`/`assembleRelease` 通过，`islandDown` 已确认打包进 APK。仍需实机确认：岛体与小球上的点击/长按/拖拽手感不受影响。

## 本次已改动的文件

### 构建环境
- `gradle-here.ps1`（新增）：自动发现 JDK/SDK/Gradle 并同步 `local.properties`，供本机使用；`-Rerun` 强制重新编译。
- `local.properties`：`sdk.dir` 指向实际存在的 `D:\AndroidSDK`。
- `gradle-local.ps1`：保持原样未动（其硬编码路径在本机不存在）。

### 音频服务（两个 bug + 焦点状态机）
- `music/MusicPlayer.kt`：`release()` 不再取消单例作用域（原来会导致通知栏 Stop 之后永久无法播放）；改用 `playJob` 取消单次解析，并把 token 读取移到 `launch` 之后，修掉竞态。
- 同文件 `play()` 加 `try/finally`：URL 解析失败、`setDataSource` 抛错、新增的 20 秒准备超时都会回收前台服务 / wake lock / 音频焦点。
- 同文件焦点回调改为完整状态机：临时丢失记住待恢复、`AUDIOFOCUS_GAIN` 续播、永久丢失才放弃、ducking 交给系统；`AudioFocusRequest` 全程复用；`start()` 全部包 `runCatching`。
- `music/MusicPlaybackService.kt`：通知按 1 秒节流刷新（仅在状态变化时重发），暂停/播放图标与文案不再停留在启动那一刻；`onDestroy` 移除回调。

### 灵动岛接线
- `ui/island/HostIslandWiring.kt`（新增）：宿主侧持有 `Scheduler` / `Telemetry` / `IslandState` / `Showtime`，并提供 `refresh()`、`announce()`、`runSequence()`。
- `ui/island/HostIslandModule.kt`（新增）：把 `guiSections` 的 12 个真实模块映射成岛体 `Module`（名称、分类、按分类派生的强调色），并从 `ClickGuiState` / `MusicPlayer` 采集每行的真实状态（主题明暗、语言、悬浮按钮尺寸、音乐曲目、开关状态）。
- `ui/components/DynamicIslandHud.kt`：状态改为参数传入；400ms 心跳里调用 `wiring.refresh()`，只有模块状态**真的变化**才播报通知。
- `ui/screens/ClickGuiScreen.kt`：修掉"开关灵动岛卡片后必须关掉再重开窗口才生效"的作用域 bug（`isChecked` 原来读在已经算完的 `Box` 内容 lambda 里）；关闭时 `stop()` 清理待执行回调。
- `ui/components/ModuleCard.kt` / `ClickGuiWindow.kt`：Dynamic Island 模块详情页嵌入控制面板，外包 `MCHackTheme`，并传入真实模块列表。

### 去掉演示动画与演示数据（按"要真实模块提示"的要求）
- `island/model/Telemetry.kt`：新增宿主注入通道（`HostMetric` / `setHostMetric` / `hasHostMetric`）。`metric()` 与 `miniVal()` 优先读宿主注入值，原型的假数据表只作为无宿主数据时的兜底。
- `island/core/Showtime.kt`：删除 `lv.drift()`——原型每 390ms 对约 20 个字段做随机漫步，是"演示动画"的数据源头；`advance()` 现在只在存在宿主指标时执行。
- `island/core/IslandState.kt`：新增 `notifyHost(module, status, unit, fraction)`，先注入真实值再走通知。
- `island/panel/Panel.kt`：模块区改为渲染 `HostIslandModule`（新增 `HostModuleRow`：真实状态 + 分类 + 读数），标题换成 `MD3 ClickGUI`，副标题改为真实模块数与帧率；动作按钮换成 `Preview`（逐个播报真实模块）/ `Music` / `Clear`。原型仍有兜底：没有宿主模块时才显示那 15 个演示模块。
- 未改动但已不可达：`island/cards/*`、`Expanded.kt`、`Avatar.kt`、`motion/Anims.kt` 的卡片演出逻辑（`notifyHost` 默认 `expand = false`，只走紧凑态通知）。


### 低风险小修
- `ui/theme/Theme.kt`：`NexusTheme` 的 34 色板派生与动态取色方案改为 `remember` 缓存（原来每次重组重算，含 `pow` 亮度计算）。
- `ui/components/SlidingSelection.kt`：颜色写入移到 `SideEffect`；动画逻辑移入只以选中项为 key 的 `LaunchedEffect`（原来以 `(selectedId, measured)` 为 key，且 `measured` 依赖 `bounds`，选项一多就可能反复重启动画）；修正中断续播语义（原来 `snapTo(0f)` 会把起点变成"上一次的目标"，与注释声称的"从当前位置开始"相反）。
- `ui/components/ClickGuiWindow.kt`：登录页与配置页文案接入 `uiText`（中文模式下原来是英文）；`ModuleListPanel` 的 `derivedStateOf` 补上 `languageIndex` key（原来切换语言后搜索结果不会重算）。
- `ui/language/UiStrings.kt`：补 `Hide password` / `Show password` 两条中文。
- `ui/components/MusicFeaturePanels.kt`、`ui/modules/MusicModules.kt`：废弃的 `Icons.Default.Logout/ArrowBack/Login` 改为 `Icons.AutoMirrored.Filled.*`，这三条编译告警已消除。

### 验证
- `:app:assembleDebug` 与 `:app:assembleRelease`（含 R8）均成功，无 `e:` 错误；产物 19.48 MB / 12.99 MB。
- 新类已确认打包进 APK：`HostIslandWiring`、`IslandControlPanel`、`startPlayback`、`resumeOnFocusGain`、`MusicPlaybackService$refreshNotification`。
- **未做**：真机/模拟器实机验证（本机无 AVD）。控制面板在设置列中的高度与滚动、岛上通知的实际观感需要在设备上确认。

### 已知残留
- `Icons` 全量库未替换（按你的选择暂时保留）：debug 包与构建时间仍承担那 10,655 个图标类。
- 岛动画期的每帧分配（6 个 `Path`、1 个 `Paint`、渐变、`RenderEffect`）未处理。
- 音乐封面无缓存/全分辨率解码、歌单非懒加载未处理。

## 复现命令

```powershell
./gradle-here.ps1 -WhatIf                 # 打印解析到的工具路径
./gradle-here.ps1 :app:assembleDebug      # 已在 2m31s 内成功
E:\Dev\gradle-dists\gradle-9.7.1\bin\gradle.bat -p . :app:assembleDebug
```
