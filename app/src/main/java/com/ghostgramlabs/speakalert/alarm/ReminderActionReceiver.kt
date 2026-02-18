package com.ghostgramlabs.speakalert.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.service.ReminderPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminderId", -1L)
        val action = intent.action
        if (reminderId == -1L) return

        Log.d("VoiceReminder", "Action $action Received for reminder $reminderId")

        val app = context.applicationContext as VoiceReminderApp
        val repository = app.container.reminderRepository
        val settingsRepository = app.container.settingsRepository
        val scheduler = app.container.alarmScheduler

        when (action) {
            "ACTION_PLAY" -> {
                // Play from notification - start playback service
                // NOTE: Do NOT cancel the alert notification - it stays visible until Done/Snooze
                val audioPath = intent.getStringExtra("audioPath")
                val reminderText = intent.getStringExtra("reminderText")
                val title = intent.getStringExtra("title") ?: "SpeakAlert"
                
                // Start playback (playback service has its own notification)
                if (!audioPath.isNullOrBlank()) {
                    ReminderPlaybackService.start(context, reminderId, title, audioPath, null)
                } else if (!reminderText.isNullOrBlank()) {
                    // Check TTS setting before speaking text
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val ttsEnabled = settingsRepository.speakTextIfNoVoice.first()
                            if (ttsEnabled) {
                                ReminderPlaybackService.start(context, reminderId, title, null, reminderText)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                return // Don't proceed to DB operations for play action
            }
        }

        // Dismiss standard notification
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
        
        // Stop Playback Service (if running)
        ReminderPlaybackService.stop(context)

        CoroutineScope(Dispatchers.IO).launch {
            val reminder = repository.getReminder(reminderId) ?: return@launch

            val missedRepository = app.container.missedReminderRepository

            when (action) {
                "ACTION_STOP_PLAYBACK" -> {
                    // Just log, service is already stopped by line 52
                    com.ghostgramlabs.speakalert.util.FileLogger.log("ACTION_STOP_PLAYBACK: Playback stopped by user")
                }

                "ACTION_DONE" -> {
                    val now = System.currentTimeMillis()
                    
                    // Cleanup missed entry if it exists (since we are handling it now)
                    missedRepository.deleteMissedReminderByReminderId(reminderId)
                    
                    if (reminder.recurrenceType == com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE) {
                        // ONE-TIME REMINDER: Mark as completed and cancel alarm
                        val updated = reminder.copy(
                            isCompleted = true,
                            completedAt = now,
                            snoozeUntil = null
                        )
                        repository.updateReminder(updated)
                        scheduler.cancel(reminder)
                        com.ghostgramlabs.speakalert.util.FileLogger.log("ACTION_DONE (Dismiss): One-time reminder completed and cancelled")
                    } else {
                        // RECURRING REMINDER: Dismiss alert only.
                        // We check if it's already been advanced by ReminderAlarmReceiver.
                        // If nextTriggerAt is already in the future, we just cleanup lastFiredAt/snoozeUntil.
                        
                        var updated = reminder.copy(
                            snoozeUntil = null
                        )
                        
                        // IDEMPOTENCY CHECK: If nextTriggerAt is in the past (<= now), we need to advance it.
                        // If it's already in the future, it was likely auto-rescheduled by ReminderAlarmReceiver.
                        if (reminder.nextTriggerAt <= now) {
                            com.ghostgramlabs.speakalert.util.FileLogger.log("ACTION_DONE: nextTriggerAt is past (${reminder.nextTriggerAt}), advancing recurrence")
                            
                            updated = updated.copy(lastFiredAt = now)
                            // Advance recurrence
                            updated = RecurrenceUtils.updateForNextOccurrence(updated)
                            
                            // Compute next trigger time relative to the occurrence we are dismissing
                            val calculationBase = maxOf(now, reminder.nextTriggerAt)
                            val nextTrigger = RecurrenceUtils.computeNextTrigger(updated, calculationBase)
                            if (nextTrigger != null) {
                                updated = updated.copy(nextTriggerAt = nextTrigger)
                                repository.updateReminder(updated)
                                scheduler.schedule(updated)
                                com.ghostgramlabs.speakalert.util.FileLogger.log("ACTION_DONE: Recurring - scheduled next at $nextTrigger")
                            } else {
                                // Recurrence has ended
                                updated = updated.copy(isCompleted = true, completedAt = now)
                                repository.updateReminder(updated)
                                scheduler.cancel(updated)
                                com.ghostgramlabs.speakalert.util.FileLogger.log("ACTION_DONE: Recurring - recurrence ended, marked completed")
                            }
                        } else {
                            com.ghostgramlabs.speakalert.util.FileLogger.log("ACTION_DONE: nextTriggerAt is already future (${reminder.nextTriggerAt}), skipping advancement")
                            // Just update database with cleared snooze (already done in updated var)
                            repository.updateReminder(updated)
                        }
                    }
                }
                
                "ACTION_SNOOZE" -> {
                    // =====================================================
                    // SNOOZE LOGIC DOCUMENTATION:
                    // -----------------------------------------------------
                    // Snooze is TEMPORARY and does NOT alter recurrence.
                    // 
                    // 1. We set snoozeUntil = now + snoozeDuration
                    // 2. AlarmScheduler uses: snoozeUntil ?: nextTriggerAt
                    //    So snooze alarm fires at snoozeUntil
                    // 3. When snooze alarm fires, snoozeUntil is cleared
                    // 4. When user clicks "Done" on recurring reminder:
                    //    - snoozeUntil is cleared (already null)
                    //    - nextTriggerAt advances to next occurrence
                    //    - Recurrence pattern resumes normally
                    // 
                    // RULE: Snooze NEVER modifies nextTriggerAt or recurrence.
                    // =====================================================
                    
                    // Get snooze duration from preferences (default 10)
                    val snoozeMinutes = settingsRepository.defaultSnoozeDuration.first()
                    val snoozeUntil = com.ghostgramlabs.speakalert.util.DateUtils.normalizeToMinute(System.currentTimeMillis() + (snoozeMinutes * 60 * 1000))
                    
                    // Set snoozeUntil WITHOUT modifying nextTriggerAt or recurrence
                    val updated = reminder.copy(snoozeUntil = snoozeUntil)
                    repository.updateReminder(updated)
                    scheduler.schedule(updated) // Schedule the snooze alarm
                    
                    // Cleanup missed entry if it exists (since we handled it by snoozing)
                    missedRepository.deleteMissedReminderByReminderId(reminderId)
                    
                    com.ghostgramlabs.speakalert.util.FileLogger.log("ACTION_SNOOZE: Snoozed until $snoozeUntil (recurrence unchanged)")
                }
            }
        }
    }
}
