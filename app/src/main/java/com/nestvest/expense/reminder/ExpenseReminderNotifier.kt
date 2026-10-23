package com.nestvest.expense.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.nestvest.MainActivity
import com.nestvest.R

class ExpenseReminderNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        })
    }

    fun available(): Boolean {
        createChannel()
        return (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun post() {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(OPEN_CAPTURE, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(NOTIFICATION_ID, android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_nestvest)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(android.app.Notification.VISIBILITY_PRIVATE)
            .build())
    }

    fun dismiss() { manager.cancel(NOTIFICATION_ID) }

    companion object {
        const val CHANNEL = "daily-expense"
        const val NOTIFICATION_ID = 2030
        const val OPEN_CAPTURE = "openExpenseCapture"
    }
}
