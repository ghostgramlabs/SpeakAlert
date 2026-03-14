package com.ghostgramlabs.speakalert.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import com.ghostgramlabs.speakalert.ui.components.ActionSheetRow
import com.ghostgramlabs.speakalert.util.isDefaultAppDisplayName

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MissedReminderRecoveryDialog(
    missedReminders: List<MissedReminderEntity>,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit
) {
    val previewItems = missedReminders.take(3)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (missedReminders.size == 1) {
                    "You missed 1 reminder"
                } else {
                    "You missed ${missedReminders.size} reminders"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Play the latest missed reminder now, or keep everything in the Missed tab and review later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    previewItems.forEach { missed ->
                        Text(
                            text = buildMissedDisplayTitle(missed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (missedReminders.size > previewItems.size) {
                        Text(
                            text = "+${missedReminders.size - previewItems.size} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            ActionSheetRow(
                icon = Icons.Filled.PlayArrow,
                label = "Play latest missed reminder",
                subLabel = "Start playback right away",
                onClick = onPlayNow,
                emphasize = true
            )

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
            ) {
                Text("Keep in Missed tab")
            }
        }
    }
}

// Keep missed reminder labels user-friendly for untitled/legacy reminders.
private fun buildMissedDisplayTitle(missed: MissedReminderEntity): String {
    val userTitle = missed.title
        .trim()
        .takeIf { it.isNotEmpty() && !it.isDefaultAppDisplayName() }
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
