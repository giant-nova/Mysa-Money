package com.giantnovadevs.mysamoney.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.giantnovadevs.mysamoney.MainActivity
import com.giantnovadevs.mysamoney.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "recurring_expense_channel"
        const val NOTIFICATION_ID_PREFIX = 1001 // Base ID for our notifications
    }

    /**
     * Creates the Notification Channel. This is required on Android 8.0+
     * We'll call this when the application starts.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recurring Expenses"
            val descriptionText = "Notifications for automatically logged expenses"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds and displays a single notification.
     */
    fun showNotification(title: String, message: String, notificationId: Int) {
        // Create an intent that opens the app when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // Make sure you have a simple icon at 'R.drawable.ic_notification'
            // For now, it will use the default app icon.
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Set the tap action
            .setAutoCancel(true) // Dismiss notification on tap

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If we don't have permission, just return and do nothing
            return
        }
        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            // notificationId needs to be unique for each notification
            notify(notificationId, builder.build())
        }
    }
}