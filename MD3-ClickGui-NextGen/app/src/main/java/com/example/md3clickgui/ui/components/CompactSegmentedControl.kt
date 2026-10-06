package com.example.md3clickgui.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.md3clickgui.ui.theme.NexusDimensions
import com.example.md3clickgui.ui.theme.NexusIconShape
import com.example.md3clickgui.ui.theme.NexusMotion

@Composable
internal fun CompactSegmentedControl(
    options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit,
    label: String, modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val position by animateFloatAsState(selectedIndex.coerceIn(options.indices).toFloat(),
        NexusMotion.feedbackSpec<Float>(), label = "segmentSlide")
    Surface(modifier.height(NexusDimensions.controlHeight), shape = NexusIconShape,
        color = colors.surfaceContainerHigh) {
        Row(Modifier.padding(3.dp).selectableGroup().semantics { contentDescription = label }
            .drawBehind {
                val segmentWidth = size.width / options.size
                drawRoundRect(colors.secondaryContainer, Offset(segmentWidth * position, 0f),
                    Size(segmentWidth, size.height), CornerRadius(7.dp.toPx()))
            }) {
            options.forEachIndexed { index, option ->
                Box(Modifier.weight(1f).fillMaxHeight().selectable(
                    selected = selectedIndex == index, role = Role.RadioButton,
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = { onSelect(index) }), contentAlignment = Alignment.Center) {
                    Text(option, style = MaterialTheme.typography.labelLarge,
                        color = if (index == selectedIndex) colors.onSecondaryContainer else colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
