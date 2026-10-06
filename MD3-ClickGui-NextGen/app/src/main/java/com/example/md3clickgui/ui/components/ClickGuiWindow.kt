package com.example.md3clickgui.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.slideInHorizontally
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.key
import com.example.md3clickgui.ui.model.GuiModule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import com.example.md3clickgui.ui.model.GuiSection
import com.example.md3clickgui.ui.modules.guiSections
import com.example.md3clickgui.ui.modules.music.MusicModule
import com.example.md3clickgui.ui.modules.SectionIds
import com.example.md3clickgui.ui.modules.music.FeaturedModule
import com.example.md3clickgui.ui.modules.music.RecentModule
import com.example.md3clickgui.ui.state.ClickGuiState
import com.example.md3clickgui.ui.state.GuiConfig
import com.example.md3clickgui.ui.theme.NexusDimensions
import com.example.md3clickgui.ui.theme.NexusCornerShape
import com.example.md3clickgui.ui.theme.NexusIconShape
import com.example.md3clickgui.ui.theme.NexusSpacing
import com.example.md3clickgui.ui.theme.NexusMotion
import com.example.md3clickgui.ui.language.uiText
import androidx.compose.ui.unit.IntOffset

private val NexusInputBorderThickness = 2.dp

internal const val ClosePanelLabel = "Close Material panel"

@Composable
fun ClickGuiWindow(
    state: ClickGuiState,
    sections: List<GuiSection> = guiSections
) {
    if (sections.isEmpty()) return
    var sectionIndex by rememberSaveable { mutableIntStateOf(sections.indexOfFirst { it.modules.isNotEmpty() }.coerceAtLeast(0)) }
    var categoriesExpanded by rememberSaveable { mutableStateOf(true) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val safeSectionIndex = sectionIndex.coerceIn(0, sections.lastIndex)
    val section = sections[safeSectionIndex]
    val isConfigSection = section.id == SectionIds.CONFIG
    val modulesById = remember(sections) { sections.flatMap { it.modules }.associateBy { it.id } }

    val categoryNames = remember(sections) {
        sections.flatMap { category -> category.modules.map { it.id to category.name } }.toMap()
    }
    val allModules = remember(sections) { sections.flatMap { it.modules } }
    LaunchedEffect(section.id, state.isAuthenticated, state.isLoginLoading) {
        if (state.canInteractWithModules() && !isConfigSection && section.modules.none { it.id == state.selectedModuleId }) {
            section.modules.firstOrNull()?.let { state.selectModule(it.id) } ?: state.closeDetailsPanel()
        }
    }
    LaunchedEffect(state.isLoginLoading) {
        if (state.isLoginLoading) {
            delay(1_500)
            state.finishLoginLoading()
        }
    }
    val panelTarget = when {
        !state.isAuthenticated -> LoginPanelKey
        state.isLoginLoading -> WelcomeLoadingPanelKey
        state.isDetailsPanelOpen -> state.selectedModuleId
        else -> null
    }
    val colors = MaterialTheme.colorScheme
    val contentSlidePx = with(LocalDensity.current) { 8.dp.roundToPx() }
    BoxWithConstraints(Modifier.fillMaxSize().padding(NexusDimensions.workspacePadding)) {
        val showCategoryLabels = categoriesExpanded && maxWidth >= 720.dp
        val railWidth by animateDpAsState(
            if (showCategoryLabels) NexusDimensions.categoryRailExpanded else NexusDimensions.categoryRailCollapsed,
            animationSpec = NexusMotion.enterSpec<Dp>(), label = "categoryWidth"
        )
        val remainingWidth = (maxWidth - railWidth - NexusDimensions.workspaceGap * 2).coerceAtLeast(0.dp)
        val stableListWidth = ((maxWidth - NexusDimensions.categoryRailExpanded - NexusDimensions.workspaceGap * 2) * 0.36f).coerceIn(176.dp, 280.dp)
        val moduleListWidth = minOf(stableListWidth, remainingWidth * 0.45f)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(NexusDimensions.workspaceGap)) {
            CategoryRail(
                modifier = Modifier.width(railWidth), expanded = showCategoryLabels, selectedIndex = safeSectionIndex,
                onSelect = { index ->
                    sectionIndex = index
                    searchQuery = ""
                    if (state.canInteractWithModules() && sections[index].id != SectionIds.CONFIG) {
                        sections[index].modules.firstOrNull()?.let { state.selectModule(it.id) } ?: state.closeDetailsPanel()
                    }
                },
                onToggleExpanded = { categoriesExpanded = !categoriesExpanded },
                sections = sections, languageIndex = state.languageIndex,
                accountName = state.accountName, accountExpiryText = state.accountExpiryText
            )

            val enterFadeSpec = NexusMotion.enterSpec<Float>()
            val enterExitFadeSpec = NexusMotion.exitSpec<Float>()
            val enterSlideSpec = NexusMotion.enterSpec<IntOffset>()
            AnimatedContent(
                targetState = isConfigSection,
                transitionSpec = {
                    val direction = if (targetState) 1 else -1
                    (fadeIn(enterFadeSpec) +
                        slideInHorizontally(enterSlideSpec) { direction * contentSlidePx }) togetherWith
                        fadeOut(enterExitFadeSpec)
                },
                modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds(), label = "workspaceTransition"
            ) { configVisible ->
                if (configVisible) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            CompactSearchField(searchQuery, { searchQuery = it },
                                uiText(state.languageIndex, "Search configurations"), Modifier.weight(1f))
                            IconButton(onClick = state::closeWindow) {
                                Icon(Icons.Default.Close, contentDescription = uiText(state.languageIndex, ClosePanelLabel))
                            }
                        }
                        Spacer(Modifier.height(NexusSpacing.extraSmall))
                        ConfigPanel(state, searchQuery, state.languageIndex, Modifier.weight(1f))
                    }
                } else {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(NexusDimensions.workspaceGap)) {
                        AnimatedContent(
                            targetState = safeSectionIndex,
                            transitionSpec = {
                                fadeIn(enterFadeSpec) togetherWith fadeOut(enterExitFadeSpec)
                            },
                            modifier = Modifier.width(moduleListWidth).fillMaxHeight().clipToBounds(),
                            label = "categoryPanelTransition"
                        ) { index ->
                            ModuleListPanel(sections[index], allModules, categoryNames, state, searchQuery, { searchQuery = it })
                        }
                        Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                            AnimatedContent(
                                targetState = panelTarget,
                                transitionSpec = {
                                    (fadeIn(enterFadeSpec) +
                                        slideInHorizontally(enterSlideSpec) { contentSlidePx }) togetherWith
                                        fadeOut(enterExitFadeSpec)
                                },
                                modifier = Modifier.fillMaxSize(), label = "detailsPanelTransition"
                            ) { targetId ->
                                val module = modulesById[targetId]
                                when {
                                    targetId == LoginPanelKey -> LoginPanel(state::submitCredentials, state.languageIndex, Modifier.fillMaxSize())
                                    targetId == WelcomeLoadingPanelKey -> WelcomeNexusPanel(Modifier.fillMaxSize())
                                    module?.id == MusicModule.model.id -> MusicModulePanel(
                                        modifier = Modifier.fillMaxSize(),
                                        languageIndex = state.languageIndex,
                                        onClose = state::closeWindow
                                    )
                                    module?.id == FeaturedModule.model.id -> FeaturedPanel(
                                        languageIndex = state.languageIndex,
                                        onClose = state::closeWindow,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    module?.id == RecentModule.model.id -> RecentPanel(
                                        languageIndex = state.languageIndex,
                                        onClose = state::closeWindow,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    module != null -> ModuleSettingsPanel(
                                        module = module, state = state, modifier = Modifier.fillMaxSize(), languageIndex = state.languageIndex,
                                        categoryName = categoryNames[module.id].orEmpty(), onClose = state::closeWindow
                                    )
                                    else -> Surface(Modifier.fillMaxSize(), shape = NexusCornerShape, color = colors.surfaceContainerLow) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(uiText(state.languageIndex, "Select a module"), color = colors.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                            if (panelTarget == LoginPanelKey || panelTarget == WelcomeLoadingPanelKey || panelTarget == null) {
                                IconButton(onClick = state::closeWindow, modifier = Modifier.align(Alignment.TopEnd)) {
                                    Icon(Icons.Default.Close, contentDescription = uiText(state.languageIndex, ClosePanelLabel))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleListPanel(
    section: GuiSection, allModules: List<GuiModule>, categoryNames: Map<String, String>, state: ClickGuiState,
    searchQuery: String, onSearchQueryChange: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val filteredModules by remember(section, allModules, searchQuery, state.languageIndex) {
        derivedStateOf {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                section.modules
            } else {
                allModules.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        uiText(state.languageIndex, it.name).contains(query, ignoreCase = true)
                }
            }
        }
    }
    val selection = rememberSlidingSelection(state.selectedModuleId?.takeIf { state.isDetailsPanelOpen && filteredModules.any { module -> module.id == it } },
        colors.primaryContainer, colors.surfaceContainer)

    val groupedModules = if (searchQuery.isBlank()) {
        listOf(section.name to filteredModules)
    } else {
        filteredModules.groupBy { categoryNames[it.id].orEmpty() }.toList()
    }
    Surface(Modifier.fillMaxSize(), shape = NexusCornerShape, color = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxSize().padding(NexusSpacing.extraSmall)) {
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = NexusSpacing.extraSmall),
                horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(0.46f)) {
                    Text(uiText(state.languageIndex, if (searchQuery.isBlank()) section.name else "Search results"),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${uiText(state.languageIndex, "Modules")} · ${filteredModules.size}",
                        style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CompactSearchField(searchQuery, onSearchQueryChange, uiText(state.languageIndex, "Search"),
                    Modifier.weight(0.54f), fieldHeight = NexusDimensions.controlHeight)
            }
            Spacer(Modifier.height(NexusDimensions.rowGap))

            Box(Modifier.weight(1f).clipToBounds().verticalScroll(rememberScrollState())) {
                Column(with(selection) { Modifier.fillMaxWidth().selectionContainer() }, verticalArrangement = Arrangement.spacedBy(NexusDimensions.rowGap)) {
                    if (filteredModules.isEmpty()) {
                        Text(uiText(state.languageIndex, if (searchQuery.isBlank()) "No modules yet" else "No matching modules"),
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(NexusSpacing.medium))
                    }
                    groupedModules.forEach { (groupName, modules) ->
                        if (searchQuery.isNotBlank() && groupName.isNotEmpty()) {
                            key("header-$groupName") {
                                Text(
                                    text = "${uiText(state.languageIndex, groupName)} · ${modules.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().padding(
                                        start = NexusSpacing.small,
                                        top = NexusSpacing.small,
                                        bottom = NexusSpacing.extraSmall
                                    )
                                )
                            }
                        }
                        modules.forEach { module ->
                            key(module.id) { ModuleCard(module, state, state.languageIndex, categoryNames[module.id].orEmpty(), selection) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSearchField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    fieldHeight: Dp = 44.dp) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.height(fieldHeight), shape = NexusIconShape, color = colors.surfaceContainerHigh) {
        Row(Modifier.fillMaxSize().padding(horizontal = NexusSpacing.small), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant)
            Spacer(Modifier.width(NexusSpacing.small))
            BasicTextField(
                value = value, onValueChange = onValueChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.weight(1f).semantics { contentDescription = label },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        inner()
                    }
                }
            )
        }
    }
}

private const val LoginPanelKey = "__login_panel__"
private const val WelcomeLoadingPanelKey = "__welcome_loading_panel__"

@Composable
private fun ConfigPanel(
    state: ClickGuiState,
    searchQuery: String,
    languageIndex: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var configName by rememberSaveable { mutableStateOf("") }
    val filteredConfigs = state.configurations.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery.trim(), ignoreCase = true)
    }
    Surface(
        modifier = modifier,
        color = colors.surfaceContainer,
        shape = NexusCornerShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(NexusSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(NexusSpacing.medium)
        ) {
            Text(uiText(languageIndex, "Configurations"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NexusOutlinedTextField(
                    value = configName,
                    onValueChange = { configName = it },
                    modifier = Modifier.weight(1f).height(56.dp),
                    label = { Text(uiText(languageIndex, "Configuration name")) }
                )
                FilledTonalButton(
                    onClick = {
                        if (state.createBlankConfig(configName)) configName = ""
                    },
                    modifier = Modifier.height(56.dp),
                    shape = NexusCornerShape
                ) { Text(uiText(languageIndex, "Create blank config")) }
            }
            if (filteredConfigs.isEmpty()) {
                Text(uiText(languageIndex, "No configurations yet"), color = colors.onSurfaceVariant)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(NexusSpacing.small)
                ) {
                    filteredConfigs.forEach { config -> ConfigRow(config, state, languageIndex) }
                }
            }
        }
    }
}

@Composable
private fun ConfigRow(config: GuiConfig, state: ClickGuiState, languageIndex: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceContainerLow,
        shape = NexusCornerShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(NexusSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small)
        ) {
            Text(config.name, modifier = Modifier.weight(1f), color = colors.onSurface, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = { state.loadConfig(config.id) }, shape = NexusCornerShape) { Text(uiText(languageIndex, "Load")) }
            FilledTonalButton(onClick = { state.saveCurrentToConfig(config.id) }, shape = NexusCornerShape) { Text(uiText(languageIndex, "Save")) }
            IconButton(onClick = { state.deleteConfig(config.id) }) {
                Icon(Icons.Default.Delete, contentDescription = uiText(languageIndex, "Delete"))
            }
        }
    }
}

@Composable
private fun LoginPanel(
    onLogin: (String, String, Boolean) -> Boolean,
    languageIndex: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberLogin by rememberSaveable { mutableStateOf(false) }
    var loginError by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.surfaceContainer,
        shape = NexusCornerShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(NexusSpacing.large),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                uiText(languageIndex, "Login"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Spacer(Modifier.size(NexusSpacing.small))
            Text(
                uiText(languageIndex, "Enter your account and password to continue"),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.size(NexusSpacing.large))
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = androidx.compose.ui.graphics.Color.Transparent,
                    backgroundColor = colors.primary.copy(alpha = 0.25f)
                )
            ) {
                NexusOutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        loginError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    label = { Text(uiText(languageIndex, "Account")) }
                )
            }
            Spacer(Modifier.size(NexusSpacing.small))
            NexusOutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    loginError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                label = { Text(uiText(languageIndex, "Password")) },
                visualTransformation = if (passwordVisible) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = uiText(languageIndex, if (passwordVisible) "Hide password" else "Show password")
                        )
                    }
                }
            )
            if (loginError) {
                Spacer(Modifier.size(NexusSpacing.small))
                Text(
                    uiText(languageIndex, "Account and password are required"),
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(
                    checked = rememberLogin,
                    onCheckedChange = { rememberLogin = it }
                )
                Text(uiText(languageIndex, "Remember me"), color = colors.onSurfaceVariant)
            }
            FilledTonalButton(
                onClick = { loginError = !onLogin(username, password, rememberLogin) },
                modifier = Modifier.fillMaxWidth(),
            shape = NexusCornerShape
            ) {
                Text(uiText(languageIndex, "Login"))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun NexusOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None
) {
    val colors = MaterialTheme.colorScheme
    val textFieldColors = OutlinedTextFieldDefaults.colors()
    val interactionSource = remember { MutableInteractionSource() }
    val inputTextStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)

    CompositionLocalProvider(LocalTextStyle provides inputTextStyle) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            singleLine = true,
            textStyle = inputTextStyle,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(colors.primary),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    colors = textFieldColors,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = textFieldColors,
                            shape = NexusCornerShape,
                            focusedBorderThickness = NexusInputBorderThickness,
                            unfocusedBorderThickness = NexusInputBorderThickness
                        )
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeNexusPanel(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.surfaceContainer,
        shape = NexusCornerShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(NexusSpacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LoadingIndicator(modifier = Modifier.size(NexusDimensions.loadingIndicator), color = colors.primary)
            Spacer(Modifier.size(NexusSpacing.extraLarge))
            Text("Welcome Material", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
        }
    }
}
