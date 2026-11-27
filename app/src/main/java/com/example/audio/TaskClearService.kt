package com.example.audio

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Lightweight lifecycle watcher service that guarantees [onTaskRemoved] is delivered
 * when the user swipes Pixel Player away from Android Recents — even if audio playback
 * was never started during the session.
 */
class TaskClearService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        SharedPlayerHolder.terminateAllProcesses(applicationContext)
        super.onTaskRemoved(rootIntent)
    }
}
