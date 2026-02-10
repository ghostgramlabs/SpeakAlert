package com.ghostgramlabs.speakalert.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.ui.home.HomeViewModel
import com.ghostgramlabs.speakalert.ui.addedit.AddEditViewModel
import com.ghostgramlabs.speakalert.ui.details.ReminderDetailsViewModel
import com.ghostgramlabs.speakalert.ui.settings.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                voiceReminderApplication().container.reminderRepository,
                voiceReminderApplication().container.missedReminderRepository,
                voiceReminderApplication().container.alarmScheduler,
                voiceReminderApplication().container.settingsRepository
            )
        }
        initializer {
            AddEditViewModel(
                voiceReminderApplication().container.reminderRepository,
                voiceReminderApplication().container.alarmScheduler,
                voiceReminderApplication().container.settingsRepository,
                voiceReminderApplication().applicationContext,
                com.ghostgramlabs.speakalert.audio.AndroidAudioRecorder(voiceReminderApplication().applicationContext),
                com.ghostgramlabs.speakalert.audio.AndroidAudioPlayer(voiceReminderApplication().applicationContext)
            )
        }
        initializer {
            ReminderDetailsViewModel(
                voiceReminderApplication().container.reminderRepository,
                voiceReminderApplication().container.alarmScheduler,
                voiceReminderApplication().container.settingsRepository,
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
