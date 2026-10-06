@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.example.md3clickgui.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.md3clickgui.music.MusicPlayer
import com.example.md3clickgui.music.NeteaseMusicApi
import com.example.md3clickgui.music.PlaylistSummary
import com.example.md3clickgui.music.RecentStore
import com.example.md3clickgui.music.Song
import com.example.md3clickgui.ui.language.uiText
import com.example.md3clickgui.ui.theme.NexusCornerShape
import com.example.md3clickgui.ui.theme.NexusIconShape
import com.example.md3clickgui.ui.theme.NexusSpacing

@Composable
fun FeaturedPanel(languageIndex: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var playlists by remember { mutableStateOf<List<PlaylistSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<PlaylistSummary?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        playlists = NeteaseMusicApi.highqualityPlaylists()
        isLoading = false
    }

    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxSize(), color = colors.surfaceContainerLow, shape = NexusCornerShape) {
        Column(Modifier.fillMaxSize().padding(NexusSpacing.extraSmall)) {
            val current = selected
            if (current == null) {
                FeatureHeader(uiText(languageIndex, "Featured"), uiText(languageIndex, "High quality playlists"), onClose)
                Spacer(Modifier.height(NexusSpacing.extraSmall))
                if (isLoading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.size(48.dp), color = colors.primary)
                    }
                } else {
                    PlaylistList(playlists, languageIndex, Modifier.weight(1f)) { selected = it }
                }
            } else {
                PlaylistSongsView(current, languageIndex, onBack = { selected = null }, onClose = onClose)
            }
        }
    }
}
@Composable
fun RecentPanel(languageIndex: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var songs by remember { mutableStateOf(RecentStore.songs()) }
    var currentIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(MusicPlayer.currentSong?.id) {
        songs = RecentStore.songs()
    }

    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxSize(), color = colors.surfaceContainerLow, shape = NexusCornerShape) {
        Column(Modifier.fillMaxSize().padding(NexusSpacing.extraSmall)) {
            FeatureHeader(uiText(languageIndex, "Recent"), uiText(languageIndex, "Recently played"), onClose)
            Spacer(Modifier.height(NexusSpacing.extraSmall))
            SongListColumn(
                songs, languageIndex, uiText(languageIndex, "Nothing played yet"),
                onPlay = { index -> currentIndex = index; MusicPlayer.play(songs[index]) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(NexusSpacing.extraSmall))
            NowPlayingBar(songs, currentIndex) { index -> currentIndex = index; MusicPlayer.play(songs[index]) }
        }
    }
}

@Composable
private fun FeatureHeader(
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
    actions: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = NexusIconShape, color = colors.surfaceContainer) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = NexusSpacing.medium, end = NexusSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = NexusSpacing.extraSmall)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            actions?.invoke()
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
@Composable
private fun SongListColumn(
    songs: List<Song>,
    languageIndex: Int,
    emptyText: String,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable (Song) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    if (songs.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        return
    }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(NexusSpacing.extraSmall)) {
        songs.forEachIndexed { index, song ->
            val trailingContent: (@Composable () -> Unit)? = trailing?.let { t -> { t(song) } }
            SongRow(
                song = song,
                isCurrent = song.id == MusicPlayer.currentSong?.id,
                onClick = { onPlay(index) },
                trailing = trailingContent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
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
                modifier = Modifier.size(36.dp).background(
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
            if (trailing != null) trailing() else Text(formatTime(song.durationMs),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaylistList(items: List<PlaylistSummary>, languageIndex: Int, modifier: Modifier = Modifier, onSelect: (PlaylistSummary) -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (items.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(uiText(languageIndex, "No results"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        return
    }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(NexusSpacing.extraSmall)) {
        items.forEach { item -> PlaylistRow(item, languageIndex) { onSelect(item) } }
    }
}

@Composable
private fun PlaylistRow(item: PlaylistSummary, languageIndex: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = NexusIconShape, color = colors.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth().padding(horizontal = NexusSpacing.small, vertical = NexusSpacing.small),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small)) {
            Box(modifier = Modifier.size(36.dp).background(colors.surfaceContainerHighest, NexusIconShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.trackCount} ${uiText(languageIndex, "tracks")}",
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaylistSongsView(playlist: PlaylistSummary, languageIndex: Int, onBack: () -> Unit, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var songs by remember(playlist.id) { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember(playlist.id) { mutableStateOf(false) }
    var currentIndex by remember(playlist.id) { mutableIntStateOf(-1) }

    LaunchedEffect(playlist.id) {
        isLoading = true
        songs = NeteaseMusicApi.playlistSongs(playlist.id)
        isLoading = false
    }

    Column(Modifier.fillMaxSize()) {
        Surface(shape = NexusIconShape, color = colors.surfaceContainer) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = NexusSpacing.extraSmall, end = NexusSpacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                Column(Modifier.weight(1f).padding(vertical = NexusSpacing.extraSmall)) {
                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
            }
        }
        Spacer(Modifier.height(NexusSpacing.extraSmall))
        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                LoadingIndicator(Modifier.size(48.dp), color = colors.primary)
            }
        } else {
            SongListColumn(
                songs, languageIndex, uiText(languageIndex, "No results"),
                onPlay = { index -> currentIndex = index; MusicPlayer.play(songs[index]) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(NexusSpacing.extraSmall))
        NowPlayingBar(songs, currentIndex) { index -> currentIndex = index; MusicPlayer.play(songs[index]) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NowPlayingBar(queue: List<Song>, currentIndex: Int, onPlayAt: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val song = MusicPlayer.currentSong
    Surface(shape = NexusCornerShape, color = colors.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = NexusSpacing.medium, vertical = NexusSpacing.small),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NexusSpacing.small)) {
            Column(Modifier.weight(1f)) {
                Text(song?.title ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song?.artist?.isNotEmpty() == true) {
                    Text(song.artist, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            FilledTonalIconButton(onClick = { onPlayAt(currentIndex - 1) }, enabled = currentIndex > 0) {
                Icon(Icons.Default.SkipPrevious, contentDescription = null)
            }
            FilledIconButton(onClick = { MusicPlayer.toggle() }, enabled = song != null) {
                Icon(if (MusicPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
            }
            FilledTonalIconButton(
                onClick = { onPlayAt(currentIndex + 1) },
                enabled = currentIndex >= 0 && currentIndex < queue.lastIndex
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
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
