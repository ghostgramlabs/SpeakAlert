package com.ghostgramlabs.speakalert.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val reminderRepository: com.ghostgramlabs.speakalert.data.repository.ReminderRepository,
    private val alarmScheduler: com.ghostgramlabs.speakalert.alarm.AlarmScheduler
) : ViewModel() {

    val autoPlayEnabled = settingsRepository.autoPlayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoPlayOnUnlockOnly = settingsRepository.autoPlayOnUnlockOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultSnoozeDuration = settingsRepository.defaultSnoozeDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val defaultFollowUpMinutes = settingsRepository.defaultFollowUpMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val speakTextIfNoVoice = settingsRepository.speakTextIfNoVoice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val privatePlaybackEnabled = settingsRepository.privatePlaybackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dndBypassEnabled = settingsRepository.dndBypassEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val toneOnlyMode = settingsRepository.toneOnlyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val toneOnlyAlertToneUri = settingsRepository.toneOnlyAlertToneUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ttsLanguageMode = settingsRepository.ttsLanguageMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val persistUntilDone = settingsRepository.persistUntilDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val followUpMaxRepeats = settingsRepository.followUpMaxRepeats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val themeMode = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val fullScreenAlertEnabled = settingsRepository.fullScreenAlertEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAutoPlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoPlayEnabled(enabled)
        }
    }

    fun setAutoPlayOnUnlockOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoPlayOnUnlockOnly(enabled)
        }
    }

    fun setDefaultSnoozeDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultSnoozeDuration(minutes)
        }
    }

    fun setDefaultFollowUpMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultFollowUpMinutes(minutes)
        }
    }

    fun setFollowUpMaxRepeats(count: Int) {
        viewModelScope.launch {
            settingsRepository.setFollowUpMaxRepeats(count)
        }
    }

    fun setSpeakTextIfNoVoice(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSpeakTextIfNoVoice(enabled)
        }
    }

    fun setPrivatePlaybackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrivatePlaybackEnabled(enabled)
        }
    }

    fun setDndBypassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDndBypassEnabled(enabled)
        }
    }

    fun setToneOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setToneOnlyMode(enabled)
        }
    }

    fun setToneOnlyAlertToneUri(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setToneOnlyAlertToneUri(uri)
        }
    }

    fun setTtsLanguageMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setTtsLanguageMode(mode)
        }
    }

    fun setPersistUntilDone(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPersistUntilDone(enabled)
        }
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setFullScreenAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFullScreenAlertEnabled(enabled)
        }
    }

    val debugLoggingEnabled = settingsRepository.debugLoggingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDebugLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDebugLoggingEnabled(enabled)
            // If enabled, force a log to confirm
            if (enabled) {
                 com.ghostgramlabs.speakalert.util.FileLogger.isEnabled = true
                 com.ghostgramlabs.speakalert.util.FileLogger.log("Logging enabled via Settings")
            }
        }
    }
    
    val appVolume = settingsRepository.appVolume
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    
    val loopTimeoutMinutes = settingsRepository.loopTimeoutMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    // Quiet Time State
    val quietTimeEnabled = settingsRepository.quietTimeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val quietTimeStartHour = settingsRepository.quietTimeStartHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 22)
    val quietTimeStartMinute = settingsRepository.quietTimeStartMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val quietTimeEndHour = settingsRepository.quietTimeEndHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)
    val quietTimeEndMinute = settingsRepository.quietTimeEndMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setAppVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.setAppVolume(volume)
        }
    }
    
    fun setLoopTimeoutMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setLoopTimeoutMinutes(minutes)
        }
    }
    
    fun setQuietTimeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setQuietTimeEnabled(enabled)
        }
    }
    
    fun setQuietTimeStart(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setQuietTimeStart(hour, minute)
        }
    }
    
    fun setQuietTimeEnd(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setQuietTimeEnd(hour, minute)
        }
    }
    
    fun sendLogs(context: android.content.Context) {
        val logFile = com.ghostgramlabs.speakalert.util.FileLogger.getLogFile()
        if (logFile == null || !logFile.exists()) {
             android.widget.Toast.makeText(context, "No log file found", android.widget.Toast.LENGTH_SHORT).show()
             return
        }
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            logFile
        )
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("ghostgramlabs@gmail.com"))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "$APP_DISPLAY_NAME App Logs")
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            context.startActivity(android.content.Intent.createChooser(intent, "Send Logs"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "No email app found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun scheduleTestReminder() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val triggerTime = now + 10_000 // 10 seconds from now
            
            val reminder = com.ghostgramlabs.speakalert.data.model.ReminderEntity(
                title = "Test Reminder",
                reminderText = "This is a test reminder to verify playback. It triggers 10 seconds after creation.",
                nextTriggerAt = triggerTime,
                recurrenceType = com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE,
                createdAt = now,
                // updatedAt = now -- Field doesn't exist in Entity
            )
            
            val id = reminderRepository.insertReminder(reminder)
            // We need to schedule it too! insert doesn't auto-schedule usually?
            // "AddEditViewModel" schedules it manually.
            val savedReminder = reminder.copy(id = id)
            alarmScheduler.schedule(savedReminder)
        }
    }
}
