package com.example.voicereminder.ui.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.alarm.AlarmScheduler
import com.example.voicereminder.audio.AndroidAudioPlayer
import com.example.voicereminder.data.model.ReminderEntity
import com.example.voicereminder.data.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ReminderDetailsViewModel(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    context: Context
) : ViewModel() {

    private val _reminder = MutableStateFlow<ReminderEntity?>(null)
    val reminder = _reminder.asStateFlow()
    
    // Player
    private val player = AndroidAudioPlayer(context)

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
    
    fun playAudio() {
        val path = _reminder.value?.audioPath ?: return
        val file = File(path)
        if (file.exists()) {
            player.playFile(file)
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
            com.example.voicereminder.service.ReminderPlaybackService.start(
                context, rem.id, title, audioPath, null
            )
        } else if (!reminderText.isNullOrBlank()) {
            com.example.voicereminder.service.ReminderPlaybackService.start(
                context, rem.id, title, null, reminderText
            )
        }
    }
    
    fun stopAudio() {
        player.stop()
    }
    
    fun updateRecurrence(model: com.example.voicereminder.domain.models.RecurrenceModel?) {
        viewModelScope.launch {
            val current = _reminder.value ?: return@launch
            
            // 1. Cancel existing alarm
            scheduler.cancel(current)
            
            // 2. Prepare serialization
            val newType = if (model == null) {
                com.example.voicereminder.domain.models.RecurrenceType.NONE
            } else {
                when (model) {
                    is com.example.voicereminder.domain.models.RecurrenceModel.Daily -> com.example.voicereminder.domain.models.RecurrenceType.DAILY
                    is com.example.voicereminder.domain.models.RecurrenceModel.Weekly -> com.example.voicereminder.domain.models.RecurrenceType.WEEKLY
                    is com.example.voicereminder.domain.models.RecurrenceModel.Monthly -> com.example.voicereminder.domain.models.RecurrenceType.MONTHLY
                    is com.example.voicereminder.domain.models.RecurrenceModel.Custom -> com.example.voicereminder.domain.models.RecurrenceType.CUSTOM
                }
            }
            
            val newJson = if (model != null) com.example.voicereminder.domain.RecurrenceUtils.toJson(model) else null
            
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
            
            val nextTrigger = if (newType == com.example.voicereminder.domain.models.RecurrenceType.NONE) {
                 // Keep current if valid? Or should we reset?
                 // If it was repeating, it had a nextTrigger. Changing to None means "Fire this one, then stop".
                 current.nextTriggerAt
            } else {
                 com.example.voicereminder.domain.RecurrenceUtils.computeNextTrigger(tempEntity, System.currentTimeMillis())
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

    override fun onCleared() {
        super.onCleared()
        player.stop()
    }
}
