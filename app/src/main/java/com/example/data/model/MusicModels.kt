package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class NavDestination(val route: String) {
    HOME("home"),
    LIBRARY("library"),
    PLAYLISTS("playlists"),
    EQUALIZER("equalizer"),
    SETTINGS("settings")
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class SortOption(val label: String) {
    TITLE("Name"),
    ARTIST("Artist"),
    DATE_ADDED("Date Added"),
    DURATION("Duration"),
    MOST_PLAYED("Most Played")
}

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FAVORITES("Favorites")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class ColorPreset(val label: String, val seedArgb: Long) {
    DYNAMIC_ARTWORK("Album Dynamic", 0xFF4C8DF6),
    ELECTRIC_INDIGO("Nebula Blue", 0xFF4285F4),
    TERRACOTTA_SUNSET("Periwinkle Glow", 0xFF8B72FF),
    EMERALD_MOSS("Sky Cyan", 0xFF2CB5E8),
    ROSE_QUARTZ("Soft Lavender", 0xFFC58AF9),
    AMBER_VINYL("Warm Coral", 0xFFD95D39)
}

data class Track(
    val id: String,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val artworkPath: String?,
    val artworkSeedColor: Long?,
    val dateAdded: Long,
    val lastPlayedAt: Long?,
    val playCount: Int,
    val isFavorite: Boolean,
    val lyricsText: String?,
    val mimeType: String,
    val fileSizeBytes: Long,
    val isAvailable: Boolean = true
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val trackIds: List<String> = emptyList(),
    val coverArtIndex: Int = 0
)

data class AlbumGroup(
    val name: String,
    val artist: String,
    val artworkPath: String?,
    val seedColor: Long?,
    val tracks: List<Track>
)

data class ArtistGroup(
    val name: String,
    val artworkPath: String?,
    val seedColor: Long?,
    val tracks: List<Track>,
    val albumCount: Int
)

data class LyricLine(
    val timestampMs: Long?,
    val text: String
)

data class EqBand(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val levelMb: Int, // millibels, typically -1500..1500
    val minMb: Int = -1500,
    val maxMb: Int = 1500
)

data class EqPreset(
    val name: String,
    val gainsDb: List<Float> // 5 bands in dB (-12f..12f)
) {
    companion object {
        val BuiltIn = listOf(
            EqPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
            EqPreset("Bass Boost", listOf(6.5f, 4.5f, 0.5f, 1.0f, 2.0f)),
            EqPreset("Vocal", listOf(-2.0f, 1.5f, 4.5f, 3.5f, 0.5f)),
            EqPreset("Electronic", listOf(5.0f, 2.5f, -1.0f, 3.5f, 5.5f)),
            EqPreset("Acoustic", listOf(2.5f, 2.0f, 1.5f, 2.5f, 3.0f)),
            EqPreset("Late Night", listOf(3.0f, 1.5f, 0.0f, -1.5f, -2.5f))
        )
    }
}

data class EqState(
    val isEnabled: Boolean = false,
    val isHardwareSupported: Boolean = true,
    val activePreset: String = "Flat",
    val bands: List<EqBand> = defaultBands()
) {
    companion object {
        fun defaultBands(): List<EqBand> = listOf(
            EqBand(0, 60, "60Hz", 0, -1500, 1500),
            EqBand(1, 230, "230Hz", 0, -1500, 1500),
            EqBand(2, 910, "910Hz", 0, -1500, 1500),
            EqBand(3, 3600, "3.6kHz", 0, -1500, 1500),
            EqBand(4, 14000, "14kHz", 0, -1500, 1500)
        )
    }
}

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L
)

data class ArtworkPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val surfaceTint: Color
)

enum class AtmosProfile(val label: String, val subtitle: String) {
    MUSIC("Music", "Balanced 3D soundstage & crisp vocals"),
    CINEMA("Cinema", "Wide surround depth & rich sub-bass"),
    VOICE("Voice", "Focused center clarity & gentle room air"),
    DYNAMIC("Dynamic", "Adaptive spatial energy for all genres")
}

data class ActiveAudioFormat(
    val mimeType: String = "",
    val codecLabel: String = "Standard Audio",
    val channelCount: Int = 2,
    val sampleRateHz: Int = 0,
    val bitrateBps: Int = 0,
    val isDolbyAtmos: Boolean = false,
    val isDolbyDigitalPlus: Boolean = false,
    val isMultichannel: Boolean = false
) {
    val channelLayoutLabel: String
        get() = when {
            channelCount >= 8 -> "7.1 Surround"
            channelCount >= 6 -> "5.1 Surround"
            channelCount > 2 -> "$channelCount-Ch Surround"
            channelCount == 1 -> "Mono"
            else -> "Stereo"
        }
}

data class SpatialAudioState(
    val isEnabled: Boolean = false,
    val profile: AtmosProfile = AtmosProfile.MUSIC,
    val virtualizerStrength: Int = 550,
    val bassBoostStrength: Int = 250,
    val supportsHardwareAtmosJoc: Boolean = false,
    val supportsHardwareAc4: Boolean = false,
    val supportsHardwareEac3: Boolean = false,
    val isSystemSpatializerAvailable: Boolean = false,
    val isSystemSpatializerEnabled: Boolean = false,
    val isHeadTrackerAvailable: Boolean = false,
    val isDspVirtualizerSupported: Boolean = true
) {
    val hasHardwareDolbyDecoder: Boolean
        get() = supportsHardwareAtmosJoc || supportsHardwareAc4 || supportsHardwareEac3
}
