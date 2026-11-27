package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.Spatializer
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.os.Build
import com.example.data.model.AtmosProfile
import com.example.data.model.SpatialAudioState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Inspects real device Dolby Atmos codec decoders (E-AC-3 JOC, AC-4, E-AC-3),
 * observes Android 12L/13+ Spatializer & Head Tracking state, and drives real
 * Android DSP Virtualizer + BassBoost effects bound to ExoPlayer's audio session.
 */
class SpatialAudioController(private val context: Context) {

    private var virtualizer: Virtualizer? = null
    private var bassBoost: BassBoost? = null
    private var attachedSessionId: Int = 0
    private var spatializerListener: Any? = null

    private val _state = MutableStateFlow(detectInitialCapabilities())
    val state: StateFlow<SpatialAudioState> = _state.asStateFlow()

    init {
        registerSpatializerObserver()
    }

    private fun detectInitialCapabilities(): SpatialAudioState {
        var supportsAtmosJoc = false
        var supportsAc4 = false
        var supportsEac3 = false

        runCatching {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (info in codecList.codecInfos) {
                if (info.isEncoder) continue
                for (type in info.supportedTypes) {
                    when {
                        type.equals("audio/eac3-joc", ignoreCase = true) -> supportsAtmosJoc = true
                        type.equals("audio/ac4", ignoreCase = true) -> supportsAc4 = true
                        type.equals("audio/eac3", ignoreCase = true) -> supportsEac3 = true
                    }
                }
            }
        }

        var spatializerAvailable = false
        var spatializerEnabled = false
        var headTrackerAvailable = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            runCatching {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val sp = am?.spatializer
                if (sp != null) {
                    val hasLevel = sp.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
                    spatializerAvailable = hasLevel && sp.isAvailable
                    spatializerEnabled = hasLevel && sp.isEnabled
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        headTrackerAvailable = sp.isHeadTrackerAvailable
                    }
                }
            }
        }

        return SpatialAudioState(
            isEnabled = false,
            profile = AtmosProfile.MUSIC,
            virtualizerStrength = 550,
            bassBoostStrength = 250,
            supportsHardwareAtmosJoc = supportsAtmosJoc,
            supportsHardwareAc4 = supportsAc4,
            supportsHardwareEac3 = supportsEac3,
            isSystemSpatializerAvailable = spatializerAvailable,
            isSystemSpatializerEnabled = spatializerEnabled,
            isHeadTrackerAvailable = headTrackerAvailable,
            isDspVirtualizerSupported = true
        )
    }

    private fun registerSpatializerObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            runCatching {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                val sp = am.spatializer
                val listener = object : Spatializer.OnSpatializerStateChangedListener {
                    override fun onSpatializerEnabledChanged(spat: Spatializer, enabled: Boolean) {
                        refreshSystemSpatializerState(spat)
                    }

                    override fun onSpatializerAvailableChanged(spat: Spatializer, available: Boolean) {
                        refreshSystemSpatializerState(spat)
                    }
                }
                sp.addOnSpatializerStateChangedListener(context.mainExecutor, listener)
                spatializerListener = listener
            }
        }
    }

    private fun refreshSystemSpatializerState(sp: Spatializer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val hasLevel = sp.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
            val headTracker = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { sp.isHeadTrackerAvailable }.getOrDefault(false)
            } else {
                false
            }
            _state.update {
                it.copy(
                    isSystemSpatializerAvailable = hasLevel && sp.isAvailable,
                    isSystemSpatializerEnabled = hasLevel && sp.isEnabled,
                    isHeadTrackerAvailable = headTracker
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == attachedSessionId) return
        releaseEffects()
        attachedSessionId = audioSessionId

        val current = _state.value
        var dspSupported = false

        runCatching {
            val virt = Virtualizer(0, audioSessionId)
            if (virt.strengthSupported) {
                virt.setStrength(current.virtualizerStrength.coerceIn(0, 1000).toShort())
            }
            virt.enabled = current.isEnabled
            virtualizer = virt
            dspSupported = true
        }

        runCatching {
            val bass = BassBoost(0, audioSessionId)
            if (bass.strengthSupported) {
                bass.setStrength(current.bassBoostStrength.coerceIn(0, 1000).toShort())
            }
            bass.enabled = current.isEnabled
            bassBoost = bass
        }

        _state.update { it.copy(isDspVirtualizerSupported = dspSupported) }
    }

    fun applySavedState(
        enabled: Boolean,
        profile: AtmosProfile,
        virtualizerStrength: Int,
        bassBoostStrength: Int
    ) {
        val clampedVirt = virtualizerStrength.coerceIn(0, 1000)
        val clampedBass = bassBoostStrength.coerceIn(0, 1000)
        _state.update {
            it.copy(
                isEnabled = enabled,
                profile = profile,
                virtualizerStrength = clampedVirt,
                bassBoostStrength = clampedBass
            )
        }
        applyEffectsToHardware(enabled, clampedVirt, clampedBass)
    }

    fun setEnabled(enabled: Boolean) {
        val cur = _state.value
        _state.update { it.copy(isEnabled = enabled) }
        applyEffectsToHardware(enabled, cur.virtualizerStrength, cur.bassBoostStrength)
    }

    fun applyProfile(profile: AtmosProfile) {
        val (virt, bass) = when (profile) {
            AtmosProfile.MUSIC -> 600 to 300
            AtmosProfile.CINEMA -> 880 to 520
            AtmosProfile.VOICE -> 320 to 120
            AtmosProfile.DYNAMIC -> 740 to 400
        }
        val enabled = _state.value.isEnabled
        _state.update {
            it.copy(
                profile = profile,
                virtualizerStrength = virt,
                bassBoostStrength = bass
            )
        }
        applyEffectsToHardware(enabled, virt, bass)
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val cur = _state.value
        _state.update { it.copy(virtualizerStrength = clamped) }
        applyEffectsToHardware(cur.isEnabled, clamped, cur.bassBoostStrength)
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val cur = _state.value
        _state.update { it.copy(bassBoostStrength = clamped) }
        applyEffectsToHardware(cur.isEnabled, cur.virtualizerStrength, clamped)
    }

    @Suppress("DEPRECATION")
    private fun applyEffectsToHardware(enabled: Boolean, virtStrength: Int, bassStrength: Int) {
        runCatching {
            virtualizer?.let { v ->
                if (v.strengthSupported) v.setStrength(virtStrength.toShort())
                v.enabled = enabled
            }
        }
        runCatching {
            bassBoost?.let { b ->
                if (b.strengthSupported) b.setStrength(bassStrength.toShort())
                b.enabled = enabled
            }
        }
    }

    private fun releaseEffects() {
        runCatching { virtualizer?.release() }
        runCatching { bassBoost?.release() }
        virtualizer = null
        bassBoost = null
        attachedSessionId = 0
    }

    fun release() {
        releaseEffects()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            runCatching {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val listener = spatializerListener as? Spatializer.OnSpatializerStateChangedListener
                if (am != null && listener != null) {
                    am.spatializer.removeOnSpatializerStateChangedListener(listener)
                }
            }
        }
        spatializerListener = null
    }
}
