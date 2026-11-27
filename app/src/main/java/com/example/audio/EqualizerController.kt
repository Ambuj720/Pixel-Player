package com.example.audio

import android.media.audiofx.Equalizer
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.EqState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Manages Android's real hardware/software audiofx Equalizer bound to ExoPlayer's audio session.
 */
class EqualizerController {

    private var equalizer: Equalizer? = null
    private var attachedSessionId: Int = 0

    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == attachedSessionId) return
        release()
        attachedSessionId = audioSessionId
        try {
            val eq = Equalizer(0, audioSessionId)
            val numBands = eq.numberOfBands.toInt().coerceAtMost(5)
            val range = eq.bandLevelRange
            val minMb = range.getOrNull(0)?.toInt() ?: -1500
            val maxMb = range.getOrNull(1)?.toInt() ?: 1500

            val currentLevels = _state.value.bands.map { it.levelMb }
            val hardwareBands = (0 until numBands).map { i ->
                val centerHz = (eq.getCenterFreq(i.toShort()) / 1000).coerceAtLeast(30)
                val targetMb = (currentLevels.getOrNull(i) ?: 0).coerceIn(minMb, maxMb)
                runCatching { eq.setBandLevel(i.toShort(), targetMb.toShort()) }
                EqBand(
                    index = i,
                    centerFreqHz = centerHz,
                    label = formatFreq(centerHz),
                    levelMb = targetMb,
                    minMb = minMb,
                    maxMb = maxMb
                )
            }
            eq.enabled = _state.value.isEnabled
            equalizer = eq
            _state.value = _state.value.copy(
                isHardwareSupported = true,
                bands = if (hardwareBands.isNotEmpty()) hardwareBands else EqState.defaultBands()
            )
        } catch (_: Exception) {
            equalizer = null
            _state.value = _state.value.copy(isHardwareSupported = false)
        }
    }

    fun applySavedState(enabled: Boolean, presetName: String, bandLevelsMb: List<Int>) {
        val currentBands = _state.value.bands.mapIndexed { idx, band ->
            val level = (bandLevelsMb.getOrNull(idx) ?: 0).coerceIn(band.minMb, band.maxMb)
            runCatching { equalizer?.setBandLevel(band.index.toShort(), level.toShort()) }
            band.copy(levelMb = level)
        }
        runCatching { equalizer?.enabled = enabled }
        _state.value = _state.value.copy(
            isEnabled = enabled,
            activePreset = presetName,
            bands = currentBands
        )
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val updatedBands = _state.value.bands.map { band ->
            if (band.index == bandIndex) {
                val clamped = levelMb.coerceIn(band.minMb, band.maxMb)
                runCatching { equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort()) }
                band.copy(levelMb = clamped)
            } else {
                band
            }
        }
        _state.value = _state.value.copy(
            activePreset = "Custom",
            bands = updatedBands
        )
    }

    fun applyPreset(preset: EqPreset) {
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val gainDb = preset.gainsDb.getOrElse(idx) { 0f }
            val mb = (gainDb * 100f).roundToInt().coerceIn(band.minMb, band.maxMb)
            runCatching { equalizer?.setBandLevel(band.index.toShort(), mb.toShort()) }
            band.copy(levelMb = mb)
        }
        _state.value = _state.value.copy(
            activePreset = preset.name,
            bands = updatedBands
        )
    }

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        attachedSessionId = 0
    }

    private fun formatFreq(hz: Int): String {
        return if (hz >= 1000) {
            val khz = hz / 1000f
            if (khz % 1f < 0.05f) "${khz.toInt()}kHz" else String.format("%.1fkHz", khz)
        } else {
            "${hz}Hz"
        }
    }
}
