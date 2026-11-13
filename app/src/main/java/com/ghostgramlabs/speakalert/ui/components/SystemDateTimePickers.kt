package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

fun shouldUseSystemDateTimePickers(locale: Locale = Locale.getDefault()): Boolean {
    return DecimalFormatSymbols.getInstance(locale).zeroDigit != '0'
}

/**
 * Material's date picker takes its first weekday from the region and offers no way to set it,
 * so a chosen "Week starts on" day also selects the platform dialog, which does.
 */
fun shouldUseSystemDatePicker(context: android.content.Context): Boolean =
    shouldUseSystemDateTimePickers() ||
        com.ghostgramlabs.speakalert.util.WeekStartPreferences.firstDayOfWeek(context) != 0

/** The platform dialog theme matching the app's own light or dark theme, not the system's. */
@Composable
private fun systemPickerTheme(): Int =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        com.ghostgramlabs.speakalert.R.style.SpeakAlertPicker_Dark
    } else {
        com.ghostgramlabs.speakalert.R.style.SpeakAlertPicker_Light
    }

@Composable
fun SystemDatePickerDialog(
    initialSelectedDateMillisUtc: Long,
    onDismiss: () -> Unit,
    onConfirm: (selectedDateMillisUtc: Long) -> Unit,
) {
    val context = LocalContext.current
    val pickerTheme = systemPickerTheme()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    DisposableEffect(initialSelectedDateMillisUtc) {
        val initial = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = initialSelectedDateMillisUtc
        }
        var confirmed = false
        val dialog = android.app.DatePickerDialog(
            context,
            pickerTheme,
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
        com.ghostgramlabs.speakalert.util.WeekStartPreferences.firstDayOfWeek(context)
            .takeIf { it != 0 }?.let { dialog.datePicker.firstDayOfWeek = it }
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
    val pickerTheme = systemPickerTheme()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    DisposableEffect(initialHour, initialMinute, is24Hour) {
        var confirmed = false
        val dialog = android.app.TimePickerDialog(
            context,
            pickerTheme,
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
