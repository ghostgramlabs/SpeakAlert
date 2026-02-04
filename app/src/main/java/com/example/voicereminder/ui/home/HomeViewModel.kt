package com.example.voicereminder.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.alarm.AlarmScheduler
import com.example.voicereminder.data.model.MissedReminderEntity
import com.example.voicereminder.data.model.ReminderEntity
import com.example.voicereminder.data.repository.MissedReminderRepository
import com.example.voicereminder.data.repository.ReminderRepository
import com.example.voicereminder.service.ReminderPlaybackService
import com.example.voicereminder.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayReminders: List<ReminderEntity> = emptyList(),
    val upcomingReminders: List<ReminderEntity> = emptyList(),
    val completedReminders: List<ReminderEntity> = emptyList(),
    val missedReminders: List<MissedReminderEntity> = emptyList()
)

class HomeViewModel(
    private val repository: ReminderRepository,
    private val missedRepository: MissedReminderRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllRemindersStream(),
        missedRepository.allMissedReminders
    ) { reminders, missed ->
        // Filter lists
        val active = reminders.filter { !it.isCompleted }
        val completed = reminders.filter { it.isCompleted }.sortedByDescending { it.completedAt }
        
        val today = active.filter { 
            // "Today" means:
            // 1. Scheduled for today (nextTriggerAt is today, regardless of past/future)
            // 2. BUT exclude if already handled today (lastFiredAt is today AND nextTriggerAt is not in future)
            
            val isScheduledForToday = DateUtils.isToday(it.nextTriggerAt)
            val isOverdueForToday = it.nextTriggerAt < System.currentTimeMillis() && DateUtils.isToday(it.nextTriggerAt)
            val isWaitingToFire = it.nextTriggerAt > System.currentTimeMillis()
            
            // For recurring: show if next trigger is today (even if lastFired was earlier today)
            // For one-time: show if scheduled for today and not yet fired OR overdue from today
            val isTodayFired = it.lastFiredAt != null && DateUtils.isToday(it.lastFiredAt!!)
            
            // Show in Today if:
            // - Scheduled for today AND waiting to fire (future today)
            // - OR Overdue but scheduled for today (user might want to see it)
            // - EXCLUDE if already fired today AND it's a one-time (recurring should show for next occurrence)
            when {
                isWaitingToFire && isScheduledForToday -> true  // Future today
                isOverdueForToday && !isTodayFired -> true       // Overdue today, not yet handled
                it.recurrenceType != com.example.voicereminder.domain.models.RecurrenceType.NONE && isScheduledForToday -> true  // Recurring scheduled for today
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
            missedReminders = missed
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
            missedRepository.deleteMissedReminder(missed)
        }
    }

    fun markAsDone(reminder: ReminderEntity) {
        viewModelScope.launch {
            var updated = reminder.copy(
                snoozeUntil = null,
                lastFiredAt = System.currentTimeMillis() // Assuming done means interacted
            )
            
            // If One-time, mark completed.
             if (reminder.recurrenceType == com.example.voicereminder.domain.models.RecurrenceType.NONE) {
                 updated = updated.copy(isCompleted = true, completedAt = System.currentTimeMillis())
             } else {
                 // Recurring:
                 // Ideally calculate next trigger if not already in future?
                 // But Receiver usually does that. 
                 // If we mark done, we just ensure it's not showing as overdue?
                 // Current logic leaves nextTriggerAt alone (which should be in future if fired).
                 // If user marks done, we simply update it.
             }
            repository.updateReminder(updated)
        }
    }
    
    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            // Cancel any scheduled alarm first
            alarmScheduler.cancel(reminder)
            // Then delete from database
            repository.deleteReminder(reminder)
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
}
