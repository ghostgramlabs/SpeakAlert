package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE)
    )
    
    if (!showTimePicker) {
        // Date Picker with Quick Presets
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            showTimePicker = true
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        ) {
            Column {
                // Quick Preset Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val calendar = Calendar.getInstance()
                    
                    // In 1 hour
                    OutlinedButton(
                        onClick = {
                            calendar.add(Calendar.HOUR_OF_DAY, 1)
                            onConfirm(com.ghostgramlabs.speakalert.util.DateUtils.normalizeToMinute(calendar.timeInMillis))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("In 1 hour", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // Tomorrow morning (9 AM)
                    OutlinedButton(
                        onClick = {
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                            calendar.set(Calendar.HOUR_OF_DAY, 9)
                            calendar.set(Calendar.MINUTE, 0)
                            onConfirm(calendar.timeInMillis)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tomorrow 9AM", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // Next week
                    OutlinedButton(
                        onClick = {
                            calendar.add(Calendar.WEEK_OF_YEAR, 1)
                            onConfirm(com.ghostgramlabs.speakalert.util.DateUtils.normalizeToMinute(calendar.timeInMillis))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next week", style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                DatePicker(state = datePickerState)
            }
        }
    } else {
        // Time Picker
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Time") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate?.let { dateMillis ->
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = dateMillis
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onConfirm(calendar.timeInMillis)
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
