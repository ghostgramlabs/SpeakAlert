package com.ghostgramlabs.speakalert.ui.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.speakalert.alarm.AlarmScheduler
import com.ghostgramlabs.speakalert.audio.AndroidAudioPlayer
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ReminderDetailsViewModel(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val settingsRepository: com.ghostgramlabs.speakalert.data.repository.SettingsRepository,
    context: Context,
    private val player: com.ghostgramlabs.speakalert.audio.AudioPlayer = com.ghostgramlabs.speakalert.audio.AndroidAudioPlayer(context) 
) : ViewModel() {

    private val _reminder = MutableStateFlow<ReminderEntity?>(null)
    val reminder = _reminder.asStateFlow()
    
    private var isTtsEnabled = true
    
    init {
        viewModelScope.launch {
            settingsRepository.speakTextIfNoVoice.collect { enabled ->
                isTtsEnabled = enabled
            }
        }
    }
    
    // Player injected

    fun loadReminder(id: Long) {
        viewModelScope.launch {
            _reminder.value = repository.getReminder(id)
        }
    }
    
    fun deleteReminder() {
        viewModelScope.launch {
            _reminder.value?.let {
                stopAudio()
                scheduler.cancel(it)
                repository.deleteReminder(it)
            }
        }
    }
    
    fun toggleDone() {
        viewModelScope.launch {
            val current = _reminder.value ?: return@launch
            val newCompleted = !current.isCompleted
            val updated = current.copy(
                isCompleted = newCompleted,
                completedAt = if (newCompleted) System.currentTimeMillis() else null
            )
            repository.updateReminder(updated)
            
            if (newCompleted) {
                scheduler.cancel(current)
            } else {
                scheduler.schedule(updated)
            }
            _reminder.value = updated
        }
    }

    fun dismissReminder() {
        viewModelScope.launch {
            val current = _reminder.value ?: return@launch
            stopAudio()
            
            if (current.recurrenceType == com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE) {
                // One-time: mark as completed
                val updated = current.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    snoozeUntil = null
                )
                repository.updateReminder(updated)
                scheduler.cancel(current)
                _reminder.value = updated
            } else {
                // Recurring: system already advanced nextTriggerAt if it fired.
                // We just ensure we clear snooze and audio state.
                if (current.snoozeUntil != null) {
                    val updated = current.copy(snoozeUntil = null)
                    repository.updateReminder(updated)
                    // If we clear snooze, ensure next occurrence is still scheduled
                    scheduler.schedule(updated) 
                    _reminder.value = updated
                }
            }
        }
    }
    
    fun playAudio() {
        val path = _reminder.value?.audioPath ?: return
        val file = File(path)
        if (file.exists()) {
            player.playFile(file)
        }
    }

    fun playAudio(context: Context) {
        val rem = _reminder.value ?: return
        val audioPath = rem.audioPath
        val reminderText = rem.reminderText
        val title = rem.title ?: "SpeakAlert"

        if (!audioPath.isNullOrBlank() && File(audioPath).exists()) {
             com.ghostgramlabs.speakalert.service.ReminderPlaybackService.start(
                context, rem.id, title, audioPath, null
            )
        } else if (!reminderText.isNullOrBlank()) {
             // Fallback to TTS
             com.ghostgramlabs.speakalert.service.ReminderPlaybackService.start(
                context, rem.id, title, null, reminderText
            )
        }
    }
    
    /**
     * Start autoplay using the foreground service (plays audio or TTS).
     * Call this when opening from notification with autoplay=true.
     */
    fun startAutoplay(context: android.content.Context) {
        val rem = _reminder.value ?: return
        val audioPath = rem.audioPath
        val reminderText = rem.reminderText
        val title = rem.title ?: "SpeakAlert"
        
        // Use foreground service for autoplay
        if (!audioPath.isNullOrBlank() && File(audioPath).exists()) {
            com.ghostgramlabs.speakalert.service.ReminderPlaybackService.start(
                context, rem.id, title, audioPath, null
            )
        } else if (!reminderText.isNullOrBlank() && isTtsEnabled) {
            com.ghostgramlabs.speakalert.service.ReminderPlaybackService.start(
                context, rem.id, title, null, reminderText
            )
        }
    }
    
    fun stopAudio() {
        player.stop()
    }
    
    fun updateRecurrence(model: com.ghostgramlabs.speakalert.domain.models.RecurrenceModel?) {
        viewModelScope.launch {
            val current = _reminder.value ?: return@launch
            
            // 1. Cancel existing alarm
            scheduler.cancel(current)
            
            // 2. Prepare serialization
            val newType = if (model == null) {
                com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE
            } else {
                when (model) {
                    is com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.Daily -> com.ghostgramlabs.speakalert.domain.models.RecurrenceType.DAILY
                    is com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.Weekly -> com.ghostgramlabs.speakalert.domain.models.RecurrenceType.WEEKLY
                    is com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.Monthly -> com.ghostgramlabs.speakalert.domain.models.RecurrenceType.MONTHLY
                    is com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.Custom -> com.ghostgramlabs.speakalert.domain.models.RecurrenceType.CUSTOM
                }
            }
            
            val newJson = if (model != null) com.ghostgramlabs.speakalert.domain.RecurrenceUtils.toJson(model) else null
            
            // 3. Compute next trigger
            // We temporarily update the entity to use the utility
            val tempEntity = current.copy(
                recurrenceType = newType,
                recurrenceJson = newJson
            )
            
            // If changing to NONE, we keep the existing nextTriggerAt unless it's in the past?
            // If changing FROM None TO Recurring, we need to compute next.
            // computeNextTrigger usually computes the *subsequent* trigger.
            // If we are just editing properties, maybe we just want to ensure the next trigger is valid.
            
            val nextTrigger = if (newType == com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE) {
                 // Keep current if valid? Or should we reset?
                 // If it was repeating, it had a nextTrigger. Changing to None means "Fire this one, then stop".
                 current.nextTriggerAt
            } else {
                 com.ghostgramlabs.speakalert.domain.RecurrenceUtils.computeNextTrigger(tempEntity, System.currentTimeMillis())
            }
            
            // 4. Update Entity
            val finalEntity = tempEntity.copy(
                nextTriggerAt = nextTrigger ?: 0L,
                isCompleted = nextTrigger == null
            )
            
            repository.updateReminder(finalEntity)
            
            // 5. Schedule new
            // Note: if NONE, we still verify schedule logic (it will fire once)
            // But scheduler works? Yes.
            if (nextTrigger != null && !finalEntity.isCompleted) {
                scheduler.schedule(finalEntity)
            }
            
            
            _reminder.value = finalEntity
        }
    }

    fun reschedule(timestamp: Long) {
        viewModelScope.launch {
            val current = _reminder.value ?: return@launch
            
            // Ensure timestamp is in the future
            val finalTime = if (timestamp <= System.currentTimeMillis()) {
                System.currentTimeMillis() + 60_000 // Fallback: 1 minute from now
            } else {
                timestamp
            }
            
            // Un-complete and update trigger time
            val updated = current.copy(
                isCompleted = false,
                completedAt = null,
                nextTriggerAt = finalTime
            )
            
            repository.updateReminder(updated)
            scheduler.schedule(updated)
            _reminder.value = updated
        }
    }
    
    fun markAsMissed(playAudio: Boolean) {
        viewModelScope.launch {
            val current = _reminder.value ?: return@launch
            
            // Un-complete but DO NOT reschedule (keep past time)
            // This effectively puts it in "Missed" state (past deadline, not completed)
            val updated = current.copy(
                isCompleted = false,
                completedAt = null
                // nexTriggerAt remains as is (in the past)
            )
            
            repository.updateReminder(updated)
            // IMPORTANT: Do NOT call scheduler.schedule() because time is past.
            // We don't want immediate firing/notification.
            
            _reminder.value = updated
            
            if (playAudio) {
                playAudio()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.stop()
    }
}
