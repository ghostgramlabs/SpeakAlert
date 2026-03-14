package com.ghostgramlabs.speakalert.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.ui.components.ActionSheetRow
import com.ghostgramlabs.speakalert.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastUndoneActionSheet(
    scheduledTime: Long,
    onReschedule: () -> Unit,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Reminder missed",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Scheduled for ${DateUtils.formatDateTime(scheduledTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ActionSheetRow(
                icon = Icons.Filled.AccessTime,
                label = "Set a new time",
                subLabel = "Reschedule this reminder",
                onClick = onReschedule,
                emphasize = true
            )

            ActionSheetRow(
                icon = Icons.Filled.PlayArrow,
                label = "Just play the reminder",
                subLabel = "Listen now without rescheduling",
                onClick = onPlayNow,
                emphasize = false
            )

            ActionSheetRow(
                icon = Icons.Filled.Close,
                label = "Cancel",
                subLabel = "Keep as done",
                onClick = onCancel,
                isDestructive = true
            )
        }
    }
}
