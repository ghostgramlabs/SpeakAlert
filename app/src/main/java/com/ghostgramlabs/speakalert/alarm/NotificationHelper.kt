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
import com.ghostgramlabs.speakalert.ui.alert.ReminderAlertActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.FullScreenIntentSupport

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "voice_reminder_channel"
        const val DND_BYPASS_CHANNEL_ID = "voice_reminder_dnd_bypass_channel_v2"
        const val TONE_ONLY_CHANNEL_ID = "voice_reminder_tone_only_channel"
        const val TONE_ONLY_DND_BYPASS_CHANNEL_ID = "voice_reminder_tone_only_dnd_bypass_channel_v2"
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
        createNotificationChannel(dndBypassEnabled = true)
    }

    fun refreshChannels(dndBypassEnabled: Boolean = true) {
        createNotificationChannel(dndBypassEnabled)
    }

    private fun createNotificationChannel(dndBypassEnabled: Boolean) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canBypassDnd = dndBypassEnabled && notificationManager.isNotificationPolicyAccessGranted

        val normalChannel = NotificationChannel(
            CHANNEL_ID,
            "$APP_DISPLAY_NAME Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for $APP_DISPLAY_NAME reminders"
            enableVibration(true)
            setBypassDnd(false)
        }
        val toneOnlyChannel = NotificationChannel(
            TONE_ONLY_CHANNEL_ID,
            "$APP_DISPLAY_NAME Tone-Only Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Tone-only notifications with vibration only; playback tone is handled by $APP_DISPLAY_NAME"
            enableVibration(true)
            setSound(null, null)
            setBypassDnd(false)
        }
        notificationManager.createNotificationChannel(normalChannel)
        notificationManager.createNotificationChannel(toneOnlyChannel)
        if (canBypassDnd) {
            val bypassChannel = NotificationChannel(
                DND_BYPASS_CHANNEL_ID,
                "$APP_DISPLAY_NAME Reminders (DND bypass)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminder alarms that can alert during Do Not Disturb when Android access is granted"
                enableVibration(true)
                setBypassDnd(true)
            }
            val toneOnlyBypassChannel = NotificationChannel(
                TONE_ONLY_DND_BYPASS_CHANNEL_ID,
                "$APP_DISPLAY_NAME Tone-Only Alerts (DND bypass)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tone-only reminders that can alert during Do Not Disturb when Android access is granted"
                enableVibration(true)
                setSound(null, null)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(bypassChannel)
            notificationManager.createNotificationChannel(toneOnlyBypassChannel)
        }
        Log.d(TAG, "Notification channels created: $CHANNEL_ID, $DND_BYPASS_CHANNEL_ID, $TONE_ONLY_CHANNEL_ID, $TONE_ONLY_DND_BYPASS_CHANNEL_ID")
    }

    fun showNotification(
        reminderId: Long, 
        title: String?, 
        message: String?,
        audioPath: String? = null,
        reminderText: String? = null,
        autoplayOnTap: Boolean = true,
        toneOnlyMode: Boolean = false,
        useFullScreenAlert: Boolean = false,
        isFollowUpAlert: Boolean = false,
        dndBypassEnabled: Boolean = true,
        silentAlert: Boolean = false,
        // True when a reminder sound/tone is actively playing, so we offer "Silence" instead of
        // "Play". Keeps the one-tap Play button for the no-autoplay case (no regression).
        playingSound: Boolean = false,
        // When true, the notification is ongoing (pinned) and a swipe no longer completes the
        // reminder — it only silences it, leaving the reminder pending until Done/Snooze.
        persistUntilDone: Boolean = false
    ): Boolean {
        Log.d(TAG, "showNotification called for reminderId=$reminderId, title=$title")
        createNotificationChannel(dndBypassEnabled)
        val canBypassDnd = dndBypassEnabled &&
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .isNotificationPolicyAccessGranted
        val channelId = when {
            toneOnlyMode && canBypassDnd -> TONE_ONLY_DND_BYPASS_CHANNEL_ID
            toneOnlyMode -> TONE_ONLY_CHANNEL_ID
            canBypassDnd -> DND_BYPASS_CHANNEL_ID
            else -> CHANNEL_ID
        }
        
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

        val fullScreenPendingIntent = if (useFullScreenAlert && FullScreenIntentSupport.canUseFullScreenIntent(context)) {
            val fullScreenIntent = Intent(context, ReminderAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminderId", reminderId)
                putExtra(ReminderAlertActivity.EXTRA_ALERT_TITLE, title)
                putExtra(ReminderAlertActivity.EXTRA_ALERT_MESSAGE, message)
                putExtra(ReminderAlertActivity.EXTRA_PLAYBACK_AUDIO_PATH, audioPath)
                putExtra(ReminderAlertActivity.EXTRA_PLAYBACK_TEXT, reminderText)
                putExtra(ReminderAlertActivity.EXTRA_IS_FOLLOW_UP_ALERT, isFollowUpAlert)
            }
            PendingIntent.getActivity(
                context,
                reminderId.toInt() + 40000,
                fullScreenIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            if (useFullScreenAlert) {
                Log.w(TAG, "Full-screen alert requested but access is not granted; falling back to standard notification.")
            }
            null
        }

        // Action: Play (starts playback service) — used when nothing is currently playing.
        val playIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "ACTION_PLAY"
            putExtra("reminderId", reminderId)
            if (audioPath != null) putExtra("audioPath", audioPath)
            if (reminderText != null) putExtra("reminderText", reminderText)
            putExtra("title", title ?: context.getString(R.string.notif_default_title))
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

        // Delete Intent: fires when notification is swiped away by user.
        // When persistent, a swipe only silences (keeps the reminder pending); otherwise it
        // completes/advances the reminder as before.
        val dismissIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = if (persistUntilDone) "ACTION_SILENCE" else "ACTION_DISMISS"
            putExtra("reminderId", reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 30000,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Silence — stop the alarm sound but keep the reminder pending.
        val silenceIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = "ACTION_SILENCE"
            putExtra("reminderId", reminderId)
        }
        val silencePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 50000,
            silenceIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: context.getString(R.string.notif_default_title))
            .setContentText(message ?: context.getString(R.string.notif_default_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message ?: context.getString(R.string.notif_default_text)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLocalOnly(false)
            .extend(
                NotificationCompat.WearableExtender()
                    .setDismissalId("reminder_$reminderId")
            )
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent) // Handle notification swipe-dismiss
            .setAutoCancel(false) // Notification stays until user acts
            .setOngoing(persistUntilDone) // When persistent, pin it so it isn't swiped away
            // setSilent(true) below suppresses sound, vibration, and lights on Android Q+,
            // so the silent-path defaults here are effectively informational.
            .setDefaults(if (toneOnlyMode || silentAlert) {
                NotificationCompat.DEFAULT_LIGHTS
            } else {
                NotificationCompat.DEFAULT_ALL
            })

        if (silentAlert) {
            builder.setSilent(true)
        }

        if (fullScreenPendingIntent != null) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        // Pop-up actions (phones show up to three). First slot adapts to context, then Done + Snooze.
        //  - "Done" was previously (confusingly) labelled "Dismiss" while still completing the task.
        //  - "Silence" stops the alarm sound but leaves the reminder so the user can decide later.
        //  - When nothing is playing, keep the one-tap "Play" so voice reminders are still
        //    audible from the notification without opening the app (no regression).
        val canPlayOnDemand = !audioPath.isNullOrBlank() || !reminderText.isNullOrBlank()
        when {
            playingSound ->
                builder.addAction(android.R.drawable.ic_lock_silent_mode, context.getString(R.string.notif_action_silence), silencePendingIntent)
            canPlayOnDemand -> {
                val playLabel = if (!audioPath.isNullOrBlank()) {
                    context.getString(R.string.notif_action_play)
                } else {
                    context.getString(R.string.notif_action_speak)
                }
                builder.addAction(android.R.drawable.ic_media_play, playLabel, playPendingIntent)
            }
        }
        builder.addAction(android.R.drawable.checkbox_on_background, context.getString(R.string.notif_action_done), donePendingIntent)
        builder.addAction(android.R.drawable.ic_lock_idle_alarm, context.getString(R.string.notif_action_snooze), snoozePendingIntent)

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

