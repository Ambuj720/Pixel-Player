package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.CreateOrEditPlaylistDialog
import com.example.ui.components.PlaylistCard
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatDurationMs
import com.example.ui.viewmodel.PlayerUiState

@Composable
fun PlaylistsScreen(
    uiState: PlayerUiState,
    onCreatePlaylist: (name: String, description: String) -> Unit,
    onRenamePlaylist: (playlistId: String, name: String, description: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onClosePlaylistDetail: () -> Unit,
    onPlayTracks: (List<Track>, Int, Boolean) -> Unit,
    onAddTrackToPlaylist: (playlistId: String, trackId: String) -> Unit,
    onRemoveTrackFromPlaylist: (playlistId: String, trackId: String) -> Unit,
    onMoveTrackInPlaylist: (playlistId: String, fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPlaylist = uiState.selectedPlaylistId?.let { id ->
        uiState.playlists.firstOrNull { it.id == id }
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    if (selectedPlaylist != null) {
        BackHandler(onBack = onClosePlaylistDetail)
        PlaylistDetailView(
            playlist = selectedPlaylist,
            allTracks = uiState.tracks,
            tracksById = uiState.tracksById,
            currentTrackId = uiState.playback.currentTrack?.id,
            onBack = onClosePlaylistDetail,
            onRename = { name, desc -> onRenamePlaylist(selectedPlaylist.id, name, desc) },
            onDelete = { onDeletePlaylist(selectedPlaylist.id) },
            onPlayAll = { tracks, shuffle -> onPlayTracks(tracks, 0, shuffle) },
            onPlayAtIndex = { tracks, idx -> onPlayTracks(tracks, idx, false) },
            onAddTrack = { trackId -> onAddTrackToPlaylist(selectedPlaylist.id, trackId) },
            onRemoveTrack = { trackId -> onRemoveTrackFromPlaylist(selectedPlaylist.id, trackId) },
            onMoveTrack = { from, to -> onMoveTrackInPlaylist(selectedPlaylist.id, from, to) },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.playlists.size} local collections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Playlist")
                }
            }

            if (uiState.playlists.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No playlists yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Organize your favorite tracks, reorder songs, and shuffle entire collections.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 166.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.playlists, key = { it.id }) { pl ->
                        val plTracks = pl.trackIds.mapNotNull { uiState.tracksById[it] }
                        PlaylistCard(
                            playlist = pl,
                            leadTrack = plTracks.firstOrNull(),
                            onClick = { onOpenPlaylist(pl.id) },
                            onQuickPlay = {
                                if (plTracks.isNotEmpty()) onPlayTracks(plTracks, 0, false)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrEditPlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc ->
                onCreatePlaylist(name, desc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    allTracks: List<Track>,
    tracksById: Map<String, Track>,
    currentTrackId: String?,
    onBack: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: () -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onPlayAtIndex: (List<Track>, Int) -> Unit,
    onAddTrack: (String) -> Unit,
    onRemoveTrack: (String) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks = remember(playlist.trackIds, tracksById) {
        playlist.trackIds.mapNotNull { tracksById[it] }
    }
    val totalDuration = remember(tracks) { tracks.sumOf { it.durationMs } }

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddTracksDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "top_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("playlist_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to playlists")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist")
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete playlist",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item(key = "hero") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TrackArtwork(
                            artworkPath = tracks.firstOrNull()?.artworkPath,
                            title = playlist.name,
                            seedColor = tracks.firstOrNull()?.artworkSeedColor,
                            fallbackIndex = playlist.coverArtIndex,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (playlist.description.isNotBlank()) {
                                Text(
                                    text = playlist.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${tracks.size} songs • ${formatDurationMs(totalDuration)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { if (tracks.isNotEmpty()) onPlayAll(tracks, false) },
                            enabled = tracks.isNotEmpty(),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play")
                        }
                        FilledTonalButton(
                            onClick = { if (tracks.isNotEmpty()) onPlayAll(tracks, true) },
                            enabled = tracks.isNotEmpty(),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                        FilledTonalButton(
                            onClick = { showAddTracksDialog = true },
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add songs")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (tracks.isEmpty()) {
            item(key = "empty_playlist") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("This playlist is empty", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = { showAddTracksDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Songs from Library")
                    }
                }
            }
        } else {
            itemsIndexed(tracks, key = { idx, t -> "${t.id}_$idx" }) { index, track ->
                val isCurrent = track.id == currentTrackId
                Surface(
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onPlayAtIndex(tracks, index) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArtwork(
                            artworkPath = track.artworkPath,
                            title = track.title,
                            seedColor = track.artworkSeedColor,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onMoveTrack(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { onMoveTrack(index, index + 1) },
                            enabled = index < tracks.lastIndex,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { onRemoveTrack(track.id) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveCircleOutline,
                                contentDescription = "Remove from playlist",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        CreateOrEditPlaylistDialog(
            initialName = playlist.name,
            initialDescription = playlist.description,
            isEditing = true,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, desc ->
                onRename(name, desc)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${playlist.name}\"?") },
            text = { Text("The songs themselves will remain in your local music library.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddTracksDialog) {
        val available = allTracks.filterNot { playlist.trackIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { showAddTracksDialog = false },
            title = { Text("Add songs to ${playlist.name}") },
            text = {
                if (available.isEmpty()) {
                    Text("All library tracks are already in this playlist.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(320.dp)
                    ) {
                        itemsIndexed(available, key = { _, t -> t.id }) { _, track ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onAddTrack(track.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTracksDialog = false }) { Text("Done") }
            }
        )
    }
}
