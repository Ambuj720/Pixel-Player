package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.components.formatDurationMs
import com.example.ui.components.timeOfDayGreeting
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.PlayerUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeScreen(
    uiState: PlayerUiState,
    playbackProgress: StateFlow<PlaybackProgress>,
    onSearchChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onScanDeviceClick: () -> Unit,
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

    // Dynamic ambient aura tinted by the active album palette, cached in draw phase for 0 per-frame allocation
    val auraPrimary = MaterialTheme.colorScheme.primaryContainer
    val auraTertiary = MaterialTheme.colorScheme.tertiaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val primaryGlow = Brush.radialGradient(
                    colors = listOf(auraPrimary.copy(alpha = 0.52f), Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.04f),
                    radius = (size.width * 0.85f).coerceAtLeast(1f)
                )
                val tertiaryGlow = Brush.radialGradient(
                    colors = listOf(auraTertiary.copy(alpha = 0.36f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.14f),
                    radius = (size.width * 0.72f).coerceAtLeast(1f)
                )
                val bottomFade = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.38f to surfaceColor.copy(alpha = 0.65f),
                        0.62f to surfaceColor
                    )
                )
                onDrawBehind {
                    drawRect(surfaceColor)
                    drawRect(primaryGlow)
                    drawRect(tertiaryGlow)
                    drawRect(bottomFade)
                }
            }
    ) {
        val isWideLandscape = maxWidth >= 600.dp
        val isShortLandscape = maxHeight < 480.dp && maxWidth > maxHeight

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                bottom = when {
                    uiState.tracks.isEmpty() -> 28.dp
                    isShortLandscape -> 84.dp
                    else -> 104.dp
                }
            ),
            verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 14.dp else 20.dp)
        ) {
            // Editorial Greeting Header with Live Status Eyebrow & Stats Pill
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
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.tracks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                                shape = CircleShape,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${uiState.tracks.size} tracks • ${uiState.playlists.size} playlists • ${formatDurationMs(uiState.totalDurationMs)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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

            // Frosted Pill Search Bar
            item(key = "search_bar", contentType = "search_bar") {
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
                            tint = MaterialTheme.colorScheme.primary,
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
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("home_search_input")
                )
            }

            // Live device scan progress indicator
            if (uiState.isScanningDevice) {
                item(key = "scanning_banner", contentType = "scanning_banner") {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .testTag("home_scanning_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Scanning device for supported audio files…",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Live search results view if typing
            if (uiState.filters.searchQuery.isNotBlank()) {
                val matches = uiState.filteredTracks
                item(key = "search_results_header", contentType = "section_header") {
                    SectionHeader(
                        title = "Search Results",
                        countBadge = matches.size.toString(),
                        actionLabel = "Open Library",
                        onAction = { onNavigate(NavDestination.LIBRARY) }
                    )
                }
                if (matches.isEmpty()) {
                    item(key = "search_empty", contentType = "empty_notice") {
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
                // Flagship First-Launch Empty State Card
                item(key = "empty_hero", contentType = "empty_hero") {
                    EmptyHomeHeroCard(
                        isImporting = uiState.isImporting || uiState.isScanningDevice,
                        isLandscape = isWideLandscape,
                        onScanDeviceClick = onScanDeviceClick,
                        onImportClick = onImportClick,
                        onGenerateSamplesClick = onGenerateSamplesClick,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                // 1. Layered Spotlight Continue Listening Hero Card
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
                            SectionHeader(
                                title = "Quick Play",
                                subtitle = "Jump right back in"
                            )
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
                    item(key = "recently_played", contentType = "track_shelf") {
                        TrackHorizontalShelf(
                            title = "Recently Played",
                            subtitle = "Your latest rotation",
                            tracks = uiState.recentlyPlayed,
                            currentTrackId = currentTrack?.id,
                            isPlaying = uiState.playback.isPlaying,
                            onPlayTrack = { track -> onPlayTrack(track, uiState.recentlyPlayed) }
                        )
                    }
                }

                // 4. Your Playlists Carousel
                item(key = "your_playlists", contentType = "playlist_shelf") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(
                            title = "Your Playlists",
                            countBadge = if (uiState.playlists.isNotEmpty()) uiState.playlists.size.toString() else null,
                            actionLabel = "New Playlist",
                            onAction = onCreatePlaylistClick
                        )
                        if (uiState.playlists.isEmpty()) {
                            Surface(
                                onClick = onCreatePlaylistClick,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
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
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Add, contentDescription = null)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Create your first playlist",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Curate moods, sessions, and favorites offline",
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
                    item(key = "recently_added", contentType = "track_shelf") {
                        TrackHorizontalShelf(
                            title = "Recently Added",
                            subtitle = "Fresh imports",
                            tracks = uiState.recentlyAdded,
                            currentTrackId = currentTrack?.id,
                            isPlaying = uiState.playback.isPlaying,
                            onPlayTrack = { track -> onPlayTrack(track, uiState.recentlyAdded) }
                        )
                    }
                }

                // 6. Most Played Carousel
                if (uiState.mostPlayed.isNotEmpty()) {
                    item(key = "most_played", contentType = "track_shelf") {
                        TrackHorizontalShelf(
                            title = "Most Played",
                            subtitle = "On repeat",
                            tracks = uiState.mostPlayed,
                            currentTrackId = currentTrack?.id,
                            isPlaying = uiState.playback.isPlaying,
                            onPlayTrack = { track -> onPlayTrack(track, uiState.mostPlayed) }
                        )
                    }
                }

                // 7. Albums Carousel
                if (uiState.albums.isNotEmpty()) {
                    item(key = "albums", contentType = "album_shelf") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = "Albums",
                                countBadge = uiState.albums.size.toString()
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = uiState.albums,
                                    key = { it.name },
                                    contentType = { "album_card" }
                                ) { album ->
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
                    item(key = "artists", contentType = "artist_shelf") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = "Artists",
                                countBadge = uiState.artists.size.toString()
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = uiState.artists,
                                    key = { it.name },
                                    contentType = { "artist_card" }
                                ) { artist ->
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
                        countBadge = uiState.tracks.size.toString(),
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
    onScanDeviceClick: () -> Unit,
    onImportClick: () -> Unit,
    onGenerateSamplesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp),
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
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "HI-RES OFFLINE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
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
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Scan your device or import MP3, FLAC, WAV, M4A, AAC, or OGG audio directly. Everything stays 100% private and offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onScanDeviceClick,
                            enabled = !isImporting,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("empty_scan_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scan Device",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = onImportClick,
                            enabled = !isImporting,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("empty_import_button")
                        ) {
                            Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = onGenerateSamplesClick,
                            enabled = !isImporting,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("generate_samples_button")
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = "Create demo WAVs", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        } else {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp)
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
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                )
                            )
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "HI-RES OFFLINE STUDIO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Your local music studio awaits",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Scan your device or import MP3, FLAC, WAV, M4A, AAC, or OGG audio directly. Everything stays 100% private and offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onScanDeviceClick,
                        enabled = !isImporting,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("empty_scan_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Device Audio",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = onImportClick,
                            enabled = !isImporting,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("empty_import_button")
                        ) {
                            Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import Files",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = onGenerateSamplesClick,
                            enabled = !isImporting,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val accentGlow = MaterialTheme.colorScheme.primary

    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        ),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .drawWithCache {
                val baseGradient = Brush.linearGradient(
                    colors = listOf(
                        primaryContainer,
                        tertiaryContainer.copy(alpha = 0.92f),
                        secondaryContainer
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                val highlightOrb = Brush.radialGradient(
                    colors = listOf(accentGlow.copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                    radius = (size.width * 0.65f).coerceAtLeast(1f)
                )
                onDrawBehind {
                    drawRect(baseGradient)
                    drawRect(highlightOrb)
                }
            }
            .clickable(onClick = onCardClick)
            .testTag("continue_listening_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPlaying) "NOW PLAYING" else "CONTINUE LISTENING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Surface(
                    onClick = onShuffleAll,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
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
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier.size(86.dp)
                ) {
                    TrackArtwork(
                        artworkPath = track.artworkPath,
                        title = track.title,
                        seedColor = track.artworkSeedColor,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${track.genre} • ${formatDurationMs(durationMs)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    onClick = onPlayOrResume,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("continue_play_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            if (isCurrentlyLoaded) {
                Spacer(modifier = Modifier.height(14.dp))
                ContinueListeningProgressBar(
                    progressFlow = progressFlow,
                    fallbackDurationMs = durationMs
                )
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

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { fraction },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDurationMs(progressState.positionMs),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
            )
            Text(
                text = formatDurationMs(effectiveDuration),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
            )
        }
    }
}

@Composable
private fun TrackHorizontalShelf(
    title: String,
    subtitle: String? = null,
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Track) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            countBadge = tracks.size.toString()
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = tracks,
                key = { "${title}_${it.id}" },
                contentType = { "home_track_card" }
            ) { track ->
                val isCurrent = track.id == currentTrackId
                Surface(
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = if (isCurrent) 3.dp else 1.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .width(158.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onPlayTrack(track) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(138.dp)
                        ) {
                            TrackArtwork(
                                artworkPath = track.artworkPath,
                                title = track.title,
                                seedColor = track.artworkSeedColor,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.45f to Color.Transparent,
                                                1.0f to Color.Black.copy(alpha = 0.42f)
                                            )
                                        )
                                    )
                            )
                            Surface(
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Rounded.GraphicEq
                                        else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
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
    subtitle: String? = null,
    countBadge: String? = null,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (countBadge != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = countBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
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
