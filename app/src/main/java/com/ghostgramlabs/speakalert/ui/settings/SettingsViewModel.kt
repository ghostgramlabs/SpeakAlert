package com.ghostgramlabs.speakalert.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
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

    val speakTextIfNoVoice = settingsRepository.speakTextIfNoVoice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    fun setSpeakTextIfNoVoice(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSpeakTextIfNoVoice(enabled)
        }
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
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
            putExtra(android.content.Intent.EXTRA_SUBJECT, "SpeakAlert App Logs")
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

    fun requestAppReview(context: android.content.Context) {
        val manager = com.google.android.play.core.review.ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val activity = findActivity(context)
                if (activity != null) {
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        // The flow has finished. The API does not indicate whether the user
                        // reviewed or not, or even whether the review dialog was shown.
                    }
                } else {
                    openPlayStore(context)
                }
            } else {
                // There was some problem, continue regardless of the result.
                openPlayStore(context)
            }
        }
    }

    private fun findActivity(context: android.content.Context): android.app.Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return ctx as? android.app.Activity
    }

    private fun openPlayStore(context: android.content.Context) {
        val packageName = context.packageName
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("market://details?id=$packageName")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
