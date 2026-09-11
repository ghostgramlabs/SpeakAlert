package com.ghostgramlabs.speakalert.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickStartGuide(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }
    val pages = listOf(
        R.string.quick_create_title to R.string.quick_create_body,
        R.string.quick_ready_title to R.string.quick_ready_body,
        R.string.quick_actions_title to R.string.quick_actions_body,
        R.string.quick_try_title to R.string.quick_try_body,
        R.string.quick_settings_title to R.string.quick_settings_body
    )
    val scrollState = rememberScrollState()
    LaunchedEffect(page) { scrollState.scrollTo(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(0.9f).padding(horizontal = 24.dp)
                .navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.quick_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.quick_progress, page + 1, pages.size),
                style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = (page + 1).toFloat() / pages.size,
                modifier = Modifier.fillMaxWidth())
            Column(Modifier.weight(1f).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(pages[page].first), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(pages[page].second), style = MaterialTheme.typography.bodyLarge)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (page > 0) {
                    OutlinedButton(onClick = { page-- }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.quick_back))
                    }
                }
                Button(onClick = { if (page == pages.lastIndex) onOpenSettings() else page++ },
                    modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (page == pages.lastIndex) R.string.quick_review_settings else R.string.quick_next))
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.quick_later))
            }
        }
    }
}
