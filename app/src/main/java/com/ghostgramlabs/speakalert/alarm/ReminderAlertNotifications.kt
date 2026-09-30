package com.ghostgramlabs.speakalert.alarm

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
     * The reminder was deleted or marked done in the app. Its alert goes regardless of any
     * setting: "Keep notification until done" pins it, so nothing else could clear it.
     */
    fun onResolved(reminderId: Long)

    /**
     * The user opened or listened to the reminder. The alert goes too, unless they chose
     * "Keep notification until done" - then only Done or Snooze should clear it.
     */
    suspend fun onSeen(reminderId: Long)

    object None : ReminderAlertNotifications {
        override fun onResolved(reminderId: Long) = Unit
        override suspend fun onSeen(reminderId: Long) = Unit
    }
}

class SystemReminderAlertNotifications(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : ReminderAlertNotifications {
    override fun onResolved(reminderId: Long) {
        NotificationHelper.cancelAlertNotification(context, reminderId)
    }

    override suspend fun onSeen(reminderId: Long) {
        if (settingsRepository.persistUntilDone.first()) return
        NotificationHelper.cancelAlertNotification(context, reminderId)
    }
}
