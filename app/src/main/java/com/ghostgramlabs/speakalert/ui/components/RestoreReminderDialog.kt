package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreReminderDialog(
    onDismiss: () -> Unit,
    onReschedule: () -> Unit,
    onMoveToMissed: () -> Unit,
    onPlay: () -> Unit,
    onKeepAsDone: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Restore reminder",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "This reminder was completed earlier. Choose how you want to restore it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ActionSheetRow(
                icon = Icons.Filled.Schedule,
                label = "Reschedule reminder",
                subLabel = "Pick a new date and time",
                onClick = {
                    onDismiss()
                    onReschedule()
                },
                emphasize = true
            )

            ActionSheetRow(
                icon = Icons.Filled.Notifications,
                label = "Move to Missed",
                subLabel = "Keep it in the Missed tab for later",
                onClick = {
                    onDismiss()
                    onMoveToMissed()
                }
            )

            ActionSheetRow(
                icon = Icons.Filled.PlayArrow,
                label = "Play reminder",
                subLabel = "Hear it right now without restoring",
                onClick = {
                    onDismiss()
                    onPlay()
                }
            )

            ActionSheetRow(
                icon = Icons.Filled.Done,
                label = "Keep as Done",
                subLabel = "Leave the reminder completed",
                onClick = {
                    onDismiss()
                    onKeepAsDone()
                },
                isDestructive = true
            )
        }
    }
}
