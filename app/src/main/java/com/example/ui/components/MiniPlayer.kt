package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlaybackProgress
import com.example.data.model.Track
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    progressFlow: StateFlow<PlaybackProgress>,
    durationMs: Long,
    onExpand: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cornerRadius = if (compact) 20.dp else 24.dp
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(cornerRadius),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 6.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -18f) onExpand()
                }
            }
            .clickable(onClick = onExpand)
            .testTag("mini_player")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    artworkPath = track.artworkPath,
                    title = track.title,
                    seedColor = track.artworkSeedColor,
                    shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
                    modifier = Modifier.size(if (compact) 40.dp else 48.dp)
                )

                Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))

                Column(modifier = Modifier.weight(1f)) {
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

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.padding(11.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            MiniPlayerProgressBar(
                progressFlow = progressFlow,
                fallbackDurationMs = durationMs
            )
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
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
        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
    )
}
