package com.example.voicereminder.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereminder.data.model.ReminderEntity
import com.example.voicereminder.ui.AppViewModelProvider
import com.example.voicereminder.util.DateUtils
import com.example.voicereminder.ui.components.ReminderCard
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
    val context = LocalContext.current

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
    
    val filters = listOf(
        FilterType.UPCOMING to "Upcoming",
        FilterType.TODAY to "Today",
        FilterType.COMPLETED to "Done",
        FilterType.MISSED to "Missed"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Clean, minimal top bar
            TopAppBar(
                title = { },
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
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            // Modern, prominent FAB
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
            
            // Filter Chips - Scrollable
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(filters) { (type, label) ->
                    val isSelected = selectedFilter == type
                    val badgeCount = if (type == FilterType.MISSED) uiState.missedReminders.size else 0
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = type },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(label)
                                if (badgeCount > 0) {
                                    Badge(
                                        containerColor = if (isSelected) 
                                            MaterialTheme.colorScheme.onPrimary 
                                        else 
                                            MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            "$badgeCount",
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = null
                    )
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
                            FilterType.UPCOMING -> Icons.Filled.Mic
                            FilterType.COMPLETED -> Icons.Filled.Schedule
                            else -> Icons.Filled.Schedule
                        },
                        title = when (selectedFilter) {
                            FilterType.TODAY -> "Nothing for today"
                            FilterType.UPCOMING -> "No reminders yet"
                            FilterType.COMPLETED -> "Nothing completed"
                            else -> "No reminders"
                        },
                        subtitle = when (selectedFilter) {
                            FilterType.UPCOMING -> "Your voice reminders will appear here.\nTap the microphone to record your first reminder."
                            FilterType.TODAY -> "Enjoy your quiet day!"
                            else -> null
                        }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list, key = { it.id }) { reminder ->
                            val summary = remember(reminder) {
                                com.example.voicereminder.domain.RecurrenceUtils.getRecurrenceSummary(
                                    reminder.recurrenceType, reminder.recurrenceJson, reminder.nextTriggerAt
                                )
                            }
                            val icon = when(reminder.recurrenceType) {
                                com.example.voicereminder.domain.models.RecurrenceType.NONE -> Icons.Filled.Schedule
                                else -> Icons.Filled.DateRange
                            }
                            
                            // Smart fallback label
                            val displayTitle = reminder.title ?: run {
                                val timeStr = DateUtils.formatRelativeTime(reminder.nextTriggerAt)
                                if (!reminder.audioPath.isNullOrBlank()) "Voice reminder"
                                else "Reminder at $timeStr"
                            }
                            
                            // Date format: Today • 6:30 PM
                            val dateStr = com.example.voicereminder.util.DateUtils.formatSmartDate(reminder.nextTriggerAt)
                            
                            val context = LocalContext.current
                            
                            ReminderCard(
                                title = if (!reminder.title.isNullOrBlank()) reminder.title else "Voice Reminder",
                                badgeTime = DateUtils.formatTimeOnly(reminder.nextTriggerAt),
                                dateLabel = DateUtils.formatDateLabel(reminder.nextTriggerAt),
                                recurrenceSummary = com.example.voicereminder.domain.RecurrenceUtils.getRecurrenceSummary(reminder.recurrenceType, reminder.recurrenceJson, reminder.nextTriggerAt),
                                recurrenceIcon = if (reminder.recurrenceType != com.example.voicereminder.domain.models.RecurrenceType.NONE) Icons.Filled.Repeat else null,
                                hasAudio = !reminder.audioPath.isNullOrBlank(),
                                hasText = !reminder.reminderText.isNullOrBlank(),
                                isPlaying = currentPlayingId == reminder.id,
                                onPlayClick = { 
                                    viewModel.playReminder(context, reminder) 
                                },
                                onStopClick = {
                                    com.example.voicereminder.service.ReminderPlaybackService.stop(context)
                                },
                                onClick = { navigateToItemUpdate(reminder.id) },
                                onDeleteClick = { viewModel.deleteReminder(reminder) }
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
    missedReminders: List<com.example.voicereminder.data.model.MissedReminderEntity>,
    onFireClick: (com.example.voicereminder.data.model.MissedReminderEntity) -> Unit,
    onDismissClick: (com.example.voicereminder.data.model.MissedReminderEntity) -> Unit
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
    missed: com.example.voicereminder.data.model.MissedReminderEntity,
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
