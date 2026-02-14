package com.ghostgramlabs.speakalert.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
            Text(
                text = "Reminder missed",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Scheduled for ${DateUtils.formatDateTime(scheduledTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "What would you like to do?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
            
            // 1. Reschedule (Primary)
            ActionItem(
                icon = Icons.Filled.AccessTime,
                label = "Set a new time",
                onClick = onReschedule,
                highlight = true
            )
            
            // 2. Play Voice Now (Neutral)
            ActionItem(
                icon = Icons.Filled.PlayArrow,
                label = "Just play the reminder",
                onClick = onPlayNow,
                highlight = false
            )
            
            // 3. Keep as Done / Cancel (Secondary)
            ActionItem(
                icon = Icons.Filled.Close,
                label = "Cancel",
                subLabel = "Keep as done",
                onClick = onCancel,
                highlight = false,
                isDestructive = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    subLabel: String? = null,
    onClick: () -> Unit,
    highlight: Boolean,
    isDestructive: Boolean = false
) {
    val containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else contentColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else contentColor
                )
                if (subLabel != null) {
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
