package com.example.voicereminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.voicereminder.data.AppContainer
import com.example.voicereminder.data.AppContainerImpl

import kotlinx.coroutines.launch

class VoiceReminderApp : Application() {

    // instance for manual Dependency Injection
    lateinit var container: AppContainer
    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
        createNotificationChannels()
        
        // Initialize file-based logging for debugging
        com.example.voicereminder.util.FileLogger.init(this)
        
        applicationScope.launch {
            container.settingsRepository.debugLoggingEnabled.collect { enabled ->
                com.example.voicereminder.util.FileLogger.isEnabled = enabled
                if (enabled) {
                    com.example.voicereminder.util.FileLogger.log("App Started / Logging Enabled")
                }
            }
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Playback channel for foreground service
            val playbackChannel = NotificationChannel(
                "playback_channel",
                "Reminder Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows controls while playing reminders"
                setSound(null, null) // No sound for playback notification
            }
            notificationManager.createNotificationChannel(playbackChannel)
            
            // Reminder alerts channel (already created in NotificationHelper, but ensure it exists)
            val alertChannel = NotificationChannel(
                "voice_reminder_channel",
                "Reminder Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
