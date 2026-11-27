package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.EqState
import com.example.data.model.SleepTimerState
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.MonoFontFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    eqState: EqState,
    playbackSpeed: Float,
    sleepTimer: SleepTimerState,
    onToggleEq: (Boolean) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onBandLevelChange: (bandIndex: Int, levelMb: Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "eq_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Audio Studio & EQ",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (eqState.isHardwareSupported) "Real-time Android DSP 5-Band Equalizer"
                        else "Audio DSP active when playback session starts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = onToggleEq,
                    modifier = Modifier.testTag("eq_master_switch")
                )
            }
        }

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
                    text = "Presets",
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
                        speeds.forEach { spd ->
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
                    FilledTonalButton(onClick = onOpenSleepTimer, shape = RoundedCornerShape(16.dp)) {
                        Text(if (sleepTimer.isActive) "Adjust" else "Set Timer")
                    }
                }
            }
        }
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

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Zero dB center reference line
        drawLine(
            color = gridColor,
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 2f
        )

        if (bands.isEmpty()) return@Canvas

        val stepX = if (bands.size > 1) w / (bands.size - 1).toFloat() else w
        val points = bands.mapIndexed { i, band ->
            val span = (band.maxMb - band.minMb).coerceAtLeast(1).toFloat()
            val norm = (band.levelMb - band.minMb).toFloat() / span // 0..1
            val y = h - (norm * (h * 0.82f) + h * 0.09f)
            Offset(i * stepX, y)
        }

        val curvePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(listOf(fillTop, fillBottom))
        )

        drawPath(
            path = curvePath,
            color = lineColor,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(color = lineColor, radius = 9f, center = pt)
        }
    }
}
