package com.ghostgramlabs.speakalert.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.speakalert.alarm.AlarmScheduler
import com.ghostgramlabs.speakalert.alarm.ToneAlertPlayer
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.MissedReminderRepository
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.service.ReminderPlaybackService
import com.ghostgramlabs.speakalert.util.DateUtils
import com.ghostgramlabs.speakalert.util.ReminderAudioSource
import com.ghostgramlabs.speakalert.util.isDefaultAppDisplayName
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

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
        val missedReminderIds = missed.map { it.reminderId }.toSet()
        // Keep one-time reminders in a single place: if it already has a Missed entry,
        // show it only in Missed tab (not in Active tabs).
        val active = reminders.filter {
            !it.isCompleted &&
                !(it.recurrenceType == RecurrenceType.NONE && missedReminderIds.contains(it.id))
        }
        val completed = reminders.filter { it.isCompleted }.sortedByDescending { it.completedAt }

        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Classification is based on the *next* trigger only.
        val today = active.filter {
            DateUtils.isToday(it.nextTriggerAt)
        }.sortedBy { it.nextTriggerAt }

        val upcoming = active.filter {
            it.nextTriggerAt > endOfToday
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
                ToneAlertPlayer.stop()
                
                // Start playback for audio OR text
                if (ReminderAudioSource.isPlayable(context, audioPath)) {
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

    fun markMissedRemindersDone(missedReminders: List<MissedReminderEntity>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            missedReminders.forEach { missed ->
                val reminder = repository.getReminder(missed.reminderId)
                if (reminder == null) {
                    missedRepository.deleteMissedReminderById(missed.id)
                    return@forEach
                }

                if (reminder.recurrenceType == RecurrenceType.NONE) {
                    val updated = reminder.copy(
                        isCompleted = true,
                        completedAt = now,
                        snoozeUntil = null,
                        pendingFollowUpAt = null
                    )
                    alarmScheduler.cancel(reminder)
                    repository.updateReminder(updated)
                } else if (reminder.nextTriggerAt <= now) {
                    var updated = reminder.copy(
                        lastFiredAt = now,
                        snoozeUntil = null,
                        pendingFollowUpAt = null
                    )
                    updated = com.ghostgramlabs.speakalert.domain.RecurrenceUtils.updateForNextOccurrence(updated)
                    val nextTrigger = com.ghostgramlabs.speakalert.domain.RecurrenceUtils.computeNextTrigger(
                        updated,
                        maxOf(now, reminder.nextTriggerAt)
                    )
                    if (nextTrigger != null) {
                        updated = updated.copy(nextTriggerAt = nextTrigger)
                        alarmScheduler.schedule(updated)
                    } else {
                        updated = updated.copy(isCompleted = true, completedAt = now)
                        alarmScheduler.cancel(updated)
                    }
                    repository.updateReminder(updated)
                } else {
                    repository.updateReminder(
                        reminder.copy(
                            snoozeUntil = null,
                            pendingFollowUpAt = null
                        )
                    )
                }

                missedRepository.deleteMissedReminderById(missed.id)
            }
        }
    }

    fun remindAgainForMissed(missedReminders: List<MissedReminderEntity>) {
        viewModelScope.launch {
            val snoozeMinutes = settingsRepository.defaultSnoozeDuration.first()
            val remindAt = DateUtils.normalizeToMinute(System.currentTimeMillis() + snoozeMinutes * 60 * 1000L)

            missedReminders.forEach { missed ->
                val reminder = repository.getReminder(missed.reminderId)
                if (reminder == null) {
                    missedRepository.deleteMissedReminderById(missed.id)
                    return@forEach
                }

                // For one-time reminders, move nextTriggerAt itself so UI + scheduling both show the new time.
                // For recurring reminders, keep recurrence anchor and use temporary snoozeUntil.
                val updated = if (reminder.recurrenceType == RecurrenceType.NONE) {
                    reminder.copy(
                        isCompleted = false,
                        completedAt = null,
                        nextTriggerAt = remindAt,
                        snoozeUntil = null,
                        pendingFollowUpAt = null
                    )
                } else {
                    reminder.copy(
                        isCompleted = false,
                        completedAt = null,
                        snoozeUntil = remindAt,
                        pendingFollowUpAt = null
                    )
                }
                repository.updateReminder(updated)
                alarmScheduler.schedule(updated)
                missedRepository.deleteMissedReminderById(missed.id)
            }
        }
    }

    fun completeReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                snoozeUntil = null,
                pendingFollowUpAt = null
            )
            missedRepository.deleteMissedReminderByReminderId(reminder.id)
            alarmScheduler.cancel(reminder)
            repository.updateReminder(updated)
        }
    }

    fun markTodayAsDone(reminder: ReminderEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var updated = reminder.copy(
                lastFiredAt = now,
                snoozeUntil = null,
                pendingFollowUpAt = null
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
                
                // IMPORTANT: Clear lastFiredAt so it shows up in "Today" list if applicable
                val updated = original.copy(
                    nextTriggerAt = finalTime,
                    snoozeUntil = null,
                    isCompleted = false,
                    lastFiredAt = null,
                    pendingFollowUpAt = null
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
            ToneAlertPlayer.stop()
            // Start playback for audio OR text
            if (ReminderAudioSource.isPlayable(context, reminder.audioPath)) {
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
            
            // IMPORTANT: Clear lastFiredAt so it shows up in "Today" list if applicable
            val updated = reminder.copy(
                isCompleted = false,
                completedAt = null,
                nextTriggerAt = finalTime,
                snoozeUntil = null,
                lastFiredAt = null,
                pendingFollowUpAt = null
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
                title = buildMissedDisplayTitle(reminder.title, reminder.reminderText),
                scheduledTime = reminder.nextTriggerAt,
                detectedTime = System.currentTimeMillis(),
                reminderText = reminder.reminderText
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

    // Keep missed tab labels consistent for untitled reminders.
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
