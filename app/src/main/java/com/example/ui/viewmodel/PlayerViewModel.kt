package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.PlaybackState
import com.example.data.MusicRepository
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AlbumGroup
import com.example.data.model.ArtistGroup
import com.example.data.model.ColorPreset
import com.example.data.model.EqPreset
import com.example.data.model.EqState
import com.example.data.model.LibraryTab
import com.example.data.model.NavDestination
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.SleepTimerState
import com.example.data.model.SortOption
import com.example.data.model.ThemeMode
import com.example.data.model.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LibraryUiFilters(
    val searchQuery: String = "",
    val activeTab: LibraryTab = LibraryTab.SONGS,
    val sortOption: SortOption = SortOption.DATE_ADDED,
    val isGridView: Boolean = false,
    val selectedGenre: String? = null,
    val selectedArtistFilter: String? = null,
    val selectedAlbumFilter: String? = null
)

data class PlayerUiState(
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val playback: PlaybackState = PlaybackState(),
    val eqState: EqState = EqState(),
    val preferences: UserPreferences = UserPreferences(),
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val filters: LibraryUiFilters = LibraryUiFilters(),
    val activeDestination: NavDestination = NavDestination.HOME,
    val selectedPlaylistId: String? = null,
    val isNowPlayingExpanded: Boolean = false,
    val isImporting: Boolean = false,
    val statusBanner: String? = null
) {
    val tracksById: Map<String, Track>
        get() = tracks.associateBy { it.id }

    val recentlyPlayed: List<Track>
        get() = tracks.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }.take(12)

    val mostPlayed: List<Track>
        get() = tracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(12)

    val recentlyAdded: List<Track>
        get() = tracks.sortedByDescending { it.dateAdded }.take(12)

    val favorites: List<Track>
        get() = tracks.filter { it.isFavorite }

    val genres: List<String>
        get() = tracks.map { it.genre }.distinct().sorted()

    val albums: List<AlbumGroup>
        get() = tracks.groupBy { it.album }
            .map { (albumName, albumTracks) ->
                val lead = albumTracks.first()
                AlbumGroup(
                    name = albumName,
                    artist = lead.artist,
                    artworkPath = albumTracks.firstNotNullOfOrNull { it.artworkPath },
                    seedColor = albumTracks.firstNotNullOfOrNull { it.artworkSeedColor },
                    tracks = albumTracks
                )
            }.sortedBy { it.name.lowercase() }

    val artists: List<ArtistGroup>
        get() = tracks.groupBy { it.artist }
            .map { (artistName, artistTracks) ->
                ArtistGroup(
                    name = artistName,
                    artworkPath = artistTracks.firstNotNullOfOrNull { it.artworkPath },
                    seedColor = artistTracks.firstNotNullOfOrNull { it.artworkSeedColor },
                    tracks = artistTracks,
                    albumCount = artistTracks.map { it.album }.distinct().size
                )
            }.sortedBy { it.name.lowercase() }

    val filteredTracks: List<Track>
        get() {
            val q = filters.searchQuery.trim().lowercase()
            val base = tracks.filter { track ->
                val matchesTab = when (filters.activeTab) {
                    LibraryTab.FAVORITES -> track.isFavorite
                    else -> true
                }
                val matchesQuery = q.isEmpty() ||
                    track.title.lowercase().contains(q) ||
                    track.artist.lowercase().contains(q) ||
                    track.album.lowercase().contains(q) ||
                    track.genre.lowercase().contains(q)
                val matchesGenre = filters.selectedGenre == null || track.genre == filters.selectedGenre
                val matchesArtist = filters.selectedArtistFilter == null || track.artist == filters.selectedArtistFilter
                val matchesAlbum = filters.selectedAlbumFilter == null || track.album == filters.selectedAlbumFilter
                matchesTab && matchesQuery && matchesGenre && matchesArtist && matchesAlbum
            }
            return when (filters.sortOption) {
                SortOption.TITLE -> base.sortedBy { it.title.lowercase() }
                SortOption.ARTIST -> base.sortedBy { it.artist.lowercase() }
                SortOption.DATE_ADDED -> base.sortedByDescending { it.dateAdded }
                SortOption.DURATION -> base.sortedByDescending { it.durationMs }
                SortOption.MOST_PLAYED -> base.sortedByDescending { it.playCount }
            }
        }
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val preferencesRepo = UserPreferencesRepository(application)
    private val audioEngine = AudioEngine(application)

    private val _filters = MutableStateFlow(LibraryUiFilters())
    private val _activeDestination = MutableStateFlow(NavDestination.HOME)
    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    private val _isNowPlayingExpanded = MutableStateFlow(false)
    private val _isImporting = MutableStateFlow(false)
    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _sleepTimer = MutableStateFlow(SleepTimerState())

    private var sleepTimerJob: Job? = null
    private var sessionRestored = false

    private data class UiNavOverlay(
        val filters: LibraryUiFilters,
        val destination: NavDestination,
        val playlistId: String?,
        val expanded: Boolean,
        val importing: Boolean,
        val banner: String?
    )

    private val navOverlayFlow = combine(
        _filters,
        _activeDestination,
        _selectedPlaylistId,
        _isNowPlayingExpanded,
        _isImporting,
        _statusBanner
    ) { arr ->
        UiNavOverlay(
            filters = arr[0] as LibraryUiFilters,
            destination = arr[1] as NavDestination,
            playlistId = arr[2] as String?,
            expanded = arr[3] as Boolean,
            importing = arr[4] as Boolean,
            banner = arr[5] as String?
        )
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        repository.tracks,
        repository.playlists,
        audioEngine.playbackState,
        audioEngine.equalizerController.state,
        preferencesRepo.preferences,
        _sleepTimer,
        navOverlayFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val tracks = arr[0] as List<Track>
        @Suppress("UNCHECKED_CAST")
        val playlists = arr[1] as List<Playlist>
        val playback = arr[2] as PlaybackState
        val eqState = arr[3] as EqState
        val prefs = arr[4] as UserPreferences
        val sleep = arr[5] as SleepTimerState
        val overlay = arr[6] as UiNavOverlay

        PlayerUiState(
            tracks = tracks,
            playlists = playlists,
            playback = playback,
            eqState = eqState,
            preferences = prefs,
            sleepTimer = sleep,
            filters = overlay.filters,
            activeDestination = overlay.destination,
            selectedPlaylistId = overlay.playlistId,
            isNowPlayingExpanded = overlay.expanded,
            isImporting = overlay.importing,
            statusBanner = overlay.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState()
    )

    init {
        audioEngine.onTrackStarted = { track ->
            viewModelScope.launch {
                repository.recordPlayback(track.id)
            }
        }
        audioEngine.onSessionCheckpoint = { trackId, positionMs, queueIds ->
            viewModelScope.launch {
                preferencesRepo.savePlaybackSession(trackId, positionMs, queueIds)
            }
        }

        // Apply saved preferences & restore last session cleanly
        viewModelScope.launch {
            val prefs = preferencesRepo.preferences.first()
            audioEngine.setPlaybackSpeed(prefs.playbackSpeed)
            audioEngine.setRepeatMode(prefs.repeatMode)
            audioEngine.setShuffleEnabled(prefs.shuffleEnabled)
            audioEngine.setVolume(prefs.volume)
            audioEngine.equalizerController.applySavedState(
                enabled = prefs.eqEnabled,
                presetName = prefs.eqPreset,
                bandLevelsMb = prefs.eqBandsMb
            )
        }

        // Sync Room metadata updates (like favorite / lyrics changes) into active queue & restore last session
        viewModelScope.launch {
            repository.tracks.collect { tracks ->
                audioEngine.updateTrackMetadataInQueue(tracks.associateBy { it.id })
                if (!sessionRestored && tracks.isNotEmpty()) {
                    sessionRestored = true
                    restoreLastPlaybackSession(tracks)
                }
            }
        }
    }

    private suspend fun restoreLastPlaybackSession(tracks: List<Track>) {
        if (audioEngine.playbackState.value.queue.isNotEmpty()) return
        val prefs = preferencesRepo.preferences.first()
        val byId = tracks.associateBy { it.id }
        val savedQueue = prefs.lastQueueIds.mapNotNull { byId[it] }
        val queueToLoad = if (savedQueue.isNotEmpty()) savedQueue else tracks
        val targetIdx = prefs.lastTrackId?.let { id ->
            queueToLoad.indexOfFirst { it.id == id }
        }?.takeIf { it >= 0 } ?: 0

        val targetTrack = queueToLoad[targetIdx]
        val accessible = repository.verifyTrackAvailable(targetTrack.uriString)
        if (accessible) {
            audioEngine.playQueue(
                tracks = queueToLoad,
                startIndex = targetIdx,
                startPositionMs = prefs.lastPositionMs.coerceAtMost(targetTrack.durationMs),
                autoPlay = false
            )
        }
    }

    // --- Navigation ---
    fun selectDestination(dest: NavDestination) {
        _activeDestination.value = dest
        if (dest != NavDestination.PLAYLISTS) {
            _selectedPlaylistId.value = null
        }
    }

    fun openPlaylist(playlistId: String) {
        _selectedPlaylistId.value = playlistId
        _activeDestination.value = NavDestination.PLAYLISTS
    }

    fun closePlaylistDetail() {
        _selectedPlaylistId.value = null
    }

    fun openNowPlaying() {
        _isNowPlayingExpanded.value = true
    }

    fun collapseNowPlaying() {
        _isNowPlayingExpanded.value = false
    }

    fun openLibraryFilteredByArtist(artist: String) {
        _filters.update {
            it.copy(
                activeTab = LibraryTab.SONGS,
                selectedArtistFilter = artist,
                selectedAlbumFilter = null,
                selectedGenre = null
            )
        }
        _activeDestination.value = NavDestination.LIBRARY
    }

    fun openLibraryFilteredByAlbum(album: String) {
        _filters.update {
            it.copy(
                activeTab = LibraryTab.SONGS,
                selectedAlbumFilter = album,
                selectedArtistFilter = null,
                selectedGenre = null
            )
        }
        _activeDestination.value = NavDestination.LIBRARY
    }

    // --- Importing & Local Demo Samples ---
    fun importAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            val added = repository.importUris(uris)
            _isImporting.value = false
            showBanner(
                if (added > 0) "Imported $added audio ${if (added == 1) "track" else "tracks"} to your local library"
                else "Selected files are already in your library or unsupported"
            )
        }
    }

    fun generateLocalDemoTracks() {
        viewModelScope.launch {
            _isImporting.value = true
            val count = repository.generateLocalDemoTracks()
            _isImporting.value = false
            showBanner("Created $count local studio WAV tracks ready for playback")
        }
    }

    // --- Playback Controls ---
    fun playTrack(track: Track, contextList: List<Track> = uiState.value.tracks) {
        viewModelScope.launch {
            val available = repository.verifyTrackAvailable(track.uriString)
            if (!available) {
                showBanner("\"${track.title}\" is no longer accessible on device storage")
                return@launch
            }
            val list = if (contextList.any { it.id == track.id }) contextList else listOf(track)
            val index = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            audioEngine.playQueue(list, index, autoPlay = true)
        }
    }

    fun playAll(tracks: List<Track>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val ordered = if (shuffle) tracks.shuffled() else tracks
        viewModelScope.launch {
            if (shuffle) {
                audioEngine.setShuffleEnabled(true)
                preferencesRepo.setShuffleEnabled(true)
            }
            audioEngine.playQueue(ordered, 0, autoPlay = true)
        }
    }

    fun togglePlayPause() = audioEngine.togglePlayPause()
    fun nextTrack() = audioEngine.next()
    fun previousTrack() = audioEngine.previous()
    fun seekTo(positionMs: Long) = audioEngine.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = audioEngine.seekBy(deltaMs)
    fun jumpToQueueIndex(index: Int) = audioEngine.jumpToQueueIndex(index)
    fun addToQueue(track: Track) {
        audioEngine.addToQueue(track)
        showBanner("Added \"${track.title}\" to queue")
    }

    fun playNextInQueue(track: Track) {
        audioEngine.playNextInQueue(track)
        showBanner("\"${track.title}\" will play next")
    }

    fun removeFromQueue(index: Int) = audioEngine.removeFromQueue(index)
    fun moveQueueItem(from: Int, to: Int) = audioEngine.moveQueueItem(from, to)

    fun toggleShuffle() {
        val next = !uiState.value.playback.shuffleEnabled
        audioEngine.setShuffleEnabled(next)
        viewModelScope.launch { preferencesRepo.setShuffleEnabled(next) }
    }

    fun cycleRepeatMode() {
        val next = when (uiState.value.playback.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        audioEngine.setRepeatMode(next)
        viewModelScope.launch { preferencesRepo.setRepeatMode(next) }
    }

    fun setPlaybackSpeed(speed: Float) {
        audioEngine.setPlaybackSpeed(speed)
        viewModelScope.launch { preferencesRepo.setPlaybackSpeed(speed) }
    }

    fun setVolume(volume: Float) {
        audioEngine.setVolume(volume)
        viewModelScope.launch { preferencesRepo.setVolume(volume) }
    }

    fun toggleMute() = audioEngine.toggleMute()

    fun clearPlaybackError() = audioEngine.clearError()

    // --- Track Actions ---
    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, track.isFavorite)
        }
    }

    fun updateTrackLyrics(trackId: String, lyrics: String) {
        viewModelScope.launch {
            repository.updateLyrics(trackId, lyrics)
            showBanner("Lyrics saved")
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            val queueIdx = uiState.value.playback.queue.indexOfFirst { it.id == track.id }
            if (queueIdx >= 0) {
                audioEngine.removeFromQueue(queueIdx)
            }
            repository.deleteTrack(track.id)
            showBanner("Removed \"${track.title}\" from library")
        }
    }

    // --- Playlists ---
    fun createPlaylist(name: String, description: String, initialTrackId: String? = null) {
        viewModelScope.launch {
            repository.createPlaylist(
                name = name,
                description = description,
                initialTrackIds = listOfNotNull(initialTrackId)
            )
            showBanner("Created playlist \"$name\"")
        }
    }

    fun renamePlaylist(playlistId: String, name: String, description: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, name, description)
            showBanner("Updated playlist")
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            if (_selectedPlaylistId.value == playlistId) {
                _selectedPlaylistId.value = null
            }
            repository.deletePlaylist(playlistId)
            showBanner("Playlist deleted")
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            showBanner("Added track to playlist")
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun moveTrackInPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
        val playlist = uiState.value.playlists.firstOrNull { it.id == playlistId } ?: return
        if (fromIndex !in playlist.trackIds.indices || toIndex !in playlist.trackIds.indices) return
        val updated = playlist.trackIds.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        viewModelScope.launch {
            repository.reorderPlaylistTracks(playlistId, updated)
        }
    }

    // --- Search, Sort & Filters ---
    fun updateSearchQuery(query: String) {
        _filters.update { it.copy(searchQuery = query) }
    }

    fun setLibraryTab(tab: LibraryTab) {
        _filters.update { it.copy(activeTab = tab) }
    }

    fun setSortOption(sort: SortOption) {
        _filters.update { it.copy(sortOption = sort) }
    }

    fun toggleGridView() {
        _filters.update { it.copy(isGridView = !it.isGridView) }
    }

    fun setGenreFilter(genre: String?) {
        _filters.update { it.copy(selectedGenre = if (it.selectedGenre == genre) null else genre) }
    }

    fun clearActiveFilters() {
        _filters.update {
            it.copy(
                selectedGenre = null,
                selectedArtistFilter = null,
                selectedAlbumFilter = null
            )
        }
    }

    // --- Equalizer ---
    fun setEqEnabled(enabled: Boolean) {
        audioEngine.equalizerController.setEnabled(enabled)
        persistEq()
    }

    fun setEqBandLevel(bandIndex: Int, levelMb: Int) {
        audioEngine.equalizerController.setBandLevel(bandIndex, levelMb)
        persistEq()
    }

    fun applyEqPreset(preset: EqPreset) {
        audioEngine.equalizerController.applyPreset(preset)
        persistEq()
    }

    private fun persistEq() {
        val eq = audioEngine.equalizerController.state.value
        viewModelScope.launch {
            preferencesRepo.setEqSettings(
                enabled = eq.isEnabled,
                preset = eq.activePreset,
                bandsMb = eq.bands.map { it.levelMb }
            )
        }
    }

    // --- Sleep Timer ---
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimer.value = SleepTimerState()
            return
        }
        val totalMs = minutes * 60_000L
        _sleepTimer.value = SleepTimerState(isActive = true, remainingMs = totalMs, totalMs = totalMs)
        showBanner("Sleep timer set for $minutes min")
        sleepTimerJob = viewModelScope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(1000L)
                remaining = (remaining - 1000L).coerceAtLeast(0L)
                _sleepTimer.update { it.copy(remainingMs = remaining) }
            }
            if (isActive) {
                _sleepTimer.value = SleepTimerState()
                audioEngine.fadeOutAndPause()
                showBanner("Sleep timer finished — playback paused")
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimer.value = SleepTimerState()
        showBanner("Sleep timer cancelled")
    }

    // --- Settings & Theme ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepo.setThemeMode(mode) }
    }

    fun setColorPreset(preset: ColorPreset) {
        viewModelScope.launch { preferencesRepo.setColorPreset(preset) }
    }

    fun setDynamicAlbumColors(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setDynamicAlbumColors(enabled) }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setGaplessEnabled(enabled) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setReducedMotion(enabled) }
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        audioEngine.releaseListener()
    }
}
