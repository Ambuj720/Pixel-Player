package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ColorPreset
import com.example.data.model.RepeatMode
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cadence_preferences")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPreset: ColorPreset = ColorPreset.DYNAMIC_ARTWORK,
    val dynamicAlbumColors: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val volume: Float = 0.85f,
    val eqEnabled: Boolean = false,
    val eqPreset: String = "Flat",
    val eqBandsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val gaplessEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val lastTrackId: String? = null,
    val lastPositionMs: Long = 0L,
    val lastQueueIds: List<String> = emptyList()
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PRESET = stringPreferencesKey("color_preset")
        val DYNAMIC_ALBUM_COLORS = booleanPreferencesKey("dynamic_album_colors")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val VOLUME = floatPreferencesKey("volume")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS_MB = stringPreferencesKey("eq_bands_mb")
        val GAPLESS_ENABLED = booleanPreferencesKey("gapless_enabled")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LAST_TRACK_ID = stringPreferencesKey("last_track_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs: Preferences ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            colorPreset = prefs[Keys.COLOR_PRESET]?.let { runCatching { ColorPreset.valueOf(it) }.getOrNull() }
                ?: ColorPreset.DYNAMIC_ARTWORK,
            dynamicAlbumColors = prefs[Keys.DYNAMIC_ALBUM_COLORS] ?: true,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            repeatMode = prefs[Keys.REPEAT_MODE]?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() }
                ?: RepeatMode.OFF,
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
            volume = prefs[Keys.VOLUME] ?: 0.85f,
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: false,
            eqPreset = prefs[Keys.EQ_PRESET] ?: "Flat",
            eqBandsMb = prefs[Keys.EQ_BANDS_MB]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.takeIf { it.size == 5 }
                ?: listOf(0, 0, 0, 0, 0),
            gaplessEnabled = prefs[Keys.GAPLESS_ENABLED] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            lastTrackId = prefs[Keys.LAST_TRACK_ID],
            lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            lastQueueIds = prefs[Keys.LAST_QUEUE_IDS]
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setColorPreset(preset: ColorPreset) {
        context.dataStore.edit {
            it[Keys.COLOR_PRESET] = preset.name
            if (preset == ColorPreset.DYNAMIC_ARTWORK) {
                it[Keys.DYNAMIC_ALBUM_COLORS] = true
            }
        }
    }

    suspend fun setDynamicAlbumColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_ALBUM_COLORS] = enabled }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setRepeatMode(mode: RepeatMode) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = mode.name }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
    }

    suspend fun setVolume(volume: Float) {
        context.dataStore.edit { it[Keys.VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setEqSettings(enabled: Boolean, preset: String, bandsMb: List<Int>) {
        context.dataStore.edit {
            it[Keys.EQ_ENABLED] = enabled
            it[Keys.EQ_PRESET] = preset
            it[Keys.EQ_BANDS_MB] = bandsMb.joinToString(",")
        }
    }

    suspend fun setGaplessEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS_ENABLED] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun savePlaybackSession(trackId: String?, positionMs: Long, queueIds: List<String>) {
        context.dataStore.edit {
            if (trackId != null) {
                it[Keys.LAST_TRACK_ID] = trackId
            } else {
                it.remove(Keys.LAST_TRACK_ID)
            }
            it[Keys.LAST_POSITION_MS] = positionMs.coerceAtLeast(0L)
            it[Keys.LAST_QUEUE_IDS] = queueIds.joinToString("|")
        }
    }
}
