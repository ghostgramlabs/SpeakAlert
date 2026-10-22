package com.ghostgramlabs.speakalert.util

import android.content.Context
import com.ghostgramlabs.speakalert.data.model.ReminderEntity

/**
 * Whether reminders are set as alarm clocks ("Extra-reliable reminders"). Kept in
 * SharedPreferences rather than DataStore because alarms are scheduled synchronously,
 * including from boot and alarm receivers.
 */
internal object AlarmDeliveryPreferences {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("alarm_delivery", Context.MODE_PRIVATE)

    fun useAlarmClock(context: Context): Boolean = prefs(context).getBoolean("alarm_clock", false)

    fun setUseAlarmClock(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("alarm_clock", enabled).apply()
    }

    /**
     * When the next active reminder is due - the one the setting applies to next. Taken from
     * the reminders rather than AlarmManager.getNextAlarmClock(), which reports only the
     * soonest alarm of any app, so a wake-up alarm would hide ours.
     */
    fun nextReminderAt(reminders: List<ReminderEntity>, now: Long = System.currentTimeMillis()): Long? =
        reminders.asSequence()
            .filter { !it.isCompleted }
            .mapNotNull { reminder -> (reminder.snoozeUntil ?: reminder.nextTriggerAt).takeIf { it > now } }
            .minOrNull()
}
