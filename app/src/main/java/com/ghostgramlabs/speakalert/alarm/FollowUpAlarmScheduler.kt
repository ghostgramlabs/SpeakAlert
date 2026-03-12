package com.ghostgramlabs.speakalert.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object FollowUpAlarmScheduler {

    private const val REQUEST_CODE_OFFSET = 700_000

    fun schedule(context: Context, reminderId: Long, triggerAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(context, reminderId, triggerAt)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, reminderId, 0L))
    }

    private fun pendingIntent(context: Context, reminderId: Long, triggerAt: Long): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_OFFSET + reminderId.toInt(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                putExtra("reminderId", reminderId)
                putExtra("fireTime", triggerAt)
                putExtra("isFollowUpAlarm", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
