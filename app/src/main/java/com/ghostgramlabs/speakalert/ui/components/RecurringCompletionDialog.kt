package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ghostgramlabs.speakalert.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringCompletionDialog(
    onDismiss: () -> Unit,
    onMarkTodayAsDone: () -> Unit,
    onStopCompletely: () -> Unit,
    onEditSchedule: () -> Unit
) {
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
                text = stringResource(R.string.rcd_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.rcd_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ActionSheetRow(
                icon = Icons.Filled.Done,
                label = stringResource(R.string.rcd_mark_occurrence),
                subLabel = stringResource(R.string.rcd_sub_keep_next),
                onClick = onMarkTodayAsDone,
                emphasize = true
            )
            ActionSheetRow(
                icon = Icons.Filled.Edit,
                label = stringResource(R.string.rcd_edit_schedule),
                subLabel = stringResource(R.string.rcd_sub_change_rules),
                onClick = onEditSchedule
            )
            ActionSheetRow(
                icon = Icons.Filled.Stop,
                label = stringResource(R.string.rcd_stop_recurring),
                subLabel = stringResource(R.string.rcd_sub_delete_future),
                onClick = onStopCompletely,
                isDestructive = true
            )

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}
