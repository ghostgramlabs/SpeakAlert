package com.ghostgramlabs.speakalert.data

import android.content.Context
import com.ghostgramlabs.speakalert.data.database.AppDatabase
import com.ghostgramlabs.speakalert.data.repository.OfflineReminderRepository
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository

interface AppContainer {
    val reminderRepository: ReminderRepository

    val settingsRepository: com.ghostgramlabs.speakalert.data.repository.SettingsRepository
    val missedReminderRepository: com.ghostgramlabs.speakalert.data.repository.MissedReminderRepository
    val alarmScheduler: com.ghostgramlabs.speakalert.alarm.AlarmScheduler
}

class AppContainerImpl(private val context: Context) : AppContainer {
    override val reminderRepository: ReminderRepository by lazy {
        OfflineReminderRepository(context, AppDatabase.getDatabase(context).reminderDao())
    }
    override val alarmScheduler: com.ghostgramlabs.speakalert.alarm.AlarmScheduler by lazy {
        com.ghostgramlabs.speakalert.alarm.AndroidAlarmScheduler(context)
    }
    
    override val missedReminderRepository: com.ghostgramlabs.speakalert.data.repository.MissedReminderRepository by lazy {
        com.ghostgramlabs.speakalert.data.repository.MissedReminderRepositoryImpl(
            context,
            AppDatabase.getDatabase(context).missedReminderDao()
        )
    }

    override val settingsRepository: com.ghostgramlabs.speakalert.data.repository.SettingsRepository by lazy {
        com.ghostgramlabs.speakalert.data.repository.SettingsRepository(context)
    }
}
