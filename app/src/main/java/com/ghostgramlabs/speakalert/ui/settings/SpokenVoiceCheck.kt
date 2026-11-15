package com.ghostgramlabs.speakalert.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.util.TtsVoiceCheck
import java.util.Locale

/**
 * Tells the user whether the phone can speak the language reminders are read in, and where to
 * download the voice when it cannot. [ttsLanguageMode] is the "Spoken language" choice
 * (2 = English; otherwise the app's language, which is also what Auto-detect falls back to).
 * The check runs again whenever the screen resumes, so it updates after a download.
 */
@Composable
fun SpokenVoiceCheck(ttsLanguageMode: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = if (ttsLanguageMode == 2) Locale.ENGLISH else Locale.getDefault()
    val languageName = locale.getDisplayLanguage(Locale.getDefault()).ifBlank { locale.toLanguageTag() }
    var result by remember { mutableStateOf<TtsVoiceCheck.Result?>(null) }
    var run by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) run++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(locale, run) {
        result = null
        result = TtsVoiceCheck.check(context, locale)
    }

    val status = result?.status
    Column(modifier = modifier.fillMaxWidth().testTag("voice_check"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (status) {
                null -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                TtsVoiceCheck.Status.WORKING -> Icon(
                    Icons.Filled.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                )
                else -> Icon(
                    Icons.Filled.Warning, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = when (status) {
                    null -> stringResource(R.string.set_voice_check_checking, languageName)
                    TtsVoiceCheck.Status.WORKING -> stringResource(R.string.set_voice_check_ok, languageName)
                    TtsVoiceCheck.Status.ALTERNATIVE_VOICE -> stringResource(R.string.set_voice_check_alternative, languageName)
                    TtsVoiceCheck.Status.NOT_WORKING -> stringResource(R.string.set_voice_check_missing, languageName)
                    TtsVoiceCheck.Status.NO_ENGINE -> stringResource(R.string.set_voice_check_no_engine)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (status == null || status == TtsVoiceCheck.Status.WORKING) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f)
            )
        }
        if (status != null && status != TtsVoiceCheck.Status.WORKING) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = {
                    if (!TtsVoiceCheck.openVoiceDownload(context, result?.enginePackage)) {
                        Toast.makeText(context, R.string.set_voice_settings_unavailable, Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.set_voice_download)) }
                TextButton(onClick = { run++ }) { Text(stringResource(R.string.set_voice_check_again)) }
            }
        }
    }
}
