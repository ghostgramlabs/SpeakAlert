package com.ghostgramlabs.speakalert.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(
    onDismiss: () -> Unit
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
                .fillMaxHeight(0.92f)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.set_app_guide),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.help_intro, APP_DISPLAY_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HelpSection(
                    title = stringResource(R.string.help_s1_title),
                    content = stringResource(R.string.help_s1_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s2_title),
                    content = stringResource(R.string.help_s2_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s3_title),
                    content = stringResource(R.string.help_s3_content, APP_DISPLAY_NAME)
                )
                HelpSection(
                    title = stringResource(R.string.help_s4_title),
                    content = stringResource(R.string.help_s4_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s5_title),
                    content = stringResource(R.string.help_s5_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s6_title),
                    content = stringResource(R.string.help_s6_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s7_title),
                    content = stringResource(R.string.help_s7_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s8_title),
                    content = stringResource(R.string.help_s8_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s9_title),
                    content = stringResource(R.string.help_s9_content, APP_DISPLAY_NAME)
                )
                HelpSection(
                    title = stringResource(R.string.help_s10_title),
                    content = stringResource(R.string.help_s10_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s11_title),
                    content = stringResource(R.string.help_s11_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s12_title),
                    content = stringResource(R.string.help_s12_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s13_title),
                    content = stringResource(R.string.help_s13_content)
                )
                HelpSection(
                    title = stringResource(R.string.help_s14_title),
                    content = stringResource(R.string.help_s14_content)
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
