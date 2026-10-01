package com.rubicon.demo

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Runs in `:remote`. RubiconApp creates the SDK there too, as every process runs Application.onCreate;
 * the SDK sees it is not the main process and does nothing: no file, no request (Disk 2d).
 */
class RemoteService : Service() {
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val analytics = demo.analytics
        analytics.track("From Remote Process")
        analytics.flush()
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
