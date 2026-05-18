package com.ghostgramlabs.speakalert.ui.addedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.domain.models.*
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.util.normalizeLocalizedDigitsOrNull
import com.ghostgramlabs.speakalert.util.toLocalizedIntOrNull
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceSelectionSheet(
    initialType: RecurrenceType,
    initialJson: String?,
    minEndDateTimeMillis: Long,
    onRecurrenceSelected: (RecurrenceModel?) -> Unit,
    onDismiss: () -> Unit
) {
    var showMonthlySheet by remember { mutableStateOf(false) }
    var showCustomSheet by remember { mutableStateOf(false) }
    var showWeeklySheet by remember { mutableStateOf(false) }
    val mainSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mainSheetMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f

    val initialModel = remember(initialType, initialJson) {
        RecurrenceUtils.fromJson(initialType, initialJson)
    }

    if (!showMonthlySheet && !showCustomSheet && !showWeeklySheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = mainSheetState,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 0.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = mainSheetMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SheetHeader(
                    title = "Repeat",
                    subtitle = "Choose how this reminder should repeat."
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceOptionItem("Does not repeat", selected = initialType == RecurrenceType.NONE) {
                        onRecurrenceSelected(null)
                        onDismiss()
                    }
                    RecurrenceOptionItem("Daily", selected = initialType == RecurrenceType.DAILY) {
                        onRecurrenceSelected(RecurrenceModel.Daily())
                        onDismiss()
                    }
                    RecurrenceOptionItem("Weekly", selected = initialType == RecurrenceType.WEEKLY, hasSubMenu = true) {
                        showMonthlySheet = false
                        showCustomSheet = false
                        showWeeklySheet = true
                    }
                    RecurrenceOptionItem("Monthly", selected = initialType == RecurrenceType.MONTHLY, hasSubMenu = true) {
                        showWeeklySheet = false
                        showCustomSheet = false
                        showMonthlySheet = true
                    }
                    RecurrenceOptionItem("Yearly", selected = initialType == RecurrenceType.YEARLY) {
                        onRecurrenceSelected(RecurrenceModel.Yearly())
                        onDismiss()
                    }
                    RecurrenceOptionItem("Custom", selected = initialType == RecurrenceType.CUSTOM, hasSubMenu = true) {
                        showWeeklySheet = false
                        showMonthlySheet = false
                        showCustomSheet = true
                    }
                }
            }
        }
    }

    if (showMonthlySheet) {
        MonthlyConfigSheet(
            initialModel = initialModel as? RecurrenceModel.Monthly,
            minEndDateTimeMillis = minEndDateTimeMillis,
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
            minEndDateTimeMillis = minEndDateTimeMillis,
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
            minEndDateTimeMillis = minEndDateTimeMillis,
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
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.84f)
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                this.selected = selected
                role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
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
    minEndDateTimeMillis: Long,
    onSave: (RecurrenceModel.Monthly) -> Unit,
    onCancel: () -> Unit
) {
    var variant by remember { mutableStateOf(initialModel?.variant ?: MonthlyVariant.DAY_OF_MONTH) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.92f
    
    // Multi-day selection
    val selectedDays = remember { 
        mutableStateListOf<Int>().apply {
            if (initialModel != null && initialModel.daysOfMonth.isNotEmpty()) {
                addAll(initialModel.daysOfMonth)
            }
            // Default to empty - user must select
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
            maxOf(
                initialModel?.endRule?.endDateMillis ?: defaultEndDateMillis(),
                minEndDateTimeMillis
            )
        )
    }
    var occurrences by remember { 
        mutableIntStateOf(
            initialModel?.endRule?.count ?: 5
        )
    }
    
    // Missed Policy - default to SKIP_TO_NEXT now
    var missedPolicy by remember { mutableStateOf(initialModel?.missedPolicy ?: MissedPolicy.SKIP_TO_NEXT) }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .heightIn(max = maxSheetHeight)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp)
            ) {
                SheetHeader(
                    title = "Monthly settings",
                    subtitle = "Pick which days or end rule to use."
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Repeat on", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                modifier = Modifier.size(width = 48.dp, height = 36.dp),
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

                Spacer(modifier = Modifier.height(20.dp))

                EndRuleControls(
                    endRuleType = endRuleType,
                    endDate = endDate,
                    occurrences = occurrences,
                    minEndDateTimeMillis = minEndDateTimeMillis,
                    onTypeChange = { endRuleType = it },
                    onDateChange = { endDate = it },
                    onOccurrencesChange = { occurrences = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons - Pinned to bottom
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Back") }
                Button(
                    onClick = {
                        val endRule = when(endRuleType) {
                            "DATE" -> RecurrenceEndRule(type = EndRuleType.UNTIL_DATE, endDateMillis = maxOf(endDate, minEndDateTimeMillis))
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
                    modifier = Modifier.weight(1f),
                    enabled = variant == MonthlyVariant.LAST_DAY || selectedDays.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Save") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WeeklyConfigSheet(
    initialModel: RecurrenceModel.Weekly? = null,
    minEndDateTimeMillis: Long,
    onSave: (RecurrenceModel.Weekly) -> Unit,
    onCancel: () -> Unit
) {
    val days = remember { mutableStateListOf<Int>().apply {
        if (initialModel != null) {
            addAll(initialModel.daysOfWeek)
        }
    }}
    val weekdays = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.92f

    // End Rule state
    var endRuleType by remember {
        mutableStateOf(when (initialModel?.endRule?.type) {
            EndRuleType.UNTIL_DATE -> "DATE"
            EndRuleType.AFTER_OCCURRENCES -> "COUNT"
            else -> "NEVER"
        })
    }
    var endDate by remember {
        mutableLongStateOf(
            maxOf(
                initialModel?.endRule?.endDateMillis ?: defaultEndDateMillis(),
                minEndDateTimeMillis
            )
        )
    }
    var occurrences by remember {
        mutableIntStateOf(initialModel?.endRule?.count ?: 5)
    }
    
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .heightIn(max = maxSheetHeight)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp)
            ) {
                SheetHeader(
                    title = "Weekly settings",
                    subtitle = "Choose the weekdays to repeat on."
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Select Days", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

                Spacer(modifier = Modifier.height(20.dp))

                EndRuleControls(
                    endRuleType = endRuleType,
                    endDate = endDate,
                    occurrences = occurrences,
                    minEndDateTimeMillis = minEndDateTimeMillis,
                    onTypeChange = { endRuleType = it },
                    onDateChange = { endDate = it },
                    onOccurrencesChange = { occurrences = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons - Pinned to bottom
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Back") }
                Button(
                    onClick = {
                        val endRule = when (endRuleType) {
                            "DATE" -> RecurrenceEndRule(type = EndRuleType.UNTIL_DATE, endDateMillis = maxOf(endDate, minEndDateTimeMillis))
                            "COUNT" -> RecurrenceEndRule(type = EndRuleType.AFTER_OCCURRENCES, count = occurrences)
                            else -> RecurrenceEndRule(type = EndRuleType.NEVER)
                        }
                        onSave(RecurrenceModel.Weekly(days.toSet(), endRule = endRule))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = days.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Save") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomConfigSheet(
    initialModel: RecurrenceModel.Custom? = null,
    minEndDateTimeMillis: Long,
    onSave: (RecurrenceModel.Custom) -> Unit,
    onCancel: () -> Unit
) {
    var intervalText by remember { mutableStateOf(initialModel?.interval?.toString() ?: "1") }
    var unit by remember { mutableStateOf(initialModel?.unit ?: TimeUnit.DAYS) }
    var expanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.92f

    // End Rule state
    var endRuleType by remember {
        mutableStateOf(when (initialModel?.endRule?.type) {
            EndRuleType.UNTIL_DATE -> "DATE"
            EndRuleType.AFTER_OCCURRENCES -> "COUNT"
            else -> "NEVER"
        })
    }
    var endDate by remember {
        mutableLongStateOf(
            maxOf(
                initialModel?.endRule?.endDateMillis ?: defaultEndDateMillis(),
                minEndDateTimeMillis
            )
        )
    }
    var occurrences by remember {
        mutableIntStateOf(initialModel?.endRule?.count ?: 5)
    }

    // Validate interval
    val intervalValue = intervalText.toLocalizedIntOrNull() ?: 0
    val isValid = intervalValue >= 1

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .heightIn(max = maxSheetHeight)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp)
            ) {
                SheetHeader(
                    title = "Custom interval",
                    subtitle = "Set the gap between reminder repeats."
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Repeat every",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { newValue ->
                            val normalized = newValue.normalizeLocalizedDigitsOrNull()
                            if (normalized != null && normalized.length <= 3) {
                                intervalText = normalized
                            }
                        },
                        modifier = Modifier.weight(0.75f),
                        singleLine = true,
                        isError = intervalText.isNotEmpty() && !isValid,
                        label = { Text("Number") },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(modifier = Modifier.weight(1.25f)) {
                        OutlinedTextField(
                            value = unitLabel(unit, intervalValue.takeIf { it > 0 } ?: 2),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            TimeUnit.values().forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(unitLabel(u, intervalValue.takeIf { it > 0 } ?: 2)) },
                                    onClick = { unit = u; expanded = false }
                                )
                            }
                        }
                    }
                }

                if (isValid) {
                    Text(
                        text = "Repeats every $intervalValue ${unitLabel(unit, intervalValue).lowercase()}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                
                if (!isValid && intervalText.isNotEmpty()) {
                    Text(
                        "Interval must be at least 1",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                EndRuleControls(
                    endRuleType = endRuleType,
                    endDate = endDate,
                    occurrences = occurrences,
                    minEndDateTimeMillis = minEndDateTimeMillis,
                    onTypeChange = { endRuleType = it },
                    onDateChange = { endDate = it },
                    onOccurrencesChange = { occurrences = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons - Pinned to bottom
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Back") }
                Button(
                    onClick = {
                        val endRule = when (endRuleType) {
                            "DATE" -> RecurrenceEndRule(type = EndRuleType.UNTIL_DATE, endDateMillis = maxOf(endDate, minEndDateTimeMillis))
                            "COUNT" -> RecurrenceEndRule(type = EndRuleType.AFTER_OCCURRENCES, count = occurrences)
                            else -> RecurrenceEndRule(type = EndRuleType.NEVER)
                        }
                        onSave(RecurrenceModel.Custom(intervalValue, unit, endRule = endRule))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isValid,
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Save") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun unitLabel(unit: TimeUnit, count: Int): String {
    return when (unit) {
        TimeUnit.MINUTES -> if (count == 1) "Minute" else "Minutes"
        TimeUnit.HOURS -> if (count == 1) "Hour" else "Hours"
        TimeUnit.DAYS -> if (count == 1) "Day" else "Days"
        TimeUnit.WEEKS -> if (count == 1) "Week" else "Weeks"
        TimeUnit.MONTHS -> if (count == 1) "Month" else "Months"
        TimeUnit.YEARS -> if (count == 1) "Year" else "Years"
    }
}

private fun defaultEndDateMillis(): Long {
    return System.currentTimeMillis() + 30L * 24 * 3600 * 1000
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EndRuleControls(
    endRuleType: String,
    endDate: Long,
    occurrences: Int,
    minEndDateTimeMillis: Long,
    onTypeChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onOccurrencesChange: (Int) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateTimeFormatter = remember { java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()) }
    val dateFormatter = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val timeFormatter = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    val clampedEndDate = maxOf(endDate, minEndDateTimeMillis)

    LaunchedEffect(minEndDateTimeMillis, endDate) {
        if (endDate < minEndDateTimeMillis) {
            onDateChange(minEndDateTimeMillis)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Ends",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = endRuleType == "NEVER",
                onClick = { onTypeChange("NEVER") },
                label = { Text("Forever") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            FilterChip(
                selected = endRuleType == "DATE",
                onClick = {
                    if (endDate < minEndDateTimeMillis) {
                        onDateChange(minEndDateTimeMillis)
                    }
                    onTypeChange("DATE")
                },
                label = { Text("Until date/time") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            FilterChip(
                selected = endRuleType == "COUNT",
                onClick = { onTypeChange("COUNT") },
                label = { Text("After count") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        when (endRuleType) {
            "DATE" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Ends by ${dateTimeFormatter.format(java.util.Date(clampedEndDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Must be on or after ${dateTimeFormatter.format(java.util.Date(minEndDateTimeMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Date: ${dateFormatter.format(java.util.Date(clampedEndDate))}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Time: ${timeFormatter.format(java.util.Date(clampedEndDate))}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            "COUNT" -> {
                var fieldValue by remember(endRuleType) {
                    val initialText = occurrences.toString()
                    mutableStateOf(
                        TextFieldValue(
                            text = initialText,
                            selection = TextRange(0, initialText.length)
                        )
                    )
                }
                val parsed = fieldValue.text.toLocalizedIntOrNull()
                val isValid = parsed != null && parsed in 1..999
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        val normalized = newValue.text.normalizeLocalizedDigitsOrNull()
                        if (normalized != null && normalized.length <= 3) {
                            fieldValue = TextFieldValue(
                                text = normalized,
                                selection = TextRange(normalized.length)
                            )
                            normalized.toLocalizedIntOrNull()?.let { onOccurrencesChange(it.coerceIn(1, 999)) }
                        }
                    },
                    label = { Text("Number of occurrences") },
                    supportingText = { Text("Allowed: 1-999") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = fieldValue.text.isNotEmpty() && !isValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        val initialUtc = remember(clampedEndDate) { localMillisToUtcStartOfDay(clampedEndDate) }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedUtc = dateState.selectedDateMillis
                        if (pickedUtc != null) {
                            val candidate = mergeUtcDateKeepingLocalTime(pickedUtc, clampedEndDate)
                            val clamped = maxOf(candidate, minEndDateTimeMillis)
                            onDateChange(clamped)
                            if (candidate < minEndDateTimeMillis) {
                                android.widget.Toast.makeText(
                                    context,
                                    "End time cannot be before the reminder time",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        showDatePicker = false
                    },
                    enabled = dateState.selectedDateMillis != null
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val initial = remember(clampedEndDate) {
            Calendar.getInstance().apply { timeInMillis = clampedEndDate }
        }
        val timeState = rememberTimePickerState(
            initialHour = initial.get(Calendar.HOUR_OF_DAY),
            initialMinute = initial.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select end time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val candidate = mergeTimeKeepingDate(clampedEndDate, timeState.hour, timeState.minute)
                        val clamped = maxOf(candidate, minEndDateTimeMillis)
                        onDateChange(clamped)
                        if (candidate < minEndDateTimeMillis) {
                            android.widget.Toast.makeText(
                                context,
                                "End time cannot be before the reminder time",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        showTimePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

private fun localMillisToUtcStartOfDay(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, local.get(Calendar.YEAR))
        set(Calendar.MONTH, local.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun mergeUtcDateKeepingLocalTime(utcMillis: Long, currentLocalMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    val current = Calendar.getInstance().apply { timeInMillis = currentLocalMillis }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, current.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, current.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun mergeTimeKeepingDate(currentLocalMillis: Long, hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = currentLocalMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
