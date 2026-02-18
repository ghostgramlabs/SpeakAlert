package com.ghostgramlabs.speakalert.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.ghostgramlabs.speakalert.MainActivity
import com.ghostgramlabs.speakalert.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "voice_reminder_channel"
        private const val TAG = "NotificationHelper"
        
        /**
         * Cancel the alert notification for a specific reminder.
         * Call this when user taps Done, Snooze, or Dismiss.
         */
        fun cancelAlertNotification(context: Context, reminderId: Long) {
            NotificationManagerCompat.from(context).cancel(reminderId.toInt())
            Log.d(TAG, "Cancelled alert notification for reminderId=$reminderId")
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "SpeakAlert Reminders"
        val descriptionText = "Notifications for SpeakAlert reminders"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created: $CHANNEL_ID")
    }

    fun showNotification(
        reminderId: Long, 
        title: String?, 
        message: String?,
        audioPath: String? = null,
        reminderText: String? = null,
        autoplayOnTap: Boolean = true
    ): Boolean {
        Log.d(TAG, "showNotification called for reminderId=$reminderId, title=$title")
        
        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                Log.e(TAG, "POST_NOTIFICATIONS permission not granted! Cannot show notification.")
                return false
            }
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminderId", reminderId)
            putExtra("autoplay", autoplayOnTap)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Play (starts playback service)
        val playIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "ACTION_PLAY"
            putExtra("reminderId", reminderId)
            if (audioPath != null) putExtra("audioPath", audioPath)
            if (reminderText != null) putExtra("reminderText", reminderText)
            putExtra("title", title ?: "Voice reminder")
        }
        val playPendingIntent = PendingIntent.getBroadcast(
             context, 
             reminderId.toInt() + 5000, 
             playIntent, 
             PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Done
        val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "ACTION_DONE"
            putExtra("reminderId", reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
             context, 
             reminderId.toInt() + 10000, 
             doneIntent, 
             PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("reminderId", reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 20000,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title ?: "Voice reminder")
            .setContentText(message ?: "Tap to play your reminder")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message ?: "Tap to play your reminder"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false) // Notification stays until user acts
            .setOngoing(false) // User can swipe to dismiss like normal notifications
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
        
        // Add Play button only if we have audio or text
        if (audioPath != null || reminderText != null) {
            builder.addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
        }
        
        builder.addAction(0, "Dismiss", donePendingIntent)
        builder.addAction(0, "Snooze", snoozePendingIntent)

        return try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
            Log.d(TAG, "Notification posted successfully for reminderId=$reminderId")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification", e)
            false
        }
    }
}
