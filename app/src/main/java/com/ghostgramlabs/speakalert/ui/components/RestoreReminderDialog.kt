package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ghostgramlabs.speakalert.R

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
                text = stringResource(R.string.rrd_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.rrd_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ActionSheetRow(
                icon = Icons.Filled.Schedule,
                label = stringResource(R.string.rrd_reschedule),
                subLabel = stringResource(R.string.rrd_sub_pick_new),
                onClick = {
                    onDismiss()
                    onReschedule()
                },
                emphasize = true
            )

            ActionSheetRow(
                icon = Icons.Filled.Notifications,
                label = stringResource(R.string.rrd_move_missed),
                subLabel = stringResource(R.string.rrd_sub_keep_missed),
                onClick = {
                    onDismiss()
                    onMoveToMissed()
                }
            )

            ActionSheetRow(
                icon = Icons.Filled.PlayArrow,
                label = stringResource(R.string.rrd_play),
                subLabel = stringResource(R.string.rrd_sub_hear_now),
                onClick = {
                    onDismiss()
                    onPlay()
                }
            )

            ActionSheetRow(
                icon = Icons.Filled.Done,
                label = stringResource(R.string.rrd_keep_done),
                subLabel = stringResource(R.string.rrd_sub_leave_done),
                onClick = {
                    onDismiss()
                    onKeepAsDone()
                },
                isDestructive = true
            )
        }
    }
}
