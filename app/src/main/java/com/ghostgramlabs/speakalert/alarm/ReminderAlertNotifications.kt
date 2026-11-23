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

    /**
     * The reminder was edited and saved. Its alert was about the occurrence the edit replaced, so
     * it goes even with "Keep notification until done".
     */
    fun onRescheduled(reminderId: Long)

    /**
     * The app came to the front, so the user has seen what is on Home. Treated like [onSeen] for
     * each alert still showing, except one whose sound is still playing: its notification holds
     * the Silence and Stop controls.
     */
    suspend fun onAppOpened(reminderIds: Collection<Long>)

    /** Whether the reminder's alert notification is still on screen. */
    fun isShowing(reminderId: Long): Boolean

    object None : ReminderAlertNotifications {
        override fun onResolved(reminderId: Long) = Unit
        override suspend fun onSeen(reminderId: Long) = Unit
        override fun onRescheduled(reminderId: Long) = Unit
        override suspend fun onAppOpened(reminderIds: Collection<Long>) = Unit
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

    override fun onRescheduled(reminderId: Long) {
        NotificationHelper.cancelAlertNotification(context, reminderId)
    }

    override suspend fun onAppOpened(reminderIds: Collection<Long>) {
        if (settingsRepository.persistUntilDone.first()) return
        val showing = activeIds()
        reminderIds
            .filter { it.toInt() in showing && !isSounding(it) }
            .forEach { NotificationHelper.cancelAlertNotification(context, it) }
    }

    override fun isShowing(reminderId: Long): Boolean = reminderId.toInt() in activeIds()

    private fun activeIds(): Set<Int> =
        context.getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.mapTo(HashSet()) { it.id }
            .orEmpty()

    private fun isSounding(reminderId: Long): Boolean =
        ToneAlertPlayer.isPlayingFor(reminderId) ||
            com.ghostgramlabs.speakalert.service.ReminderPlaybackService.playingReminderId == reminderId
}
