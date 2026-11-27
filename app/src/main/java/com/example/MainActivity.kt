package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.NavDestination
import com.example.data.model.Track
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppNavRail
import com.example.ui.components.AppSidebar
import com.example.ui.components.CreateOrEditPlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SleepTimerDialog
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicLibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CadenceTheme
import com.example.ui.viewmodel.PlayerUiState
import com.example.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CadenceApp()
        }
    }
}

@Composable
fun CadenceApp(viewModel: PlayerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Zero-permission Storage Access Framework multi-audio importer
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioUris(uris)
        }
    }

    val launchAudioImport = {
        audioPickerLauncher.launch(
            arrayOf(
                "audio/mpeg",
                "audio/wav",
                "audio/x-wav",
                "audio/mp4",
                "audio/aac",
                "audio/flac",
                "audio/ogg",
                "audio/eac3",
                "audio/eac3-joc",
                "audio/ac4",
                "audio/ac3",
                "video/mp4",
                "audio/*"
            )
        )
    }

    var trackForPlaylistDialog by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var pendingTrackForNewPlaylist by remember { mutableStateOf<String?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    BackHandler(
        enabled = uiState.activeDestination != NavDestination.HOME &&
            !uiState.isNowPlayingExpanded &&
            uiState.selectedPlaylistId == null
    ) {
        viewModel.selectDestination(NavDestination.HOME)
    }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissBanner()
    }

    CadenceTheme(
        themeMode = uiState.preferences.themeMode,
        colorPreset = uiState.preferences.colorPreset,
        dynamicAlbumColorsEnabled = uiState.preferences.dynamicAlbumColors,
        artworkSeedColor = uiState.playback.currentTrack?.artworkSeedColor,
        reducedMotion = uiState.preferences.reducedMotion
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (keyEvent.key) {
                        Key.Spacebar -> {
                            viewModel.togglePlayPause()
                            true
                        }
                        Key.DirectionRight -> {
                            viewModel.seekBy(5_000L)
                            true
                        }
                        Key.DirectionLeft -> {
                            viewModel.seekBy(-5_000L)
                            true
                        }
                        Key.DirectionUp -> {
                            viewModel.setVolume(uiState.playback.volume + 0.08f)
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.setVolume(uiState.playback.volume - 0.08f)
                            true
                        }
                        Key.N -> {
                            viewModel.nextTrack()
                            true
                        }
                        Key.P -> {
                            viewModel.previousTrack()
                            true
                        }
                        Key.M -> {
                            viewModel.toggleMute()
                            true
                        }
                        Key.S -> {
                            viewModel.toggleShuffle()
                            true
                        }
                        Key.R -> {
                            viewModel.cycleRepeatMode()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpandedDesktop = maxWidth >= 900.dp
                val isMediumTablet = maxWidth in 600.dp..899.dp

                val activeContent: @Composable (Modifier) -> Unit = { contentMod ->
                    ActiveDestinationContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onImportClick = launchAudioImport,
                        onOpenAddToPlaylist = { track -> trackForPlaylistDialog = track },
                        onOpenCreatePlaylist = {
                            pendingTrackForNewPlaylist = null
                            showCreatePlaylistDialog = true
                        },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        modifier = contentMod
                    )
                }

                if (isExpandedDesktop) {
                    // 3-Pane Desktop / Large Tablet Layout: Sidebar + Central Content + Right Now Playing Panel
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AppSidebar(
                                activeDestination = uiState.activeDestination,
                                playlists = uiState.playlists,
                                selectedPlaylistId = uiState.selectedPlaylistId,
                                onSelectDestination = viewModel::selectDestination,
                                onOpenPlaylist = viewModel::openPlaylist,
                                onImportClick = launchAudioImport
                            )
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                activeContent(Modifier.fillMaxSize())
                            }
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            NowPlayingScreen(
                                playback = uiState.playback,
                                spatialAudio = uiState.spatialAudio,
                                sleepTimer = uiState.sleepTimer,
                                isSidePanel = true,
                                onCollapse = {},
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onPrevious = viewModel::previousTrack,
                                onNext = viewModel::nextTrack,
                                onSeekTo = viewModel::seekTo,
                                onToggleShuffle = viewModel::toggleShuffle,
                                onCycleRepeat = viewModel::cycleRepeatMode,
                                onVolumeChange = viewModel::setVolume,
                                onToggleMute = viewModel::toggleMute,
                                onSpeedChange = viewModel::setPlaybackSpeed,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onOpenSleepTimer = { showSleepTimerDialog = true },
                                onOpenAudioStudio = { viewModel.selectDestination(NavDestination.EQUALIZER) },
                                onAddToPlaylist = { trackForPlaylistDialog = it },
                                onJumpToQueueIndex = viewModel::jumpToQueueIndex,
                                onMoveQueueItem = viewModel::moveQueueItem,
                                onRemoveQueueItem = viewModel::removeFromQueue,
                                onSaveLyrics = viewModel::updateTrackLyrics,
                                modifier = Modifier
                                    .width(360.dp)
                                    .fillMaxHeight()
                            )
                        }
                    }
                } else if (isMediumTablet) {
                    // Tablet Layout: Left NavigationRail + Main Content + Floating MiniPlayer
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AppNavRail(
                                activeDestination = uiState.activeDestination,
                                onSelect = viewModel::selectDestination,
                                onImportClick = launchAudioImport
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                activeContent(Modifier.fillMaxSize())
                                val currentTrack = uiState.playback.currentTrack
                                if (currentTrack != null) {
                                    MiniPlayer(
                                        track = currentTrack,
                                        isPlaying = uiState.playback.isPlaying,
                                        positionMs = uiState.playback.positionMs,
                                        durationMs = uiState.playback.durationMs,
                                        onExpand = viewModel::openNowPlaying,
                                        onTogglePlayPause = viewModel::togglePlayPause,
                                        onNext = viewModel::nextTrack,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .navigationBarsPadding()
                                            .padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Smartphone Compact Layout: Main Content + Docked MiniPlayer + Expressive Bottom NavigationBar
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val currentTrack = uiState.playback.currentTrack
                                if (currentTrack != null) {
                                    MiniPlayer(
                                        track = currentTrack,
                                        isPlaying = uiState.playback.isPlaying,
                                        positionMs = uiState.playback.positionMs,
                                        durationMs = uiState.playback.durationMs,
                                        onExpand = viewModel::openNowPlaying,
                                        onTogglePlayPause = viewModel::togglePlayPause,
                                        onNext = viewModel::nextTrack
                                    )
                                }
                                AppBottomNavigation(
                                    activeDestination = uiState.activeDestination,
                                    onSelect = viewModel::selectDestination
                                )
                            }
                        }
                    ) { innerPadding ->
                        activeContent(
                            Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        )
                    }
                }

                // Expandable Full-Screen Now Playing Sheet (Mobile & Tablet)
                if (!isExpandedDesktop) {
                    AnimatedVisibility(
                        visible = uiState.isNowPlayingExpanded,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        NowPlayingScreen(
                            playback = uiState.playback,
                            spatialAudio = uiState.spatialAudio,
                            sleepTimer = uiState.sleepTimer,
                            isSidePanel = false,
                            onCollapse = viewModel::collapseNowPlaying,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onPrevious = viewModel::previousTrack,
                            onNext = viewModel::nextTrack,
                            onSeekTo = viewModel::seekTo,
                            onToggleShuffle = viewModel::toggleShuffle,
                            onCycleRepeat = viewModel::cycleRepeatMode,
                            onVolumeChange = viewModel::setVolume,
                            onToggleMute = viewModel::toggleMute,
                            onSpeedChange = viewModel::setPlaybackSpeed,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onOpenSleepTimer = { showSleepTimerDialog = true },
                            onOpenAudioStudio = {
                                viewModel.collapseNowPlaying()
                                viewModel.selectDestination(NavDestination.EQUALIZER)
                            },
                            onAddToPlaylist = { trackForPlaylistDialog = it },
                            onJumpToQueueIndex = viewModel::jumpToQueueIndex,
                            onMoveQueueItem = viewModel::moveQueueItem,
                            onRemoveQueueItem = viewModel::removeFromQueue,
                            onSaveLyrics = viewModel::updateTrackLyrics
                        )
                    }
                }
            }
        }

        // Modals & Dialogs
        trackForPlaylistDialog?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = uiState.playlists,
                onDismiss = { trackForPlaylistDialog = null },
                onSelectPlaylist = { plId -> viewModel.addTrackToPlaylist(plId, track.id) },
                onCreateNewPlaylist = {
                    pendingTrackForNewPlaylist = track.id
                    trackForPlaylistDialog = null
                    showCreatePlaylistDialog = true
                }
            )
        }

        if (showCreatePlaylistDialog) {
            CreateOrEditPlaylistDialog(
                onDismiss = {
                    showCreatePlaylistDialog = false
                    pendingTrackForNewPlaylist = null
                },
                onConfirm = { name, desc ->
                    viewModel.createPlaylist(name, desc, pendingTrackForNewPlaylist)
                    showCreatePlaylistDialog = false
                    pendingTrackForNewPlaylist = null
                }
            )
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                sleepTimer = uiState.sleepTimer,
                onSelectMinutes = viewModel::startSleepTimer,
                onCancelTimer = viewModel::cancelSleepTimer,
                onDismiss = { showSleepTimerDialog = false }
            )
        }
    }
}

@Composable
private fun ActiveDestinationContent(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    onImportClick: () -> Unit,
    onOpenAddToPlaylist: (Track) -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState.activeDestination) {
        NavDestination.HOME -> HomeScreen(
            uiState = uiState,
            onSearchChange = viewModel::updateSearchQuery,
            onImportClick = onImportClick,
            onGenerateSamplesClick = viewModel::generateLocalDemoTracks,
            onPlayTrack = viewModel::playTrack,
            onTogglePlayPause = viewModel::togglePlayPause,
            onShuffleAll = { viewModel.playAll(uiState.tracks, shuffle = true) },
            onOpenNowPlaying = viewModel::openNowPlaying,
            onOpenPlaylist = viewModel::openPlaylist,
            onCreatePlaylistClick = onOpenCreatePlaylist,
            onOpenAlbum = viewModel::openLibraryFilteredByAlbum,
            onOpenArtist = viewModel::openLibraryFilteredByArtist,
            onNavigate = viewModel::selectDestination,
            onToggleFavorite = viewModel::toggleFavorite,
            onAddToQueue = viewModel::addToQueue,
            onPlayNext = viewModel::playNextInQueue,
            onAddToPlaylist = onOpenAddToPlaylist,
            modifier = modifier
        )

        NavDestination.LIBRARY -> MusicLibraryScreen(
            uiState = uiState,
            onImportClick = onImportClick,
            onGenerateSamplesClick = viewModel::generateLocalDemoTracks,
            onSearchChange = viewModel::updateSearchQuery,
            onTabSelected = viewModel::setLibraryTab,
            onSortSelected = viewModel::setSortOption,
            onToggleGridView = viewModel::toggleGridView,
            onGenreSelected = viewModel::setGenreFilter,
            onClearFilters = viewModel::clearActiveFilters,
            onOpenAlbum = viewModel::openLibraryFilteredByAlbum,
            onOpenArtist = viewModel::openLibraryFilteredByArtist,
            onPlayTrack = viewModel::playTrack,
            onPlayAll = viewModel::playAll,
            onToggleFavorite = viewModel::toggleFavorite,
            onAddToQueue = viewModel::addToQueue,
            onPlayNext = viewModel::playNextInQueue,
            onAddToPlaylist = onOpenAddToPlaylist,
            onDeleteTrack = viewModel::deleteTrack,
            modifier = modifier
        )

        NavDestination.PLAYLISTS -> PlaylistsScreen(
            uiState = uiState,
            onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
            onRenamePlaylist = viewModel::renamePlaylist,
            onDeletePlaylist = viewModel::deletePlaylist,
            onOpenPlaylist = viewModel::openPlaylist,
            onClosePlaylistDetail = viewModel::closePlaylistDetail,
            onPlayTracks = { list, index, shuffle ->
                if (shuffle) viewModel.playAll(list, shuffle = true)
                else list.getOrNull(index)?.let { viewModel.playTrack(it, list) }
            },
            onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
            onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist,
            onMoveTrackInPlaylist = viewModel::moveTrackInPlaylist,
            modifier = modifier
        )

        NavDestination.EQUALIZER -> EqualizerScreen(
            eqState = uiState.eqState,
            spatialAudio = uiState.spatialAudio,
            activeAudioFormat = uiState.playback.activeAudioFormat,
            playbackSpeed = uiState.playback.playbackSpeed,
            sleepTimer = uiState.sleepTimer,
            onToggleEq = viewModel::setEqEnabled,
            onApplyPreset = viewModel::applyEqPreset,
            onBandLevelChange = viewModel::setEqBandLevel,
            onToggleDolbyAtmos = viewModel::setDolbyAtmosEnabled,
            onSelectAtmosProfile = viewModel::setAtmosProfile,
            onVirtualizerStrengthChange = viewModel::setSpatialVirtualizerStrength,
            onBassBoostStrengthChange = viewModel::setBassBoostStrength,
            onSpeedChange = viewModel::setPlaybackSpeed,
            onOpenSleepTimer = onOpenSleepTimer,
            modifier = modifier
        )

        NavDestination.SETTINGS -> SettingsScreen(
            uiState = uiState,
            onThemeModeChange = viewModel::setThemeMode,
            onColorPresetChange = viewModel::setColorPreset,
            onDynamicAlbumColorsChange = viewModel::setDynamicAlbumColors,
            onDolbyAtmosChange = viewModel::setDolbyAtmosEnabled,
            onGaplessChange = viewModel::setGaplessEnabled,
            onReducedMotionChange = viewModel::setReducedMotion,
            onImportClick = onImportClick,
            onGenerateSamplesClick = viewModel::generateLocalDemoTracks,
            modifier = modifier
        )
    }
}
