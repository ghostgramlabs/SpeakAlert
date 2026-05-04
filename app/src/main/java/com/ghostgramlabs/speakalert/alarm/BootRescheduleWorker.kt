package com.ghostgramlabs.speakalert.alarm

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.MissedPolicy
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.util.FileLogger
import com.ghostgramlabs.speakalert.util.isDefaultAppDisplayName
import com.ghostgramlabs.speakalert.widget.SpeakAlertWidgetUpdater
import kotlinx.coroutines.flow.first

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
 *    Instead, we log them as missed and schedule the *next* occurrence.
 *    Boot-triggered reschedules keep those items in the missed inbox without
 *    surfacing a notification while the device is still coming back up.
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
            val settingsRepository = app.container.settingsRepository
            val scheduler = app.container.alarmScheduler
            val missedRepository = app.container.missedReminderRepository
            val notificationHelper = NotificationHelper(applicationContext)
            val settingsDefaultPolicy = parseMissedPolicy(settingsRepository.defaultMissedPolicy.first())
            val toneOnlyMode = settingsRepository.toneOnlyMode.first()
            val toneOnlyAlertToneUri = settingsRepository.toneOnlyAlertToneUri.first()
            val dndBypassEnabled = settingsRepository.dndBypassEnabled.first()
            val loopTimeoutMinutes = settingsRepository.loopTimeoutMinutes.first()

            val activeReminders = repository.getAllActiveReminders()
            FileLogger.log("BOOT_WORKER: Found ${activeReminders.size} active reminders")

            val now = System.currentTimeMillis()
            val triggerAction = inputData.getString(KEY_TRIGGER_ACTION)
            val suppressRestartNotifications = triggerAction == Intent.ACTION_BOOT_COMPLETED

            activeReminders.forEach { reminder ->
                val triggerTime = reminder.snoozeUntil ?: reminder.nextTriggerAt
                val followUpAt = reminder.pendingFollowUpAt
                var persistedReminder = reminder
                
                // STRICT BOOT CHECK:
                // If triggerTime is in the past (<= now), we CANNOT schedule it via AlarmManager
                // because it would fire immediately and trigger the FGS restriction.
                if (triggerTime > now) {
                    // Future reminder: Schedule normally. These fire via AlarmManager
                    // in their own broadcast context (NOT BOOT_COMPLETED), so they
                    // can safely start foreground services and autoplay.
                    scheduler.schedule(reminder, isBootReschedule = true)
                } else {
                    // Past/Missed/Immediate reminder: Handle INLINE to avoid FGS start frequency limit/restriction
                    FileLogger.log("BOOT_WORKER: Reminder ${reminder.id} is past due ($triggerTime <= $now). Handling inline as missed.")

                    val recurrenceModel = RecurrenceUtils.fromJson(reminder.recurrenceType, reminder.recurrenceJson)
                    val recurrencePolicy = recurrenceModel?.missedPolicy?.takeIf {
                        it != MissedPolicy.SKIP_TO_NEXT || settingsDefaultPolicy == MissedPolicy.SKIP_TO_NEXT
                    }
                    val missedPolicy = recurrencePolicy ?: reminder.missedPolicy.takeIf {
                        it != MissedPolicy.SKIP_TO_NEXT || settingsDefaultPolicy == MissedPolicy.SKIP_TO_NEXT
                    } ?: settingsDefaultPolicy
                    val shouldShowMissedNotification =
                        reminder.recurrenceType == RecurrenceType.NONE || missedPolicy == MissedPolicy.FIRE_ON_RESUME
                     
                    // 1. Add to Missed Inbox
                    val missedEntry = MissedReminderEntity(
                        reminderId = reminder.id,
                        title = buildMissedDisplayTitle(reminder.title, reminder.reminderText),
                        scheduledTime = triggerTime,
                        detectedTime = now,
                        reminderText = reminder.reminderText
                    )
                    missedRepository.insertMissedReminder(missedEntry)
                     
                    // 2. Show Missed Notification (No Service Start)
                    if (shouldShowMissedNotification && !suppressRestartNotifications) {
                        val dateTimeStr = com.ghostgramlabs.speakalert.util.DateUtils.formatDateTime(triggerTime)
                        val textContent = reminder.reminderText?.let { "\n$it" } ?: ""
                        val notificationMessage = "Scheduled: $dateTimeStr$textContent"

                        val notificationShown = notificationHelper.showNotification(
                            reminder.id,
                            "Missed: ${buildMissedDisplayTitle(reminder.title, reminder.reminderText)}",
                            notificationMessage,
                            audioPath = reminder.audioPath,
                            reminderText = reminder.reminderText,
                            autoplayOnTap = false,
                            toneOnlyMode = toneOnlyMode,
                            dndBypassEnabled = dndBypassEnabled
                        )
                        if (toneOnlyMode && notificationShown) {
                            ToneAlertPlayer.start(applicationContext, loopTimeoutMinutes, toneOnlyAlertToneUri, dndBypass = dndBypassEnabled)
                        }
                    } else if (shouldShowMissedNotification) {
                        FileLogger.log("BOOT_WORKER: Suppressed missed notification for ${reminder.id} during device restart")
                    } else {
                        FileLogger.log("BOOT_WORKER: MissedPolicy SKIP_TO_NEXT - no missed notification for ${reminder.id}")
                    }
                     
                    // 3. Schedule Next Occurrence / Mark Complete
                    var updatedReminder = reminder.copy(
                        lastFiredAt = now,
                        snoozeUntil = null,
                        pendingFollowUpAt = followUpAt?.takeIf { it > now }
                    )
                    
                    if (reminder.recurrenceType != RecurrenceType.NONE) {
                        // Advance recurrence
                        updatedReminder = RecurrenceUtils.updateForNextOccurrence(updatedReminder)
                        val nextTrigger = RecurrenceUtils.computeNextTrigger(updatedReminder, now)
                        
                        if (nextTrigger != null) {
                            updatedReminder = updatedReminder.copy(nextTriggerAt = nextTrigger)
                            scheduler.schedule(updatedReminder, isBootReschedule = true)
                            FileLogger.log("BOOT_WORKER: Scheduled next recurrence at $nextTrigger")
                        } else {
                            // Recurrence ended
                            updatedReminder = updatedReminder.copy(isCompleted = true, completedAt = now)
                            scheduler.cancel(updatedReminder)
                            FileLogger.log("BOOT_WORKER: Recurrence ended, marked completed")
                        }
                    } else {
                        // One-time reminder remains active until explicit Done/Dismiss.
                        // Clear any stale pending alarm so we don't duplicate notifications.
                        scheduler.cancel(updatedReminder)
                        FileLogger.log("BOOT_WORKER: One-time reminder kept active (awaiting user action)")
                    }
                    
                    persistedReminder = updatedReminder
                    repository.updateReminder(persistedReminder)
                }

                if (followUpAt != null) {
                    if (followUpAt > now) {
                        FollowUpAlarmScheduler.schedule(applicationContext, reminder.id, followUpAt)
                    } else {
                        if (persistedReminder.pendingFollowUpAt != null) {
                            persistedReminder = persistedReminder.copy(pendingFollowUpAt = null)
                            repository.updateReminder(persistedReminder)
                        }
                        if (!suppressRestartNotifications) {
                            val followUpPayload = buildReminderAlertPayload(
                                reminder = persistedReminder,
                                isFollowUpTrigger = true,
                                hasPlayableAudio = false,
                                hasAudioConfigured = false
                            )
                            val notificationShown = notificationHelper.showNotification(
                                reminder.id,
                                followUpPayload.title,
                                followUpPayload.message,
                                audioPath = followUpPayload.playbackAudioPath,
                                reminderText = followUpPayload.playbackText,
                                autoplayOnTap = false,
                                toneOnlyMode = toneOnlyMode,
                                isFollowUpAlert = true,
                                dndBypassEnabled = dndBypassEnabled
                            )
                            if (toneOnlyMode && notificationShown) {
                                ToneAlertPlayer.start(applicationContext, loopTimeoutMinutes, toneOnlyAlertToneUri, dndBypass = dndBypassEnabled)
                            }
                        } else {
                            FileLogger.log("BOOT_WORKER: Suppressed follow-up notification for ${reminder.id} during device restart")
                        }
                    }
                }
            }

            FileLogger.log("BOOT_WORKER: Alarm rescheduling complete")
            runCatching {
                SpeakAlertWidgetUpdater.requestUpdate(applicationContext)
            }.onFailure { error ->
                FileLogger.logError("BOOT_WORKER", "Failed to refresh widgets after boot reschedule", error)
            }
            Result.success()
        } catch (e: Exception) {
            FileLogger.logError("BOOT_WORKER", "Error rescheduling alarms", e)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "boot_reschedule_alarms"
        const val KEY_TRIGGER_ACTION = "trigger_action"
    }

    private fun parseMissedPolicy(raw: String): MissedPolicy {
        return when (raw) {
            "FIRE_ON_RESUME", "FIRE" -> MissedPolicy.FIRE_ON_RESUME
            "SKIP_TO_NEXT", "SKIP" -> MissedPolicy.SKIP_TO_NEXT
            else -> MissedPolicy.SKIP_TO_NEXT
        }
    }
    private fun buildMissedDisplayTitle(title: String?, reminderText: String?): String {
        val userTitle = title
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.isDefaultAppDisplayName() }
        if (userTitle != null) return userTitle

        val textFallback = reminderText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { text ->
                val words = text.split(Regex("\\s+"))
                if (words.size > 8) words.take(8).joinToString(" ") else text
            }
        return textFallback ?: "Reminder"
    }
}
