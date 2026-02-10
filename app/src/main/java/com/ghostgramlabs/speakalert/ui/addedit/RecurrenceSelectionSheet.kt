package com.ghostgramlabs.speakalert.ui.addedit

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.domain.models.*
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceSelectionSheet(
    initialType: RecurrenceType,
    initialJson: String?,
    onRecurrenceSelected: (RecurrenceModel?) -> Unit,
    onDismiss: () -> Unit
) {
    var showMonthlySheet by remember { mutableStateOf(false) }
    var showCustomSheet by remember { mutableStateOf(false) }
    var showWeeklySheet by remember { mutableStateOf(false) }

    val initialModel = remember(initialType, initialJson) {
        RecurrenceUtils.fromJson(initialType, initialJson)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                "Repeat",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Options
            RecurrenceOptionItem("Does not repeat", selected = initialType == RecurrenceType.NONE) {
                onRecurrenceSelected(null)
                onDismiss()
            }
            RecurrenceOptionItem("Daily", selected = initialType == RecurrenceType.DAILY) {
                onRecurrenceSelected(RecurrenceModel.Daily())
                onDismiss()
            }
            RecurrenceOptionItem("Weekly", selected = initialType == RecurrenceType.WEEKLY, hasSubMenu = true) {
                showWeeklySheet = true
            }
            RecurrenceOptionItem("Monthly", selected = initialType == RecurrenceType.MONTHLY, hasSubMenu = true) {
                showMonthlySheet = true
            }
            RecurrenceOptionItem("Custom", selected = initialType == RecurrenceType.CUSTOM, hasSubMenu = true) {
                showCustomSheet = true
            }
        }
    }

    if (showMonthlySheet) {
        MonthlyConfigSheet(
            initialModel = initialModel as? RecurrenceModel.Monthly,
            onSave = { model ->
                onRecurrenceSelected(model)
                onDismiss() // Close main sheet too
            },
            onCancel = { showMonthlySheet = false }
        )
    }

    if (showWeeklySheet) {
        WeeklyConfigSheet(
            initialModel = initialModel as? RecurrenceModel.Weekly,
            onSave = { model -> 
                onRecurrenceSelected(model)
                onDismiss()
            },
            onCancel = { showWeeklySheet = false }
        )
    }
    
    if (showCustomSheet) {
        CustomConfigSheet(
            initialModel = initialModel as? RecurrenceModel.Custom,
            onSave = { model -> 
                onRecurrenceSelected(model)
                onDismiss()
            },
            onCancel = { showCustomSheet = false }
        )
    }
}

@Composable
fun RecurrenceOptionItem(
    text: String, 
    selected: Boolean, 
    hasSubMenu: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text, 
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (hasSubMenu) {
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (selected) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// --- SUB SHEETS ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonthlyConfigSheet(
    initialModel: RecurrenceModel.Monthly? = null,
    onSave: (RecurrenceModel.Monthly) -> Unit,
    onCancel: () -> Unit
) {
    var variant by remember { mutableStateOf(initialModel?.variant ?: MonthlyVariant.DAY_OF_MONTH) }
    
    // Multi-day selection
    val selectedDays = remember { 
        mutableStateListOf<Int>().apply {
            if (initialModel != null && initialModel.daysOfMonth.isNotEmpty()) {
                addAll(initialModel.daysOfMonth)
            } else {
                // Default to current day of month
                add(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            }
        }
    }
    
    // End Rule
    var endRuleType by remember { 
        mutableStateOf(when(initialModel?.endRule?.type) {
            EndRuleType.UNTIL_DATE -> "DATE"
            EndRuleType.AFTER_OCCURRENCES -> "COUNT"
            else -> "NEVER"
        })
    } 
    var endDate by remember { 
        mutableLongStateOf(
            initialModel?.endRule?.endDateMillis 
            ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
        )
    }
    var occurrences by remember { 
        mutableIntStateOf(
            initialModel?.endRule?.count ?: 5
        )
    }
    
    // Missed Policy - default to SKIP_TO_NEXT now
    var missedPolicy by remember { mutableStateOf(initialModel?.missedPolicy ?: MissedPolicy.SKIP_TO_NEXT) }

    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text("Monthly Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Repeat on", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = variant == MonthlyVariant.DAY_OF_MONTH,
                    onClick = { variant = MonthlyVariant.DAY_OF_MONTH },
                    label = { Text("Day(s) of Month") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = variant == MonthlyVariant.LAST_DAY,
                    onClick = { variant = MonthlyVariant.LAST_DAY },
                    label = { Text("Last Day") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            
            if (variant == MonthlyVariant.DAY_OF_MONTH) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select days (tap to toggle)", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Day selector grid - 7 columns × 5 rows (1-31)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (day in 1..31) {
                        val isSelected = day in selectedDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    if (selectedDays.size > 1) {
                                        selectedDays.remove(day)
                                    }
                                    // Prevent deselecting last day
                                } else {
                                    selectedDays.add(day)
                                }
                            },
                            label = { 
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            modifier = Modifier.size(width = 44.dp, height = 32.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Selected: ${selectedDays.sorted().joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = {
                        val endRule = when(endRuleType) {
                            "DATE" -> RecurrenceEndRule(type = EndRuleType.UNTIL_DATE, endDateMillis = endDate)
                            "COUNT" -> RecurrenceEndRule(type = EndRuleType.AFTER_OCCURRENCES, count = occurrences)
                            else -> RecurrenceEndRule(type = EndRuleType.NEVER)
                        }
                        onSave(RecurrenceModel.Monthly(
                            variant = variant,
                            daysOfMonth = if (variant == MonthlyVariant.DAY_OF_MONTH) selectedDays.toSet() else emptySet(),
                            endRule = endRule,
                            missedPolicy = missedPolicy
                        ))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyConfigSheet(
    initialModel: RecurrenceModel.Weekly? = null,
    onSave: (RecurrenceModel.Weekly) -> Unit,
    onCancel: () -> Unit
) {
    val days = remember { mutableStateListOf<Int>().apply {
        if (initialModel != null) {
            addAll(initialModel.daysOfWeek)
        }
    }}
    val weekdays = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")
    
    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom=32.dp)) {
            Text("Weekly Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select Days", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, 
                modifier = Modifier.fillMaxWidth()
            ) {
                weekdays.forEach { (day, label) ->
                    val isSelected = days.contains(day)
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (isSelected) days.remove(day) else days.add(day) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
             Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = { onSave(RecurrenceModel.Weekly(days.toSet())) },
                    modifier = Modifier.weight(1f),
                    enabled = days.isNotEmpty()
                ) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomConfigSheet(
    initialModel: RecurrenceModel.Custom? = null,
    onSave: (RecurrenceModel.Custom) -> Unit,
    onCancel: () -> Unit
) {
    var intervalText by remember { mutableStateOf(initialModel?.interval?.toString() ?: "1") }
    var unit by remember { mutableStateOf(initialModel?.unit ?: TimeUnit.DAYS) }
    var expanded by remember { mutableStateOf(false) }
    
    // Validate interval
    val intervalValue = intervalText.toIntOrNull() ?: 0
    val isValid = intervalValue >= 1

    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom=32.dp)) {
            Text("Custom Interval", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Every", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { newValue ->
                        // Allow empty or digits only
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            intervalText = newValue
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    isError = intervalText.isNotEmpty() && !isValid,
                    placeholder = { Text("1") }
                )
                Spacer(modifier = Modifier.width(16.dp))
                
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(unit.name)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TimeUnit.values().forEach { u ->
                            DropdownMenuItem(text = { Text(u.name) }, onClick = { unit = u; expanded = false })
                        }
                    }
                }
            }
            
            if (!isValid && intervalText.isNotEmpty()) {
                Text(
                    "Interval must be at least 1",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
             Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = { onSave(RecurrenceModel.Custom(intervalValue, unit)) },
                    modifier = Modifier.weight(1f),
                    enabled = isValid
                ) { Text("Save") }
            }
        }
    }
}
