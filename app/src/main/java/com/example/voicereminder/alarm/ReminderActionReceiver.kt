package com.example.voicereminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.voicereminder.VoiceReminderApp
import com.example.voicereminder.domain.RecurrenceUtils
import com.example.voicereminder.service.ReminderPlaybackService
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
                    ReminderPlaybackService.start(context, reminderId, title, null, reminderText)
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
                    com.example.voicereminder.util.FileLogger.log("ACTION_STOP_PLAYBACK: Playback stopped by user")
                }

                "ACTION_DONE" -> {
                    val now = System.currentTimeMillis()
                    
                    // Cleanup missed entry if it exists (since we are handling it now)
                    missedRepository.deleteMissedReminderByReminderId(reminderId)
                    
                    if (reminder.recurrenceType == com.example.voicereminder.domain.models.RecurrenceType.NONE) {
                        // ONE-TIME REMINDER: Mark as completed and cancel alarm
                        val updated = reminder.copy(
                            isCompleted = true,
                            completedAt = now,
                            snoozeUntil = null
                        )
                        repository.updateReminder(updated)
                        scheduler.cancel(reminder)
                        com.example.voicereminder.util.FileLogger.log("ACTION_DONE: One-time reminder completed and cancelled")
                    } else {
                        // RECURRING REMINDER: Acknowledge this occurrence and schedule next
                        // Do NOT cancel the alarm. Do NOT mark as completed.
                        
                        // Update reminder state
                        var updated = reminder.copy(
                            lastFiredAt = now,
                            snoozeUntil = null
                        )
                        
                        // Decrement occurrence count if applicable
                        updated = RecurrenceUtils.updateForNextOccurrence(updated)
                        
                        // Compute next trigger time
                        val nextTrigger = RecurrenceUtils.computeNextTrigger(updated, now)
                        com.example.voicereminder.util.FileLogger.log("ACTION_DONE: Recurring - computed nextTrigger=$nextTrigger")
                        
                        if (nextTrigger != null) {
                            updated = updated.copy(nextTriggerAt = nextTrigger)
                            repository.updateReminder(updated)
                            scheduler.schedule(updated)
                            com.example.voicereminder.util.FileLogger.log("ACTION_DONE: Recurring - scheduled next at $nextTrigger")
                        } else {
                            // Recurrence has ended (e.g., after N occurrences or past end date)
                            updated = updated.copy(isCompleted = true, completedAt = now)
                            repository.updateReminder(updated)
                            scheduler.cancel(updated)
                            com.example.voicereminder.util.FileLogger.log("ACTION_DONE: Recurring - recurrence ended, marked completed")
                        }
                    }
                }
                
                "ACTION_SNOOZE" -> {
                    // Snooze for 10 minutes (default) - or get preference
                    val snoozeMinutes = 10
                    val snoozeUntil = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)
                    
                    val updated = reminder.copy(snoozeUntil = snoozeUntil)
                    repository.updateReminder(updated)
                    scheduler.schedule(updated) // Schedule the snooze alarm
                    
                    // Cleanup missed entry if it exists (since we handled it by snoozing)
                    missedRepository.deleteMissedReminderByReminderId(reminderId)
                    
                    com.example.voicereminder.util.FileLogger.log("ACTION_SNOOZE: Snoozed until $snoozeUntil")
                }
            }
        }
    }
}
