package com.ghostgramlabs.speakalert.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity

@Composable
fun MissedReminderRecoveryDialog(
    missedReminders: List<MissedReminderEntity>,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit
) {
    val previewItems = missedReminders.take(3)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (missedReminders.size == 1) {
                    "You missed 1 reminder"
                } else {
                    "You missed ${missedReminders.size} reminders"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                previewItems.forEach { missed ->
                    Text(
                        text = "- ${buildMissedDisplayTitle(missed)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (missedReminders.size > previewItems.size) {
                    Text(
                        text = "+${missedReminders.size - previewItems.size} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onPlayNow) {
                Text("Play missed reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Keep missed reminder labels user-friendly for untitled/legacy reminders.
private fun buildMissedDisplayTitle(missed: MissedReminderEntity): String {
    val userTitle = missed.title
        .trim()
        .takeIf { it.isNotEmpty() && !it.equals("SpeakAlert", ignoreCase = true) }
    if (userTitle != null) return userTitle

    val textFallback = missed.reminderText
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { text ->
            val words = text.split(Regex("\\s+"))
            if (words.size > 8) words.take(8).joinToString(" ") else text
        }
    return textFallback ?: "Reminder"
}
