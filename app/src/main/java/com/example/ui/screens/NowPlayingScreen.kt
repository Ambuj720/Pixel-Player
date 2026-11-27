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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.audio.PlaybackState
import com.example.data.model.SleepTimerState
import com.example.data.model.SpatialAudioState
import com.example.data.model.Track
import com.example.ui.components.ExpressiveProgressBar
import com.example.ui.components.LyricsPanel
import com.example.ui.components.PlaybackControls
import com.example.ui.components.QueuePanel
import com.example.ui.components.TrackArtwork
import com.example.ui.components.VolumeControlRow
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MonoFontFamily

private enum class NowPlayingTab(val label: String) {
    PLAYER("Player"),
    QUEUE("Queue"),
    LYRICS("Lyrics")
}

@Composable
fun NowPlayingScreen(
    playback: PlaybackState,
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
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
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
    if (!isSidePanel) {
        BackHandler(onBack = onCollapse)
    }

    val track = playback.currentTrack
    var activeTab by remember { mutableStateOf(NowPlayingTab.PLAYER) }

    val backdropBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surface
        )
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxSize()
            .background(backdropBrush)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropBrush)
                .let { mod ->
                    if (!isSidePanel) mod.statusBarsPadding().navigationBarsPadding()
                    else mod.statusBarsPadding()
                }
        ) {
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
                        albumName = track.album,
                        sleepTimer = sleepTimer,
                        onCollapse = onCollapse,
                        onOpenSleepTimer = onOpenSleepTimer,
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )

                    // Mode switcher for Player / Queue / Lyrics
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        NowPlayingTab.entries.forEachIndexed { index, tab ->
                            SegmentedButton(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = NowPlayingTab.entries.size
                                ),
                                modifier = Modifier.testTag("now_playing_tab_${tab.name.lowercase()}")
                            ) {
                                Text(
                                    text = if (tab == NowPlayingTab.QUEUE) "Queue (${playback.queue.size})"
                                    else tab.label,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    when (activeTab) {
                        NowPlayingTab.PLAYER -> {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                val useSplit = !isSidePanel && maxWidth >= 680.dp
                                if (useSplit) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 28.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(0.95f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedTrackArtwork(
                                                track = track,
                                                isPlaying = playback.isPlaying,
                                                onSwipeNext = onNext,
                                                onSwipePrevious = onPrevious,
                                                modifier = Modifier
                                                    .widthIn(max = 360.dp)
                                                    .aspectRatio(1f)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier
                                                .weight(1.05f)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            NowPlayingControlsBody(
                                                track = track,
                                                playback = playback,
                                                spatialAudio = spatialAudio,
                                                sleepTimer = sleepTimer,
                                                compact = false,
                                                onToggleFavorite = { onToggleFavorite(track) },
                                                onSeekTo = onSeekTo,
                                                onTogglePlayPause = onTogglePlayPause,
                                                onPrevious = onPrevious,
                                                onNext = onNext,
                                                onToggleShuffle = onToggleShuffle,
                                                onCycleRepeat = onCycleRepeat,
                                                onVolumeChange = onVolumeChange,
                                                onToggleMute = onToggleMute,
                                                onSpeedChange = onSpeedChange,
                                                onOpenQueue = { activeTab = NowPlayingTab.QUEUE },
                                                onOpenLyrics = { activeTab = NowPlayingTab.LYRICS },
                                                onOpenSleepTimer = onOpenSleepTimer,
                                                onOpenAudioStudio = onOpenAudioStudio
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(
                                                horizontal = if (isSidePanel) 18.dp else 24.dp,
                                                vertical = 10.dp
                                            ),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AnimatedTrackArtwork(
                                            track = track,
                                            isPlaying = playback.isPlaying,
                                            onSwipeNext = onNext,
                                            onSwipePrevious = onPrevious,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = if (isSidePanel) 270.dp else 340.dp)
                                                .aspectRatio(1f)
                                        )
                                        Spacer(modifier = Modifier.height(if (isSidePanel) 18.dp else 24.dp))
                                        NowPlayingControlsBody(
                                            track = track,
                                            playback = playback,
                                            spatialAudio = spatialAudio,
                                            sleepTimer = sleepTimer,
                                            compact = isSidePanel,
                                            onToggleFavorite = { onToggleFavorite(track) },
                                            onSeekTo = onSeekTo,
                                            onTogglePlayPause = onTogglePlayPause,
                                            onPrevious = onPrevious,
                                            onNext = onNext,
                                            onToggleShuffle = onToggleShuffle,
                                            onCycleRepeat = onCycleRepeat,
                                            onVolumeChange = onVolumeChange,
                                            onToggleMute = onToggleMute,
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
                                positionMs = playback.positionMs,
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
    albumName: String,
    sleepTimer: SleepTimerState,
    onCollapse: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { mod ->
                if (!isSidePanel) {
                    mod.pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 22f) onCollapse()
                        }
                    }
                } else mod
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isSidePanel) {
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
                text = "NOW PLAYING",
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
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenAudioStudio: () -> Unit
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
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
    Spacer(modifier = Modifier.height(10.dp))
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.SpatialAudio,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
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
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = playback.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    ExpressiveProgressBar(
        positionMs = playback.positionMs,
        durationMs = playback.durationMs,
        onSeek = onSeekTo
    )

    Spacer(modifier = Modifier.height(12.dp))

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

    Spacer(modifier = Modifier.height(14.dp))

    VolumeControlRow(
        volume = playback.volume,
        isMuted = playback.isMuted,
        onVolumeChange = onVolumeChange,
        onToggleMute = onToggleMute
    )

    Spacer(modifier = Modifier.height(10.dp))

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
                speeds.forEach { spd ->
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
