package com.example.md3clickgui.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.md3clickgui.music.CoverArt
import com.example.md3clickgui.music.MusicPlayer
import com.example.md3clickgui.music.NeteaseMusicApi
import com.example.md3clickgui.music.Song
import com.example.md3clickgui.ui.language.uiText
import com.example.md3clickgui.ui.theme.NexusCornerShape
import com.example.md3clickgui.ui.theme.NexusIconShape
import com.example.md3clickgui.ui.theme.NexusMotion
import com.example.md3clickgui.ui.theme.NexusSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.IntOffset

/** Full player panel for the Music module: search, results, and a now-playing bar with seeking. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MusicModulePanel(
    modifier: Modifier = Modifier,
    languageIndex: Int = 0,
    onClose: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val player = MusicPlayer
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var queue by remember { mutableStateOf<List<Song>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var cover by remember { mutableStateOf<Bitmap?>(null) }
    var accent by remember { mutableStateOf<Color?>(null) }

    fun runSearch(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            isLoading = true
            hint = null
            val found = NeteaseMusicApi.search(trimmed)
            results = found
            hint = if (found.isEmpty()) uiText(languageIndex, "No results") else null
            isLoading = false
        }
    }

    fun playAt(index: Int) {
        val song = queue.getOrNull(index) ?: return
        currentIndex = index
        player.play(song)
    }

    LaunchedEffect(player.isPlaying, player.currentSong?.id) {
        while (player.isPlaying) {
            player.syncPosition()
            delay(500)
        }
    }

    LaunchedEffect(player.currentSong?.id) {
        cover = null
        accent = null
        val song = player.currentSong
        if (song != null && song.coverUrl.isNotEmpty()) {
            val bmp = withContext(Dispatchers.IO) { CoverArt.loadCover(song.coverUrl) }
            if (bmp != null) {
                cover = bmp
                val extracted = CoverArt.dominantColor(bmp)
                if (extracted != 0) accent = Color(extracted)
            }
        }
    }

    Surface(modifier.fillMaxSize(), color = colors.surfaceContainerLow, shape = NexusCornerShape) {
        Column(Modifier.fillMaxSize().padding(NexusSpacing.extraSmall)) {
            Surface(shape = NexusIconShape, color = colors.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(start = NexusSpacing.medium, end = NexusSpacing.extraSmall)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = NexusSpacing.extraSmall)) {
                            Text(uiText(languageIndex, "Music"), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(uiText(languageIndex, "NetEase Cloud Music"), style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant)
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = uiText(languageIndex, ClosePanelLabel),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(NexusSpacing.extraSmall))

            MusicSearchField(
                value = query,
                onValueChange = { query = it },
                onSearch = { runSearch(it) },
                isLoading = isLoading,
                label = uiText(languageIndex, "Search songs"),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(NexusSpacing.extraSmall))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.size(48.dp), color = colors.primary)
                    }
                    results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp),
                                tint = colors.onSurfaceVariant)
                            Spacer(Modifier.height(NexusSpacing.small))
                            Text(hint ?: uiText(languageIndex, "Search and tap a song to play"),
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    else -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(NexusSpacing.extraSmall)
                    ) {
                        results.forEachIndexed { index, song ->
                            SongRow(
                                song = song,
                                isCurrent = song.id == player.currentSong?.id,
                                onClick = {
                                    queue = results
                                    playAt(index)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(NexusSpacing.extraSmall))

            NowPlayingCard(player, queue, currentIndex, ::playAt, languageIndex, cover, accent)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MusicSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.height(44.dp), shape = NexusIconShape, color = colors.surfaceContainerHigh) {
        Row(Modifier.fillMaxSize().padding(horizontal = NexusSpacing.small), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant)
            Spacer(Modifier.width(NexusSpacing.small))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(value) }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        inner()
                    }
                }
            )
            if (isLoading) {
                LoadingIndicator(Modifier.size(18.dp), color = colors.primary)
            } else {
                FilledTonalIconButton(onClick = { onSearch(value) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tileShape = NexusIconShape
    Surface(
        onClick = onClick,
        shape = NexusIconShape,
        color = if (isCurrent) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (isCurrent) colors.onPrimaryContainer else colors.onSurface
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = NexusSpacing.small, vertical = NexusSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isCurrent) colors.primary else colors.surfaceContainerHighest,
                        shape = tileShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrent) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isCurrent) colors.onPrimary else colors.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.artist.isNotEmpty()) {
                    Text(song.artist, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(formatTime(song.durationMs), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

/** Cover artwork; falls back to an accent-tinted gradient + note when no image is available. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArtworkTile(isBuffering: Boolean, cover: Bitmap?, accent: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val brush = Brush.linearGradient(listOf(accent, colors.tertiaryContainer))
    Box(
        modifier = modifier.clip(NexusCornerShape).background(brush),
        contentAlignment = Alignment.Center
    ) {
        when {
            cover != null -> Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            isBuffering -> LoadingIndicator(Modifier.size(28.dp), color = Color.White)
            else -> Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NowPlayingCard(
    player: MusicPlayer,
    queue: List<Song>,
    currentIndex: Int,
    onPlayAt: (Int) -> Unit,
    languageIndex: Int,
    cover: Bitmap?,
    accent: Color?
) {
    val colors = MaterialTheme.colorScheme
    val accentColor by animateColorAsState(accent ?: colors.primary)
    val song = player.currentSong
    val duration = player.durationMs.coerceAtLeast(1L)
    val position = player.positionMs.coerceIn(0L, duration)
    Surface(shape = NexusCornerShape, color = colors.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(NexusSpacing.medium)) {
            // Hoisted out of transitionSpec, which is not a composable scope.
            val trackFadeSpec = NexusMotion.enterSpec<Float>()
            val trackExitFadeSpec = NexusMotion.exitSpec<Float>()
            val trackSlideSpec = NexusMotion.enterSpec<IntOffset>()
            AnimatedContent(
                targetState = song,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    if (targetState != null) {
                        (fadeIn(animationSpec = trackFadeSpec) + slideInVertically(
                            animationSpec = trackSlideSpec,
                            initialOffsetY = { it / 3 }
                        )).togetherWith(fadeOut(animationSpec = trackExitFadeSpec))
                    } else {
                        fadeIn(animationSpec = trackFadeSpec) togetherWith fadeOut(animationSpec = trackExitFadeSpec)
                    }
                },
                label = "nowPlaying"
            ) { target ->
                if (target == null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small)) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(colors.surfaceContainerHigh, NexusIconShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null,
                                modifier = Modifier.size(24.dp), tint = colors.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(uiText(languageIndex, "Not playing"), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(uiText(languageIndex, "Search and tap a song to play"),
                                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        }
                    }
                } else {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NexusSpacing.medium)) {
                            ArtworkTile(player.isBuffering, cover, accentColor, Modifier.size(64.dp))
                            Column(Modifier.weight(1f)) {
                                Text(target.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (target.artist.isNotEmpty()) {
                                    Text(target.artist, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(Modifier.height(NexusSpacing.medium))

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small)) {
                            Text(formatTime(position), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            Slider(
                                value = position.toFloat(),
                                onValueChange = { player.seekTo(it.toLong()) },
                                valueRange = 0f..duration.toFloat(),
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = colors.surfaceContainerHighest
                                )
                            )
                            Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NexusSpacing.large, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(onClick = { onPlayAt(currentIndex - 1) }, enabled = currentIndex > 0) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = uiText(languageIndex, "Previous"))
                            }
                            FilledIconButton(
                                onClick = { player.toggle() },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = uiText(languageIndex, if (player.isPlaying) "Pause" else "Play"),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { onPlayAt(currentIndex + 1) },
                                enabled = currentIndex >= 0 && currentIndex < queue.lastIndex
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = uiText(languageIndex, "Next"))
                            }
                        }

                        player.error?.let { message ->
                            Spacer(Modifier.height(NexusSpacing.small))
                            Surface(shape = RoundedCornerShape(8.dp), color = colors.errorContainer) {
                                Text(
                                    message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onErrorContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = NexusSpacing.small, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}