package com.example.voicereminder.data

import android.content.Context
import com.example.voicereminder.data.database.AppDatabase
import com.example.voicereminder.data.repository.OfflineReminderRepository
import com.example.voicereminder.data.repository.ReminderRepository

interface AppContainer {
    val reminderRepository: ReminderRepository

    val settingsRepository: com.example.voicereminder.data.repository.SettingsRepository
    val missedReminderRepository: com.example.voicereminder.data.repository.MissedReminderRepository
    val alarmScheduler: com.example.voicereminder.alarm.AlarmScheduler
}

class AppContainerImpl(private val context: Context) : AppContainer {
    override val reminderRepository: ReminderRepository by lazy {
        OfflineReminderRepository(AppDatabase.getDatabase(context).reminderDao())
    }
    override val alarmScheduler: com.example.voicereminder.alarm.AlarmScheduler by lazy {
        com.example.voicereminder.alarm.AndroidAlarmScheduler(context)
    }
    
    override val missedReminderRepository: com.example.voicereminder.data.repository.MissedReminderRepository by lazy {
        com.example.voicereminder.data.repository.MissedReminderRepositoryImpl(AppDatabase.getDatabase(context).missedReminderDao())
    }

    override val settingsRepository: com.example.voicereminder.data.repository.SettingsRepository by lazy {
        com.example.voicereminder.data.repository.SettingsRepository(context)
    }
}
