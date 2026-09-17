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
import androidx.compose.ui.res.stringResource
import com.ghostgramlabs.speakalert.R

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
                text = stringResource(R.string.pua_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.pua_scheduled_for, DateUtils.formatDateTime(scheduledTime)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ActionSheetRow(
                icon = Icons.Filled.AccessTime,
                label = stringResource(R.string.pua_set_new_time),
                subLabel = stringResource(R.string.pua_sub_reschedule),
                onClick = onReschedule,
                emphasize = true
            )

            ActionSheetRow(
                icon = Icons.Filled.PlayArrow,
                label = stringResource(R.string.pua_just_play),
                subLabel = stringResource(R.string.pua_sub_play_now),
                onClick = onPlayNow,
                emphasize = false
            )

            ActionSheetRow(
                icon = Icons.Filled.Close,
                label = stringResource(R.string.action_cancel),
                subLabel = stringResource(R.string.pua_sub_keep_done),
                onClick = onCancel,
                isDestructive = true
            )
        }
    }
}
