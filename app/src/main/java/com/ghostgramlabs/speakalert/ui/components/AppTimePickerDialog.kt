package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ghostgramlabs.speakalert.R

/**
 * The app's one time picker dialog.
 *
 * Every caller used to build its own `AlertDialog(text = { TimePicker(...) })`. That slot is
 * height-constrained and scrolled by AlertDialog, while the Material clock dial demands a fixed
 * minimum: where the two disagree the dial's internal weighted rows are asked to share a negative
 * amount of space, and Compose crashes measuring it - `Cannot round NaN value`, reported from
 * Android 8.1 hardware in the field. Hosting the picker in a plain Dialog gives it the room it
 * asks for instead of squeezing it.
 *
 * Where the window is genuinely too short for a dial - landscape, split screen, a small phone -
 * it falls back to [TimeInput], the two-field entry Material provides for exactly that case,
 * rather than trying to fit a clock that cannot fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    title: String = stringResource(R.string.dtp_pick_time),
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )
    // The dial plus this dialog's title and buttons need roughly this much height. Below it the
    // text entry is not a downgrade, it is the only thing that fits.
    val tallEnoughForDial = LocalConfiguration.current.screenHeightDp >= MIN_DIAL_HEIGHT_DP

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (tallEnoughForDial) {
                    TimePicker(state = state)
                } else {
                    TimeInput(state = state)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(state.hour, state.minute) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_apply))
                    }
                }
            }
        }
    }
}

/** Dial (256dp) plus this dialog's title, padding and buttons, with room to spare. */
private const val MIN_DIAL_HEIGHT_DP = 500
