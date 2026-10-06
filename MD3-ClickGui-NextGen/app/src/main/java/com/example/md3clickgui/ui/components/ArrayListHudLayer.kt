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

/** Positions offered by [ArrayListModule]; the index is the Choice value stored by the settings row. */
private enum class ArraylistCorner(val top: Boolean, val start: Boolean) {
    TopEnd(top = true, start = false),
    TopStart(top = true, start = true),
    BottomEnd(top = false, start = false),
    BottomStart(top = false, start = true);

    companion object {
        fun of(index: Int): ArraylistCorner = entries.getOrElse(index) { TopEnd }
    }
}

/**
 * Safety bound only: every enabled module gets a row, and the catalog cannot reach this. It exists
 * so a future module explosion cannot render an unbounded list.
 */
private const val MaxRows = 32

/** The arraylist lists features that toggle; these bindings describe settings or panel content. */
private val NonListingBindings = setOf(
    ModuleBinding.Theme,
    ModuleBinding.Language,
    ModuleBinding.FloatingButton,
    ModuleBinding.Content
)

/**
 * Arraylist HUD: every enabled module as a compact Material 3 row, longest label first.
 *
 * The layer must never swallow input meant for the workspace, which constrains how it is built:
 *
 * - the anchor box fills the screen only to position its child; it carries no gesture handling, so
 *   taps outside the child pass straight through;
 * - the list itself is wrapped in a shrink-to-fit box, so the region that does take touch is only
 *   as large as the rows. A `LazyColumn` consumes pointer events by itself in order to scroll, so
 *   giving it the full screen would make the whole workspace untappable — the panel would look
 *   unresponsive wherever the list was laid out.
 */
@Composable
fun ArrayListHudLayer(
    state: ClickGuiState,
    sections: List<GuiSection> = guiSections,
    modifier: Modifier = Modifier
) {
    val module = ArrayListModule.model
    // Deliberately independent of the window: the arraylist is a HUD, so it keeps showing the
    // enabled modules while the ClickGUI is collapsed. The module's own switch is the only control.
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
        // Shrink-to-fit, so the layer's own footprint is exactly the rows and nothing else. The list
        // is never given a height budget: one row per enabled module, however many that is. Being
        // content-sized also keeps it non-interactive — it never consumes a gesture, so a tap in
        // that corner still reaches the workspace underneath.
        Box(modifier = Modifier.wrapContentSize(align = contentAlignment)) {
            // A lazy list, not a plain Column: only a lazy layout can animate an item from its old
            // position to its new one when the order changes. The rows are additionally keyed by
            // module id, so a row is the same item across re-sorts instead of a slot that changes
            // identity.
            // One spatial token for all three: the row sliding in, sliding out, and moving to a new
            // position when the list re-sorts.
            val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
            val fromEdge = if (corner.start) -1f else 1f
            LazyColumn(
                // Content-sized, so there is nothing to scroll; leaving scrolling on would let the
                // list swallow drags meant for the workspace under it.
                userScrollEnabled = false,
                horizontalAlignment = if (corner.start) Alignment.Start else Alignment.End,
                verticalArrangement = Arrangement.spacedBy(NexusSpacing.extraSmall, Alignment.Top)
            ) {
            items(items = entries, key = { it.module.id }) { row ->
                // Seeded false so a row that appears later still animates in.
                val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = visible,
                    // Rows come in from the side the list is anchored to: a right-hand list travels
                    // in from the right edge, a left-hand one from the left, at the full row width so
                    // the motion reads as a slide. Exit mirrors the entry.
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()) +
                        slideInHorizontally(slideSpec) { width -> (width * fromEdge).toInt() },
                    exit = fadeOut(MaterialTheme.motionScheme.defaultSpatialSpec<Float>()) +
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

/**
 * Enabled, listable modules in catalog order, then reordered by label length (longest first) so the
 * stack reads as the usual arraylist wedge. Ties keep catalog order, which keeps the layout stable.
 */
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

/** One entry: optional module icon, then the module name. */
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
    // The icon follows the text size so the row keeps its proportions at every size.
    val iconSize = with(LocalDensity.current) { (textSize.value + 3f).sp.toDp() }
    // Padding scales with the label too. A fixed inset would dominate the row once the text is
    // small — at 1sp the label is one pixel tall, so a constant 6dp would leave a mostly empty row.
    // The floor keeps a minimum of breathing room at the smallest sizes.
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

/**
 * Category accent from the fixed section order, so a module keeps one colour across recompositions.
 *
 * Solid roles only — the `*Container` variants are pale surfaces, and as an icon tint on the row
 * they read as washed out. These are the saturated roles, which stay legible on the row surface.
 */
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
