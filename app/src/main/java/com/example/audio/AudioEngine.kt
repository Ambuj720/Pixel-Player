package com.example.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.ActiveAudioFormat
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 0.85f,
    val isMuted: Boolean = false,
    val activeAudioFormat: ActiveAudioFormat = ActiveAudioFormat(),
    val errorMessage: String? = null
) {
    val currentTrack: Track?
        get() = queue.getOrNull(currentIndex)
}

class AudioEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val exoPlayer: ExoPlayer = SharedPlayerHolder.getOrCreatePlayer(context)
    val equalizerController = EqualizerController()
    val spatialAudioController = SpatialAudioController(context)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    var onTrackStarted: ((Track) -> Unit)? = null
    var onSessionCheckpoint: ((trackId: String?, positionMs: Long, queueIds: List<String>) -> Unit)? = null

    private var positionTickerJob: Job? = null
    private var preMuteVolume: Float = 0.85f
    private var serviceStarted = false
    private var lastRecordedTrackId: String? = null

    private val playerListener = object : Player.Listener {
        @OptIn(UnstableApi::class)
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            equalizerController.attachToSession(audioSessionId)
            spatialAudioController.attachToSession(audioSessionId)
        }

        override fun onTracksChanged(tracks: Tracks) {
            val format = extractSelectedAudioFormat(tracks)
            if (format != null) {
                _playbackState.update { it.copy(activeAudioFormat = format) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying, errorMessage = null) }
            if (isPlaying) {
                startServiceIfNeeded()
                startPositionTicker()
            } else {
                stopPositionTicker()
                emitCheckpoint()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = exoPlayer.currentMediaItemIndex
            val dur = exoPlayer.duration.takeIf { it > 0 }
                ?: _playbackState.value.queue.getOrNull(idx)?.durationMs
                ?: 0L
            _playbackState.update {
                it.copy(
                    currentIndex = idx,
                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                    durationMs = dur,
                    errorMessage = null
                )
            }
            val track = _playbackState.value.queue.getOrNull(idx)
            if (track != null && track.id != lastRecordedTrackId) {
                lastRecordedTrackId = track.id
                onTrackStarted?.invoke(track)
            }
            emitCheckpoint()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val dur = exoPlayer.duration.takeIf { it > 0 }
                    ?: _playbackState.value.currentTrack?.durationMs
                    ?: 0L
                _playbackState.update { it.copy(durationMs = dur) }
            } else if (playbackState == Player.STATE_ENDED) {
                _playbackState.update { it.copy(isPlaying = false, positionMs = it.durationMs) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val trackTitle = _playbackState.value.currentTrack?.title ?: "Audio file"
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    errorMessage = "Unable to play \"$trackTitle\". The audio file may have been moved or removed."
                )
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        @OptIn(UnstableApi::class)
        val initialSession = exoPlayer.audioSessionId
        if (initialSession != 0) {
            equalizerController.attachToSession(initialSession)
            spatialAudioController.attachToSession(initialSession)
        }
    }

    fun setSpatializationEnabled(enabled: Boolean) {
        SharedPlayerHolder.configureSpatialization(enabled)
        spatialAudioController.setEnabled(enabled)
    }

    private fun extractSelectedAudioFormat(tracks: Tracks): ActiveAudioFormat? {
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        val fmt = group.getTrackFormat(i)
                        val mime = fmt.sampleMimeType.orEmpty()
                        val channels = fmt.channelCount.takeIf { it > 0 } ?: 2
                        val sampleRate = fmt.sampleRate.takeIf { it > 0 } ?: 0
                        val bitrate = fmt.bitrate.takeIf { it > 0 } ?: 0
                        val isAtmos = mime.equals(MimeTypes.AUDIO_E_AC3_JOC, ignoreCase = true) ||
                            mime.equals(MimeTypes.AUDIO_AC4, ignoreCase = true)
                        val isDdp = mime.equals(MimeTypes.AUDIO_E_AC3, ignoreCase = true)
                        val label = when {
                            mime.equals(MimeTypes.AUDIO_E_AC3_JOC, ignoreCase = true) -> "Dolby Atmos (E-AC-3 JOC)"
                            mime.equals(MimeTypes.AUDIO_AC4, ignoreCase = true) -> "Dolby Atmos (AC-4)"
                            mime.equals(MimeTypes.AUDIO_E_AC3, ignoreCase = true) -> "Dolby Digital Plus"
                            mime.equals(MimeTypes.AUDIO_AC3, ignoreCase = true) -> "Dolby Digital"
                            mime.equals(MimeTypes.AUDIO_FLAC, ignoreCase = true) -> "FLAC Lossless"
                            mime.equals(MimeTypes.AUDIO_RAW, ignoreCase = true) -> "PCM WAV"
                            mime.equals(MimeTypes.AUDIO_MPEG, ignoreCase = true) -> "MP3"
                            mime.equals(MimeTypes.AUDIO_AAC, ignoreCase = true) -> "AAC"
                            mime.equals(MimeTypes.AUDIO_OPUS, ignoreCase = true) -> "Opus"
                            mime.equals(MimeTypes.AUDIO_VORBIS, ignoreCase = true) -> "Vorbis OGG"
                            mime.isNotBlank() -> mime.substringAfter('/').uppercase()
                            else -> "Standard Audio"
                        }
                        return ActiveAudioFormat(
                            mimeType = mime,
                            codecLabel = label,
                            channelCount = channels,
                            sampleRateHz = sampleRate,
                            bitrateBps = bitrate,
                            isDolbyAtmos = isAtmos,
                            isDolbyDigitalPlus = isDdp,
                            isMultichannel = channels > 2
                        )
                    }
                }
            }
        }
        return null
    }

    fun playQueue(tracks: List<Track>, startIndex: Int, startPositionMs: Long = 0L, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val mediaItems = tracks.map { it.toMediaItem() }

        _playbackState.update {
            it.copy(
                queue = tracks,
                currentIndex = safeIndex,
                positionMs = startPositionMs,
                durationMs = tracks[safeIndex].durationMs,
                errorMessage = null
            )
        }

        exoPlayer.setMediaItems(mediaItems, safeIndex, startPositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoPlay

        if (autoPlay) {
            startServiceIfNeeded()
            val track = tracks[safeIndex]
            lastRecordedTrackId = track.id
            onTrackStarted?.invoke(track)
        }
        emitCheckpoint()
    }

    fun updateTrackMetadataInQueue(updatedTracksById: Map<String, Track>) {
        _playbackState.update { state ->
            if (state.queue.isEmpty()) return@update state
            val refreshed = state.queue.map { old -> updatedTracksById[old.id] ?: old }
            state.copy(queue = refreshed)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.mediaItemCount == 0) return
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
            exoPlayer.play()
        } else if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            startServiceIfNeeded()
            exoPlayer.play()
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun play() {
        if (exoPlayer.mediaItemCount == 0) return
        startServiceIfNeeded()
        exoPlayer.play()
    }

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
            exoPlayer.play()
        } else if (_playbackState.value.queue.isNotEmpty()) {
            exoPlayer.seekTo(0, 0L)
            exoPlayer.play()
        }
    }

    fun previous() {
        if (exoPlayer.currentPosition > 3500L) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
            exoPlayer.play()
        } else if (_playbackState.value.queue.isNotEmpty()) {
            exoPlayer.seekTo(_playbackState.value.queue.lastIndex, 0L)
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(1L))
        exoPlayer.seekTo(clamped)
        _playbackState.update { it.copy(positionMs = clamped) }
    }

    fun seekBy(deltaMs: Long) {
        seekTo(exoPlayer.currentPosition + deltaMs)
    }

    fun jumpToQueueIndex(index: Int) {
        if (index !in _playbackState.value.queue.indices) return
        exoPlayer.seekTo(index, 0L)
        exoPlayer.play()
    }

    fun addToQueue(track: Track) {
        val currentQueue = _playbackState.value.queue
        if (currentQueue.isEmpty()) {
            playQueue(listOf(track), 0, autoPlay = true)
            return
        }
        exoPlayer.addMediaItem(track.toMediaItem())
        _playbackState.update { it.copy(queue = it.queue + track) }
        emitCheckpoint()
    }

    fun playNextInQueue(track: Track) {
        val state = _playbackState.value
        if (state.queue.isEmpty()) {
            playQueue(listOf(track), 0, autoPlay = true)
            return
        }
        val insertIndex = (state.currentIndex + 1).coerceIn(0, state.queue.size)
        exoPlayer.addMediaItem(insertIndex, track.toMediaItem())
        val updated = state.queue.toMutableList().apply { add(insertIndex, track) }
        _playbackState.update { it.copy(queue = updated) }
        emitCheckpoint()
    }

    fun removeFromQueue(index: Int) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        val updated = state.queue.toMutableList().apply { removeAt(index) }
        exoPlayer.removeMediaItem(index)
        val newCurrent = when {
            updated.isEmpty() -> -1
            index < state.currentIndex -> state.currentIndex - 1
            index == state.currentIndex -> index.coerceAtMost(updated.lastIndex)
            else -> state.currentIndex
        }
        _playbackState.update { it.copy(queue = updated, currentIndex = newCurrent) }
        emitCheckpoint()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val state = _playbackState.value
        if (fromIndex !in state.queue.indices || toIndex !in state.queue.indices || fromIndex == toIndex) return
        exoPlayer.moveMediaItem(fromIndex, toIndex)
        val updated = state.queue.toMutableList()
        val moved = updated.removeAt(fromIndex)
        updated.add(toIndex, moved)
        val newCurrent = exoPlayer.currentMediaItemIndex.coerceIn(-1, updated.lastIndex)
        _playbackState.update { it.copy(queue = updated, currentIndex = newCurrent) }
        emitCheckpoint()
    }

    fun setShuffleEnabled(enabled: Boolean) {
        exoPlayer.shuffleModeEnabled = enabled
        _playbackState.update { it.copy(shuffleEnabled = enabled) }
    }

    fun setRepeatMode(mode: RepeatMode) {
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _playbackState.update { it.copy(repeatMode = mode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        exoPlayer.playbackParameters = PlaybackParameters(clamped)
        _playbackState.update { it.copy(playbackSpeed = clamped) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (clamped > 0f) preMuteVolume = clamped
        exoPlayer.volume = clamped
        _playbackState.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
    }

    fun toggleMute() {
        if (_playbackState.value.isMuted || _playbackState.value.volume == 0f) {
            setVolume(preMuteVolume.coerceAtLeast(0.5f))
        } else {
            preMuteVolume = _playbackState.value.volume
            exoPlayer.volume = 0f
            _playbackState.update { it.copy(volume = 0f, isMuted = true) }
        }
    }

    fun fadeOutAndPause() {
        scope.launch {
            val originalVol = _playbackState.value.volume
            for (step in 10 downTo 1) {
                exoPlayer.volume = originalVol * (step / 10f)
                delay(120L)
            }
            exoPlayer.pause()
            exoPlayer.volume = originalVol
        }
    }

    fun clearError() {
        _playbackState.update { it.copy(errorMessage = null) }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            var tickCounter = 0
            while (isActive) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.takeIf { it > 0 } ?: _playbackState.value.durationMs
                val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                _playbackState.update {
                    it.copy(positionMs = pos, durationMs = dur, bufferedPositionMs = buf)
                }
                tickCounter++
                if (tickCounter % 20 == 0) {
                    emitCheckpoint()
                }
                delay(180L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun emitCheckpoint() {
        val st = _playbackState.value
        onSessionCheckpoint?.invoke(
            st.currentTrack?.id,
            st.positionMs,
            st.queue.map { it.id }
        )
    }

    private fun startServiceIfNeeded() {
        if (serviceStarted) return
        serviceStarted = true
        runCatching {
            val intent = Intent(context.applicationContext, PlaybackService::class.java)
            context.applicationContext.startService(intent)
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setGenre(genre)
        if (!artworkPath.isNullOrBlank()) {
            val artFile = File(artworkPath)
            if (artFile.exists()) {
                metaBuilder.setArtworkUri(Uri.fromFile(artFile))
            }
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(uriString))
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    fun releaseListener() {
        stopPositionTicker()
        exoPlayer.removeListener(playerListener)
        equalizerController.release()
        spatialAudioController.release()
    }
}
