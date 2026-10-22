package com.ghostgramlabs.speakalert.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ghostgramlabs.speakalert.MainActivity
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.util.AlarmDeliveryPreferences

/** Distinguishes the alarm-clock "show" intent from the notification's tap intent for the same reminder. */
private const val ACTION_SHOW_ALARM = "com.ghostgramlabs.speakalert.action.SHOW_ALARM"

/**
 * Sets [operation] as an exact alarm, or as an alarm clock when the user turned that on in
 * Settings. Exact alarms (setExactAndAllowWhileIdle) can be held back by Android's app standby
 * and by phone makers' battery savers until the app is next opened - a reminder reported an
 * hour late on an Infinix phone. Alarm clocks are exempt from that, but the system shows the
 * next one as a status-bar alarm icon, so it is opt-in. Tapping that icon opens the reminder.
 * Without exact-alarm permission there is nothing better than an inexact alarm.
 */
internal fun scheduleReminderAlarm(
    context: Context,
    reminderId: Long,
    triggerAt: Long,
    operation: PendingIntent
) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        Log.w("VoiceReminder", "Cannot schedule exact alarm, falling back to inexact")
        com.ghostgramlabs.speakalert.util.FileLogger.log("SCHEDULER: No exact alarm permission, using INEXACT")
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        return
    }
    if (!AlarmDeliveryPreferences.useAlarmClock(context)) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        com.ghostgramlabs.speakalert.util.FileLogger.log("SCHEDULER: Set EXACT alarm for id=$reminderId")
        return
    }
    val showIntent = PendingIntent.getActivity(
        context,
        reminderId.toInt(),
        Intent(context, MainActivity::class.java).apply {
            action = ACTION_SHOW_ALARM
            putExtra("reminderId", reminderId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation)
    com.ghostgramlabs.speakalert.util.FileLogger.log("SCHEDULER: Set ALARM CLOCK for id=$reminderId")
}

/**
 * Switches alarm-clock delivery, then sets every future alarm again so the change applies now
 * rather than one occurrence at a time. Past-due reminders are left alone: setting them again
 * would ring them immediately.
 */
suspend fun setAlarmClockDelivery(
    context: Context,
    enabled: Boolean,
    repository: com.ghostgramlabs.speakalert.data.repository.ReminderRepository,
    scheduler: AlarmScheduler
) {
    AlarmDeliveryPreferences.setUseAlarmClock(context, enabled)
    val now = System.currentTimeMillis()
    repository.getAllActiveReminders().forEach { reminder ->
        if ((reminder.snoozeUntil ?: reminder.nextTriggerAt) > now) {
            scheduler.schedule(reminder)
        }
        reminder.pendingFollowUpAt?.takeIf { it > now }?.let { followUpAt ->
            FollowUpAlarmScheduler.schedule(context, reminder.id, followUpAt)
        }
    }
}

interface AlarmScheduler {
    fun schedule(reminder: ReminderEntity, isBootReschedule: Boolean = false)
    fun cancel(reminder: ReminderEntity)
}

class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(reminder: ReminderEntity, isBootReschedule: Boolean) {
        // SNOOZE PRIORITY: If snoozeUntil is set, use it instead of nextTriggerAt.
        // This is TEMPORARY - snoozeUntil is cleared when the alarm fires.
        // After snooze fires, recurrence resumes from nextTriggerAt (unchanged).
        val triggerTime = reminder.snoozeUntil ?: reminder.nextTriggerAt
        
        com.ghostgramlabs.speakalert.util.FileLogger.log("SCHEDULER: Scheduling alarm id=${reminder.id} (boot=$isBootReschedule) for $triggerTime (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(triggerTime))})")
        
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra("reminderId", reminder.id)
            putExtra("isBootReschedule", isBootReschedule)
            putExtra("fireTime", triggerTime)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("VoiceReminder", "Scheduling alarm for id=${reminder.id} at $triggerTime")

        try {
            scheduleReminderAlarm(context, reminder.id, triggerTime, pendingIntent)
            com.ghostgramlabs.speakalert.util.FileLogger.log("SCHEDULER: Alarm scheduled successfully")
        } catch (e: SecurityException) {
            Log.e("VoiceReminder", "SecurityException scheduling alarm", e)
            com.ghostgramlabs.speakalert.util.FileLogger.logError("SCHEDULER", "SecurityException", e)
        }
    }

    override fun cancel(reminder: ReminderEntity) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra("reminderId", reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        FollowUpAlarmScheduler.cancel(context, reminder.id)
    }
}
