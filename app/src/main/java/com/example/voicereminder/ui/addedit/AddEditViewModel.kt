package com.example.voicereminder.ui.addedit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.alarm.AlarmScheduler
import com.example.voicereminder.audio.AndroidAudioRecorder
import com.example.voicereminder.audio.AndroidAudioPlayer
import com.example.voicereminder.data.model.ReminderEntity
import com.example.voicereminder.data.repository.ReminderRepository
import com.example.voicereminder.domain.RecurrenceUtils
import com.example.voicereminder.domain.models.RecurrenceModel
import com.example.voicereminder.domain.models.RecurrenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import com.example.voicereminder.data.repository.SettingsRepository

data class AddEditUiState(
    val initialReminderId: Long = -1L,
    val title: String = "",
    val reminderText: String = "",
    val isRecording: Boolean = false,
    val isPlaying: Boolean = false,
    val recordedAudioPath: String? = null,
    val triggerTime: Long = System.currentTimeMillis() + 10 * 60 * 1000,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceJson: String? = null,
    val loopPlayback: Boolean = false,
    val showError: Boolean = false,
    val playbackProgress: Float = 0f,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val recordingElapsedSeconds: Int = 0 // For displaying recording time
)


class AddEditViewModel(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository,
    context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState = _uiState.asStateFlow()
    
    // Audio
    private val audioDir = File(context.filesDir, "reminders").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "temp_recordings").apply { mkdirs() }
    private val recorder = AndroidAudioRecorder(context)
    private val player = AndroidAudioPlayer(context)
    private var tempAudioFile: File? = null
    private var playbackJob: kotlinx.coroutines.Job? = null
    private var recordingTimerJob: kotlinx.coroutines.Job? = null
    
    companion object {
        const val MAX_RECORDING_SECONDS = 5 * 60 // 5 minutes
    }

    init {
        player.onCompletion = {
            stopPlayback()
        }
        
        // Observe Volume
        viewModelScope.launch {
            settingsRepository.appVolume.collect { volume ->
                player.setVolume(volume)
            }
        }
    }


    fun loadReminder(id: Long) {
        viewModelScope.launch {
            val reminder = repository.getReminder(id)
            if (reminder != null) {
                _uiState.value = _uiState.value.copy(
                    initialReminderId = reminder.id,
                    title = reminder.title ?: "",
                    reminderText = reminder.reminderText ?: "",
                    recordedAudioPath = reminder.audioPath,
                    triggerTime = reminder.nextTriggerAt,
                    recurrenceType = reminder.recurrenceType,
                    recurrenceJson = reminder.recurrenceJson,
                    loopPlayback = reminder.loopPlayback
                )
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
    }

    fun updateReminderText(newText: String) {
        _uiState.value = _uiState.value.copy(reminderText = newText, showError = false)
    }

    fun startRecording() {
        // Stop any playback first
        stopPlayback()
        
        // Record to TEMP file first (not final location)
        val fileName = "temp_${UUID.randomUUID()}.m4a"
        val file = File(tempDir, fileName)
        tempAudioFile = file
        
        try {
            recorder.start(file)
            _uiState.value = _uiState.value.copy(
                isRecording = true, 
                recordedAudioPath = null, 
                showError = false,
                recordingElapsedSeconds = 0
            )
            
            // Start timer to track elapsed time and auto-stop at max
            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                var elapsed = 0
                while (_uiState.value.isRecording && elapsed < MAX_RECORDING_SECONDS) {
                    kotlinx.coroutines.delay(1000)
                    elapsed++
                    _uiState.value = _uiState.value.copy(recordingElapsedSeconds = elapsed)
                }
                // Auto-stop if max reached
                if (elapsed >= MAX_RECORDING_SECONDS && _uiState.value.isRecording) {
                    stopRecording()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(showError = true)
        }
    }

    /**
     * Stop recording and transition to Preview mode.
     * The app stays on this screen - does NOT navigate away.
     */
    fun stopRecording() {
        recordingTimerJob?.cancel()
        recorder.stop()
        // Show preview with temp file path
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            recordedAudioPath = tempAudioFile?.absolutePath
        )
    }
    
    /**
     * Cancel/delete the current recording.
     */
    fun cancelRecording() {
        stopPlayback()
        tempAudioFile?.delete()
        tempAudioFile = null
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            recordedAudioPath = null
        )
    }
    
    fun playRecording() {
        val path = _uiState.value.recordedAudioPath ?: return
        val file = File(path)
        if (file.exists()) {
            stopPlayback()
            _uiState.value = _uiState.value.copy(isPlaying = true)
            player.playFile(file)
            startPlaybackTracking()
        }
    }
    
    fun stopPlayback() {
        playbackJob?.cancel()
        player.stop()
        _uiState.value = _uiState.value.copy(isPlaying = false, playbackProgress = 0f)
    }

    fun seekTo(progress: Float) {
        if (_uiState.value.isPlaying) {
            val duration = player.getDuration()
            val position = (duration * progress).toInt()
            player.seekTo(position)
        }
    }

    private fun startPlaybackTracking() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (true) {
                if (player.isPlaying()) {
                    val duration = player.getDuration()
                    val position = player.getCurrentPosition()
                    if (duration > 0) {
                        val progress = position.toFloat() / duration
                        _uiState.value = _uiState.value.copy(playbackProgress = progress)
                    }
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    fun setTriggerTime(time: Long) {
        _uiState.value = _uiState.value.copy(triggerTime = time)
    }
    
    fun setRecurrence(type: RecurrenceType, json: String? = null) {
        _uiState.value = _uiState.value.copy(recurrenceType = type, recurrenceJson = json)
    }

    fun setRecurrence(model: RecurrenceModel?) {
        if (model == null) {
            setRecurrence(RecurrenceType.NONE, null)
            return
        }
        val type = when (model) {
            is RecurrenceModel.Daily -> RecurrenceType.DAILY
            is RecurrenceModel.Weekly -> RecurrenceType.WEEKLY
            is RecurrenceModel.Monthly -> RecurrenceType.MONTHLY
            is RecurrenceModel.Custom -> RecurrenceType.CUSTOM
        }
        val json = RecurrenceUtils.toJson(model)
        setRecurrence(type, json)
    }
    
    fun setLoopPlayback(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(loopPlayback = enabled)
    }

    /**
     * Save the reminder. This is async - observe saveCompleted to know when done.
     * On success: moves temp file to final location, saves to DB, sets saveCompleted=true
     * On validation error: sets showError=true, stays on screen
     */
    fun saveReminder() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // VALIDATION: Must have Audio OR Text
            val hasAudio = state.recordedAudioPath != null
            val hasText = state.reminderText.isNotBlank()
            
            if (!hasAudio && !hasText) {
                _uiState.value = _uiState.value.copy(showError = true)
                return@launch
            }
            
            // Start saving
            _uiState.value = _uiState.value.copy(isSaving = true, showError = false)
            
            try {
                // Move temp file to final location on IO thread
                val finalAudioPath = if (tempAudioFile != null) {
                    withContext(Dispatchers.IO) {
                        val finalFileName = "${UUID.randomUUID()}.m4a"
                        val finalFile = File(audioDir, finalFileName)
                        tempAudioFile?.copyTo(finalFile, overwrite = true)
                        tempAudioFile?.delete()
                        finalFile.absolutePath
                    }
                } else {
                    state.recordedAudioPath
                }

                // Auto-advance start time if in past for recurring reminders
                var finalTriggerTime = state.triggerTime
                if (state.recurrenceType != RecurrenceType.NONE && finalTriggerTime < System.currentTimeMillis()) {
                    val tempReminder = ReminderEntity(
                        id = 0,
                        title = "",
                        nextTriggerAt = finalTriggerTime,
                        recurrenceType = state.recurrenceType,
                        recurrenceJson = state.recurrenceJson
                    )
                    val nextFutureTime = RecurrenceUtils.computeNextTrigger(tempReminder, System.currentTimeMillis())
                    if (nextFutureTime != null) {
                        finalTriggerTime = nextFutureTime
                        com.example.voicereminder.util.FileLogger.log("AddEdit: Auto-advanced past recurring reminder to $finalTriggerTime")
                    }
                }

                // Generate smart fallback label if empty
                val smartLabel = if (state.title.isBlank()) {
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = finalTriggerTime
                    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                    val timeStr = sdf.format(cal.time)
                    when {
                        finalAudioPath != null -> "Voice reminder"
                        !state.reminderText.isBlank() -> "Reminder at $timeStr"
                        else -> "Reminder at $timeStr"
                    }
                } else state.title

                val reminder = ReminderEntity(
                    id = if (state.initialReminderId != -1L) state.initialReminderId else 0L,
                    title = smartLabel,
                    reminderText = if (state.reminderText.isBlank()) null else state.reminderText,
                    audioPath = finalAudioPath,
                    nextTriggerAt = finalTriggerTime,
                    recurrenceType = state.recurrenceType,
                    recurrenceJson = state.recurrenceJson,
                    loopPlayback = state.loopPlayback
                )
                
                if (state.initialReminderId != -1L) {
                    repository.updateReminder(reminder)
                    scheduler.schedule(reminder)
                } else {
                    val id = repository.insertReminder(reminder)
                    val savedReminder = reminder.copy(id = id)
                    scheduler.schedule(savedReminder)
                }
                
                // Signal success - screen should now navigate back
                _uiState.value = _uiState.value.copy(isSaving = false, saveCompleted = true)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isSaving = false, showError = true)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        player.stop()
        playbackJob?.cancel()
        // Clean up temp file if not saved
        tempAudioFile?.delete()
    }
}
