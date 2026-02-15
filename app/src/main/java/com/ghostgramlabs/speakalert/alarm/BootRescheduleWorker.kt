package com.ghostgramlabs.speakalert.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.util.FileLogger

/**
 * Worker that reschedules all active reminders after a device reboot or time change.
 *
 * This runs via WorkManager OUTSIDE the BOOT_COMPLETED broadcast context, which is
 * critical for Android 15+ compliance. Starting a mediaPlayback foreground service
 * from a BOOT_COMPLETED receiver is restricted on Android 15+, so we use WorkManager
 * to break that static code path.
 */
class BootRescheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        FileLogger.log("BOOT_WORKER: Starting alarm rescheduling")
        return try {
            val app = applicationContext as VoiceReminderApp
            val repository = app.container.reminderRepository
            val scheduler = app.container.alarmScheduler

            val activeReminders = repository.getAllActiveReminders()
            FileLogger.log("BOOT_WORKER: Found ${activeReminders.size} active reminders to reschedule")

            activeReminders.forEach { reminder ->
                scheduler.schedule(reminder, isBootReschedule = true)
            }

            FileLogger.log("BOOT_WORKER: Alarm rescheduling complete")
            Result.success()
        } catch (e: Exception) {
            FileLogger.logError("BOOT_WORKER", "Error rescheduling alarms", e)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "boot_reschedule_alarms"
    }
}
