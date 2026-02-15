package com.ghostgramlabs.speakalert.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.util.FileLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Worker that reschedules all active reminders after a device reboot or time change.
 *
 * This runs via WorkManager OUTSIDE the BOOT_COMPLETED broadcast context.
 * 
 * CRITICAL ANDROID 15+ COMPLIANCE:
 * On Android 15+, BOOT_COMPLETED receivers (and their direct code paths) cannot start
 * restricted foreground service types (mediaPlayback).
 * 
 * To avoid crashing:
 * 1. Future reminders are scheduled normally (AlarmManager).
 * 2. Past/Missed reminders (that would fire immediately) are handled INLINE here.
 *    We do NOT schedule an immediate alarm for them, because that would trigger
 *    ReminderPlaybackService (FGS) and crash.
 *    Instead, we log them as missed, show a notification, and schedule the *next* occurrence.
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
            val missedRepository = app.container.missedReminderRepository
            val notificationHelper = NotificationHelper(applicationContext)

            val activeReminders = repository.getAllActiveReminders()
            FileLogger.log("BOOT_WORKER: Found ${activeReminders.size} active reminders")

            val now = System.currentTimeMillis()

            activeReminders.forEach { reminder ->
                val triggerTime = reminder.snoozeUntil ?: reminder.nextTriggerAt
                
                // STRICT BOOT CHECK:
                // If triggerTime is in the past (<= now), we CANNOT schedule it via AlarmManager
                // because it would fire immediately and trigger the FGS restriction.
                if (triggerTime > now) {
                    // Future reminder: Schedule normally. These fire via AlarmManager
                    // in their own broadcast context (NOT BOOT_COMPLETED), so they
                    // can safely start foreground services and autoplay.
                    scheduler.schedule(reminder)
                } else {
                    // Past/Missed/Immediate reminder: Handle INLINE to avoid FGS start frequency limit/restriction
                    FileLogger.log("BOOT_WORKER: Reminder ${reminder.id} is past due ($triggerTime <= $now). Handling inline as missed.")
                    
                    // 1. Add to Missed Inbox
                    val missedEntry = MissedReminderEntity(
                        reminderId = reminder.id,
                        title = reminder.title ?: "SpeakAlert",
                        scheduledTime = triggerTime,
                        detectedTime = now,
                        reminderText = reminder.reminderText
                    )
                    missedRepository.insertMissedReminder(missedEntry)
                    
                    // 2. Show Missed Notification (No Service Start)
                    val dateTimeStr = com.ghostgramlabs.speakalert.util.DateUtils.formatDateTime(triggerTime)
                    val textContent = reminder.reminderText?.let { "\n$it" } ?: ""
                    val notificationMessage = "Scheduled: $dateTimeStr$textContent"
                    
                    notificationHelper.showNotification(
                        reminder.id,
                        "Missed: ${reminder.title ?: "SpeakAlert"}",
                        notificationMessage,
                        audioPath = reminder.audioPath,
                        reminderText = reminder.reminderText
                    )
                    
                    // 3. Schedule Next Occurrence / Mark Complete
                    var updatedReminder = reminder.copy(
                        lastFiredAt = now,
                        snoozeUntil = null
                    )
                    
                    if (reminder.recurrenceType != RecurrenceType.NONE) {
                        // Advance recurrence
                        updatedReminder = RecurrenceUtils.updateForNextOccurrence(updatedReminder)
                        val nextTrigger = RecurrenceUtils.computeNextTrigger(updatedReminder, now)
                        
                        if (nextTrigger != null) {
                            updatedReminder = updatedReminder.copy(nextTriggerAt = nextTrigger)
                            scheduler.schedule(updatedReminder)
                            FileLogger.log("BOOT_WORKER: Scheduled next recurrence at $nextTrigger")
                        } else {
                            // Recurrence ended
                            updatedReminder = updatedReminder.copy(isCompleted = true, completedAt = now)
                            scheduler.cancel(updatedReminder)
                            FileLogger.log("BOOT_WORKER: Recurrence ended, marked completed")
                        }
                    } else {
                        // One-time reminder
                        updatedReminder = updatedReminder.copy(isCompleted = true, completedAt = now)
                        scheduler.cancel(updatedReminder)
                        FileLogger.log("BOOT_WORKER: One-time reminder marked completed")
                    }
                    
                    repository.updateReminder(updatedReminder)
                }
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
