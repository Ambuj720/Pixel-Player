package com.example.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.ActiveAudioFormat
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L
)

data class PlaybackState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
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

    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

    var onTrackStarted: ((Track) -> Unit)? = null
    var onSessionCheckpoint: ((trackId: String?, positionMs: Long, queueIds: List<String>) -> Unit)? = null

    private var positionTickerJob: Job? = null
    private var preMuteVolume: Float = 0.85f
    private var serviceStarted = false
    private var lastRecordedTrackId: String? = null
    private var cachedQueueIds: List<String> = emptyList()
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Bounded in-memory cache (max 256 KB) keyed by seed color so tracks sharing a palette reuse 1 tiny JPEG
    private val fallbackArtworkCache = object : LruCache<Int, ByteArray>(256 * 1024) {
        override fun sizeOf(key: Int, value: ByteArray): Int = value.size
    }

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
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            _playbackState.update {
                it.copy(
                    currentIndex = idx,
                    durationMs = dur,
                    errorMessage = null
                )
            }
            _playbackProgress.value = PlaybackProgress(
                positionMs = pos,
                durationMs = dur,
                bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            )
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
                _playbackProgress.update { it.copy(durationMs = dur) }
            } else if (playbackState == Player.STATE_ENDED) {
                val dur = _playbackState.value.durationMs
                _playbackState.update { it.copy(isPlaying = false) }
                _playbackProgress.update { it.copy(positionMs = dur, durationMs = dur) }
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

    private val shutdownHook: () -> Unit = { releaseListener() }

    init {
        exoPlayer.addListener(playerListener)
        @OptIn(UnstableApi::class)
        val initialSession = exoPlayer.audioSessionId
        if (initialSession != 0) {
            equalizerController.attachToSession(initialSession)
            spatialAudioController.attachToSession(initialSession)
        }
        SharedPlayerHolder.registerShutdownHook(shutdownHook)
        connectMediaSessionService()
    }

    private fun connectMediaSessionService() {
        if (controllerFuture != null) return
        runCatching {
            val appContext = context.applicationContext
            val sessionToken = SessionToken(
                appContext,
                ComponentName(appContext, PlaybackService::class.java)
            )
            val future = MediaController.Builder(appContext, sessionToken).buildAsync()
            controllerFuture = future
            future.addListener(
                { runCatching { future.get() } },
                ContextCompat.getMainExecutor(appContext)
            )
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

        val startDuration = tracks[safeIndex].durationMs
        cachedQueueIds = tracks.map { it.id }
        _playbackState.update {
            it.copy(
                queue = tracks,
                currentIndex = safeIndex,
                durationMs = startDuration,
                errorMessage = null
            )
        }
        _playbackProgress.value = PlaybackProgress(
            positionMs = startPositionMs,
            durationMs = startDuration,
            bufferedPositionMs = startPositionMs
        )

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
            var anyChanged = false
            val refreshed = ArrayList<Track>(state.queue.size)
            for (old in state.queue) {
                val updated = updatedTracksById[old.id]
                if (updated != null && updated != old) {
                    refreshed.add(updated)
                    anyChanged = true
                } else {
                    refreshed.add(old)
                }
            }
            if (!anyChanged) state else state.copy(queue = refreshed)
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
        val dur = _playbackState.value.durationMs.coerceAtLeast(1L)
        val clamped = positionMs.coerceIn(0L, dur)
        exoPlayer.seekTo(clamped)
        _playbackProgress.update { it.copy(positionMs = clamped, durationMs = dur) }
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
        val updatedQueue = currentQueue + track
        cachedQueueIds = cachedQueueIds + track.id
        _playbackState.update { it.copy(queue = updatedQueue) }
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
        cachedQueueIds = updated.map { it.id }
        _playbackState.update { it.copy(queue = updated) }
        emitCheckpoint()
    }

    fun removeFromQueue(index: Int) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        val updated = state.queue.toMutableList().apply { removeAt(index) }
        cachedQueueIds = updated.map { it.id }
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
        cachedQueueIds = updated.map { it.id }
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
                val next = PlaybackProgress(positionMs = pos, durationMs = dur, bufferedPositionMs = buf)
                if (_playbackProgress.value != next) {
                    _playbackProgress.value = next
                }
                tickCounter++
                if (tickCounter % 16 == 0) {
                    emitCheckpoint()
                }
                delay(250L)
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
            _playbackProgress.value.positionMs,
            cachedQueueIds
        )
    }

    private fun startServiceIfNeeded() {
        connectMediaSessionService()
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
            .setDisplayTitle(title)
            .setSubtitle(artist)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setGenre(genre)
            .setIsPlayable(true)

        if (!artworkPath.isNullOrBlank()) {
            // Zero-copy URI reference: Media3 loads off main thread without copying cover bytes into RAM
            metaBuilder.setArtworkUri(Uri.fromFile(File(artworkPath)))
        } else {
            val fallbackBytes = resolveFallbackArtworkBytes(artworkSeedColor)
            if (fallbackBytes != null) {
                metaBuilder.setArtworkData(fallbackBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        }

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(uriString))
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    private fun resolveFallbackArtworkBytes(seedColor: Long?): ByteArray? {
        val seed = (seedColor ?: 0xFF5B6CFFL).toInt()
        fallbackArtworkCache.get(seed)?.let { return it }

        val bytes = generateFallbackArtworkBytes(seed)
        if (bytes != null) {
            fallbackArtworkCache.put(seed, bytes)
        }
        return bytes
    }

    private fun generateFallbackArtworkBytes(seed: Int): ByteArray? = runCatching {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmp)

        val hsv = FloatArray(3)
        Color.colorToHSV(seed, hsv)
        val startColor = Color.HSVToColor(floatArrayOf(hsv[0], (hsv[1] * 0.85f).coerceIn(0.35f, 0.9f), 0.78f))
        val endColor = Color.HSVToColor(floatArrayOf((hsv[0] + 38f) % 360f, (hsv[1] * 0.9f).coerceIn(0.4f, 0.95f), 0.34f))

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size * 0.42f, size * 0.66f, size * 0.11f, glyphPaint)
        canvas.drawRect(size * 0.48f, size * 0.28f, size * 0.55f, size * 0.66f, glyphPaint)
        canvas.drawRect(size * 0.48f, size * 0.28f, size * 0.68f, size * 0.36f, glyphPaint)

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 76, out)
        bmp.recycle()
        out.toByteArray()
    }.getOrNull()

    fun releaseListener() {
        SharedPlayerHolder.unregisterShutdownHook(shutdownHook)
        stopPositionTicker()
        runCatching { exoPlayer.removeListener(playerListener) }
        controllerFuture?.let { runCatching { MediaController.releaseFuture(it) } }
        controllerFuture = null
        fallbackArtworkCache.evictAll()
        runCatching { equalizerController.release() }
        runCatching { spatialAudioController.release() }
        scope.cancel()
    }
}
