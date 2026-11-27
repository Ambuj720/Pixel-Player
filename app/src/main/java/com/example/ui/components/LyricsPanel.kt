package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlaybackProgress
import com.example.data.media.MetadataParser
import com.example.data.model.Track
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class IndexedTimestamp(val lineIndex: Int, val timestampMs: Long)

private fun findActiveLyricIndex(
    synced: List<IndexedTimestamp>,
    positionMs: Long
): Int {
    if (synced.isEmpty()) return -1
    var low = 0
    var high = synced.lastIndex
    var result = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (synced[mid].timestampMs <= positionMs) {
            result = synced[mid].lineIndex
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return result
}

@Composable
fun LyricsPanel(
    track: Track?,
    progressFlow: StateFlow<PlaybackProgress>,
    onSeekTo: (Long) -> Unit,
    onSaveLyrics: (trackId: String, lyrics: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    val lines = remember(track.lyricsText) { MetadataParser.parseLyrics(track.lyricsText) }
    val syncedTimestamps = remember(lines) {
        lines.mapIndexedNotNull { idx, line ->
            line.timestampMs?.let { IndexedTimestamp(idx, it) }
        }.sortedBy { it.timestampMs }
    }
    val hasSyncedTimestamps = syncedTimestamps.isNotEmpty()
    var showEditDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasSyncedTimestamps) "Synced Lyrics" else "Lyrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = { showEditDialog = true },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.testTag("edit_lyrics_button")
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (lines.isEmpty()) "Add Lyrics" else "Edit", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (lines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No lyrics attached yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add plain text or time-synced [mm:ss.xx] LRC lyrics for \"${track.title}\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val activeIndex = if (hasSyncedTimestamps) {
                val activeIndexFlow = remember(syncedTimestamps, progressFlow) {
                    progressFlow
                        .map { findActiveLyricIndex(syncedTimestamps, it.positionMs) }
                        .distinctUntilChanged()
                }
                val initialIndex = remember(syncedTimestamps, progressFlow) {
                    findActiveLyricIndex(syncedTimestamps, progressFlow.value.positionMs)
                }
                activeIndexFlow.collectAsStateWithLifecycle(initialValue = initialIndex).value
            } else {
                -1
            }

            val listState = rememberLazyListState()
            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0 && activeIndex < lines.size) {
                    listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = lines,
                    key = { index, line -> "${index}_${line.timestampMs ?: -1L}" },
                    contentType = { _, _ -> "lyric_line" }
                ) { index, line ->
                    val isActive = index == activeIndex
                    val textColor by animateColorAsState(
                        targetValue = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            hasSyncedTimestamps -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        label = "lyric_color"
                    )
                    Text(
                        text = line.text,
                        style = if (isActive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = line.timestampMs != null) {
                                line.timestampMs?.let(onSeekTo)
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        var draft by remember(track.id) { mutableStateOf(track.lyricsText.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Lyrics for ${track.title}") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("[00:05.00] Paste synced LRC or plain lyrics...") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSaveLyrics(track.id, draft)
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
