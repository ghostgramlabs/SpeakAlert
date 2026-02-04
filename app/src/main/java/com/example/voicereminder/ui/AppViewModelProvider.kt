package com.example.voicereminder.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.voicereminder.VoiceReminderApp
import com.example.voicereminder.ui.home.HomeViewModel
import com.example.voicereminder.ui.addedit.AddEditViewModel
import com.example.voicereminder.ui.details.ReminderDetailsViewModel
import com.example.voicereminder.ui.settings.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                voiceReminderApplication().container.reminderRepository,
                voiceReminderApplication().container.missedReminderRepository,
                voiceReminderApplication().container.alarmScheduler
            )
        }
        initializer {
            AddEditViewModel(
                voiceReminderApplication().container.reminderRepository,
                voiceReminderApplication().container.alarmScheduler,
                voiceReminderApplication().container.settingsRepository,
                voiceReminderApplication().applicationContext // For audio dir
            )
        }
        initializer {
            ReminderDetailsViewModel(
                voiceReminderApplication().container.reminderRepository,
                voiceReminderApplication().container.alarmScheduler,
                voiceReminderApplication().applicationContext
            )

        }
        initializer {
            SettingsViewModel(
                voiceReminderApplication().container.settingsRepository,
                voiceReminderApplication().container.reminderRepository,
                voiceReminderApplication().container.alarmScheduler
            )
        }
    }
}

fun CreationExtras.voiceReminderApplication(): VoiceReminderApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VoiceReminderApp)
