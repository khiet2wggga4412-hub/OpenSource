package dev.liquid.clickgui.ui

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter

val LocalPackResources = staticCompositionLocalOf<Resources?> { null }

@Composable
fun PackResourceIcon(
    resourceId: Int,
    contentDescription: String?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val resources = LocalPackResources.current
    val bitmap = remember(resources, resourceId) {
        resources?.let { BitmapFactory.decodeResource(it, resourceId)?.asImageBitmap() }
    } ?: return
    Image(
        painter = BitmapPainter(bitmap),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier,
    )
}
