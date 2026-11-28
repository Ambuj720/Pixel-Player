package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ColorPreset
import com.example.data.model.ThemeMode
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.PlayerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: PlayerUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorPresetChange: (ColorPreset) -> Unit,
    onDynamicAlbumColorsChange: (Boolean) -> Unit,
    onDolbyAtmosChange: (Boolean) -> Unit,
    onGaplessChange: (Boolean) -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    onScanDeviceClick: () -> Unit,
    onGenerateSamplesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val totalDuration = uiState.totalDurationMs

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isShortLandscape = maxHeight < 480.dp && maxWidth > maxHeight

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = if (isShortLandscape) 10.dp else 16.dp,
                bottom = if (isShortLandscape) 84.dp else 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 14.dp else 18.dp)
        ) {
            item(key = "settings_header") {
                Column {
                    Text(
                        text = "Settings",
                        style = if (isShortLandscape) MaterialTheme.typography.headlineMedium
                        else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dynamic Material You theming, playback engine & local privacy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        // Theme & Dynamic Material You Colors
        item(key = "theme_card") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Appearance & Dynamic Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = prefs.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                            ) {
                                Text(mode.label)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Album-Artwork Dynamic Colors", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Extract dominant tones from the playing track's artwork",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.dynamicAlbumColors,
                            onCheckedChange = onDynamicAlbumColorsChange,
                            modifier = Modifier.testTag("dynamic_colors_switch")
                        )
                    }

                    Text("Color Theme Preset", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ColorPreset.entries.forEach { preset ->
                            val selected = prefs.colorPreset == preset
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onColorPresetChange(preset) }
                                    .testTag("color_preset_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.seedArgb.toInt()))
                                            .border(
                                                width = if (selected) 2.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Audio & Motion Accessibility
        item(key = "playback_prefs") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Playback & Accessibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dolby Atmos & Spatial Audio", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (uiState.spatialAudio.hasHardwareDolbyDecoder) {
                                    "Hardware Dolby decoder detected • E-AC-3 JOC / AC-4 & 3D spatializer"
                                } else {
                                    "Media3 spatialization + real-time 3D DSP Virtualizer & Bass Boost"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.spatialAudio.isEnabled,
                            onCheckedChange = onDolbyAtmosChange,
                            modifier = Modifier.testTag("settings_dolby_atmos_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gapless Queue Playback", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Preload adjacent tracks in ExoPlayer for seamless transitions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.gaplessEnabled,
                            onCheckedChange = onGaplessChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reduced Motion", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Minimize spring transitions and UI animations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.reducedMotion,
                            onCheckedChange = onReducedMotionChange,
                            modifier = Modifier.testTag("reduced_motion_switch")
                        )
                    }
                }
            }
        }

        // Local Storage & Privacy Card
        item(key = "storage_card") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("100% On-Device Music Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Your imported audio files, metadata, lyrics, and playlists are stored exclusively on your device using Android Room and DataStore. Nothing is uploaded to external servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${uiState.tracks.size} tracks • ${uiState.playlists.size} playlists • ${formatDurationMs(totalDuration)} total listening time",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = onScanDeviceClick,
                        enabled = !uiState.isScanningDevice,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_scan_device_button")
                    ) {
                        if (uiState.isScanningDevice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isScanningDevice) "Scanning Device…" else "Scan Device for Audio Files",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = onImportClick,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
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
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
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
}
