package com.ghostgramlabs.speakalert.alarm

import android.app.NotificationManager
import android.content.Context
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Clears a reminder's alert notification when the user has dealt with the reminder inside the
 * app rather than from the notification itself. Cancelling from code does not fire the
 * notification's delete intent, so this never marks anything Done or Silenced by itself.
 */
interface ReminderAlertNotifications {
    /**
     * The reminder was deleted or marked done in the app. Stop its active playback and clear
     * its notification regardless of "Keep notification until done".
     */
    fun onResolved(reminderId: Long)

    /**
     * The user opened or listened to the reminder. The alert goes too, unless they chose
     * "Keep notification until done" - then only Done or Snooze should clear it.
     */
    suspend fun onSeen(reminderId: Long)

    /** Whether the reminder's alert notification is still on screen. */
    fun isShowing(reminderId: Long): Boolean

    object None : ReminderAlertNotifications {
        override fun onResolved(reminderId: Long) = Unit
        override suspend fun onSeen(reminderId: Long) = Unit
        override fun isShowing(reminderId: Long) = false
    }
}

class SystemReminderAlertNotifications(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : ReminderAlertNotifications {
    override fun onResolved(reminderId: Long) {
        ToneAlertPlayer.stop(reminderId)
        com.ghostgramlabs.speakalert.service.ReminderPlaybackService.stop(context, reminderId)
        NotificationHelper.cancelAlertNotification(context, reminderId)
    }

    override suspend fun onSeen(reminderId: Long) {
        if (settingsRepository.persistUntilDone.first()) return
        NotificationHelper.cancelAlertNotification(context, reminderId)
    }

    override fun isShowing(reminderId: Long): Boolean =
        context.getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.any { it.id == reminderId.toInt() } == true
}
