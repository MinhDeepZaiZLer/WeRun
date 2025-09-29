package com.example.werun.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Service này cần thiết để có quyền truy cập MediaSession
 * Người dùng cần enable trong Settings > Apps > Special app access > Notification access
 */
class NotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Không cần xử lý gì, chỉ cần service chạy để có quyền truy cập MediaSession
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Không cần xử lý
    }
}