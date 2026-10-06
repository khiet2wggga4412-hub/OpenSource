package com.example.md3clickgui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.md3clickgui.ui.language.uiText
import com.example.md3clickgui.ui.model.GuiModule
import com.example.md3clickgui.ui.model.GuiSection
import com.example.md3clickgui.ui.model.ModuleBinding
import com.example.md3clickgui.ui.model.ModuleSetting
import com.example.md3clickgui.ui.modules.visual.ArrayListModule
import com.example.md3clickgui.ui.modules.SectionIds
import com.example.md3clickgui.ui.modules.guiSections
import com.example.md3clickgui.ui.state.ClickGuiState
import com.example.md3clickgui.ui.theme.NexusMotion
import com.example.md3clickgui.ui.theme.NexusSpacing

private enum class ArraylistCorner(val top: Boolean, val start: Boolean) {
    TopEnd(top = true, start = false),
    TopStart(top = true, start = true),
    BottomEnd(top = false, start = false),
    BottomStart(top = false, start = true);

    companion object {
        fun of(index: Int): ArraylistCorner = entries.getOrElse(index) { TopEnd }
    }
}

private const val MaxRows = 32

private val NonListingBindings = setOf(
    ModuleBinding.Theme,
    ModuleBinding.Language,
    ModuleBinding.ShortcutButton,
    ModuleBinding.Content
)

@Composable
fun ArrayListHudLayer(
    state: ClickGuiState,
    sections: List<GuiSection> = guiSections,
    modifier: Modifier = Modifier
) {
    val module = ArrayListModule.model

    if (!state.isChecked(module)) return

    val colors = MaterialTheme.colorScheme
    val choices = module.settings.filterIsInstance<ModuleSetting.Choice>()
    val sliders = module.settings.filterIsInstance<ModuleSetting.Slider>()
    val toggles = module.settings.filterIsInstance<ModuleSetting.Toggle>()
    if (choices.isEmpty() || sliders.size < 2 || toggles.size < 2) return

    val textSize = state.sliderValue(module, sliders[0])
    val showIcons = state.toggleValue(module, toggles[0])
    val showBackground = state.toggleValue(module, toggles[1])
    val backgroundOpacity = state.sliderValue(module, sliders[1])
    val corner = ArraylistCorner.of(state.choiceIndex(module, choices[0]))

    val entries = orderedEntries(state, sections)
    if (entries.isEmpty()) return

    val padded = Modifier.fillMaxSize().padding(NexusSpacing.medium)
    val (anchor, contentAlignment) = when {
        corner.top && corner.start -> padded to Alignment.TopStart
        corner.top -> padded to Alignment.TopEnd
        corner.start -> padded to Alignment.BottomStart
        else -> padded to Alignment.BottomEnd
    }
    Box(modifier = modifier.then(anchor), contentAlignment = contentAlignment) {

        Box(modifier = Modifier.wrapContentSize(align = contentAlignment)) {

            val slideSpec = NexusMotion.enterSpec<IntOffset>()
            val fromEdge = if (corner.start) -1f else 1f
            LazyColumn(

                userScrollEnabled = false,
                horizontalAlignment = if (corner.start) Alignment.Start else Alignment.End,
                verticalArrangement = Arrangement.spacedBy(NexusSpacing.extraSmall, Alignment.Top)
            ) {
            items(items = entries, key = { it.module.id }) { row ->

                val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = visible,

                    enter = fadeIn(NexusMotion.enterSpec<Float>()) +
                        slideInHorizontally(slideSpec) { width -> (width * fromEdge).toInt() },
                    exit = fadeOut(NexusMotion.exitSpec<Float>()) +
                        slideOutHorizontally(slideSpec) { width -> (width * fromEdge).toInt() },
                    modifier = Modifier.animateItem(placementSpec = slideSpec)
                ) {
                    ArrayListRow(
                        label = uiText(state.languageIndex, row.module.name),
                        icon = row.module.icon,
                        iconTint = sectionAccent(row.sectionId, colors),
                        textSize = textSize.sp,
                        showIcon = showIcons,
                        showBackground = showBackground,
                        backgroundOpacity = backgroundOpacity
                    )
                }
            }
        }
    }
}
}

private data class ArraylistRow(val sectionId: String, val module: GuiModule, val label: String)

private fun orderedEntries(state: ClickGuiState, sections: List<GuiSection>): List<ArraylistRow> {
    val result = ArrayList<ArraylistRow>(MaxRows)
    for (section in sections) {
        if (section.id == SectionIds.CONFIG) continue
        for (candidate in section.modules) {
            if (candidate.id == ArrayListModule.model.id) continue
            if (candidate.binding in NonListingBindings) continue
            if (!state.isChecked(candidate)) continue
            result += ArraylistRow(section.id, candidate, uiText(state.languageIndex, candidate.name))
        }
    }
    return result.sortedByDescending { it.label.length }.take(MaxRows)
}

@Composable
private fun ArrayListRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    textSize: TextUnit,
    showIcon: Boolean,
    showBackground: Boolean,
    backgroundOpacity: Float
) {
    val colors = MaterialTheme.colorScheme
    val container = if (showBackground) {
        colors.surfaceContainerHigh.copy(alpha = backgroundOpacity.coerceIn(0.2f, 1f))
    } else {
        Color.Transparent
    }

    val iconSize = with(LocalDensity.current) { (textSize.value + 3f).sp.toDp() }

    val verticalPadding = (textSize.value * 0.45f).dp.coerceAtLeast(1.dp)
    val horizontalPadding = (textSize.value * 0.6f).dp.coerceAtLeast(1.dp)
    Surface(
        shape = RoundedCornerShape(iconSize.coerceAtLeast(4.dp)),
        color = container,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.widthIn(max = 220.dp).padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(horizontalPadding)
        ) {
            if (showIcon) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = textSize, lineHeight = textSize * 1.25f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun sectionAccent(sectionId: String, colors: androidx.compose.material3.ColorScheme): Color {
    val palette = listOf(
        colors.primary,
        colors.tertiary,
        colors.secondary,
        colors.primary,
        colors.tertiary,
        colors.secondary
    )
    val index = guiSections.indexOfFirst { it.id == sectionId }
    if (index < 0) return colors.primary
    return palette[index % palette.size]
}
