package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlaybackProgress
import com.example.audio.PlaybackState
import com.example.data.model.SleepTimerState
import com.example.data.model.SpatialAudioState
import com.example.data.model.Track
import com.example.ui.components.ExpressiveProgressBar
import com.example.ui.components.LyricsPanel
import com.example.ui.components.PlaybackControls
import com.example.ui.components.QueuePanel
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.flow.StateFlow

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

private enum class NowPlayingTab(val label: String) {
    PLAYER("Player"),
    QUEUE("Queue"),
    LYRICS("Lyrics")
}

@Composable
fun NowPlayingScreen(
    playback: PlaybackState,
    playbackProgress: StateFlow<PlaybackProgress>,
    spatialAudio: SpatialAudioState,
    sleepTimer: SleepTimerState,
    isSidePanel: Boolean,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenAudioStudio: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onSaveLyrics: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playback.currentTrack
    var activeTab by remember { mutableStateOf(NowPlayingTab.PLAYER) }

    BackHandler(enabled = activeTab != NowPlayingTab.PLAYER || !isSidePanel) {
        if (activeTab != NowPlayingTab.PLAYER) {
            activeTab = NowPlayingTab.PLAYER
        } else if (!isSidePanel) {
            onCollapse()
        }
    }

    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceLowColor = MaterialTheme.colorScheme.surfaceContainerLow
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backdropBrush = remember(primaryContainerColor, surfaceLowColor, surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                primaryContainerColor.copy(alpha = 0.62f),
                surfaceLowColor,
                surfaceColor
            )
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxSize()
            .background(backdropBrush)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropBrush)
                .let { mod ->
                    if (!isSidePanel) {
                        mod
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    } else {
                        mod.statusBarsPadding()
                    }
                }
        ) {
            val isShortViewport = maxHeight < 480.dp
            if (track == null) {
                EmptyNowPlayingView(
                    isSidePanel = isSidePanel,
                    onCollapse = onCollapse
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header bar
                    NowPlayingHeader(
                        isSidePanel = isSidePanel,
                        compact = isShortViewport,
                        activeTab = activeTab,
                        albumName = track.album,
                        queueCount = playback.queue.size,
                        sleepTimer = sleepTimer,
                        onBackToPlayer = { activeTab = NowPlayingTab.PLAYER },
                        onCollapse = onCollapse,
                        onOpenSleepTimer = onOpenSleepTimer,
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )

                    if (!isShortViewport) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    when (activeTab) {
                        NowPlayingTab.PLAYER -> {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                val isShortLandscape = maxHeight < 440.dp
                                val useSplit = !isSidePanel && (maxWidth >= 600.dp || (maxWidth > maxHeight && maxWidth >= 480.dp))
                                if (useSplit) {
                                    val splitArtSize = minOf(
                                        maxHeight - if (isShortLandscape) 14.dp else 28.dp,
                                        maxWidth * 0.38f,
                                        340.dp
                                    ).coerceAtLeast(132.dp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(
                                                horizontal = if (isShortLandscape) 20.dp else 28.dp,
                                                vertical = if (isShortLandscape) 4.dp else 12.dp
                                            ),
                                        horizontalArrangement = Arrangement.spacedBy(if (isShortLandscape) 22.dp else 32.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(0.88f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedTrackArtwork(
                                                track = track,
                                                isPlaying = playback.isPlaying,
                                                onSwipeNext = onNext,
                                                onSwipePrevious = onPrevious,
                                                modifier = Modifier.size(splitArtSize)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier
                                                .weight(1.12f)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            NowPlayingControlsBody(
                                                track = track,
                                                playback = playback,
                                                playbackProgress = playbackProgress,
                                                spatialAudio = spatialAudio,
                                                sleepTimer = sleepTimer,
                                                compact = isShortLandscape,
                                                onToggleFavorite = { onToggleFavorite(track) },
                                                onSeekTo = onSeekTo,
                                                onTogglePlayPause = onTogglePlayPause,
                                                onPrevious = onPrevious,
                                                onNext = onNext,
                                                onToggleShuffle = onToggleShuffle,
                                                onCycleRepeat = onCycleRepeat,
                                                onSpeedChange = onSpeedChange,
                                                onOpenQueue = { activeTab = NowPlayingTab.QUEUE },
                                                onOpenLyrics = { activeTab = NowPlayingTab.LYRICS },
                                                onOpenSleepTimer = onOpenSleepTimer,
                                                onOpenAudioStudio = onOpenAudioStudio
                                            )
                                        }
                                    }
                                } else {
                                    val horizontalPad = if (isSidePanel) 18.dp else 24.dp
                                    val maxArtByWidth = (maxWidth - horizontalPad * 2).coerceAtLeast(160.dp)
                                    val maxArtByHeight = (maxHeight * 0.45f).coerceIn(168.dp, if (isSidePanel) 260.dp else 316.dp)
                                    val artSize = minOf(maxArtByWidth, maxArtByHeight)
                                    val isCompactHeight = maxHeight < 540.dp

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(
                                                start = horizontalPad,
                                                end = horizontalPad,
                                                top = 8.dp,
                                                bottom = 18.dp
                                            ),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AnimatedTrackArtwork(
                                            track = track,
                                            isPlaying = playback.isPlaying,
                                            onSwipeNext = onNext,
                                            onSwipePrevious = onPrevious,
                                            modifier = Modifier.size(artSize)
                                        )
                                        Spacer(modifier = Modifier.height(if (isSidePanel) 16.dp else 20.dp))
                                        NowPlayingControlsBody(
                                            track = track,
                                            playback = playback,
                                            playbackProgress = playbackProgress,
                                            spatialAudio = spatialAudio,
                                            sleepTimer = sleepTimer,
                                            compact = isSidePanel || isCompactHeight,
                                            onToggleFavorite = { onToggleFavorite(track) },
                                            onSeekTo = onSeekTo,
                                            onTogglePlayPause = onTogglePlayPause,
                                            onPrevious = onPrevious,
                                            onNext = onNext,
                                            onToggleShuffle = onToggleShuffle,
                                            onCycleRepeat = onCycleRepeat,
                                            onSpeedChange = onSpeedChange,
                                            onOpenQueue = { activeTab = NowPlayingTab.QUEUE },
                                            onOpenLyrics = { activeTab = NowPlayingTab.LYRICS },
                                            onOpenSleepTimer = onOpenSleepTimer,
                                            onOpenAudioStudio = onOpenAudioStudio
                                        )
                                    }
                                }
                            }
                        }

                        NowPlayingTab.QUEUE -> {
                            QueuePanel(
                                queue = playback.queue,
                                currentIndex = playback.currentIndex,
                                isPlaying = playback.isPlaying,
                                onJumpToIndex = onJumpToQueueIndex,
                                onMoveItem = onMoveQueueItem,
                                onRemoveItem = onRemoveQueueItem,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }

                        NowPlayingTab.LYRICS -> {
                            LyricsPanel(
                                track = track,
                                progressFlow = playbackProgress,
                                onSeekTo = onSeekTo,
                                onSaveLyrics = onSaveLyrics,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingHeader(
    isSidePanel: Boolean,
    compact: Boolean,
    activeTab: NowPlayingTab,
    albumName: String,
    queueCount: Int,
    sleepTimer: SleepTimerState,
    onBackToPlayer: () -> Unit,
    onCollapse: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val isSubView = activeTab != NowPlayingTab.PLAYER
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { mod ->
                if (!isSidePanel && !isSubView) {
                    mod.pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 22f) onCollapse()
                        }
                    }
                } else mod
            }
            .padding(horizontal = 12.dp, vertical = if (compact) 2.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSubView) {
            IconButton(
                onClick = onBackToPlayer,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("now_playing_back_to_player_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to Player",
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (!isSidePanel) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("collapse_now_playing_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse Now Playing",
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isSidePanel) Alignment.Start else Alignment.CenterHorizontally
        ) {
            Text(
                text = when (activeTab) {
                    NowPlayingTab.PLAYER -> "NOW PLAYING"
                    NowPlayingTab.QUEUE -> "UP NEXT ($queueCount)"
                    NowPlayingTab.LYRICS -> "LYRICS"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = albumName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onOpenSleepTimer,
            modifier = Modifier
                .size(48.dp)
                .testTag("sleep_timer_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = "Sleep timer",
                tint = if (sleepTimer.isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier
                .size(48.dp)
                .testTag("now_playing_add_playlist_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.PlaylistAdd,
                contentDescription = "Add to playlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnimatedTrackArtwork(
    track: Track,
    isPlaying: Boolean,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPlaying || reducedMotion) 1f else 0.93f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "art_scale"
    )
    var dragAccum by remember { mutableFloatStateOf(0f) }

    AnimatedContent(
        targetState = track,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith fadeOut()
        },
        label = "artwork_transition",
        modifier = modifier
            .scale(scale)
            .pointerInput(track.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onDragEnd = {
                        if (dragAccum < -90f) onSwipeNext()
                        else if (dragAccum > 90f) onSwipePrevious()
                        dragAccum = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccum += dragAmount
                    }
                )
            }
    ) { targetTrack ->
        Surface(
            shape = RoundedCornerShape(32.dp),
            shadowElevation = if (isPlaying) 16.dp else 6.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            TrackArtwork(
                artworkPath = targetTrack.artworkPath,
                title = targetTrack.title,
                seedColor = targetTrack.artworkSeedColor,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun NowPlayingControlsBody(
    track: Track,
    playback: PlaybackState,
    playbackProgress: StateFlow<PlaybackProgress>,
    spatialAudio: SpatialAudioState,
    sleepTimer: SleepTimerState,
    compact: Boolean,
    onToggleFavorite: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenAudioStudio: () -> Unit
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val fmt = playback.activeAudioFormat

    // Title, artist, favorite
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = onToggleFavorite,
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .testTag("now_playing_favorite_button")
        ) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (track.isFavorite) "Remove favorite" else "Favorite track",
                tint = if (track.isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Live Dolby Atmos / Spatial Audio format pill
    Spacer(modifier = Modifier.height(if (compact) 4.dp else 10.dp))
    val badgeText = remember(fmt, spatialAudio.isEnabled, spatialAudio.profile) {
        val khz = if (fmt.sampleRateHz > 0) " • ${String.format("%.1f", fmt.sampleRateHz / 1000f)} kHz" else ""
        when {
            fmt.isDolbyAtmos -> "Dolby Atmos • ${fmt.channelLayoutLabel}$khz"
            fmt.isDolbyDigitalPlus -> "Dolby Digital Plus • ${fmt.channelLayoutLabel}$khz"
            spatialAudio.isEnabled -> "Dolby Atmos Spatial (${spatialAudio.profile.label}) • ${fmt.codecLabel}$khz"
            else -> "${fmt.codecLabel} • ${fmt.channelLayoutLabel}$khz"
        }
    }
    val highlightAtmos = fmt.isDolbyAtmos || spatialAudio.isEnabled
    Surface(
        onClick = onOpenAudioStudio,
        color = if (highlightAtmos) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (highlightAtmos) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("now_playing_atmos_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.SpatialAudio,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (playback.errorMessage != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = playback.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(10.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(if (compact) 4.dp else 10.dp))

    LivePlaybackProgressBar(
        progressFlow = playbackProgress,
        fallbackDurationMs = playback.durationMs,
        onSeek = onSeekTo
    )

    Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))

    PlaybackControls(
        isPlaying = playback.isPlaying,
        shuffleEnabled = playback.shuffleEnabled,
        repeatMode = playback.repeatMode,
        onTogglePlayPause = onTogglePlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onToggleShuffle = onToggleShuffle,
        onCycleRepeat = onCycleRepeat,
        compact = compact
    )

    Spacer(modifier = Modifier.height(if (compact) 6.dp else 16.dp))

    // Expressive quick action chips: Speed, Sleep Timer, Queue, Lyrics
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            FilterChip(
                selected = playback.playbackSpeed != 1.0f,
                onClick = { speedMenuExpanded = true },
                leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = {
                    Text(
                        text = "${playback.playbackSpeed}x",
                        fontFamily = MonoFontFamily
                    )
                },
                modifier = Modifier.testTag("speed_chip")
            )
            DropdownMenu(
                expanded = speedMenuExpanded,
                onDismissRequest = { speedMenuExpanded = false }
            ) {
                PLAYBACK_SPEEDS.forEach { spd ->
                    DropdownMenuItem(
                        text = { Text("${spd}x") },
                        onClick = {
                            speedMenuExpanded = false
                            onSpeedChange(spd)
                        }
                    )
                }
            }
        }

        if (sleepTimer.isActive) {
            FilterChip(
                selected = true,
                onClick = onOpenSleepTimer,
                leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(formatDurationMs(sleepTimer.remainingMs), fontFamily = MonoFontFamily) }
            )
        }

        FilterChip(
            selected = false,
            onClick = onOpenQueue,
            leadingIcon = { Icon(Icons.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Queue") }
        )

        FilterChip(
            selected = false,
            onClick = onOpenLyrics,
            leadingIcon = { Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Lyrics") }
        )
    }
}

@Composable
private fun LivePlaybackProgressBar(
    progressFlow: StateFlow<PlaybackProgress>,
    fallbackDurationMs: Long,
    onSeek: (Long) -> Unit
) {
    val progress by progressFlow.collectAsStateWithLifecycle()
    val effectiveDuration = if (progress.durationMs > 0L) progress.durationMs else fallbackDurationMs
    ExpressiveProgressBar(
        positionMs = progress.positionMs,
        durationMs = effectiveDuration,
        onSeek = onSeek
    )
}

@Composable
private fun EmptyNowPlayingView(
    isSidePanel: Boolean,
    onCollapse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isSidePanel) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.size(92.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Ready to listen",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pick any track from your local library to light up dynamic album colors and studio controls.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!isSidePanel) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
