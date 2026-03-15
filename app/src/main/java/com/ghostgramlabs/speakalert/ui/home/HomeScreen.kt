package com.ghostgramlabs.speakalert.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.ui.AppViewModelProvider
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.DateUtils
import com.ghostgramlabs.speakalert.util.ReminderAudioSource
import com.ghostgramlabs.speakalert.util.isDefaultAppDisplayName
import com.ghostgramlabs.speakalert.ui.components.ActionSheetRow
import com.ghostgramlabs.speakalert.ui.components.PremiumHeaderCard
import com.ghostgramlabs.speakalert.ui.components.PremiumScreenBackground
import com.ghostgramlabs.speakalert.ui.components.ReminderCard
import com.ghostgramlabs.speakalert.ui.components.RecurringCompletionDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navigateToItemUpdate: (Long) -> Unit,
    navigateToAddItem: () -> Unit,
    navigateToSettings: () -> Unit,
    allowStartupOverlays: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(FilterType.TODAY) }
    var currentPlayingId by remember { mutableStateOf<Long>(-1L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var fabOffsetX by rememberSaveable { mutableStateOf(0f) }
    var fabOffsetY by rememberSaveable { mutableStateOf(0f) }

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val fabSizePx = with(density) { 72.dp.toPx() }
    val fabMarginPx = with(density) { 16.dp.toPx() }
    val minFabX = -(screenWidthPx - fabSizePx - fabMarginPx * 2).coerceAtLeast(0f)
    val minFabY = -(screenHeightPx - fabSizePx - fabMarginPx * 2).coerceAtLeast(0f)

    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Listen for Playback Status Broadcasts
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "ACTION_PLAYBACK_STATUS") {
                    val id = intent.getLongExtra("reminderId", -1L)
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    if (isPlaying) {
                        if (id != -1L) currentPlayingId = id
                    } else {
                        currentPlayingId = -1L
                    }
                }
            }
        }
        val filter = android.content.IntentFilter("ACTION_PLAYBACK_STATUS")
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Failsafe: if playback notification is gone, clear stale UI playing state.
    LaunchedEffect(context, currentPlayingId) {
        while (currentPlayingId != -1L) {
            delay(1200)
            if (!isPlaybackNotificationActive(context)) {
                currentPlayingId = -1L
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
    var isRestoringFromUndo by remember { mutableStateOf(false) }
    var showMissedRecoveryDialog by rememberSaveable { mutableStateOf(false) }
    var missedRecoveryHandled by rememberSaveable { mutableStateOf(false) }
    
    // Recurring Action States
    var reminderToStop by remember { mutableStateOf<ReminderEntity?>(null) }
    var reminderToMarkOccurrence by remember { mutableStateOf<ReminderEntity?>(null) }
    
    // Material 3 date/time picker chain for reschedule/undo
    var showRescheduleDatePicker by remember { mutableStateOf(false) }
    var showRescheduleTimePicker by remember { mutableStateOf(false) }
    var pendingRescheduleTimeMillis by remember { mutableStateOf<Long?>(null) }
    var pendingRestoreTarget by remember { mutableStateOf<ReminderEntity?>(null) }
    var pendingRestoreFromUndo by remember { mutableStateOf(false) }

    val openReschedulePicker = { reminder: ReminderEntity?, fromUndo: Boolean ->
        val now = System.currentTimeMillis()
        pendingRestoreTarget = reminder
        pendingRestoreFromUndo = fromUndo
        pendingRescheduleTimeMillis = reminder?.nextTriggerAt?.takeIf { it > now } ?: now
        showRescheduleDatePicker = true
    }
    
    // Handle Restore Dialog Actions
    if (showRestoreDialog && reminderToRestore != null) {
        val restoreTarget = reminderToRestore
        com.ghostgramlabs.speakalert.ui.components.RestoreReminderDialog(
            onDismiss = {
                showRestoreDialog = false
                reminderToRestore = null
            },
            onReschedule = {
                showRestoreDialog = false
                openReschedulePicker(restoreTarget, isRestoringFromUndo)
            },
            onMoveToMissed = {
                restoreTarget?.let { viewModel.moveToMissed(it) }
                reminderToRestore = null
                isRestoringFromUndo = false
            },
            onPlay = {
                restoreTarget?.let { viewModel.playReminder(context, it) }
                reminderToRestore = null
                isRestoringFromUndo = false
            },
            onKeepAsDone = {
                // Do nothing - reminder stays completed
                reminderToRestore = null
                isRestoringFromUndo = false
            }
        )
    }

    if (showRescheduleDatePicker && pendingRescheduleTimeMillis != null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = homeUtcStartOfTodayMillis(
                pendingRescheduleTimeMillis ?: System.currentTimeMillis()
            )
        )
        DatePickerDialog(
            onDismissRequest = {
                showRescheduleDatePicker = false
                pendingRescheduleTimeMillis = null
                pendingRestoreTarget = null
                pendingRestoreFromUndo = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = dateState.selectedDateMillis
                        if (selectedDate != null) {
                            if (homeIsDateTodayOrFuture(selectedDate)) {
                                pendingRescheduleTimeMillis = homeMergeDateWithCurrentTime(
                                    pendingRescheduleTimeMillis ?: System.currentTimeMillis(),
                                    selectedDate
                                )
                                showRescheduleDatePicker = false
                                showRescheduleTimePicker = true
                            } else {
                                Toast.makeText(context, "Date must be today or later", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = dateState.selectedDateMillis != null
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRescheduleDatePicker = false
                        pendingRescheduleTimeMillis = null
                        pendingRestoreTarget = null
                        pendingRestoreFromUndo = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showRescheduleTimePicker && pendingRescheduleTimeMillis != null) {
        val current = remember(pendingRescheduleTimeMillis) {
            java.util.Calendar.getInstance().apply {
                timeInMillis = pendingRescheduleTimeMillis ?: System.currentTimeMillis()
            }
        }
        val timeState = rememberTimePickerState(
            initialHour = current.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = current.get(java.util.Calendar.MINUTE),
            is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = {
                showRescheduleTimePicker = false
                pendingRescheduleTimeMillis = null
                pendingRestoreTarget = null
                pendingRestoreFromUndo = false
            },
            title = { Text("Select Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedTime = homeMergeTimeWithCurrentDate(
                            pendingRescheduleTimeMillis ?: System.currentTimeMillis(),
                            timeState.hour,
                            timeState.minute
                        )
                        if (selectedTime <= System.currentTimeMillis()) {
                            Toast.makeText(context, "Please select a future time", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (pendingRestoreFromUndo) {
                            viewModel.undoDelete(selectedTime)
                        } else {
                            pendingRestoreTarget?.let { viewModel.restoreReminder(it, selectedTime) }
                        }
                        showRescheduleTimePicker = false
                        pendingRescheduleTimeMillis = null
                        pendingRestoreTarget = null
                        pendingRestoreFromUndo = false
                        showRestoreDialog = false
                        reminderToRestore = null
                        isRestoringFromUndo = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRescheduleTimePicker = false
                        pendingRescheduleTimeMillis = null
                        pendingRestoreTarget = null
                        pendingRestoreFromUndo = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(uiState.missedReminders, allowStartupOverlays) {
        if (!allowStartupOverlays) {
            showMissedRecoveryDialog = false
            return@LaunchedEffect
        }
        if (uiState.missedReminders.isEmpty()) {
            showMissedRecoveryDialog = false
            missedRecoveryHandled = false
        } else if (!missedRecoveryHandled) {
            showMissedRecoveryDialog = true
            missedRecoveryHandled = true
        }
    }

    if (showMissedRecoveryDialog && uiState.missedReminders.isNotEmpty()) {
        MissedReminderRecoveryDialog(
            missedReminders = uiState.missedReminders,
            onDismiss = { showMissedRecoveryDialog = false },
            onPlayNow = {
                uiState.missedReminders.firstOrNull()?.let { missed ->
                    viewModel.fireMissedReminder(context, missed)
                }
                showMissedRecoveryDialog = false
            }
        )
    }

    // Stop Recurring Confirmation Dialog
    if (reminderToStop != null) {
        ModalBottomSheet(
            onDismissRequest = { reminderToStop = null },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Stop recurring reminder?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This will delete this reminder and all future occurrences. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ActionSheetRow(
                    icon = Icons.Filled.Stop,
                    label = "Stop recurring",
                    subLabel = "Delete this reminder and all future occurrences",
                    onClick = {
                        reminderToStop?.let { viewModel.deleteReminder(it) }
                        reminderToStop = null
                    },
                    isDestructive = true
                )
                OutlinedButton(
                    onClick = { reminderToStop = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Keep reminder")
                }
            }
        }
    }

    // Mark Occurrence Done Confirmation Dialog
    if (reminderToMarkOccurrence != null) {
        ModalBottomSheet(
            onDismissRequest = { reminderToMarkOccurrence = null },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Mark occurrence done?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This clears only today's occurrence. The reminder will still repeat on its next scheduled time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ActionSheetRow(
                    icon = Icons.Filled.Done,
                    label = "Mark this occurrence done",
                    subLabel = "Keep future occurrences scheduled",
                    onClick = {
                        reminderToMarkOccurrence?.let { viewModel.markTodayAsDone(it) }
                        reminderToMarkOccurrence = null
                    },
                    emphasize = true
                )
                OutlinedButton(
                    onClick = { reminderToMarkOccurrence = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    // Mark as Done Confirmation Dialog
    if (showMarkDoneDialog && reminderToMarkDone != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showMarkDoneDialog = false
                reminderToMarkDone = null
            },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Mark as done?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This will move the reminder to the Done tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ActionSheetRow(
                    icon = Icons.Filled.Done,
                    label = "Move to Done",
                    subLabel = "Keep this reminder in your completed history",
                    onClick = {
                        reminderToMarkDone?.let { viewModel.completeReminder(it) }
                        showMarkDoneDialog = false
                        reminderToMarkDone = null
                    },
                    emphasize = true
                )
                OutlinedButton(
                    onClick = {
                        showMarkDoneDialog = false
                        reminderToMarkDone = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
    
    
    val filters = listOf(
        FilterType.UPCOMING to "Upcoming",
        FilterType.TODAY to "Today",
        FilterType.MISSED to "Missed",
        FilterType.COMPLETED to "Done"
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        APP_DISPLAY_NAME,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    // Missed reminders badge
                    if (uiState.missedReminders.isNotEmpty()) {
                        Box(
                            modifier = Modifier.padding(end = 4.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(onClick = { selectedFilter = FilterType.MISSED }) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Missed reminders",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(top = 6.dp, end = 4.dp)
                                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (uiState.missedReminders.size > 99) "99+" else "${uiState.missedReminders.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                        maxLines = 1
                                    )
                                }
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
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = navigateToAddItem,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(72.dp)
                        .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                fabOffsetX = (fabOffsetX + dragAmount.x).coerceIn(minFabX, 0f)
                                fabOffsetY = (fabOffsetY + dragAmount.y).coerceIn(minFabY, 0f)
                            }
                        }
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Add Reminder",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        PremiumScreenBackground(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PremiumHeaderCard(
                    title = getGreeting(),
                    subtitle = getSubtitle(uiState),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 14.dp)
                        .semantics(mergeDescendants = true) {}
                )
            
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .fillMaxWidth()
                ) {
                    items(filters) { (type, label) ->
                        val isSelected = selectedFilter == type
                        val hasMissedReminders = type == FilterType.MISSED && uiState.missedReminders.isNotEmpty()

                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
                                }
                            ),
                            modifier = Modifier
                                .height(46.dp)
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
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                )

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

                val missedList = uiState.missedReminders
            
                if (selectedFilter == FilterType.MISSED) {
                    if (missedList.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.NotificationsOff,
                            title = "All caught up!",
                            subtitle = "No missed reminders"
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = {
                                    missedList.forEach { viewModel.dismissMissedReminder(it) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
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
                            currentPlayingId = currentPlayingId,
                            onFireClick = {
                                currentPlayingId = it.reminderId
                                viewModel.fireMissedReminder(context, it)
                            },
                            onStopClick = {
                                currentPlayingId = -1L
                                com.ghostgramlabs.speakalert.service.ReminderPlaybackService.stop(context)
                            },
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
                                 FilterType.COMPLETED -> Icons.Filled.Done
                                 else -> Icons.Filled.Mic
                            },
                            title = when (selectedFilter) {
                                FilterType.COMPLETED -> "Nothing completed yet"
                                FilterType.UPCOMING -> "No upcoming reminders"
                                else -> "No reminders yet"
                            },
                            subtitle = when (selectedFilter) {
                                FilterType.COMPLETED -> "Completed reminders will appear here."
                                FilterType.UPCOMING -> "Scheduled reminders for later will appear here."
                                else -> "Create a reminder using voice or text."
                            }
                        )
                } else {
                    LazyColumn(
                        state = scrollState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
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
                            // Smart fallback label
                            val timeOnly = DateUtils.formatTimeOnly(reminder.nextTriggerAt)
                            val createdTime = DateUtils.formatTimeOnly(reminder.createdAt)
                            
                            // Check if title is a legacy auto-generated one
                            val isLegacyTitle = reminder.title?.matches(Regex("Reminder at \\d{1,2}:\\d{2} [AP]M")) == true
                                    || reminder.title.equals("Voice reminder", ignoreCase = true)
                                    || reminder.title.isDefaultAppDisplayName()
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
                                hasCustomAudioFile = ReminderAudioSource.isContentUri(reminder.audioPath),
                                isPlaying = currentPlayingId == reminder.id,
                                isCompleted = reminder.isCompleted,
                                loopEnabled = reminder.loopPlayback,
                                followUpCheckMinutes = reminder.followUpCheckMinutes,
                                onPlayClick = { 
                                    currentPlayingId = reminder.id
                                    viewModel.playReminder(context, reminder) 
                                },
                                onStopClick = {
                                    if (currentPlayingId == reminder.id) currentPlayingId = -1L
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
                                                val preview = viewModel.previewUndo()
                                                val now = System.currentTimeMillis()
                                                if (preview != null && preview.nextTriggerAt < now) {
                                                    // Time is past, force picker
                                                    reminderToRestore = preview
                                                    isRestoringFromUndo = true
                                                    openReschedulePicker(preview, true)
                                                } else {
                                                    viewModel.undoDelete()
                                                }
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
}

@Composable
private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
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
    currentPlayingId: Long,
    onFireClick: (com.ghostgramlabs.speakalert.data.model.MissedReminderEntity) -> Unit,
    onStopClick: () -> Unit,
    onDismissClick: (com.ghostgramlabs.speakalert.data.model.MissedReminderEntity) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(missedReminders, key = { it.id }) { missed ->
            MissedReminderItem(
                missed = missed,
                isPlaying = currentPlayingId == missed.reminderId,
                onFireClick = { onFireClick(missed) },
                onStopClick = onStopClick,
                onDismissClick = { onDismissClick(missed) }
            )
        }
    }
}

@Composable
fun MissedReminderItem(
    missed: com.ghostgramlabs.speakalert.data.model.MissedReminderEntity,
    isPlaying: Boolean,
    onFireClick: () -> Unit,
    onStopClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val displayTitle = remember(missed.title, missed.reminderText) {
        val userTitle = missed.title
            .trim()
            .takeIf { it.isNotEmpty() && !it.isDefaultAppDisplayName() }
        if (userTitle != null) {
            userTitle
        } else {
            val textFallback = missed.reminderText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { text ->
                    val words = text.split(Regex("\\s+"))
                    if (words.size > 8) words.take(8).joinToString(" ") else text
                }
            textFallback ?: "Reminder"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
                    ) {
                        Text(
                            text = "Missed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    if (isPlaying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
                        ) {
                            Text(
                                text = "Playing now",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    // Show text body if available and not identical to title
                    if (!missed.reminderText.isNullOrBlank() && !displayTitle.equals(missed.reminderText, ignoreCase = true)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = missed.reminderText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = DateUtils.formatDateTime(missed.scheduledTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismissClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.24f)
                    )
                ) {
                    Text("Dismiss")
                }
                Button(
                    onClick = if (isPlaying) onStopClick else onFireClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "Stop" else "Play now")
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
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
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
}

private fun homeMergeDateWithCurrentTime(currentTime: Long, selectedDateMillis: Long): Long {
    val current = java.util.Calendar.getInstance().apply { timeInMillis = currentTime }
    val utcDate = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = selectedDateMillis
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

private fun homeMergeTimeWithCurrentDate(currentTime: Long, hour: Int, minute: Int): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = currentTime
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun homeUtcStartOfTodayMillis(time: Long = System.currentTimeMillis()): Long {
    return java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = time
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun homeIsDateTodayOrFuture(selectedDateMillis: Long): Boolean {
    return selectedDateMillis >= homeUtcStartOfTodayMillis()
}

private fun homeStartOfDayMillis(time: Long = System.currentTimeMillis()): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = time
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

enum class FilterType {
    TODAY, UPCOMING, COMPLETED, MISSED, ALL
}

private fun isPlaybackNotificationActive(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return notificationManager.activeNotifications.any {
        it.id == com.ghostgramlabs.speakalert.service.ReminderPlaybackService.NOTIFICATION_ID
    }
}
