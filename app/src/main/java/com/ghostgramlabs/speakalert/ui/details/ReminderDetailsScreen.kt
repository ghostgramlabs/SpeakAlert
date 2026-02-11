package com.ghostgramlabs.speakalert.ui.details

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import com.ghostgramlabs.speakalert.ui.AppViewModelProvider
import com.ghostgramlabs.speakalert.ui.components.SectionCard
import com.ghostgramlabs.speakalert.ui.components.PrimaryActionButton
import com.ghostgramlabs.speakalert.util.DateUtils

import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailsScreen(
    reminderId: Long,
    autoplay: Boolean = false,

    navigateBack: () -> Unit,
    navigateToEdit: (Long) -> Unit,
    viewModel: ReminderDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    
    LaunchedEffect(reminderId) {
        viewModel.loadReminder(reminderId)
    }

    val reminder by viewModel.reminder.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    var hasAutoPlayed by remember { mutableStateOf(false) }

    
    // Trigger autoplay when reminder is loaded and autoplay is requested
    LaunchedEffect(reminder, autoplay) {
        if (autoplay && reminder != null && !hasAutoPlayed) {
            hasAutoPlayed = true
            viewModel.startAutoplay(context)
            isPlaying = true
        }
    }

    if (reminder == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val item = reminder!!
    
    // Delete confirmation state
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Smart fallback label (matches HomeScreen logic)
    val createdTime = DateUtils.formatTimeOnly(item.createdAt)
    val isLegacyTitle = item.title?.matches(Regex("Reminder at \\d{1,2}:\\d{2} [AP]M")) == true
            || item.title.equals("Voice reminder", ignoreCase = true)
    val hasUserTitle = !item.title.isNullOrBlank() && !isLegacyTitle
    
    val displayLabel = when {
        hasUserTitle -> item.title!!
        !item.reminderText.isNullOrBlank() -> {
            val words = item.reminderText.trim().split(Regex("\\s+"))
            if (words.size > 10) words.take(10).joinToString(" ") + "..." else item.reminderText
        }
        else -> "Created at $createdTime"
    }
    
    // Check if recurring
    val isRecurring = item.recurrenceType != com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE

    // Past Action Sheet State
    var showPastActionSheet by remember { mutableStateOf(false) }
    
    // Reschedule Pickers
    val rescheduleCalendar = remember { java.util.Calendar.getInstance() }
    
    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            rescheduleCalendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            rescheduleCalendar.set(java.util.Calendar.MINUTE, minute)
            viewModel.reschedule(rescheduleCalendar.timeInMillis)
            navigateBack()
        },
        rescheduleCalendar.get(java.util.Calendar.HOUR_OF_DAY),
        rescheduleCalendar.get(java.util.Calendar.MINUTE),
        false
    )

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            rescheduleCalendar.set(java.util.Calendar.YEAR, year)
            rescheduleCalendar.set(java.util.Calendar.MONTH, month)
            rescheduleCalendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
            timePickerDialog.show()
        },
        rescheduleCalendar.get(java.util.Calendar.YEAR),
        rescheduleCalendar.get(java.util.Calendar.MONTH),
        rescheduleCalendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navigateToEdit(reminderId) }) {
                        Icon(Icons.Filled.Edit, "Edit")
                    }
                    // Delete moved to overflow-style (still icon, but with confirmation)
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // HEADER: Label as primary + Date/Time as secondary
            Column {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateUtils.formatDateTime(item.nextTriggerAt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // VOICE NOTE Section (if audio exists)
            if (item.audioPath != null) {
                SectionCard(title = "Voice Note") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    viewModel.stopAudio()
                                    isPlaying = false
                                } else {
                                    viewModel.playAudio()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                "Play Audio",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (isPlaying) "Playing..." else "Tap to play", style = MaterialTheme.typography.titleMedium)
                            Text("Voice recording", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            // TEXT CONTENT Section (if text exists)
            if (!item.reminderText.isNullOrBlank()) {
                SectionCard(title = "Text Content") {
                    Text(
                        text = item.reminderText!!,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // SCHEDULE Section (tappable, replaces "Settings")
            var showRecurrenceSheet by remember { mutableStateOf(false) }
            val recurrenceSummary = remember(item) {
                com.ghostgramlabs.speakalert.domain.RecurrenceUtils.getRecurrenceSummary(
                    item.recurrenceType,
                    item.recurrenceJson,
                    item.nextTriggerAt,
                    includeEndRule = true,
                    includeMissedPolicy = true
                )
            }
            
            SectionCard(title = "Schedule") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRecurrenceSheet = true }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = recurrenceSummary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to edit schedule",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (showRecurrenceSheet) {
                com.ghostgramlabs.speakalert.ui.addedit.RecurrenceSelectionSheet(
                    initialType = item.recurrenceType,
                    initialJson = item.recurrenceJson,
                    onRecurrenceSelected = { model ->
                        viewModel.updateRecurrence(model)
                        showRecurrenceSheet = false
                    },
                    onDismiss = { showRecurrenceSheet = false }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // ACTIONS - Safe buttons based on reminder type
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryActionButton(
                    text = when {
                        item.isCompleted -> "Mark as Undone"
                        isRecurring -> "Dismiss for now"
                        else -> "Mark as Done"
                    },
                    icon = if (item.isCompleted) Icons.Filled.Refresh else Icons.Filled.Done,
                    onClick = { 
                        if (item.isCompleted && item.nextTriggerAt < System.currentTimeMillis()) {
                            showPastActionSheet = true
                        } else {
                            viewModel.toggleDone()
                            navigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Helper text for recurring reminders
                if (isRecurring && !item.isCompleted) {
                    Text(
                        text = "Future reminders will continue as scheduled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { 
                Text(
                    "Delete Reminder?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    if (isRecurring) "This will stop all future occurrences. This cannot be undone."
                    else "This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteReminder()
                        Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show()
                        navigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        )
    }

    // Past Action Sheet
    if (showPastActionSheet) {
        PastUndoneActionSheet(
            scheduledTime = item.nextTriggerAt,
            onReschedule = {
                showPastActionSheet = false
                datePickerDialog.show()
            },
            onPlayNow = {
                showPastActionSheet = false
                viewModel.markAsMissed(playAudio = true)
                isPlaying = true
            },
            onCancel = { showPastActionSheet = false },
            onDismiss = { showPastActionSheet = false }
        )
    }
}
