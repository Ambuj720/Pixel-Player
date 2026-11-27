package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LibraryTab
import com.example.data.model.SortOption
import com.example.data.model.Track
import com.example.ui.components.AlbumCard
import com.example.ui.components.ArtistCard
import com.example.ui.components.SongRowItem
import com.example.ui.components.TrackArtwork
import com.example.ui.viewmodel.PlayerUiState

@Composable
fun MusicLibraryScreen(
    uiState: PlayerUiState,
    onImportClick: () -> Unit,
    onGenerateSamplesClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onToggleGridView: () -> Unit,
    onGenreSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    val filters = uiState.filters
    val currentTrackId = uiState.playback.currentTrack?.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Music Library",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.filteredTracks.size} tracks shown • ${uiState.tracks.size} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onToggleGridView,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("toggle_grid_button")
            ) {
                Icon(
                    imageVector = if (filters.isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                    contentDescription = if (filters.isGridView) "Switch to list view" else "Switch to grid view"
                )
            }

            Box {
                IconButton(
                    onClick = { sortMenuOpen = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sort_menu_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort tracks")
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false }
                ) {
                    SortOption.entries.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opt.label,
                                    fontWeight = if (filters.sortOption == opt) FontWeight.Bold else FontWeight.Normal,
                                    color = if (filters.sortOption == opt) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                sortMenuOpen = false
                                onSortSelected(opt)
                            }
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = onImportClick,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("library_import_button")
            ) {
                Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add")
            }
        }

        // Search bar
        OutlinedTextField(
            value = filters.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Filter by title, artist, album, or genre…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (filters.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .testTag("library_search_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Segmented tabs: Songs / Albums / Artists / Favorites
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            LibraryTab.entries.forEachIndexed { index, tab ->
                SegmentedButton(
                    selected = filters.activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    shape = SegmentedButtonDefaults.itemShape(index, LibraryTab.entries.size),
                    modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                ) {
                    Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Active artist/album filters + genre filter chips
        val hasContextualFilter = filters.selectedArtistFilter != null ||
            filters.selectedAlbumFilter != null ||
            filters.selectedGenre != null

        if (uiState.genres.isNotEmpty() || hasContextualFilter) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filters.selectedArtistFilter != null) {
                    item(key = "artist_filter") {
                        InputChip(
                            selected = true,
                            onClick = onClearFilters,
                            label = { Text("Artist: ${filters.selectedArtistFilter}") },
                            trailingIcon = { Icon(Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                if (filters.selectedAlbumFilter != null) {
                    item(key = "album_filter") {
                        InputChip(
                            selected = true,
                            onClick = onClearFilters,
                            label = { Text("Album: ${filters.selectedAlbumFilter}") },
                            trailingIcon = { Icon(Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                items(uiState.genres, key = { "genre_$it" }) { genre ->
                    FilterChip(
                        selected = filters.selectedGenre == genre,
                        onClick = { onGenreSelected(genre) },
                        label = { Text(genre) }
                    )
                }
            }
        }

        // Content area by active tab
        when (filters.activeTab) {
            LibraryTab.SONGS, LibraryTab.FAVORITES -> {
                val list = uiState.filteredTracks
                if (list.isEmpty()) {
                    LibraryEmptyState(
                        isFavoritesTab = filters.activeTab == LibraryTab.FAVORITES,
                        hasTracksInLibrary = uiState.tracks.isNotEmpty(),
                        onImportClick = onImportClick,
                        onGenerateSamplesClick = onGenerateSamplesClick,
                        onClearFilters = onClearFilters
                    )
                } else {
                    // Play All & Shuffle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(list, false) },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("library_play_all_button")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All")
                        }
                        FilledTonalButton(
                            onClick = { onPlayAll(list, true) },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("library_shuffle_button")
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }

                    if (filters.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 156.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(list, key = { it.id }) { track ->
                                val isCurrent = track.id == currentTrackId
                                Surface(
                                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable { onPlayTrack(track, list) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        TrackArtwork(
                                            artworkPath = track.artworkPath,
                                            title = track.title,
                                            seedColor = track.artworkSeedColor,
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleSmall,
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
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(list, key = { it.id }) { track ->
                                SongRowItem(
                                    track = track,
                                    isCurrentTrack = track.id == currentTrackId,
                                    isPlaying = uiState.playback.isPlaying,
                                    onPlay = { onPlayTrack(track, list) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    onAddToQueue = { onAddToQueue(track) },
                                    onPlayNext = { onPlayNext(track) },
                                    onAddToPlaylist = { onAddToPlaylist(track) },
                                    onDeleteTrack = { onDeleteTrack(track) }
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.ALBUMS -> {
                val albums = uiState.albums.filter {
                    val q = filters.searchQuery.trim().lowercase()
                    q.isEmpty() || it.name.lowercase().contains(q) || it.artist.lowercase().contains(q)
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(albums, key = { it.name }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onOpenAlbum(album.name) },
                            onQuickPlay = { onPlayAll(album.tracks, false) }
                        )
                    }
                }
            }

            LibraryTab.ARTISTS -> {
                val artists = uiState.artists.filter {
                    val q = filters.searchQuery.trim().lowercase()
                    q.isEmpty() || it.name.lowercase().contains(q)
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(artists, key = { it.name }) { artist ->
                        ArtistCard(
                            artist = artist,
                            onClick = { onOpenArtist(artist.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    isFavoritesTab: Boolean,
    hasTracksInLibrary: Boolean,
    onImportClick: () -> Unit,
    onGenerateSamplesClick: () -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                isFavoritesTab -> "No favorite tracks yet"
                hasTracksInLibrary -> "No matching tracks"
                else -> "Your local library is empty"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                isFavoritesTab -> "Tap the heart icon on any song to save it to Favorites."
                hasTracksInLibrary -> "Try clearing your active genre, artist, or search filters."
                else -> "Import MP3, FLAC, WAV, M4A, AAC, or OGG files from your device or generate local studio demo tracks."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (hasTracksInLibrary && !isFavoritesTab) {
            FilledTonalButton(onClick = onClearFilters, shape = RoundedCornerShape(18.dp)) {
                Text("Clear Filters")
            }
        } else if (!hasTracksInLibrary) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onImportClick, shape = RoundedCornerShape(18.dp)) {
                    Text("Import Audio")
                }
                FilledTonalButton(onClick = onGenerateSamplesClick, shape = RoundedCornerShape(18.dp)) {
                    Text("Create Demo WAVs")
                }
            }
        }
    }
}
