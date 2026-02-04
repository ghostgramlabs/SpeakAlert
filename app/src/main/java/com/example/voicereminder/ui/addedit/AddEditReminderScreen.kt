package com.example.voicereminder.ui.addedit

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereminder.ui.AppViewModelProvider
import com.example.voicereminder.util.DateUtils
import com.example.voicereminder.domain.models.RecurrenceType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditReminderScreen(
    reminderId: Long = -1L,
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: AddEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Load reminder if editing
    LaunchedEffect(reminderId) {
        if (reminderId != -1L) {
            viewModel.loadReminder(reminderId)
        }
    }
    
    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            Toast.makeText(context, "Reminder saved!", Toast.LENGTH_SHORT).show()
            navigateBack()
        }
    }
    
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = uiState.triggerTime }

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = java.util.Calendar.getInstance()
            newCal.timeInMillis = uiState.triggerTime
            newCal.set(java.util.Calendar.YEAR, year)
            newCal.set(java.util.Calendar.MONTH, month)
            newCal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
            viewModel.setTriggerTime(newCal.timeInMillis)
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )
    
    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val newCal = java.util.Calendar.getInstance()
            newCal.timeInMillis = uiState.triggerTime
            newCal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            newCal.set(java.util.Calendar.MINUTE, minute)
            viewModel.setTriggerTime(newCal.timeInMillis)
        },
        calendar.get(java.util.Calendar.HOUR_OF_DAY),
        calendar.get(java.util.Calendar.MINUTE),
        false
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.initialReminderId != -1L) "Edit Reminder" else "New Reminder",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelRecording()
                        onNavigateUp()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Voice Recording Section - Hero element
            com.example.voicereminder.ui.components.VoiceRecorderCard(
                isRecording = uiState.isRecording,
                isPlaying = uiState.isPlaying,
                hasRecording = uiState.recordedAudioPath != null,
                onRecordClick = {
                    if (micPermissionState.status.isGranted) {
                        viewModel.startRecording()
                    } else {
                        micPermissionState.launchPermissionRequest()
                    }
                },
                onStopClick = { viewModel.stopRecording() },
                onPlayClick = { viewModel.playRecording() },
                onStopPlaybackClick = { viewModel.stopPlayback() },
                playbackProgress = uiState.playbackProgress,
                onSeek = { viewModel.seekTo(it) },
                recordingElapsedSeconds = uiState.recordingElapsedSeconds
            )

            // Title & Note Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Label Field (was "Title")
                    val labelPlaceholders = listOf("Take medicine", "Call mom", "Water plants", "Pick up kids")
                    val randomPlaceholder = remember { labelPlaceholders.random() }
                    
                    Column {
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = { if (it.length <= 40) viewModel.updateTitle(it) },
                            label = { Text("Label") },
                            placeholder = { Text("e.g., $randomPlaceholder") },
                            supportingText = { 
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("What is this reminder about?")
                                    Text("${uiState.title.length}/40")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    }


                    // "OR" Divider to separate Voice from Text
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "OR TYPE INSTEAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    OutlinedTextField(
                        value = uiState.reminderText,
                        onValueChange = { viewModel.updateReminderText(it) },
                        label = { Text("Text Message (Read aloud)") },
                        placeholder = { Text("Type here if you prefer text over voice recording") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        isError = uiState.showError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    if (uiState.showError) {
                        Text(
                            text = "Please add a voice recording or text note",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Schedule Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    val dateFormatter = remember { java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault()) }
                    val timeFormatter = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
                    
                    // Date Row
                    ScheduleRow(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "Date",
                        value = dateFormatter.format(java.util.Date(uiState.triggerTime)),
                        onClick = { datePickerDialog.show() }
                    )
                    
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Time Row
                    ScheduleRow(
                        icon = Icons.Outlined.Schedule,
                        label = "Time",
                        value = timeFormatter.format(java.util.Date(uiState.triggerTime)),
                        onClick = { timePickerDialog.show() }
                    )
                    
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Repeat Row
                    var showRecurrenceSheet by remember { mutableStateOf(false) }
                    ScheduleRow(
                        icon = Icons.Outlined.Repeat,
                        label = "Repeat",
                        value = if (uiState.recurrenceType == RecurrenceType.NONE) "Never" else uiState.recurrenceType.name,
                        onClick = { showRecurrenceSheet = true }
                    )
                    
                    if (showRecurrenceSheet) {
                        RecurrenceSelectionSheet(
                            initialType = uiState.recurrenceType,
                            initialJson = uiState.recurrenceJson,
                            onRecurrenceSelected = { model ->
                                viewModel.setRecurrence(model)
                                showRecurrenceSheet = false
                            },
                            onDismiss = { showRecurrenceSheet = false }
                        )
                    }
                    
                    // Loop Toggle
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Loop Playback",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Repeat audio until dismissed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.loopPlayback,
                            onCheckedChange = { viewModel.setLoopPlayback(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Save Button
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveReminder() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Reminder",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            // Bottom spacing for gesture nav
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ScheduleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
