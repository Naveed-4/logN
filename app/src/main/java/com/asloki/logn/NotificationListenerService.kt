package com.asloki.logn

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListenerService : NotificationListenerService() {

    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        logNotificationEvent(sbn, "POSTED")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        logNotificationEvent(sbn, "REMOVED")
    }

    private fun logNotificationEvent(sbn: StatusBarNotification, action: String) {
        val packageName = sbn.packageName
        if (sharedPreferences.getBoolean(packageName, false)) {
            val notification = sbn.notification
            val title = notification.extras.getString(Notification.EXTRA_TITLE)
            val text = notification.extras.getString(Notification.EXTRA_TEXT)

            FileUtils.logNotification(this, packageName, title, text, action)
        }
    }
}