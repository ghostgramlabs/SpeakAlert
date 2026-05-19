package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

fun shouldUseSystemDateTimePickers(locale: Locale = Locale.getDefault()): Boolean {
    return DecimalFormatSymbols.getInstance(locale).zeroDigit != '0'
}

@Composable
fun SystemDatePickerDialog(
    initialSelectedDateMillisUtc: Long,
    onDismiss: () -> Unit,
    onConfirm: (selectedDateMillisUtc: Long) -> Unit,
) {
    val context = LocalContext.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    DisposableEffect(initialSelectedDateMillisUtc) {
        val initial = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = initialSelectedDateMillisUtc
        }
        var confirmed = false
        val dialog = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                confirmed = true
                val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                currentOnConfirm(picked)
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH),
        )
        dialog.setOnDismissListener {
            if (!confirmed) currentOnDismiss()
        }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

@Composable
fun SystemTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val context = LocalContext.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    DisposableEffect(initialHour, initialMinute, is24Hour) {
        var confirmed = false
        val dialog = android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                confirmed = true
                currentOnConfirm(hour, minute)
            },
            initialHour,
            initialMinute,
            is24Hour,
        )
        dialog.setOnDismissListener {
            if (!confirmed) currentOnDismiss()
        }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}
