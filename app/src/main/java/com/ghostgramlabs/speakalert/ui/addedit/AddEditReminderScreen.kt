package com.ghostgramlabs.speakalert.ui.addedit

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghostgramlabs.speakalert.ui.AppViewModelProvider
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
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
    
    // Save success state for animation
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            showSaveSuccess = true
            android.widget.Toast.makeText(context, "Reminder saved!", android.widget.Toast.LENGTH_SHORT).show()
            // Short delay for animation before navigating
            kotlinx.coroutines.delay(800L)
            navigateBack()
        }
    }

    LaunchedEffect(uiState.showPastTimeError) {
        if (uiState.showPastTimeError) {
             android.widget.Toast.makeText(context, "Cannot set reminders for the past", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // Persist read permission so reminder audio still works after app restart.
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers may not grant persistable permissions; continue with best effort.
        }

        val displayName = resolveDisplayName(context, uri)
        viewModel.setCustomAudio(uri.toString(), displayName)
    }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showCustomFollowUpDialog by remember { mutableStateOf(false) }
    val followUpPresets = listOf(0, 5, 10, 15)
    val isCustomFollowUp = uiState.followUpCheckMinutes > 0 && uiState.followUpCheckMinutes !in followUpPresets

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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Reminder Content Header
            Text(
                text = "Reminder Content",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Reminder Content Card (Voice + Text)
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
                    // 1. Voice Recording Section
                    com.ghostgramlabs.speakalert.ui.components.VoiceRecorderCard(
                        isRecording = uiState.isRecording,
                        isPlaying = uiState.isPlaying && !uiState.isCustomAudioFile,
                        hasRecording = uiState.recordedAudioPath != null && !uiState.isCustomAudioFile,
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
                        recordingElapsedSeconds = uiState.recordingElapsedSeconds,
                        currentAmplitude = uiState.currentAmplitude
                    )

                    // 2. OR Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // 3. Choose Audio File section (new feature extension)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (uiState.isCustomAudioFile && !uiState.recordedAudioPath.isNullOrBlank()) {
                                Text(
                                    text = uiState.customAudioFileName ?: "Selected audio file",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (uiState.isPlaying) viewModel.stopPlayback() else viewModel.playRecording()
                                        },
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = CircleShape
                                            )
                                            .semantics {
                                                contentDescription = if (uiState.isPlaying) "Stop audio preview" else "Play audio preview"
                                            }
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Slider(
                                        value = uiState.playbackProgress.coerceIn(0f, 1f),
                                        onValueChange = { viewModel.seekTo(it) },
                                        enabled = uiState.isPlaying,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.removeCustomAudio() }
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Remove")
                                    }
                                    TextButton(
                                        onClick = {
                                            audioPickerLauncher.launch(
                                                arrayOf(
                                                    "audio/mpeg",
                                                    "audio/mp3",
                                                    "audio/wav",
                                                    "audio/x-wav",
                                                    "audio/mp4",
                                                    "audio/m4a",
                                                    "audio/ogg",
                                                    "audio/*"
                                                )
                                            )
                                        },
                                        enabled = !uiState.isRecording
                                    ) {
                                        Text("Change")
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        audioPickerLauncher.launch(
                                            arrayOf(
                                                "audio/mpeg",
                                                "audio/mp3",
                                                "audio/wav",
                                                "audio/x-wav",
                                                "audio/mp4",
                                                "audio/m4a",
                                                "audio/ogg",
                                                "audio/*"
                                            )
                                        )
                                    },
                                    enabled = !uiState.isRecording
                                ) {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose Audio File")
                                }
                            }
                        }
                    }

                    // 4. OR Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // 5. Text Input (Message)
                    OutlinedTextField(
                        value = uiState.reminderText,
                        onValueChange = { if (it.length <= 1000) viewModel.updateReminderText(it) },
                        label = { Text("Message (Text-to-speech)") },
                        placeholder = { Text("e.g., Take medicine after breakfast") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp),
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        isError = uiState.showError,
                        supportingText = {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Used for Text-to-Speech when no voice or audio file is selected",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${uiState.reminderText.length}/1000",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                
                                if (uiState.reminderText.isNotEmpty() && uiState.recordedAudioPath == null && uiState.isTextToSpeechEnabled) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Will be spoken aloud when reminder plays",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    if (uiState.showError) {
                        Text(
                            text = "Please add a voice recording, audio file, or text note",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Title Section - Wrapped in Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { if (it.length <= 40) viewModel.updateTitle(it) },
                        label = { Text("Title (Optional)") },
                        placeholder = { Text("e.g., Morning medicine") },
                        supportingText = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Helps you identify the reminder later")
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

                    // UI-only enhancement: keep the same stored timestamp, but make selection clearer.
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "When should this reminder fire?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${dateFormatter.format(java.util.Date(uiState.triggerTime))} at ${timeFormatter.format(java.util.Date(uiState.triggerTime))}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Date Row
                    ScheduleRow(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "Date",
                        value = dateFormatter.format(java.util.Date(uiState.triggerTime)),
                        onClick = { showDatePickerDialog = true }
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
                        onClick = { showTimePickerDialog = true }
                    )
                    
                    if (uiState.showPastTimeError) {
                         Text(
                             text = "Time must be in the future",
                             color = MaterialTheme.colorScheme.error,
                             style = MaterialTheme.typography.bodySmall,
                             modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                         )
                    }
                    
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Repeat Row
                    var showRecurrenceSheet by remember { mutableStateOf(false) }
                    val recurrenceSummary = remember(uiState.recurrenceType, uiState.recurrenceJson, uiState.triggerTime) {
                         if (uiState.recurrenceType == RecurrenceType.NONE) {
                             "Never"
                         } else {
                             com.ghostgramlabs.speakalert.domain.RecurrenceUtils.getRecurrenceSummary(
                                 type = uiState.recurrenceType,
                                 json = uiState.recurrenceJson,
                                 nextTriggerAt = uiState.triggerTime,
                                 includeTime = false
                             )
                         }
                    }
                    ScheduleRow(
                        icon = Icons.Filled.Repeat,
                        label = "Repeat",
                        value = recurrenceSummary,
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
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AllInclusive,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (uiState.loopPlayback) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Loop Playback",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Keep playing until dismissed",
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

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (uiState.followUpCheckMinutes > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Follow-Up Check",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (uiState.followUpCheckMinutes == 0) {
                                        "If reminder is not marked done, SpeakAlert can ask again after a delay."
                                    } else {
                                        "Active: asks again after ${uiState.followUpCheckMinutes} minutes if not marked done."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.followUpCheckMinutes > 0) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                }
                            ) {
                                Text(
                                    text = if (uiState.followUpCheckMinutes > 0) "Active" else "Off",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.followUpCheckMinutes > 0) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            followUpPresets.forEach { minutes ->
                                FilterChip(
                                    selected = uiState.followUpCheckMinutes == minutes,
                                    onClick = { viewModel.setFollowUpCheckMinutes(minutes) },
                                    label = {
                                        Text(
                                            text = if (minutes == 0) "Off" else "${minutes}m",
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            FilterChip(
                                selected = isCustomFollowUp,
                                onClick = { showCustomFollowUpDialog = true },
                                label = {
                                    Text(
                                        text = if (isCustomFollowUp) "${uiState.followUpCheckMinutes}m" else "Custom",
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        if (showCustomFollowUpDialog) {
                            CustomFollowUpDurationDialog(
                                initialValue = if (isCustomFollowUp) uiState.followUpCheckMinutes else 20,
                                maxMinutes = 240,
                                onDismiss = { showCustomFollowUpDialog = false },
                                onSave = { minutes ->
                                    viewModel.setFollowUpCheckMinutes(minutes)
                                    showCustomFollowUpDialog = false
                                }
                            )
                        }
                    }
                }
            }

            if (showDatePickerDialog) {
                val dateState = rememberDatePickerState(
                    initialSelectedDateMillis = addEditDatePickerSelectionMillis(uiState.triggerTime)
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePickerDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val pickedDate = dateState.selectedDateMillis
                                if (pickedDate != null) {
                                    if (addEditIsDateTodayOrFuture(pickedDate)) {
                                        val candidate = mergeDateWithCurrentTime(uiState.triggerTime, pickedDate)
                                        viewModel.setTriggerTime(candidate)
                                        showTimePickerDialog = true
                                    } else {
                                        Toast.makeText(context, "Date must be today or later", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                showDatePickerDialog = false
                            },
                            enabled = dateState.selectedDateMillis != null
                        ) {
                            Text("Apply")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerDialog = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = dateState)
                }
            }

            if (showTimePickerDialog) {
                val cal = remember(uiState.triggerTime) {
                    java.util.Calendar.getInstance().apply { timeInMillis = uiState.triggerTime }
                }
                val timeState = rememberTimePickerState(
                    initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                    initialMinute = cal.get(java.util.Calendar.MINUTE),
                    is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                )
                AlertDialog(
                    onDismissRequest = { showTimePickerDialog = false },
                    title = { Text("Select Time") },
                    text = { TimePicker(state = timeState) },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.setTriggerTime(
                                    mergeTimeWithCurrentDate(
                                        currentTime = uiState.triggerTime,
                                        hour = timeState.hour,
                                        minute = timeState.minute
                                    )
                                )
                                showTimePickerDialog = false
                            }
                        ) {
                            Text("Apply")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePickerDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
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
                        modifier = Modifier
                            .size(24.dp)
                            .semantics { contentDescription = "Saving reminder" },
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
private fun CustomFollowUpDurationDialog(
    initialValue: Int,
    maxMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember(initialValue) {
        mutableStateOf(initialValue.coerceAtLeast(1).toString())
    }
    val parsedValue = value.toIntOrNull()
    val isValid = parsedValue != null && parsedValue in 1..maxMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom Follow-Up",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Ask again after this many minutes if reminder is not marked done.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        val digits = newValue.filter { it.isDigit() }
                        if (digits.isEmpty()) {
                            value = ""
                        } else {
                            val num = digits.toIntOrNull() ?: 0
                            if (num <= maxMinutes) {
                                value = digits
                            }
                        }
                    },
                    label = { Text("Minutes") },
                    supportingText = { Text("Allowed: 1-$maxMinutes min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = value.isNotEmpty() && !isValid,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave((parsedValue ?: 10).coerceIn(1, maxMinutes))
                },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
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
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { role = androidx.compose.ui.semantics.Role.Button },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull() ?: uri.lastPathSegment
}

private fun mergeDateWithCurrentTime(currentTime: Long, selectedDate: Long): Long {
    val current = java.util.Calendar.getInstance().apply { timeInMillis = currentTime }
    val utcDate = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = selectedDate
    }
    return java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, utcDate.get(java.util.Calendar.YEAR))
        set(java.util.Calendar.MONTH, utcDate.get(java.util.Calendar.MONTH))
        set(java.util.Calendar.DAY_OF_MONTH, utcDate.get(java.util.Calendar.DAY_OF_MONTH))
        set(java.util.Calendar.HOUR_OF_DAY, current.get(java.util.Calendar.HOUR_OF_DAY))
        set(java.util.Calendar.MINUTE, current.get(java.util.Calendar.MINUTE))
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun addEditDatePickerSelectionMillis(time: Long): Long {
    val localDate = java.util.Calendar.getInstance().apply {
        timeInMillis = time
    }
    return java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(java.util.Calendar.YEAR, localDate.get(java.util.Calendar.YEAR))
        set(java.util.Calendar.MONTH, localDate.get(java.util.Calendar.MONTH))
        set(java.util.Calendar.DAY_OF_MONTH, localDate.get(java.util.Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun addEditIsDateTodayOrFuture(selectedDate: Long): Boolean {
    val pickedLocalStart = java.util.Calendar.getInstance().apply {
        val utcDate = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = selectedDate
        }
        set(java.util.Calendar.YEAR, utcDate.get(java.util.Calendar.YEAR))
        set(java.util.Calendar.MONTH, utcDate.get(java.util.Calendar.MONTH))
        set(java.util.Calendar.DAY_OF_MONTH, utcDate.get(java.util.Calendar.DAY_OF_MONTH))
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    return pickedLocalStart >= startOfTodayMillis()
}

private fun mergeTimeWithCurrentDate(currentTime: Long, hour: Int, minute: Int): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = currentTime
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun startOfTodayMillis(): Long {
    return java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}
