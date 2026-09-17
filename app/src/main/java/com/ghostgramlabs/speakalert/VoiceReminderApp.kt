package com.ghostgramlabs.speakalert

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.ghostgramlabs.speakalert.data.AppContainer
import com.ghostgramlabs.speakalert.data.AppContainerImpl

import kotlinx.coroutines.launch
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME

class VoiceReminderApp : Application() {

    // instance for manual Dependency Injection
    lateinit var container: AppContainer
    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        com.ghostgramlabs.speakalert.util.DateUtils.init(this)
        // Keep cold process startup short. A foreground service's promotion deadline includes
        // application startup on some devices, so create its channel before any disk work.
        createNotificationChannels()
        container = AppContainerImpl(this)
        // Clock style is read from widgets, notifications and pure formatters, so seed the
        // shared holder before any UI renders.
        com.ghostgramlabs.speakalert.util.TimeFormat.initialize(
            context = this,
            scope = applicationScope,
            repository = container.settingsRepository
        )

        applicationScope.launch {
            // External-storage setup is not required for app/service startup and can be slow on
            // heavily customized devices. Initialize diagnostics away from the main thread.
            com.ghostgramlabs.speakalert.util.FileLogger.init(this@VoiceReminderApp)
            container.settingsRepository.debugLoggingEnabled.collect { enabled ->
                com.ghostgramlabs.speakalert.util.FileLogger.isEnabled = enabled
                if (enabled) {
                    com.ghostgramlabs.speakalert.util.FileLogger.log("App Started / Logging Enabled")
                }
            }
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Channel names and descriptions appear in the system's own settings, so they
            // follow the app's chosen language rather than the process default.
            val strings = com.ghostgramlabs.speakalert.util.AppLocale.localizedContext(this)

            // Playback channel for foreground service
            val playbackChannel = NotificationChannel(
                "playback_channel",
                strings.getString(R.string.channel_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = strings.getString(R.string.channel_playback_desc)
                setSound(null, null) // No sound for playback notification
            }
            notificationManager.createNotificationChannel(playbackChannel)
            
            // Reminder alerts channel (already created in NotificationHelper, but ensure it exists)
            val alertChannel = NotificationChannel(
                "voice_reminder_channel",
                strings.getString(R.string.channel_reminders_name, APP_DISPLAY_NAME),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = strings.getString(R.string.channel_reminders_desc, APP_DISPLAY_NAME)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
