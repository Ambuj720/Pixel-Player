package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveAudioFormat
import com.example.data.model.AtmosProfile
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.EqState
import com.example.data.model.SleepTimerState
import com.example.data.model.SpatialAudioState
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.MonoFontFamily
import kotlin.math.roundToInt

private val STUDIO_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    eqState: EqState,
    spatialAudio: SpatialAudioState,
    activeAudioFormat: ActiveAudioFormat,
    playbackSpeed: Float,
    sleepTimer: SleepTimerState,
    onToggleEq: (Boolean) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onBandLevelChange: (bandIndex: Int, levelMb: Int) -> Unit,
    onToggleDolbyAtmos: (Boolean) -> Unit,
    onSelectAtmosProfile: (AtmosProfile) -> Unit,
    onVirtualizerStrengthChange: (Int) -> Unit,
    onBassBoostStrengthChange: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWideLandscape = maxWidth >= 660.dp
        val isShortLandscape = maxHeight < 480.dp && maxWidth > maxHeight

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = if (isShortLandscape) 10.dp else 16.dp,
                bottom = if (isShortLandscape) 84.dp else 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 14.dp else 18.dp)
        ) {
            item(key = "studio_header") {
                Column {
                    Text(
                        text = "Audio Studio & Atmos",
                        style = if (isShortLandscape) MaterialTheme.typography.headlineMedium
                        else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dolby Atmos spatialization, 5-band DSP equalizer & playback tuning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        // Dolby Atmos & Android Spatial Audio Expressive Card
        item(key = "dolby_atmos_card") {
            Surface(
                color = if (spatialAudio.isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(30.dp),
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = if (spatialAudio.isEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (spatialAudio.isEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SpatialAudio,
                                    contentDescription = null,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dolby Atmos & Spatial Audio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (spatialAudio.isEnabled) {
                                        "${spatialAudio.profile.label} profile • ${spatialAudio.profile.subtitle}"
                                    } else {
                                        "Enable E-AC-3 JOC / AC-4 spatialization & 3D soundstage"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = spatialAudio.isEnabled,
                            onCheckedChange = onToggleDolbyAtmos,
                            modifier = Modifier.testTag("dolby_atmos_switch")
                        )
                    }

                    // Atmos Profile Selector
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AtmosProfile.entries.forEach { profile ->
                            FilterChip(
                                selected = spatialAudio.profile == profile,
                                enabled = spatialAudio.isEnabled,
                                onClick = { onSelectAtmosProfile(profile) },
                                label = { Text(profile.label) },
                                modifier = Modifier.testTag("atmos_profile_${profile.name.lowercase()}")
                            )
                        }
                    }

                    // Spatial Virtualizer Depth Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("3D Spatial Immersion", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "${(spatialAudio.virtualizerStrength / 10)}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = spatialAudio.virtualizerStrength.toFloat(),
                            onValueChange = { onVirtualizerStrengthChange(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = spatialAudio.isEnabled,
                            modifier = Modifier.testTag("spatial_virtualizer_slider")
                        )
                    }

                    // Sub-Bass Depth Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Atmos Sub-Bass Depth", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "${(spatialAudio.bassBoostStrength / 10)}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = spatialAudio.bassBoostStrength.toFloat(),
                            onValueChange = { onBassBoostStrengthChange(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = spatialAudio.isEnabled,
                            modifier = Modifier.testTag("bass_boost_slider")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Real-time Hardware & Stream Capabilities Matrix
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "DEVICE & STREAM STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CapabilityStatusRow(
                            label = "Hardware Dolby Decoder",
                            value = when {
                                spatialAudio.supportsHardwareAtmosJoc && spatialAudio.supportsHardwareAc4 -> "E-AC-3 JOC + AC-4"
                                spatialAudio.supportsHardwareAtmosJoc -> "E-AC-3 JOC (Atmos)"
                                spatialAudio.supportsHardwareAc4 -> "Dolby AC-4"
                                spatialAudio.supportsHardwareEac3 -> "Dolby Digital Plus"
                                else -> "DSP Spatial Fallback"
                            },
                            active = spatialAudio.hasHardwareDolbyDecoder
                        )
                        CapabilityStatusRow(
                            label = "Android Spatializer",
                            value = when {
                                spatialAudio.isSystemSpatializerEnabled -> "Active"
                                spatialAudio.isSystemSpatializerAvailable -> "Ready (Enable in Sound Settings)"
                                else -> "Standard Output"
                            },
                            active = spatialAudio.isSystemSpatializerEnabled
                        )
                        CapabilityStatusRow(
                            label = "Head Tracking",
                            value = if (spatialAudio.isHeadTrackerAvailable) "Supported" else "Unavailable",
                            active = spatialAudio.isHeadTrackerAvailable
                        )
                        CapabilityStatusRow(
                            label = "Active Track Stream",
                            value = buildString {
                                append(activeAudioFormat.codecLabel)
                                append(" • ")
                                append(activeAudioFormat.channelLayoutLabel)
                                if (activeAudioFormat.sampleRateHz > 0) {
                                    append(" • ")
                                    append(String.format("%.1f kHz", activeAudioFormat.sampleRateHz / 1000f))
                                }
                            },
                            active = activeAudioFormat.isDolbyAtmos || activeAudioFormat.isMultichannel
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_SOUND_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("system_spatial_settings_button")
                    ) {
                        Icon(Icons.Rounded.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sound & Spatial Audio Settings",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 5-Band Graphic Equalizer Card
        item(key = "eq_section_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "5-Band Equalizer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (eqState.isHardwareSupported) "Real-time Android audiofx DSP"
                            else "Activates when audio session starts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = onToggleEq,
                    modifier = Modifier.testTag("eq_master_switch")
                )
            }
        }

        // Frequency Curve + Preset Chips + 5-Band Sliders (2-column console in landscape, stacked in portrait)
        if (isWideLandscape) {
            item(key = "eq_studio_split") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(0.46f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(28.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Frequency Curve",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = eqState.activePreset,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                EqCurveCanvas(
                                    bands = eqState.bands,
                                    enabled = eqState.isEnabled,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(116.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "EQ Presets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EqPreset.BuiltIn.forEach { preset ->
                                    FilterChip(
                                        selected = eqState.activePreset == preset.name,
                                        onClick = { onApplyPreset(preset) },
                                        label = { Text(preset.name) },
                                        modifier = Modifier.testTag("eq_preset_${preset.name.lowercase().replace(' ', '_')}")
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.weight(0.54f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Frequency Bands",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            eqState.bands.forEach { band ->
                                val gainDb = band.levelMb / 100f
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = band.label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                        modifier = Modifier.width(54.dp)
                                    )
                                    Slider(
                                        value = band.levelMb.toFloat(),
                                        onValueChange = { value -> onBandLevelChange(band.index, value.roundToInt()) },
                                        valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                                        enabled = eqState.isEnabled,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("eq_band_slider_${band.index}")
                                    )
                                    Text(
                                        text = String.format("%+.1f dB", gainDb),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = if (eqState.isEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(62.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "speed_sleep_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Playback Speed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                STUDIO_SPEEDS.forEach { spd ->
                                    FilterChip(
                                        selected = playbackSpeed == spd,
                                        onClick = { onSpeedChange(spd) },
                                        label = { Text("${spd}x", fontFamily = MonoFontFamily) }
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Sleep Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (sleepTimer.isActive) "Fading out in ${formatDurationMs(sleepTimer.remainingMs)}"
                                        else "Automatically fade out and pause playback",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = onOpenSleepTimer,
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (sleepTimer.isActive) "Adjust Timer" else "Set Sleep Timer",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Frequency Response Curve Visualizer
            item(key = "eq_curve") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Frequency Curve",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = eqState.activePreset,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        EqCurveCanvas(
                            bands = eqState.bands,
                            enabled = eqState.isEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(128.dp)
                        )
                    }
                }
            }

            // Preset selector chips
            item(key = "eq_presets") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "EQ Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqPreset.BuiltIn.forEach { preset ->
                            FilterChip(
                                selected = eqState.activePreset == preset.name,
                                onClick = { onApplyPreset(preset) },
                                label = { Text(preset.name) },
                                modifier = Modifier.testTag("eq_preset_${preset.name.lowercase().replace(' ', '_')}")
                            )
                        }
                    }
                }
            }

            // 5-Band Interactive Gain Sliders
            item(key = "eq_sliders") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Frequency Bands",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        eqState.bands.forEach { band ->
                            val gainDb = band.levelMb / 100f
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = band.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                    modifier = Modifier.width(56.dp)
                                )
                                Slider(
                                    value = band.levelMb.toFloat(),
                                    onValueChange = { value -> onBandLevelChange(band.index, value.roundToInt()) },
                                    valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                                    enabled = eqState.isEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("eq_band_slider_${band.index}")
                                )
                                Text(
                                    text = String.format("%+.1f dB", gainDb),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = if (eqState.isEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(64.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Playback Speed Studio Card
            item(key = "speed_card") {
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
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Playback Speed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            STUDIO_SPEEDS.forEach { spd ->
                                FilterChip(
                                    selected = playbackSpeed == spd,
                                    onClick = { onSpeedChange(spd) },
                                    label = { Text("${spd}x", fontFamily = MonoFontFamily) }
                                )
                            }
                        }
                    }
                }
            }

            // Sleep Timer Card
            item(key = "sleep_card") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sleep Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (sleepTimer.isActive) "Fading out in ${formatDurationMs(sleepTimer.remainingMs)}"
                                    else "Automatically fade out and pause playback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledTonalButton(
                            onClick = onOpenSleepTimer,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = if (sleepTimer.isActive) "Adjust" else "Set Timer",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CapabilityStatusRow(
    label: String,
    value: String,
    active: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EqCurveCanvas(
    bands: List<EqBand>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val fillTop = lineColor.copy(alpha = 0.28f)
    val fillBottom = lineColor.copy(alpha = 0.02f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            val points = if (bands.isEmpty()) {
                emptyList()
            } else {
                val stepX = if (bands.size > 1) w / (bands.size - 1).toFloat() else w
                List(bands.size) { i ->
                    val band = bands[i]
                    val span = (band.maxMb - band.minMb).coerceAtLeast(1).toFloat()
                    val norm = (band.levelMb - band.minMb).toFloat() / span
                    val y = h - (norm * (h * 0.82f) + h * 0.09f)
                    Offset(i * stepX, y)
                }
            }

            val curvePath = if (points.isNotEmpty()) {
                Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.lastIndex) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }
            } else null

            val fillPath = if (curvePath != null) {
                Path().apply {
                    addPath(curvePath)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }
            } else null

            val fillBrush = Brush.verticalGradient(listOf(fillTop, fillBottom))
            val strokeStyle = Stroke(width = 6f, cap = StrokeCap.Round)

            onDrawBehind {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 2f
                )
                if (fillPath != null && curvePath != null) {
                    drawPath(path = fillPath, brush = fillBrush)
                    drawPath(path = curvePath, color = lineColor, style = strokeStyle)
                    for (pt in points) {
                        drawCircle(color = lineColor, radius = 9f, center = pt)
                    }
                }
            }
        }
    )
}
