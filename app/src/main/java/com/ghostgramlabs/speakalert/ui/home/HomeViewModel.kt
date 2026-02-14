package com.ghostgramlabs.speakalert.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.speakalert.alarm.AlarmScheduler
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.MissedReminderRepository
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.service.ReminderPlaybackService
import com.ghostgramlabs.speakalert.util.DateUtils
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayReminders: List<ReminderEntity> = emptyList(),
    val upcomingReminders: List<ReminderEntity> = emptyList(),
    val completedReminders: List<ReminderEntity> = emptyList(),
    val missedReminders: List<MissedReminderEntity> = emptyList(),
    val isTextToSpeechEnabled: Boolean = true
)

class HomeViewModel(
    private val repository: ReminderRepository,
    private val missedRepository: MissedReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: com.ghostgramlabs.speakalert.data.repository.SettingsRepository
) : ViewModel() {

    private var lastDeletedReminder: ReminderEntity? = null


    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllRemindersStream(),
        missedRepository.allMissedReminders,
        settingsRepository.speakTextIfNoVoice
    ) { reminders, missed, isTtsEnabled ->
        // Filter lists
        val active = reminders.filter { !it.isCompleted }
        val completed = reminders.filter { it.isCompleted }.sortedByDescending { it.completedAt }
        
        val today = active.filter { 
            // "Today" means:
            // 1. Scheduled for today (nextTriggerAt is today, regardless of past/future)
            // 2. BUT exclude if already handled today (lastFiredAt is today AND nextTriggerAt is not in future)
            
            val isScheduledForToday = DateUtils.isToday(it.nextTriggerAt)
            val isWaitingToFire = it.nextTriggerAt > System.currentTimeMillis()
            val isTodayFired = it.lastFiredAt != null && DateUtils.isToday(it.lastFiredAt!!)
            
            // Show in Today if:
            // 1. Scheduled for today AND hasn't been fired/done today yet
            // 2. OR Overdue but scheduled for today
            when {
                // If it already fired or was marked done today, hide it from "Today" 
                // (It will be in "Done" or advanced to "Upcoming")
                isTodayFired -> false
                
                // Future occurrence today
                isScheduledForToday && isWaitingToFire -> true
                
                // Overdue occurrence today
                isScheduledForToday && it.nextTriggerAt <= System.currentTimeMillis() -> true
                
                else -> false
            }
        }.sortedBy { it.nextTriggerAt }
        
        val upcoming = active.filter { 
            it.nextTriggerAt > System.currentTimeMillis() && !DateUtils.isToday(it.nextTriggerAt)
        }.sortedBy { it.nextTriggerAt }

        HomeUiState(
            todayReminders = today,
            upcomingReminders = upcoming,
            completedReminders = completed,
            missedReminders = missed,
            isTextToSpeechEnabled = isTtsEnabled
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun fireMissedReminder(context: Context, missed: MissedReminderEntity) {
        viewModelScope.launch {
            // "Fire Now": Trigger playback service using original reminder data
            val reminder = repository.getReminder(missed.reminderId)
            if (reminder != null) {
                val audioPath = reminder.audioPath
                val reminderText = reminder.reminderText
                val title = reminder.title ?: "Voice reminder"
                
                // Start playback for audio OR text
                if (!audioPath.isNullOrBlank()) {
                    ReminderPlaybackService.start(context, reminder.id, title, audioPath, null)
                } else if (!reminderText.isNullOrBlank()) {
                    ReminderPlaybackService.start(context, reminder.id, title, null, reminderText)
                }
                
                // Do NOT delete from missed inbox here - wait for playback completion or user dismissal
            } else {
                // Reminder deleted? Just delete missed entry
                missedRepository.deleteMissedReminder(missed)
            }
        }
    }

    fun dismissMissedReminder(missed: MissedReminderEntity) {
        viewModelScope.launch {
            missedRepository.deleteMissedReminderById(missed.id)
        }
    }

    fun completeReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                snoozeUntil = null
            )
            alarmScheduler.cancel(reminder)
            repository.updateReminder(updated)
        }
    }

    fun markTodayAsDone(reminder: ReminderEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var updated = reminder.copy(
                lastFiredAt = now,
                snoozeUntil = null
            )
            
            // Advance to next occurrence
            updated = com.ghostgramlabs.speakalert.domain.RecurrenceUtils.updateForNextOccurrence(updated)
            // Compute next trigger time, starting from either NOW or the scheduled time (whichever is later)
            // This ensures we actually skip the "current" occurrence even if marking as done early.
            val calculationBase = maxOf(now, reminder.nextTriggerAt)
            val nextTrigger = com.ghostgramlabs.speakalert.domain.RecurrenceUtils.computeNextTrigger(updated, calculationBase)
            
            if (nextTrigger != null) {
                updated = updated.copy(nextTriggerAt = nextTrigger)
                alarmScheduler.schedule(updated)
            } else {
                // Recurrence ended
                updated = updated.copy(isCompleted = true, completedAt = now)
                alarmScheduler.cancel(updated)
            }
            repository.updateReminder(updated)
        }
    }
    
    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            // Store for undo
            lastDeletedReminder = reminder
            // Cancel any scheduled alarm first
            alarmScheduler.cancel(reminder)
            // Then delete from database
            repository.deleteReminder(reminder)
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            lastDeletedReminder?.let { originalReminder ->
                var reminderToRestore = originalReminder
                val now = System.currentTimeMillis()

                // If scheduled time is in the past and it's RECURRING,
                // we should advance to the next valid occurrence from NOW.
                // (For one-time reminders, we restore as-is so it fires immediately as missed/overdue)
                if (reminderToRestore.nextTriggerAt < now && 
                    reminderToRestore.recurrenceType != com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE) {
                    
                    val nextTrigger = com.ghostgramlabs.speakalert.domain.RecurrenceUtils.computeNextTrigger(reminderToRestore, now)
                    if (nextTrigger != null) {
                        reminderToRestore = reminderToRestore.copy(nextTriggerAt = nextTrigger)
                    } else {
                        // Recurrence ended? Treat as completed if we can't find next trigger
                    }
                }

                // Insert back into repository
                repository.insertReminder(reminderToRestore)
                // Re-schedule alarm
                alarmScheduler.schedule(reminderToRestore)
                // Clear last deleted
                lastDeletedReminder = null
            }
        }
    }

    fun previewUndo(): ReminderEntity? {
        return lastDeletedReminder
    }

    fun undoDelete(forceTime: Long) {
        viewModelScope.launch {
            lastDeletedReminder?.let { original ->
                // Ensure forceTime is in the future
                val finalTime = if (forceTime <= System.currentTimeMillis()) {
                    System.currentTimeMillis() + 60_000 // Fallback: 1 minute from now
                } else {
                    forceTime
                }
                
                val updated = original.copy(
                    nextTriggerAt = finalTime,
                    snoozeUntil = null,
                    isCompleted = false
                )
                repository.insertReminder(updated)
                alarmScheduler.schedule(updated)
                lastDeletedReminder = null
            }
        }
    }

    fun playReminder(context: Context, reminder: ReminderEntity) {
        viewModelScope.launch {
            val title = reminder.title ?: "Voice reminder"
            // Start playback for audio OR text
            if (!reminder.audioPath.isNullOrBlank()) {
                ReminderPlaybackService.start(context, reminder.id, title, reminder.audioPath, null)
            } else if (!reminder.reminderText.isNullOrBlank()) {
                ReminderPlaybackService.start(context, reminder.id, title, null, reminder.reminderText)
            }
        }
    }

    fun restoreReminder(reminder: ReminderEntity, newTriggerTime: Long) {
        viewModelScope.launch {
            // Ensure newTriggerTime is in the future
            val finalTime = if (newTriggerTime <= System.currentTimeMillis()) {
                System.currentTimeMillis() + 60_000 // Fallback: 1 minute from now
            } else {
                newTriggerTime
            }
            
            val updated = reminder.copy(
                isCompleted = false,
                completedAt = null,
                nextTriggerAt = finalTime,
                snoozeUntil = null
            )
            repository.updateReminder(updated)
            alarmScheduler.schedule(updated)
        }
    }

    fun moveToMissed(reminder: ReminderEntity) {
        viewModelScope.launch {
            // Create a missed reminder entry
            val missed = MissedReminderEntity(
                reminderId = reminder.id,
                title = reminder.title ?: reminder.reminderText ?: "Reminder",
                scheduledTime = reminder.nextTriggerAt,
                detectedTime = System.currentTimeMillis()
            )
            missedRepository.insertMissedReminder(missed)
            
            // Mark reminder as not completed so it shows in missed tab
            val updated = reminder.copy(
                isCompleted = false,
                completedAt = null
            )
            repository.updateReminder(updated)
        }
    }
}
