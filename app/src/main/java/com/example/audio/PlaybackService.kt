package com.example.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.annotation.OptIn
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

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

    private val shutdownHooks = CopyOnWriteArrayList<() -> Unit>()
    private val isTerminating = AtomicBoolean(false)

    fun registerShutdownHook(hook: () -> Unit) {
        shutdownHooks.addIfAbsent(hook)
    }

    fun unregisterShutdownHook(hook: () -> Unit) {
        shutdownHooks.remove(hook)
    }

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
                    .setWakeMode(C.WAKE_MODE_LOCAL)
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

    fun releasePlayer() {
        synchronized(this) {
            player?.let { exo ->
                runCatching {
                    exo.playWhenReady = false
                    exo.stop()
                    exo.clearMediaItems()
                    exo.release()
                }
            }
            player = null
            trackSelector = null
        }
    }

    /**
     * Stops audio playback, releases DSP/equalizer/controllers, dismisses all notifications,
     * stops all app services, and terminates the app process cleanly when swiped from Recents.
     */
    fun terminateAllProcesses(context: Context) {
        if (!isTerminating.compareAndSet(false, true)) return
        val appContext = context.applicationContext

        val hooksSnapshot = shutdownHooks.toList()
        shutdownHooks.clear()
        hooksSnapshot.forEach { hook -> runCatching { hook() } }

        releasePlayer()

        runCatching { NotificationManagerCompat.from(appContext).cancelAll() }
        runCatching { appContext.stopService(Intent(appContext, PlaybackService::class.java)) }
        runCatching { appContext.stopService(Intent(appContext, TaskClearService::class.java)) }

        Handler(Looper.getMainLooper()).postDelayed({
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }, 120L)
    }
}

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.notification_channel_playback)
            .setNotificationId(NOTIFICATION_ID)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_notification_music)
            }
        setMediaNotificationProvider(notificationProvider)

        val exoPlayer = SharedPlayerHolder.getOrCreatePlayer(this)
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val session = MediaSession.Builder(this, exoPlayer)
            .setId(SESSION_ID)
            .setSessionActivity(sessionActivity)
            .build()
        mediaSession = session
        addSession(session)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH_NOTIFICATION) {
            mediaSession?.let { session ->
                onUpdateNotification(session, session.player.playWhenReady)
            }
        }
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.let { session ->
            runCatching {
                session.player.playWhenReady = false
                session.player.stop()
                session.player.clearMediaItems()
            }
            runCatching { removeSession(session) }
            runCatching { session.release() }
        }
        mediaSession = null
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        runCatching { NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID) }
        stopSelf()
        SharedPlayerHolder.terminateAllProcesses(applicationContext)
    }

    override fun onDestroy() {
        mediaSession?.let { session ->
            runCatching { removeSession(session) }
            runCatching { session.release() }
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "pixel_player_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_REFRESH_NOTIFICATION = "com.example.ACTION_REFRESH_NOTIFICATION"
        private const val SESSION_ID = "pixel_player_session"
    }
}
