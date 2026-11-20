package com.ghostgramlabs.speakalert.alarm

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** What the user changed on a reminder notification category, worst first. */
enum class AlertChannelProblem {
    /** Category switched off: the reminder still rings, but no notification appears at all. */
    BLOCKED,
    /** Below "urgent": the notification lands in the shade without popping up on screen. */
    NO_POPUP,
    /** Kept off the lock screen, so the reminder text cannot be read there. */
    HIDDEN_ON_LOCK_SCREEN
}

data class AlertChannelIssue(
    val channelId: String,
    val channelName: String,
    val problem: AlertChannelProblem
)

/**
 * Checks the notification categories (channels) a reminder alert is posted to. Android lets
 * the user switch each one off separately, and phones such as Realme, OPPO and Xiaomi do so on
 * their own. A tone-only alert then still rings, because the tone is not part of the
 * notification, but nothing shows: no banner, nothing on the lock screen, no status-bar icon.
 * The app gets no error for this, so the Reminder check has to look.
 */
object AlertChannelHealth {

    /** The channels [NotificationHelper.showNotification] can pick with these settings. */
    fun channelsInUse(
        toneOnlyMode: Boolean,
        dndBypassEnabled: Boolean,
        fullScreenAlertEnabled: Boolean
    ): List<String> = buildList {
        if (toneOnlyMode) {
            add(NotificationHelper.TONE_ONLY_CHANNEL_ID)
            if (dndBypassEnabled) add(NotificationHelper.TONE_ONLY_DND_BYPASS_CHANNEL_ID)
        } else {
            add(NotificationHelper.CHANNEL_ID)
            if (dndBypassEnabled) add(NotificationHelper.DND_BYPASS_CHANNEL_ID)
            if (fullScreenAlertEnabled) {
                add(NotificationHelper.SILENT_CHANNEL_ID)
                if (dndBypassEnabled) add(NotificationHelper.SILENT_DND_BYPASS_CHANNEL_ID)
            }
        }
    }

    /** The problem a channel with this importance and lock-screen visibility has, if any. */
    fun problemOf(importance: Int, lockscreenVisibility: Int): AlertChannelProblem? = when {
        importance == NotificationManager.IMPORTANCE_NONE -> AlertChannelProblem.BLOCKED
        // UNSPECIFIED (-1000) means the system has not settled it; do not warn about that.
        importance != NotificationManager.IMPORTANCE_UNSPECIFIED &&
            importance < NotificationManager.IMPORTANCE_HIGH -> AlertChannelProblem.NO_POPUP
        lockscreenVisibility == Notification.VISIBILITY_SECRET -> AlertChannelProblem.HIDDEN_ON_LOCK_SCREEN
        else -> null
    }

    /** The worst problem among the channels in use, or null when they are all fine. */
    fun check(
        context: Context,
        toneOnlyMode: Boolean,
        dndBypassEnabled: Boolean,
        fullScreenAlertEnabled: Boolean
    ): AlertChannelIssue? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val manager = context.getSystemService(NotificationManager::class.java) ?: return null
        return channelsInUse(toneOnlyMode, dndBypassEnabled, fullScreenAlertEnabled)
            // DND bypass channels exist only once DND access is granted.
            .mapNotNull { id -> manager.getNotificationChannel(id) }
            .mapNotNull { channel ->
                problemOf(channel.importance, channel.lockscreenVisibility)?.let {
                    AlertChannelIssue(channel.id, channel.name.toString(), it)
                }
            }
            .minByOrNull { it.problem.ordinal }
    }

    /** The problem of one channel, for logging at the moment an alert is posted to it. */
    fun problemOf(context: Context, channelId: String): AlertChannelProblem? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val channel = context.getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(channelId) ?: return null
        return problemOf(channel.importance, channel.lockscreenVisibility)
    }
}
