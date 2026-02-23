package com.ghostgramlabs.speakalert.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
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
        const val TONE_ONLY_CHANNEL_ID = "voice_reminder_tone_only_channel"
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
        val alarmToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val normalChannel = NotificationChannel(
            CHANNEL_ID,
            "SpeakAlert Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for SpeakAlert reminders"
            enableVibration(true)
        }
        val toneOnlyChannel = NotificationChannel(
            TONE_ONLY_CHANNEL_ID,
            "SpeakAlert Tone-Only Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "System alarm tone alerts for Tone-only mode"
            enableVibration(true)
            setSound(
                alarmToneUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(normalChannel)
        notificationManager.createNotificationChannel(toneOnlyChannel)
        Log.d(TAG, "Notification channels created: $CHANNEL_ID, $TONE_ONLY_CHANNEL_ID")
    }

    fun showNotification(
        reminderId: Long, 
        title: String?, 
        message: String?,
        audioPath: String? = null,
        reminderText: String? = null,
        autoplayOnTap: Boolean = true,
        toneOnlyMode: Boolean = false
    ): Boolean {
        Log.d(TAG, "showNotification called for reminderId=$reminderId, title=$title")
        val channelId = if (toneOnlyMode) TONE_ONLY_CHANNEL_ID else CHANNEL_ID
        val alarmToneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        
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
            putExtra("autoplay", autoplayOnTap && !toneOnlyMode)
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

        // Delete Intent: fires when notification is swiped away by user
        val dismissIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "ACTION_DISMISS"
            putExtra("reminderId", reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 30000,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title ?: "Voice reminder")
            .setContentText(message ?: "Tap to play your reminder")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message ?: "Tap to play your reminder"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent) // Handle notification swipe-dismiss
            .setAutoCancel(false) // Notification stays until user acts
            .setOngoing(false) // User can swipe to dismiss like normal notifications
            .setDefaults(if (toneOnlyMode) {
                NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE
            } else {
                NotificationCompat.DEFAULT_ALL
            }) // Sound, vibration, lights

        if (toneOnlyMode && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(alarmToneUri)
        }
        
        // Add Play button if we have audio or text.
        if (audioPath != null || reminderText != null) {
            val playLabel = if (!audioPath.isNullOrBlank()) "Play voice" else "Play TTS"
            builder.addAction(android.R.drawable.ic_media_play, playLabel, playPendingIntent)
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
