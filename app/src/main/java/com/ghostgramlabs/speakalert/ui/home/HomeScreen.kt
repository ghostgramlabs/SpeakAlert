package com.ghostgramlabs.speakalert.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.ui.AppViewModelProvider
import com.ghostgramlabs.speakalert.util.DateUtils
import com.ghostgramlabs.speakalert.ui.components.ReminderCard
import com.ghostgramlabs.speakalert.ui.components.RecurringCompletionDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navigateToItemUpdate: (Long) -> Unit,
    navigateToAddItem: () -> Unit,
    navigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(FilterType.TODAY) }
    var currentPlayingId by remember { mutableStateOf<Long>(-1L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Scroll state for Glassmorphism effect
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isScrolled = scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0

    // Listen for Playback Status Broadcasts
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "ACTION_PLAYBACK_STATUS") {
                    val id = intent.getLongExtra("reminderId", -1L)
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    if (isPlaying) {
                        currentPlayingId = id
                    } else if (currentPlayingId == id) {
                        currentPlayingId = -1L
                    }
                }
            }
        }
        val filter = android.content.IntentFilter("ACTION_PLAYBACK_STATUS")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // Request notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }
    
    // Restore Reminder Dialog State
    var showRestoreDialog by remember { mutableStateOf(false) }
    var reminderToRestore by remember { mutableStateOf<ReminderEntity?>(null) }
    var showMarkDoneDialog by remember { mutableStateOf(false) }
    var reminderToMarkDone by remember { mutableStateOf<ReminderEntity?>(null) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    
    // Recurring Action States
    var reminderToStop by remember { mutableStateOf<ReminderEntity?>(null) }
    var reminderToMarkOccurrence by remember { mutableStateOf<ReminderEntity?>(null) }
    
    // Date/Time Picker State
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    
    // Handle Restore Dialog Actions
    if (showRestoreDialog && reminderToRestore != null) {
        com.ghostgramlabs.speakalert.ui.components.RestoreReminderDialog(
            onDismiss = {
                showRestoreDialog = false
                reminderToRestore = null
            },
            onReschedule = {
                showDateTimePicker = true
            },
            onMoveToMissed = {
                reminderToRestore?.let { viewModel.moveToMissed(it) }
                reminderToRestore = null
            },
            onPlay = {
                reminderToRestore?.let { viewModel.playReminder(context, it) }
                reminderToRestore = null
            },
            onKeepAsDone = {
                // Do nothing - reminder stays completed
                reminderToRestore = null
            }
        )
    }

    // Stop Recurring Confirmation Dialog
    if (reminderToStop != null) {
        AlertDialog(
            onDismissRequest = { reminderToStop = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { 
                Text(
                    "Stop recurring reminder?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    "This will delete this reminder and all future occurrences. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderToStop?.let { viewModel.deleteReminder(it) }
                        reminderToStop = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Forever")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { reminderToStop = null },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        )
    }

    // Mark Occurrence Done Confirmation Dialog
    if (reminderToMarkOccurrence != null) {
        AlertDialog(
            onDismissRequest = { reminderToMarkOccurrence = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { 
                Text(
                    "Mark occurrence done?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    "This marks only today's occurrence as completed. The reminder will still repeat as scheduled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderToMarkOccurrence?.let { viewModel.markTodayAsDone(it) }
                        reminderToMarkOccurrence = null
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark Done")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { reminderToMarkOccurrence = null },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        )
    }

    // Mark as Done Confirmation Dialog
    if (showMarkDoneDialog && reminderToMarkDone != null) {
        AlertDialog(
            onDismissRequest = {
                showMarkDoneDialog = false
                reminderToMarkDone = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { 
                Text(
                    "Mark as done?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    "This will move the reminder to the Done tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderToMarkDone?.let { viewModel.completeReminder(it) }
                        showMarkDoneDialog = false
                        reminderToMarkDone = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Done")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showMarkDoneDialog = false
                        reminderToMarkDone = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Date/Time Picker for Reschedule
    if (showDateTimePicker && reminderToRestore != null) {
        com.ghostgramlabs.speakalert.ui.components.DateTimePickerDialog(
            onDismiss = {
                showDateTimePicker = false
                showRestoreDialog = false
                reminderToRestore = null
            },
            onConfirm = { selectedTimeMillis: Long ->
                reminderToRestore?.let { 
                    viewModel.restoreReminder(it, selectedTimeMillis)
                }
                showDateTimePicker = false
                showRestoreDialog = false
                reminderToRestore = null
            }
        )
    }
    
    
    val filters = listOf(
        FilterType.UPCOMING to "Upcoming",
        FilterType.TODAY to "Today",
        FilterType.MISSED to "Missed",
        FilterType.COMPLETED to "Done"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Clean, minimal top bar
            TopAppBar(
                title = { 
                    Text(
                        "SpeakAlert",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    // Missed reminders badge
                    if (uiState.missedReminders.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text("${uiState.missedReminders.size}")
                                }
                            }
                        ) {
                            IconButton(onClick = { selectedFilter = FilterType.MISSED }) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Missed reminders",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(onClick = navigateToSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isScrolled) 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) 
                    else 
                        Color.Transparent
                )
            )
        },
        floatingActionButton = {
            // Modern, prominent FAB with Hint
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Formatting for Empty State Hint
                // Show only if list is empty (simplified check) and filter is TODAY or UPCOMING
                LargeFloatingActionButton(
                    onClick = navigateToAddItem,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Add Reminder",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    text = getGreeting(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getSubtitle(uiState),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Filter Chips - Scrollable with counts and semantic colors
            // Filter Tabs - Redesigned for High Visibility & Contrast
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
            ) {
                items(filters) { (type, label) ->
                    val isSelected = selectedFilter == type
                    
                    // Check if Missed tab has items (for red dot)
                    val hasMissedReminders = type == FilterType.MISSED && uiState.missedReminders.isNotEmpty()
                    
                    // Dynamic Colors for High Contrast
                    val containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    // Custom Tab Chip
                    Surface(
                        color = containerColor,
                        contentColor = contentColor,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .clickable { selectedFilter = type }
                            .animateContentSize()
                            .semantics {
                                role = androidx.compose.ui.semantics.Role.Tab
                                selected = isSelected
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                            
                            // Red dot indicator ONLY for Missed tab
                            if (hasMissedReminders) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                }
            }

            // Content
            val missedList = uiState.missedReminders
            
            if (selectedFilter == FilterType.MISSED) {
                if (missedList.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.NotificationsOff,
                        title = "All caught up!",
                        subtitle = "No missed reminders"
                    )
                } else {
                    val context = LocalContext.current
                    
                    // Batch Actions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = { 
                                missedList.forEach { viewModel.dismissMissedReminder(it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Dismiss All", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    
                    MissedReminderList(
                        missedReminders = missedList,
                        onFireClick = { viewModel.fireMissedReminder(context, it) },
                        onDismissClick = { viewModel.dismissMissedReminder(it) }
                    )
                }
            } else {
                val list = when (selectedFilter) {
                    FilterType.TODAY -> uiState.todayReminders
                    FilterType.UPCOMING -> uiState.upcomingReminders
                    FilterType.COMPLETED -> uiState.completedReminders
                    FilterType.ALL -> (uiState.todayReminders + uiState.upcomingReminders + uiState.completedReminders).sortedBy { it.nextTriggerAt }
                    else -> emptyList()
                }

                if (list.isEmpty()) {
                    EmptyState(
                        icon = when (selectedFilter) {
                             FilterType.TODAY -> Icons.Filled.Schedule
                             FilterType.COMPLETED -> Icons.Filled.Schedule
                             else -> Icons.Filled.Mic
                        },
                        title = "No reminders yet",
                        subtitle = "Create a reminder using voice or text."
                    )
                } else {
                    LazyColumn(
                        state = scrollState,
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(list, key = { it.id }) { reminder ->
                            val summary = remember(reminder) {
                                if (reminder.recurrenceType == com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE) {
                                    null
                                } else {
                                    com.ghostgramlabs.speakalert.domain.RecurrenceUtils.getRecurrenceSummary(
                                        reminder.recurrenceType, reminder.recurrenceJson, reminder.nextTriggerAt
                                    )
                                }
                            }
                            val icon = when(reminder.recurrenceType) {
                                com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE -> Icons.Filled.Schedule
                                else -> Icons.Filled.DateRange
                            }
                            
                            // Smart fallback label
                            val timeOnly = DateUtils.formatTimeOnly(reminder.nextTriggerAt)
                            val createdTime = DateUtils.formatTimeOnly(reminder.createdAt)
                            
                            // Check if title is a legacy auto-generated one
                            val isLegacyTitle = reminder.title?.matches(Regex("Reminder at \\d{1,2}:\\d{2} [AP]M")) == true
                                    || reminder.title.equals("Voice reminder", ignoreCase = true)
                            val hasUserTitle = !reminder.title.isNullOrBlank() && !isLegacyTitle
                            
                            val displayTitle = when {
                                hasUserTitle -> reminder.title!!
                                !reminder.reminderText.isNullOrBlank() -> {
                                    val words = reminder.reminderText.trim().split(Regex("\\s+"))
                                    if (words.size > 10) words.take(10).joinToString(" ") + "..." else reminder.reminderText
                                }
                                else -> "Created at $createdTime"
                            }
                            
                            // Context-aware Date Label: Hide "Today" if in Today tab
                            val isTodayTab = selectedFilter == FilterType.TODAY
                            val rawDateLabel = DateUtils.formatDateLabel(reminder.nextTriggerAt)
                            val finalDateLabel = if (isTodayTab && rawDateLabel == "Today") "" else rawDateLabel
                            
                            val context = LocalContext.current
                            
                            ReminderCard(
                                title = displayTitle,
                                badgeTime = timeOnly,
                                dateLabel = finalDateLabel,
                                recurrenceSummary = summary,
                                recurrenceType = reminder.recurrenceType,
                                recurrenceJson = reminder.recurrenceJson,
                                hasAudio = !reminder.audioPath.isNullOrBlank(),
                                hasText = !reminder.reminderText.isNullOrBlank(),
                                isTextToSpeechEnabled = uiState.isTextToSpeechEnabled,
                                isPlaying = currentPlayingId == reminder.id,
                                isCompleted = reminder.isCompleted,
                                loopEnabled = reminder.loopPlayback,
                                onPlayClick = { 
                                    viewModel.playReminder(context, reminder) 
                                },
                                onStopClick = {
                                    com.ghostgramlabs.speakalert.service.ReminderPlaybackService.stop(context)
                                },
                                onClick = { navigateToItemUpdate(reminder.id) },
                                onEditClick = { navigateToItemUpdate(reminder.id) },
                                onDeleteClick = { 
                                    if (reminder.recurrenceType != com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE) {
                                        reminderToStop = reminder
                                    } else {
                                        viewModel.deleteReminder(reminder)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Reminder deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                },
                                onCompleteClick = {
                                    if (reminder.recurrenceType != com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE) {
                                        // Recurring: Show confirmation to mark THIS occurrence done
                                        reminderToMarkOccurrence = reminder
                                    } else {
                                        // One-time: Complete immediately
                                        viewModel.completeReminder(reminder)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun getSubtitle(uiState: HomeUiState): String {
    val activeCount = uiState.upcomingReminders.size + uiState.todayReminders.size
    return when {
        activeCount == 0 -> "You're all caught up!"
        activeCount == 1 -> "You have 1 active reminder"
        else -> "You have $activeCount active reminders"
    }
}

@Composable
fun MissedReminderList(
    missedReminders: List<com.ghostgramlabs.speakalert.data.model.MissedReminderEntity>,
    onFireClick: (com.ghostgramlabs.speakalert.data.model.MissedReminderEntity) -> Unit,
    onDismissClick: (com.ghostgramlabs.speakalert.data.model.MissedReminderEntity) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(missedReminders, key = { it.id }) { missed ->
            MissedReminderItem(
                missed = missed,
                onFireClick = { onFireClick(missed) },
                onDismissClick = { onDismissClick(missed) }
            )
        }
    }
}

@Composable
fun MissedReminderItem(
    missed: com.ghostgramlabs.speakalert.data.model.MissedReminderEntity,
    onFireClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = missed.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Missed ${DateUtils.formatRelativeTime(missed.scheduledTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismissClick) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onFireClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Play Now")
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with subtle background
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

enum class FilterType {
    TODAY, UPCOMING, COMPLETED, MISSED, ALL
}
