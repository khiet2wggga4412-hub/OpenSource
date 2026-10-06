package com.example.md3clickgui.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.md3clickgui.ui.language.uiText
import com.example.md3clickgui.ui.model.GuiSection
import com.example.md3clickgui.ui.modules.SectionIds
import com.example.md3clickgui.ui.modules.guiSections
import com.example.md3clickgui.ui.theme.NexusCornerShape
import com.example.md3clickgui.ui.theme.NexusDimensions
import com.example.md3clickgui.ui.theme.NexusIconShape
import com.example.md3clickgui.ui.theme.NexusSpacing
import com.example.md3clickgui.ui.theme.NexusMotion
import com.example.md3clickgui.ui.theme.colorSpring

/** Navigation occupies its own column, including its expand/collapse button. */
@Composable
fun CategoryRail(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onToggleExpanded: () -> Unit,
    sections: List<GuiSection> = guiSections,
    languageIndex: Int = 0,
    accountName: String? = null,
    accountExpiryText: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val selection = rememberSlidingSelection(sections.getOrNull(selectedIndex)?.id, colors.secondaryContainer, colors.surfaceContainer)
    // Sections that hold their place at the bottom of the rail, in catalog order (Music, Config).
    val pinnedIds = setOf(SectionIds.MUSIC, SectionIds.CONFIG)
    val labelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>(),
        label = "categoryLabelFade"
    )
    Surface(modifier = modifier.fillMaxHeight(), color = colors.surfaceContainerLow, shape = NexusCornerShape) {
        Column(Modifier.fillMaxSize().padding(NexusSpacing.extraSmall).clipToBounds()) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleExpanded) {
                    Icon(Icons.Default.Menu, contentDescription = uiText(languageIndex, "Toggle categories"), modifier = Modifier.size(22.dp))
                }
                    Column(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(NexusDimensions.categoryRailExpanded - 56.dp)
                        .graphicsLayer { alpha = labelAlpha }
                        .then(if (!expanded) Modifier.clearAndSetSemantics { } else Modifier)) {
                        Text(accountName ?: "Material", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (accountExpiryText != null) {
                            Text(accountExpiryText, style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
            }
            Spacer(Modifier.height(NexusSpacing.extraSmall))
            // Two sibling groups, and the order matters: the scrolling categories are drawn first and
            // the anchored ones (Music, Config) after them, so the anchored rows sit in the upper
            // layer. The selection indicator therefore slides *underneath* them and is hidden by them
            // on its way past, which is the intended rail behaviour.
            //
            // The scrolling group keeps its own clip: without it the indicator would be drawn outside
            // the scroll viewport as it travels, which is what cut a flat edge into it before.
            Column(with(selection) { Modifier.weight(1f).fillMaxWidth().selectionContainer() }) {
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState())) {
                    Column(verticalArrangement = Arrangement.spacedBy(NexusDimensions.rowGap)) {
                        sections.forEachIndexed { index, section ->
                            if (section.id !in pinnedIds) {
                                CategoryItem(section, index == selectedIndex, expanded, labelAlpha, selection, languageIndex) { onSelect(index) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(NexusSpacing.extraSmall))
            // Outside the selection container: these rows are never washed or covered by it, and they
            // paint over it because they come later in the parent's draw order.
            sections.forEachIndexed { index, section ->
                if (section.id in pinnedIds) {
                    CategoryItem(section, index == selectedIndex, expanded, labelAlpha, selection, languageIndex,
                        outsideContainer = true) { onSelect(index) }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(section: GuiSection, isSelected: Boolean, expanded: Boolean, labelAlpha: Float,
    selection: SlidingSelection, languageIndex: Int, outsideContainer: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val contentColor by animateColorAsState(
        if (isSelected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        colorSpring(), label = "categoryContentColor"
    )
    // The anchored rows carry no selected background: the indicator deliberately stays on the last
    // scrolling card, and a second highlight here would read as the indicator having moved.
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(NexusDimensions.categoryItem).semantics { selected = isSelected },
        shape = NexusIconShape,
        color = Color.Transparent,
        contentColor = contentColor
    ) {
        // Anchored rows are registered as non-slidable: the indicator never travels to them, so
        // selecting Music or Config leaves it resting where it was.
        val rowModifier = with(selection) {
            Modifier.fillMaxSize().selectionRow(section.id, slidable = !outsideContainer)
        }
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(48.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Icon(section.icon, contentDescription = uiText(languageIndex, section.name), modifier = Modifier.size(20.dp))
            }
            Row(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(NexusDimensions.categoryRailExpanded - 56.dp)
                .graphicsLayer { alpha = labelAlpha }
                .then(if (!expanded) Modifier.clearAndSetSemantics { } else Modifier),
                verticalAlignment = Alignment.CenterVertically) {
                Text(uiText(languageIndex, section.name), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (section.id != SectionIds.CONFIG) {
                    Text("${section.modules.size}", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = NexusSpacing.small))
                }
            }
        }
    }
}
