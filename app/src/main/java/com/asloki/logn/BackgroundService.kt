package com.asloki.logn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
//import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BackgroundService : Service() {

    private val notificationId = 1
    private val channelId = "ForegroundServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Your background task logic here
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("LogN is running")
            .setContentText("Logging notifications in background")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}