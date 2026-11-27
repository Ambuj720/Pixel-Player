package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.PlaybackProgress
import com.example.data.model.NavDestination
import com.example.data.model.Track
import com.example.ui.components.AlbumCard
import com.example.ui.components.ArtistCard
import com.example.ui.components.PlaylistCard
import com.example.ui.components.QuickPlayTile
import com.example.ui.components.SongRowItem
import com.example.ui.components.TrackArtwork
import com.example.ui.components.timeOfDayGreeting
import com.example.ui.viewmodel.PlayerUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeScreen(
    uiState: PlayerUiState,
    playbackProgress: StateFlow<PlaybackProgress>,
    onSearchChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onGenerateSamplesClick: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onShuffleAll: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack = uiState.playback.currentTrack
    val continueTrack = currentTrack ?: uiState.recentlyPlayed.firstOrNull() ?: uiState.tracks.firstOrNull()
    val greeting = remember { timeOfDayGreeting() }
    val quickPlayTracks = remember(uiState.recentlyPlayed, uiState.favorites, uiState.tracks) {
        val result = ArrayList<Track>(4)
        val seen = HashSet<String>(8)
        fun collectUpToFour(source: List<Track>) {
            for (t in source) {
                if (result.size >= 4) return
                if (seen.add(t.id)) result.add(t)
            }
        }
        collectUpToFour(uiState.recentlyPlayed)
        collectUpToFour(uiState.favorites)
        collectUpToFour(uiState.tracks)
        result
    }
    val homePreviewTracks = remember(uiState.tracks) { uiState.tracks.take(8) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLandscape = maxWidth >= 600.dp
        val isShortLandscape = maxHeight < 480.dp && maxWidth > maxHeight

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                bottom = when {
                    uiState.tracks.isEmpty() -> 24.dp
                    isShortLandscape -> 80.dp
                    else -> 96.dp
                }
            ),
            verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 12.dp else 18.dp)
        ) {
            // Expressive Greeting Header
            item(key = "header", contentType = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = if (isShortLandscape) 8.dp else 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = if (isShortLandscape) MaterialTheme.typography.headlineMedium
                            else MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    if (uiState.tracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.tracks.size} local tracks • ${uiState.playlists.size} playlists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                FilledTonalButton(
                    onClick = onImportClick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("home_import_button")
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Expressive Search Bar
        item(key = "search_bar") {
            OutlinedTextField(
                value = uiState.filters.searchQuery,
                onValueChange = onSearchChange,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums…",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.filters.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("home_search_input")
            )
        }

        // Live search results view if typing
        if (uiState.filters.searchQuery.isNotBlank()) {
            val matches = uiState.filteredTracks
            item(key = "search_results_header") {
                SectionHeader(
                    title = "Search Results (${matches.size})",
                    actionLabel = "Open Library",
                    onAction = { onNavigate(NavDestination.LIBRARY) }
                )
            }
            if (matches.isEmpty()) {
                item(key = "search_empty") {
                    Text(
                        text = "No tracks match \"${uiState.filters.searchQuery}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                items(
                    items = matches.take(10),
                    key = { "search_${it.id}" },
                    contentType = { "song_row" }
                ) { track ->
                    SongRowItem(
                        track = track,
                        isCurrentTrack = currentTrack?.id == track.id,
                        isPlaying = uiState.playback.isPlaying,
                        onPlay = { onPlayTrack(track, matches) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onPlayNext = { onPlayNext(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        } else if (uiState.tracks.isEmpty()) {
            // Welcoming First-Launch Empty State Card
            item(key = "empty_hero", contentType = "empty_hero") {
                EmptyHomeHeroCard(
                    isImporting = uiState.isImporting,
                    isLandscape = isWideLandscape,
                    onImportClick = onImportClick,
                    onGenerateSamplesClick = onGenerateSamplesClick,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        } else {
            // 1. Continue Listening Hero Card
            if (continueTrack != null) {
                item(key = "continue_listening", contentType = "continue_listening") {
                    ContinueListeningHeroCard(
                        track = continueTrack,
                        isCurrentlyLoaded = currentTrack?.id == continueTrack.id,
                        isPlaying = uiState.playback.isPlaying && currentTrack?.id == continueTrack.id,
                        progressFlow = playbackProgress,
                        durationMs = if (currentTrack?.id == continueTrack.id) uiState.playback.durationMs else continueTrack.durationMs,
                        onPlayOrResume = {
                            if (currentTrack?.id == continueTrack.id) onTogglePlayPause()
                            else onPlayTrack(continueTrack, uiState.tracks)
                        },
                        onCardClick = {
                            if (currentTrack?.id != continueTrack.id) {
                                onPlayTrack(continueTrack, uiState.tracks)
                            }
                            onOpenNowPlaying()
                        },
                        onShuffleAll = onShuffleAll,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            // 2. Quick Play Responsive Grid (4-column row in landscape/tablet, 2x2 in portrait)
            if (quickPlayTracks.isNotEmpty()) {
                item(key = "quick_play", contentType = "quick_play") {
                    val columns = if (isWideLandscape && quickPlayTracks.size >= 3) 4 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(title = "Quick Play")
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            quickPlayTracks.chunked(columns).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { track ->
                                        QuickPlayTile(
                                            track = track,
                                            isCurrent = currentTrack?.id == track.id,
                                            isPlaying = uiState.playback.isPlaying,
                                            onClick = { onPlayTrack(track, uiState.tracks) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Recently Played Carousel
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item(key = "recently_played") {
                    TrackHorizontalShelf(
                        title = "Recently Played",
                        tracks = uiState.recentlyPlayed,
                        currentTrackId = currentTrack?.id,
                        onPlayTrack = { track -> onPlayTrack(track, uiState.recentlyPlayed) }
                    )
                }
            }

            // 4. Your Playlists Carousel
            item(key = "your_playlists") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Your Playlists",
                        actionLabel = "New Playlist",
                        onAction = onCreatePlaylistClick
                    )
                    if (uiState.playlists.isEmpty()) {
                        Surface(
                            onClick = onCreatePlaylistClick,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Create your first playlist", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Curate moods, sessions, and favorites offline",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                items = uiState.playlists,
                                key = { it.id },
                                contentType = { "playlist_card" }
                            ) { pl ->
                                val leadTrack = remember(pl.trackIds, uiState.tracksById) {
                                    pl.trackIds.firstNotNullOfOrNull { uiState.tracksById[it] }
                                }
                                PlaylistCard(
                                    playlist = pl,
                                    leadTrack = leadTrack,
                                    onClick = { onOpenPlaylist(pl.id) },
                                    onQuickPlay = {
                                        val plTracks = pl.trackIds.mapNotNull { uiState.tracksById[it] }
                                        if (plTracks.isNotEmpty()) onPlayTrack(plTracks.first(), plTracks)
                                    },
                                    modifier = Modifier.width(172.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Recently Added Carousel
            if (uiState.recentlyAdded.isNotEmpty()) {
                item(key = "recently_added") {
                    TrackHorizontalShelf(
                        title = "Recently Added",
                        tracks = uiState.recentlyAdded,
                        currentTrackId = currentTrack?.id,
                        onPlayTrack = { track -> onPlayTrack(track, uiState.recentlyAdded) }
                    )
                }
            }

            // 6. Most Played Carousel
            if (uiState.mostPlayed.isNotEmpty()) {
                item(key = "most_played") {
                    TrackHorizontalShelf(
                        title = "Most Played",
                        tracks = uiState.mostPlayed,
                        currentTrackId = currentTrack?.id,
                        onPlayTrack = { track -> onPlayTrack(track, uiState.mostPlayed) }
                    )
                }
            }

            // 7. Albums Carousel
            if (uiState.albums.isNotEmpty()) {
                item(key = "albums") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = "Albums")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.albums, key = { it.name }) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onOpenAlbum(album.name) },
                                    onQuickPlay = {
                                        album.tracks.firstOrNull()?.let { onPlayTrack(it, album.tracks) }
                                    },
                                    modifier = Modifier.width(170.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 8. Artists Carousel
            if (uiState.artists.isNotEmpty()) {
                item(key = "artists") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = "Artists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.artists, key = { it.name }) { artist ->
                                ArtistCard(
                                    artist = artist,
                                    onClick = { onOpenArtist(artist.name) },
                                    modifier = Modifier.width(148.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 9. All Songs Preview
            item(key = "all_songs_header", contentType = "section_header") {
                SectionHeader(
                    title = "All Songs",
                    actionLabel = "View Library",
                    onAction = { onNavigate(NavDestination.LIBRARY) }
                )
            }
            items(
                items = homePreviewTracks,
                key = { "home_song_${it.id}" },
                contentType = { "song_row" }
            ) { track ->
                SongRowItem(
                    track = track,
                    isCurrentTrack = currentTrack?.id == track.id,
                    isPlaying = uiState.playback.isPlaying,
                    onPlay = { onPlayTrack(track, uiState.tracks) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
    }
}

@Composable
private fun EmptyHomeHeroCard(
    isImporting: Boolean,
    isLandscape: Boolean,
    onImportClick: () -> Unit,
    onGenerateSamplesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_listening_1795773856153),
                        contentDescription = "Expressive acoustic sculpture banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceContainerLow)
                                )
                            )
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Your local music studio awaits",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Import MP3, FLAC, WAV, M4A, AAC, or OGG audio directly from your device. Everything stays 100% private and offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onImportClick,
                            enabled = !isImporting,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("empty_import_button")
                        ) {
                            Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import Audio",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = onGenerateSamplesClick,
                            enabled = !isImporting,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("generate_samples_button")
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Demo WAVs",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_listening_1795773856153),
                        contentDescription = "Expressive acoustic sculpture banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceContainerLow)
                                )
                            )
                    )
                }
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Your local music studio awaits",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Import MP3, FLAC, WAV, M4A, AAC, or OGG audio directly from your device. Everything stays 100% private and offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onImportClick,
                        enabled = !isImporting,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("empty_import_button")
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import Audio Files",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalButton(
                        onClick = onGenerateSamplesClick,
                        enabled = !isImporting,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_samples_button")
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Demo Tracks (WAV)",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueListeningHeroCard(
    track: Track,
    isCurrentlyLoaded: Boolean,
    isPlaying: Boolean,
    progressFlow: StateFlow<PlaybackProgress>,
    durationMs: Long,
    onPlayOrResume: () -> Unit,
    onCardClick: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onCardClick)
            .testTag("continue_listening_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPlaying) "NOW PLAYING" else "CONTINUE LISTENING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Surface(
                    onClick = onShuffleAll,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("home_shuffle_all_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Shuffle All",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackArtwork(
                    artworkPath = track.artworkPath,
                    title = track.title,
                    seedColor = track.artworkSeedColor,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(78.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentlyLoaded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ContinueListeningProgressBar(
                            progressFlow = progressFlow,
                            fallbackDurationMs = durationMs
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    onClick = onPlayOrResume,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("continue_play_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueListeningProgressBar(
    progressFlow: StateFlow<PlaybackProgress>,
    fallbackDurationMs: Long
) {
    val progressState by progressFlow.collectAsStateWithLifecycle()
    val effectiveDuration = if (progressState.durationMs > 0L) progressState.durationMs else fallbackDurationMs
    val fraction = if (effectiveDuration > 0L) {
        (progressState.positionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LinearProgressIndicator(
        progress = { fraction },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
    )
}

@Composable
private fun TrackHorizontalShelf(
    title: String,
    tracks: List<Track>,
    currentTrackId: String?,
    onPlayTrack: (Track) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(tracks, key = { "${title}_${it.id}" }) { track ->
                val isCurrent = track.id == currentTrackId
                Surface(
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .width(154.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onPlayTrack(track) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        TrackArtwork(
                            artworkPath = track.artworkPath,
                            title = track.title,
                            seedColor = track.artworkSeedColor,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(134.dp)
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
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}
