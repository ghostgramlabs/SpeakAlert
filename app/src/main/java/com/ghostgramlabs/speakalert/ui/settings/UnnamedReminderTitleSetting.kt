package com.ghostgramlabs.speakalert.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.util.*
import kotlinx.coroutines.launch

@Composable
private fun titleSettings() = (LocalContext.current.applicationContext as VoiceReminderApp).container.settingsRepository

@Composable
fun rememberUnnamedReminderTitleStyle(): UnnamedReminderTitle? {
    val style by titleSettings().unnamedReminderTitle.collectAsState(initial = null)
    return style
}

@Composable
fun reminderTitle(item: ReminderEntity, style: UnnamedReminderTitle?): String {
    val fallback = when (style) {
        UnnamedReminderTitle.CREATION_TIME -> stringResource(R.string.home_created_at, DateUtils.formatTimeOnly(item.createdAt))
        UnnamedReminderTitle.REMINDER_TYPE -> stringResource(
            if (ReminderAudioSource.isContentUri(item.audioPath)) R.string.unnamed_audio_reminder
            else if (!item.audioPath.isNullOrBlank()) R.string.unnamed_voice_reminder
            else R.string.unnamed_generic_reminder
        )
        else -> ""
    }
    return reminderDisplayTitle(item.title, item.reminderText, fallback)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UnnamedReminderTitleSetting() {
    val repository = titleSettings()
    val style by repository.unnamedReminderTitle.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    Text(stringResource(R.string.unnamed_title_setting), style = MaterialTheme.typography.bodyMedium)
    Text(stringResource(R.string.unnamed_title_description), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            UnnamedReminderTitle.CREATION_TIME to R.string.unnamed_creation_time,
            UnnamedReminderTitle.REMINDER_TYPE to R.string.unnamed_reminder_type,
            UnnamedReminderTitle.NONE to R.string.unnamed_no_title
        ).forEach { (choice, label) ->
            FilterChip(selected = style == choice, enabled = style != null,
                onClick = { scope.launch { repository.setUnnamedReminderTitle(choice) } },
                label = { Text(stringResource(label)) })
        }
    }
}
