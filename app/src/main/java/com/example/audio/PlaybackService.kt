package com.example.audio

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Shared singleton holder so the UI AudioEngine and background MediaSessionService
 * control the exact same ExoPlayer & DefaultTrackSelector instance (with spatialization
 * and multichannel Dolby Atmos track selection support) without restarting audio on
 * screen rotation or navigation changes.
 */
@OptIn(UnstableApi::class)
object SharedPlayerHolder {
    @Volatile
    private var player: ExoPlayer? = null

    @Volatile
    private var trackSelector: DefaultTrackSelector? = null

    fun getOrCreatePlayer(context: Context): ExoPlayer {
        return player ?: synchronized(this) {
            player ?: run {
                val appContext = context.applicationContext
                val selector = DefaultTrackSelector(appContext).apply {
                    parameters = buildUponParameters()
                        .setConstrainAudioChannelCountToDeviceCapabilities(true)
                        .setExceedAudioConstraintsIfNecessary(true)
                        .build()
                }
                trackSelector = selector

                ExoPlayer.Builder(appContext)
                    .setTrackSelector(selector)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
                            .build(),
                        true
                    )
                    .setHandleAudioBecomingNoisy(true)
                    .build()
                    .also { player = it }
            }
        }
    }

    fun configureSpatialization(enabled: Boolean) {
        val exo = player ?: return
        val behavior = if (enabled) C.SPATIALIZATION_BEHAVIOR_AUTO else C.SPATIALIZATION_BEHAVIOR_NEVER
        exo.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .setSpatializationBehavior(behavior)
                .build(),
            true
        )
        trackSelector?.let { selector ->
            selector.parameters = selector.buildUponParameters()
                .setConstrainAudioChannelCountToDeviceCapabilities(enabled)
                .setMaxAudioChannelCount(if (enabled) Int.MAX_VALUE else 2)
                .setExceedAudioConstraintsIfNecessary(true)
                .build()
        }
    }

    fun getPlayerOrNull(): ExoPlayer? = player
}

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = SharedPlayerHolder.getOrCreatePlayer(this)
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
