package dev.liquid.clickgui.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.shapes.RoundedRectangle
import dev.liquid.clickgui.R
import dev.liquid.clickgui.BuildConfig
import dev.liquid.clickgui.glass.LiquidModuleDialog
import dev.liquid.clickgui.glass.LiquidSideNavigation
import dev.liquid.clickgui.glass.SideNavigationItem
import dev.liquid.clickgui.model.ModuleCategory
import dev.liquid.clickgui.model.ModuleEffectState
import dev.liquid.clickgui.model.ModuleUiModel
import dev.liquid.clickgui.data.TargetAppContract
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidClickGuiScreen(
    controller: ClickGuiController,
    modifier: Modifier = Modifier,
    backgroundMode: ClickGuiBackgroundMode = ClickGuiBackgroundMode.SolidWhite,
    onCollapse: () -> Unit,
    onExit: () -> Unit,
    onResize: ((Offset) -> Unit)? = null,
) {
    val palette = GlassPalette()
    val hostContext = LocalContext.current
    val packResources = remember(hostContext) {
        runCatching {
            hostContext.createPackageContext(
                BuildConfig.APPLICATION_ID,
                Context.CONTEXT_IGNORE_SECURITY,
            ).resources
        }.getOrElse { hostContext.resources }
    }
    val backdrop = rememberLayerBackdrop()
    val pageLayer = rememberLayerBackdrop()
    val dialogBackdrop = rememberCombinedBackdrop(backdrop, pageLayer)
    val navigationItems = remember(controller.uiLanguage) {
        listOf(
            SideNavigationItem(ModuleCategory.Combat, Icons.Rounded.GpsFixed, categoryLabel(ModuleCategory.Combat, controller.uiLanguage)),
            SideNavigationItem(ModuleCategory.Movement, Icons.AutoMirrored.Rounded.DirectionsRun, categoryLabel(ModuleCategory.Movement, controller.uiLanguage)),
            SideNavigationItem(ModuleCategory.Player, Icons.Rounded.Person, categoryLabel(ModuleCategory.Player, controller.uiLanguage)),
            SideNavigationItem(ModuleCategory.Visual, Icons.Rounded.Visibility, categoryLabel(ModuleCategory.Visual, controller.uiLanguage)),
            SideNavigationItem(ModuleCategory.Misc, Icons.Rounded.Tune, categoryLabel(ModuleCategory.Misc, controller.uiLanguage)),
        )
    }
    val selectedIndex = navigationItems.indexOfFirst {
        it.category == controller.selectedCategory
    }.coerceAtLeast(0)
    val focusRequester = remember { FocusRequester() }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(controller.pendingShortcutModule) {
        if (controller.pendingShortcutModule != null) {
            focusRequester.requestFocus()
        }
    }

    CompositionLocalProvider(
        LocalGlassPalette provides palette,
        LocalPackResources provides packResources,
    ) {
        BoxWithConstraints(
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    event.type == KeyEventType.KeyDown &&
                        controller.handleShortcutKey(event.key.keyCode.toInt())
                }
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            val compact = maxWidth < 600.dp
            BackdropLayer(backdrop, backgroundMode)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(pageLayer)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(if (compact) 76.dp else 96.dp)
                        .fillMaxHeight(),
                ) {
                    LiquidSideNavigation(
                        items = navigationItems,
                        selectedIndex = selectedIndex,
                        onSelected = { controller.selectedCategory = navigationItems[it].category },
                        backdrop = backdrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    if (compact) {
                        Spacer(Modifier.height(8.dp))
                        LiquidButton(
                            onClick = { controller.showAboutDialog = true },
                            backdrop = backdrop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            surfaceColor = palette.glassSurface,
                            buttonHeight = 40.dp,
                            horizontalPadding = 8.dp,
                        ) {
                            GlassIcon(Icons.Rounded.Settings, "Bridge status", palette.secondaryInk)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    ClickGuiHeader(
                        controller = controller,
                        backdrop = backdrop,
                        onCollapse = onCollapse,
                        compact = compact,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        language = controller.uiLanguage,
                    )
                    Spacer(Modifier.height(10.dp))
                    AnimatedContent(
                        targetState = controller.selectedCategory,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(240)) { it / 16 })
                                .togetherWith(fadeOut(tween(130)) + slideOutVertically(tween(160)) { -it / 16 })
                        },
                        label = "categoryPage",
                        modifier = Modifier.weight(1f),
                    ) { category ->
                        ModuleGrid(
                            modules = controller.modulesFor(category).filter { module ->
                                searchQuery.isBlank() || listOf(
                                    module.name,
                                    moduleDisplayName(module, controller.uiLanguage),
                                    module.description,
                                    module.category.displayName,
                                ).any { it.contains(searchQuery.trim(), ignoreCase = true) }
                            },
                            controller = controller,
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            LiquidModuleDialog(
                visible = controller.selectedModule != null,
                module = controller.selectedModule,
                controller = controller,
                backdrop = dialogBackdrop,
                onDismiss = { controller.selectedModule = null },
                backgroundMode = backgroundMode,
            )

            ExitDialog(
                visible = controller.showAboutDialog,
                controller = controller,
                status = controller.bridgeStatus,
                floatingButtonConfig = controller.floatingButtonConfig,
                backdrop = dialogBackdrop,
                onDismiss = { controller.showAboutDialog = false },
                language = controller.uiLanguage,
                onLanguageChange = controller::updateUiLanguage,
                backgroundMode = backgroundMode,
            )

            if (onResize != null) {
                EdgeResizeHandles(onResize = onResize)
            }

        }
    }
}

private enum class ResizeCorner { TopLeft, TopRight, BottomLeft, BottomRight }

@Composable
private fun EdgeResizeHandles(onResize: (Offset) -> Unit) {
    var activeCorner by remember { mutableStateOf<ResizeCorner?>(null) }
    val palette = LocalGlassPalette.current
    val glowAlpha by animateFloatAsState(
        targetValue = if (activeCorner == null) 0f else 1f,
        animationSpec = tween(170),
        label = "resizeGlowAlpha",
    )
    val runningLight = rememberInfiniteTransition(label = "resizeRunningLight")
    val lightPhase by runningLight.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_550, easing = LinearEasing)),
        label = "resizeLightPhase",
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    if (glowAlpha <= 0f) return@drawWithContent
                    val angle = lightPhase * (Math.PI * 2.0)
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val reach = size.width.coerceAtLeast(size.height) * 0.72f
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1688FF),
                            Color(0xFF8ED4FF),
                            Color.White,
                            Color(0xFF60B7FF),
                            Color(0xFF1688FF),
                        ),
                        start = center - direction * reach,
                        end = center + direction * reach,
                    )
                    val inset = 3.dp.toPx()
                    val borderSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                    val radius = CornerRadius(26.dp.toPx(), 26.dp.toPx())
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(inset, inset),
                        size = borderSize,
                        cornerRadius = radius,
                        style = Stroke(9.dp.toPx()),
                        alpha = glowAlpha * 0.32f,
                    )
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(inset, inset),
                        size = borderSize,
                        cornerRadius = radius,
                        style = Stroke(2.4.dp.toPx()),
                        alpha = glowAlpha,
                    )
                },
        )
        ResizeCorner.entries.forEach { corner ->
            val alignment = when (corner) {
                ResizeCorner.TopLeft -> Alignment.TopStart
                ResizeCorner.TopRight -> Alignment.TopEnd
                ResizeCorner.BottomLeft -> Alignment.BottomStart
                ResizeCorner.BottomRight -> Alignment.BottomEnd
            }
            Box(
                modifier = Modifier
                    .align(alignment)
                    .size(38.dp)
                    .pointerInput(corner, onResize) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { activeCorner = corner },
                        onDragEnd = { activeCorner = null },
                        onDragCancel = { activeCorner = null },
                    ) { change, dragAmount ->
                        change.consume()
                        onResize(
                            when (corner) {
                                ResizeCorner.TopLeft -> Offset(-dragAmount.x, -dragAmount.y)
                                ResizeCorner.TopRight -> Offset(dragAmount.x, -dragAmount.y)
                                ResizeCorner.BottomLeft -> Offset(-dragAmount.x, dragAmount.y)
                                ResizeCorner.BottomRight -> Offset(dragAmount.x, dragAmount.y)
                            },
                        )
                    }
                },
            )
        }
    }
}

private fun categoryLabel(category: ModuleCategory, language: UiLanguage): String {
    if (language == UiLanguage.English) return category.displayName
    return when (category) {
        ModuleCategory.Combat -> "战斗"
        ModuleCategory.Movement -> "移动"
        ModuleCategory.Player -> "玩家"
        ModuleCategory.Visual -> "视觉"
        ModuleCategory.Misc -> "其他"
    }
}

private fun moduleDisplayName(
    module: ModuleUiModel,
    language: UiLanguage,
): String = if (language == UiLanguage.Chinese) module.name else module.englishName

@Composable
private fun BackdropLayer(
    backdrop: LayerBackdrop,
    backgroundMode: ClickGuiBackgroundMode,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when (backgroundMode) {
                        ClickGuiBackgroundMode.SolidWhite -> Color.White
                        ClickGuiBackgroundMode.GaussianBlur -> Color.White.copy(alpha = 0.58f)
                    },
                ),
        )
    }
}

@Composable
private fun ClickGuiHeader(
    controller: ClickGuiController,
    backdrop: Backdrop,
    onCollapse: () -> Unit,
    compact: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    language: UiLanguage,
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24.dp) },
                effects = {
                    vibrancy()
                    blur(9.dp.toPx())
                    lens(18.dp.toPx(), 22.dp.toPx())
                },
                highlight = { Highlight.Ambient },
                onDrawSurface = { drawRect(palette.glassSurface) },
            )
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIcon(
            icon = Icons.Rounded.Tune,
            contentDescription = "ClickGUI",
            color = palette.secondaryInk,
            modifier = Modifier.size(27.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                if (compact) "GLASS//" else "GLASS//CLIENT",
                style = if (compact) GlassTypography.Brand.copy(fontSize = 16.sp) else GlassTypography.Brand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                "${controller.modules.count { it.enabled }} " +
                    (if (language == UiLanguage.Chinese) "已启用" else "ACTIVE") +
                    "  •  ${TargetAppContract.PACKAGE_NAME}",
                style = GlassTypography.Label.copy(color = palette.secondaryInk),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!compact) {
            ModuleSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                language = language,
                backdrop = backdrop,
            )
            Spacer(Modifier.width(7.dp))
        }
        if (!compact) {
            LiquidButton(
                onClick = { controller.showAboutDialog = true },
                backdrop = backdrop,
                modifier = Modifier
                    .width(44.dp)
                    .height(40.dp),
                surfaceColor = palette.glassSurface,
                buttonHeight = 40.dp,
                horizontalPadding = 8.dp,
            ) {
                GlassIcon(Icons.Rounded.Settings, "Bridge status", palette.secondaryInk)
            }
            Spacer(Modifier.width(5.dp))
        }
        LiquidButton(
            onClick = onCollapse,
            backdrop = backdrop,
            modifier = Modifier
                .width(44.dp)
                .height(40.dp),
            tint = palette.accent,
            surfaceColor = palette.glassSurface,
            buttonHeight = 40.dp,
            horizontalPadding = 8.dp,
        ) {
            GlassIcon(Icons.Rounded.CloseFullscreen, "Collapse to floating button", palette.ink)
        }
    }
}

@Composable
private fun ModuleSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    language: UiLanguage,
    backdrop: Backdrop,
) {
    val palette = LocalGlassPalette.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val searchInteraction = remember { MutableInteractionSource() }
    val searchPressed by searchInteraction.collectIsPressedAsState()
    val searchScale by animateFloatAsState(
        targetValue = if (searchPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 430f),
        label = "searchPressScale",
    )
    val searchWidth by animateDpAsState(
        targetValue = if (expanded || query.isNotEmpty()) 184.dp else 40.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f),
        label = "searchWidth",
    )
    Row(
        modifier = Modifier
            .width(searchWidth)
            .height(40.dp)
            .graphicsLayer {
                scaleX = searchScale
                scaleY = searchScale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(7.dp.toPx())
                    lens(12.dp.toPx(), 16.dp.toPx())
                },
                highlight = { Highlight.Ambient },
                onDrawSurface = { drawRect(palette.glassSurface.copy(alpha = 0.62f)) },
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = searchInteraction,
                indication = null,
                onClick = { expanded = true },
            )
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIcon(
            icon = Icons.Rounded.Search,
            contentDescription = if (language == UiLanguage.Chinese) "搜索功能" else "Search modules",
            color = palette.secondaryInk,
            modifier = Modifier.size(17.dp),
        )
        if (expanded || query.isNotEmpty()) {
            Spacer(Modifier.width(7.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = GlassTypography.Body.copy(fontSize = 12.sp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            BasicText(
                                if (language == UiLanguage.Chinese) "搜索功能" else "Search modules",
                                style = GlassTypography.Body.copy(
                                    color = palette.secondaryInk.copy(alpha = 0.62f),
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                        inner()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            onQueryChange("")
                            expanded = false
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                GlassIcon(
                    icon = Icons.Rounded.Close,
                    contentDescription = if (language == UiLanguage.Chinese) "清空搜索" else "Clear search",
                    color = palette.secondaryInk,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ModuleGrid(
    modules: List<ModuleUiModel>,
    controller: ClickGuiController,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(modules, key = { it.id }) { module ->
            LiquidModuleCard(module, controller, backdrop)
        }
    }
}

@Composable
private fun LiquidModuleCard(
    module: ModuleUiModel,
    controller: ClickGuiController,
    backdrop: Backdrop,
) {
    val palette = LocalGlassPalette.current
    val currentModule by rememberUpdatedState(module)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var openingDetail by remember { mutableStateOf(false) }
    var longPressSequence by remember { mutableStateOf(0) }

    LaunchedEffect(longPressSequence) {
        if (longPressSequence == 0) return@LaunchedEffect
        openingDetail = true
        // 先让卡片完成一次明显下沉，再弹出详情页，避免长按动作被弹窗截断。
        delay(210)
        controller.selectedModule = currentModule
        delay(180)
        openingDetail = false
    }

    val pressDepth by animateFloatAsState(
        targetValue = if (pressed || openingDetail) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "modulePressDepth",
    )
    val accent by animateColorAsState(
        if (module.enabled) moduleCategoryAccent(module.category) else palette.secondaryInk,
        tween(200),
        label = "moduleAccent",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (module.enabled) 1.08f else 0.94f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "moduleCloseScale",
    )
    val surfaceColor by animateColorAsState(
        targetValue = if (module.enabled) {
            Color(0xFFEAF4FF).copy(alpha = 0.92f)
        } else {
            palette.glassSurfaceStrong.copy(alpha = 0.90f)
        },
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "moduleSurfaceTransition",
    )

    val cardLayer = rememberLayerBackdrop()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                val pressedScale = 1f - pressDepth * 0.12f
                scaleX = pressedScale
                scaleY = pressedScale
                translationY = pressDepth * 5.dp.toPx()
            },
    ) {
        val textVisibilityTarget = if (maxWidth >= 172.dp) 1f else 0f
        val cardTextAlpha by animateFloatAsState(
            targetValue = textVisibilityTarget,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "moduleCardTextAlpha",
        )
        val iconCenterOffsetX = ((maxWidth - 100.dp) / 2f).coerceAtLeast(0.dp)
        val iconCenterOffsetY = ((maxHeight - 100.dp) / 2f).coerceAtLeast(0.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(cardLayer)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(28.dp) },
                    effects = {
                        vibrancy()
                        blur(7.dp.toPx())
                        lens(14.dp.toPx(), 18.dp.toPx())
                    },
                    highlight = { Highlight.Ambient },
                    innerShadow = {
                        val restingAlpha = if (module.enabled) 0.70f else 0.35f
                        InnerShadow(
                            radius = (5f + pressDepth * 8f).dp,
                            alpha = restingAlpha + (0.90f - restingAlpha) * pressDepth,
                        )
                    },
                    onDrawSurface = {
                        drawRect(surfaceColor)
                    },
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onLongClick = {
                        if (!openingDetail) longPressSequence++
                    },
                    onClick = {
                        controller.setEnabled(currentModule, !currentModule.enabled)
                    },
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        val centerProgress = 1f - cardTextAlpha
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = if (module.enabled) 1f else 0.72f
                        translationX = iconCenterOffsetX.toPx() * centerProgress
                        translationY = iconCenterOffsetY.toPx() * centerProgress
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (module.iconRes != 0) {
                    PackResourceIcon(
                        resourceId = module.iconRes,
                        contentDescription = moduleDisplayName(module, controller.uiLanguage),
                        color = accent,
                        modifier = Modifier.size(52.dp),
                    )
                } else {
                    GlassIcon(
                        icon = moduleIcon(module),
                        contentDescription = null,
                        color = accent,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            BasicText(
                moduleDisplayName(module, controller.uiLanguage),
                style = GlassTypography.Title.copy(fontSize = 17.sp),
                modifier = Modifier.graphicsLayer { alpha = cardTextAlpha },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            AnimatedContent(
                targetState = module.enabled,
                transitionSpec = {
                    (fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 2 })
                        .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(210)) { it / 2 })
                },
                label = "moduleCloseState",
                modifier = Modifier.graphicsLayer { alpha = cardTextAlpha },
            ) { enabled ->
                BasicText(
                    if (enabled) {
                        if (controller.uiLanguage == UiLanguage.Chinese) "开启" else "On"
                    } else {
                        if (controller.uiLanguage == UiLanguage.Chinese) "关闭" else "Off"
                    },
                    style = GlassTypography.Body.copy(
                        color = if (enabled) accent else palette.secondaryInk.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun moduleIcon(module: ModuleUiModel): ImageVector = when (module.id) {
    "kill_aura", "aimbot" -> Icons.Rounded.GpsFixed
    "velocity", "teams" -> Icons.Rounded.Shield
    "auto_click" -> Icons.Rounded.AdsClick
    "fly", "free_cam" -> Icons.Rounded.Flight
    "speed" -> Icons.Rounded.Speed
    "inventory_manager" -> Icons.Rounded.Inventory2
    "auto_armor" -> Icons.Rounded.Shield
    "auto_tool" -> Icons.Rounded.Build
    "auto_eat" -> Icons.Rounded.Favorite
    "esp", "tracer" -> Icons.Rounded.Visibility
    "xray" -> Icons.Rounded.GridView
    "full_bright" -> Icons.Rounded.LightMode
    "spammer" -> Icons.Rounded.Chat
    "array_list" -> Icons.Rounded.Tune
    else -> when (module.category) {
        ModuleCategory.Combat -> Icons.Rounded.Bolt
        ModuleCategory.Movement -> Icons.AutoMirrored.Rounded.DirectionsRun
        ModuleCategory.Player -> Icons.Rounded.Person
        ModuleCategory.Visual -> Icons.Rounded.CameraAlt
        ModuleCategory.Misc -> Icons.Rounded.Groups
    }
}

private fun moduleCategoryAccent(category: ModuleCategory): Color = when (category) {
    ModuleCategory.Combat -> Color(0xFF2D91FF)
    ModuleCategory.Movement -> Color(0xFF17A7FF)
    ModuleCategory.Player -> Color(0xFF4E86FF)
    ModuleCategory.Visual -> Color(0xFF00A9D8)
    ModuleCategory.Misc -> Color(0xFF637DFF)
}

private fun moduleCardState(module: ModuleUiModel, language: UiLanguage): String = when (module.effectState) {
    ModuleEffectState.CatalogOnly -> if (language == UiLanguage.Chinese) "目录" else "CATALOG"
    ModuleEffectState.Ready -> if (language == UiLanguage.Chinese) "可用" else "READY"
    ModuleEffectState.Active -> if (language == UiLanguage.Chinese) "已启用" else "ACTIVE"
    ModuleEffectState.Unsupported -> if (language == UiLanguage.Chinese) "不支持" else "UNSUPPORTED"
    ModuleEffectState.Error -> if (language == UiLanguage.Chinese) "错误" else "ERROR"
}

private fun moduleCardStatus(module: ModuleUiModel): String {
    if (!module.effectAvailable && module.effectState == ModuleEffectState.CatalogOnly) {
        return "UNAVAILABLE · ${module.effectBackend.uppercase()}"
    }
    val state = when (module.effectState) {
        ModuleEffectState.CatalogOnly -> "CATALOG"
        ModuleEffectState.Ready -> "READY"
        ModuleEffectState.Active -> "ACTIVE"
        ModuleEffectState.Unsupported -> "UNSUPPORTED"
        ModuleEffectState.Error -> "ERROR"
    }
    return "$state · ${module.effectBackend.uppercase()}"
}

@Composable
private fun QuickOverlayPinButton(
    module: ModuleUiModel,
    controller: ClickGuiController,
    backdrop: Backdrop,
) {
    val palette = LocalGlassPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val iconColor by animateColorAsState(
        targetValue = if (module.quickOverlayVisible) Color.White else palette.secondaryInk,
        animationSpec = tween(180),
        label = "quickOverlayPinColor",
    )
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.88f
            module.quickOverlayVisible -> 1.06f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 430f),
        label = "quickOverlayPinScale",
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(15.dp) },
                effects = {
                    blur(3.dp.toPx())
                    lens(8.dp.toPx(), 12.dp.toPx())
                },
                highlight = { Highlight.Ambient },
                onDrawSurface = {
                    drawRect(
                        if (module.quickOverlayVisible) palette.accent.copy(alpha = 0.88f)
                        else palette.glassSurfaceStrong.copy(alpha = 0.72f),
                    )
                },
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { controller.toggleQuickOverlay(module) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlassIcon(
            icon = Icons.Rounded.PushPin,
            contentDescription = if (module.quickOverlayVisible) {
                "Remove floating shortcut"
            } else {
                "Create floating shortcut"
            },
            color = iconColor,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun ExitDialog(
    visible: Boolean,
    controller: ClickGuiController,
    status: String,
    floatingButtonConfig: FloatingButtonConfig,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    language: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit,
    backgroundMode: ClickGuiBackgroundMode,
) {
    val palette = LocalGlassPalette.current
    val exitLayer = rememberLayerBackdrop()
    val exitBackdrop = rememberCombinedBackdrop(backdrop, exitLayer)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
            scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
            ),
        exit = fadeOut(tween(150)) +
            scaleOut(targetScale = 0.95f, animationSpec = tween(170)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim)
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.72f)
                    .clickable(interactionSource = null, indication = null, onClick = {}),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(exitLayer)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(28.dp) },
                            effects = {
                                blur(12.dp.toPx())
                                lens(22.dp.toPx(), 38.dp.toPx(), depthEffect = true)
                            },
                            highlight = { Highlight.Plain },
                            onDrawSurface = {
                                drawRect(
                                    palette.glassSurfaceStrong.copy(
                                        alpha = if (backgroundMode == ClickGuiBackgroundMode.GaussianBlur) {
                                            0.70f
                                        } else 0.94f,
                                    ),
                                )
                            },
                        ),
                )
                Column(modifier = Modifier.padding(20.dp)) {
                BasicText(
                    if (language == UiLanguage.Chinese) "界面设置" else "Interface settings",
                    style = GlassTypography.Title,
                )
                Spacer(Modifier.height(8.dp))
                BasicText(status, style = GlassTypography.Body.copy(color = palette.secondaryInk))
                Spacer(Modifier.height(4.dp))
                BasicText(
                    TargetAppContract.PACKAGE_NAME,
                    style = GlassTypography.Label.copy(color = palette.accent),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        if (language == UiLanguage.Chinese) "界面语言" else "Language",
                        style = GlassTypography.Body,
                        modifier = Modifier.weight(1f),
                    )
                    LiquidButton(
                        onClick = { onLanguageChange(UiLanguage.Chinese) },
                        backdrop = exitBackdrop,
                        tint = if (language == UiLanguage.Chinese) palette.accent else palette.secondaryInk,
                        surfaceColor = if (language == UiLanguage.Chinese) {
                            palette.accent.copy(alpha = 0.16f)
                        } else palette.glassSurface,
                        buttonHeight = 34.dp,
                    ) {
                        BasicText("中文", style = GlassTypography.Label)
                    }
                    Spacer(Modifier.width(6.dp))
                    LiquidButton(
                        onClick = { onLanguageChange(UiLanguage.English) },
                        backdrop = exitBackdrop,
                        tint = if (language == UiLanguage.English) palette.accent else palette.secondaryInk,
                        surfaceColor = if (language == UiLanguage.English) {
                            palette.accent.copy(alpha = 0.16f)
                        } else palette.glassSurface,
                        buttonHeight = 34.dp,
                    ) {
                        BasicText("English", style = GlassTypography.Label)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            if (language == UiLanguage.Chinese) "自动保存配置" else "Auto-save configuration",
                            style = GlassTypography.Body,
                        )
                        BasicText(
                            if (language == UiLanguage.Chinese) {
                                "修改后立即保存"
                            } else {
                                "Save changes immediately"
                            },
                            style = GlassTypography.Label.copy(color = palette.secondaryInk),
                        )
                    }
                    LiquidToggle(
                        selected = { controller.autoSaveEnabled },
                        onSelect = controller::updateAutoSaveEnabled,
                        backdrop = exitBackdrop,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            if (language == UiLanguage.Chinese) "高斯模糊背景" else "Gaussian background",
                            style = GlassTypography.Body,
                        )
                        BasicText(
                            if (ClickGuiRenderer.backgroundMode == ClickGuiBackgroundMode.GaussianBlur) {
                                "24dp host blur"
                            } else {
                                "Solid white"
                            },
                            style = GlassTypography.Label.copy(color = palette.secondaryInk),
                        )
                    }
                    LiquidToggle(
                        selected = {
                            ClickGuiRenderer.backgroundMode == ClickGuiBackgroundMode.GaussianBlur
                        },
                        onSelect = { enabled ->
                            controller.setBackgroundMode(
                                if (enabled) {
                                    ClickGuiBackgroundMode.GaussianBlur
                                } else {
                                    ClickGuiBackgroundMode.SolidWhite
                                },
                            )
                        },
                        backdrop = exitBackdrop,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    BasicText(
                        if (language == UiLanguage.Chinese) "悬浮按钮大小" else "Floating button size",
                        style = GlassTypography.Body,
                        modifier = Modifier.weight(1f),
                    )
                    BasicText(
                        "${floatingButtonConfig.buttonSizeDp}dp",
                        style = GlassTypography.Label.copy(color = palette.accent),
                    )
                }
                LiquidSlider(
                    value = { floatingButtonConfig.buttonSizeDp.toFloat() },
                    onValueChange = { controller.setFloatingButtonSize(it.roundToInt()) },
                    valueRange = FloatingButtonConfig.MIN_SIZE_DP.toFloat()..
                        FloatingButtonConfig.MAX_SIZE_DP.toFloat(),
                    visibilityThreshold = 0.5f,
                    backdrop = exitBackdrop,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    LiquidButton(
                        onClick = onDismiss,
                        backdrop = exitBackdrop,
                        surfaceColor = palette.glassSurface,
                    ) {
                        BasicText(
                            if (language == UiLanguage.Chinese) "返回" else "Back",
                            style = GlassTypography.Body,
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun GlassIcon(
    icon: ImageVector,
    contentDescription: String?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.size(21.dp),
    )
}
