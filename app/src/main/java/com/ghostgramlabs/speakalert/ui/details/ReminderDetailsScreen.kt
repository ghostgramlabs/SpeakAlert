package com.ghostgramlabs.speakalert.ui.details

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ghostgramlabs.speakalert.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NotificationsActive
import com.ghostgramlabs.speakalert.service.ReminderPlaybackService
import com.ghostgramlabs.speakalert.ui.AppViewModelProvider
import com.ghostgramlabs.speakalert.ui.components.PremiumHeaderCard
import com.ghostgramlabs.speakalert.ui.components.PremiumScreenBackground
import com.ghostgramlabs.speakalert.ui.components.PremiumStatusPill
import com.ghostgramlabs.speakalert.ui.components.SectionCard
import com.ghostgramlabs.speakalert.ui.components.PrimaryActionButton
import com.ghostgramlabs.speakalert.ui.components.SystemDatePickerDialog
import com.ghostgramlabs.speakalert.ui.components.SystemTimePickerDialog
import com.ghostgramlabs.speakalert.ui.components.shouldUseSystemDateTimePickers
import com.ghostgramlabs.speakalert.util.DateUtils
import com.ghostgramlabs.speakalert.util.ReminderAudioSource
import com.ghostgramlabs.speakalert.util.isDefaultAppDisplayName
import com.ghostgramlabs.speakalert.util.sanitizeUnitFloat
import com.ghostgramlabs.speakalert.domain.models.EndRuleType
import com.ghostgramlabs.speakalert.domain.models.MissedPolicy
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var playbackPositionMs by remember { mutableStateOf(0L) }
    var playbackDurationMs by remember { mutableStateOf(0L) }
    var playbackSliderValue by remember { mutableStateOf(0f) }
    var isSeekingPlayback by remember { mutableStateOf(false) }

    // Keep play/stop UI in sync with service playback lifecycle.
    DisposableEffect(context, reminderId) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != "ACTION_PLAYBACK_STATUS") return
                val id = intent.getLongExtra("reminderId", -1L)
                val playing = intent.getBooleanExtra("isPlaying", false)
                val positionMs = intent.getLongExtra("positionMs", 0L)
                val durationMs = intent.getLongExtra("durationMs", 0L)
                isPlaying = if (!playing) {
                    isSeekingPlayback = false
                    playbackPositionMs = 0L
                    playbackDurationMs = 0L
                    if (!isSeekingPlayback) {
                        playbackSliderValue = 0f
                    }
                    false
                } else if (id == reminderId) {
                    playbackPositionMs = positionMs.coerceAtLeast(0L)
                    playbackDurationMs = durationMs.coerceAtLeast(0L)
                    if (!isSeekingPlayback && durationMs > 0L) {
                        playbackSliderValue =
                            (positionMs.toFloat() / durationMs.toFloat()).sanitizeUnitFloat()
                    }
                    true
                } else {
                    isSeekingPlayback = false
                    playbackPositionMs = 0L
                    playbackDurationMs = 0L
                    if (!isSeekingPlayback) {
                        playbackSliderValue = 0f
                    }
                    false
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
            } catch (_: Exception) {
            }
        }
    }

    // Failsafe: clear stale UI if playback notification is gone.
    LaunchedEffect(context, isPlaying) {
        while (isPlaying) {
            delay(1200)
            if (!isPlaybackNotificationActive(context)) {
                isPlaying = false
                isSeekingPlayback = false
                playbackPositionMs = 0L
                playbackDurationMs = 0L
                playbackSliderValue = 0f
            }
        }
    }

    
    // Trigger autoplay when reminder is loaded and autoplay is requested
    LaunchedEffect(reminder, autoplay) {
        val item = reminder
        val now = System.currentTimeMillis()
        val isMissedOnlyState = item != null &&
            !item.isCompleted &&
            item.snoozeUntil == null &&
            item.nextTriggerAt < now

        if (autoplay && item != null && !isMissedOnlyState && !hasAutoPlayed) {
            hasAutoPlayed = true
            viewModel.startAutoplay(context)
            isPlaying = true
            isSeekingPlayback = false
            playbackPositionMs = 0L
            playbackDurationMs = 0L
            playbackSliderValue = 0f
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
            || item.title.isDefaultAppDisplayName()
    val hasUserTitle = !item.title.isNullOrBlank() && !isLegacyTitle
    
    val displayLabel = when {
        hasUserTitle -> item.title!!
        !item.reminderText.isNullOrBlank() -> {
            val words = item.reminderText.trim().split(Regex("\\s+"))
            if (words.size > 10) words.take(10).joinToString(" ") + "..." else item.reminderText
        }
        else -> stringResource(R.string.home_created_at, createdTime)
    }
    
    // Check if recurring
    val isRecurring = item.recurrenceType != com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE
    val isCustomAudioFile = remember(item.audioPath) {
        ReminderAudioSource.isContentUri(item.audioPath)
    }
    val selectedAudioLabel = stringResource(R.string.ae_selected_audio)
    val audioFileName = remember(item.audioPath, selectedAudioLabel) {
        if (isCustomAudioFile) {
            ReminderAudioSource.resolveDisplayName(context, item.audioPath)
                ?.takeIf { it.isNotBlank() }
                ?: selectedAudioLabel
        } else null
    }

    // Past Action Sheet State
    var showPastActionSheet by remember { mutableStateOf(false) }
    
    // Reschedule Pickers
    var showRescheduleConfirmation by remember { mutableStateOf(false) }
    var pendingRescheduleTime by remember { mutableStateOf<Long?>(null) }
    var showRescheduleDatePicker by remember { mutableStateOf(false) }
    var showRescheduleTimePicker by remember { mutableStateOf(false) }
    val scheduleSummary = remember(item.nextTriggerAt) { DateUtils.formatDateTime(item.nextTriggerAt) }
    val recurrenceSummary = com.ghostgramlabs.speakalert.ui.util.localizedRecurrenceSummary(
        item.recurrenceType,
        item.recurrenceJson,
        item.nextTriggerAt,
        includeTime = false
    )
    val recurrenceBadgeText = when (item.recurrenceType) {
        RecurrenceType.NONE -> stringResource(R.string.rec_onetime)
        RecurrenceType.DAILY -> stringResource(R.string.rec_daily)
        RecurrenceType.WEEKLY -> stringResource(R.string.rec_weekly)
        RecurrenceType.MONTHLY -> stringResource(R.string.rec_monthly)
        RecurrenceType.YEARLY -> stringResource(R.string.rec_yearly)
        RecurrenceType.CUSTOM -> stringResource(R.string.rec_custom)
    }
    val recurrenceModel = remember(item) {
        com.ghostgramlabs.speakalert.domain.RecurrenceUtils.fromJson(
            item.recurrenceType,
            item.recurrenceJson
        ) ?: com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.Daily()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.det_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { navigateToEdit(reminderId) }) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.action_edit))
                    }
                    // Delete moved to overflow-style (still icon, but with confirmation)
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        }
    ) { innerPadding ->
        PremiumScreenBackground(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumHeaderCard(
                    title = displayLabel,
                    subtitle = scheduleSummary,
                    eyebrow = stringResource(R.string.det_title)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumStatusPill(
                            text = recurrenceBadgeText,
                            highlighted = isRecurring
                        )
                        if (item.audioPath != null) {
                            PremiumStatusPill(
                                text = if (isCustomAudioFile) stringResource(R.string.det_audio_file) else stringResource(R.string.det_voice_note),
                                highlighted = true
                            )
                        }
                        if (!item.reminderText.isNullOrBlank()) {
                            PremiumStatusPill(
                                text = stringResource(R.string.alert_text_reminder),
                                highlighted = item.audioPath == null
                            )
                        }
                        if (item.loopPlayback) {
                            PremiumStatusPill(text = stringResource(R.string.det_loop_enabled), highlighted = true)
                        }
                        if (item.followUpCheckMinutes > 0) {
                            PremiumStatusPill(
                                text = stringResource(R.string.det_followup_pill, item.followUpCheckMinutes),
                                highlighted = true
                            )
                        }
                    }
                }
            
                // VOICE NOTE Section (if audio exists)
                if (item.audioPath != null) {
                    SectionCard(title = if (isCustomAudioFile) stringResource(R.string.det_audio_file) else stringResource(R.string.det_voice_note)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isPlaying) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            viewModel.stopAudio(context)
                                            isPlaying = false
                                            isSeekingPlayback = false
                                            playbackPositionMs = 0L
                                            playbackDurationMs = 0L
                                            playbackSliderValue = 0f
                                        } else {
                                            viewModel.playAudio(context)
                                            isPlaying = true
                                            isSeekingPlayback = false
                                            playbackPositionMs = 0L
                                            playbackDurationMs = 0L
                                            playbackSliderValue = 0f
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) stringResource(R.string.det_cd_stop_audio) else stringResource(R.string.det_cd_play_audio),
                                        tint = if (isPlaying) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPlaying) stringResource(R.string.alert_playing_now) else if (isCustomAudioFile) stringResource(R.string.det_ready_preview) else stringResource(R.string.det_ready_play),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isCustomAudioFile) stringResource(R.string.det_audio_file) else stringResource(R.string.det_voice_recording),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isCustomAudioFile) {
                                    Text(
                                        text = audioFileName ?: selectedAudioLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!isCustomAudioFile) {
                                    Text(
                                        text = stringResource(R.string.det_tap_preview),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                if (isPlaying || playbackDurationMs > 0L) {
                                    val audioProgressCd = stringResource(R.string.det_cd_audio_progress)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Slider(
                                        value = playbackSliderValue.sanitizeUnitFloat(),
                                        onValueChange = { value ->
                                            val safeValue = value.sanitizeUnitFloat()
                                            isSeekingPlayback = true
                                            playbackSliderValue = safeValue
                                            if (playbackDurationMs > 0L) {
                                                playbackPositionMs =
                                                    (playbackDurationMs * safeValue).toLong()
                                            }
                                        },
                                        onValueChangeFinished = {
                                            val targetPositionMs =
                                                (playbackDurationMs * playbackSliderValue.sanitizeUnitFloat()).toLong()
                                            if (isPlaying && playbackDurationMs > 0L) {
                                                ReminderPlaybackService.seek(
                                                    context,
                                                    targetPositionMs
                                                )
                                            }
                                            isSeekingPlayback = false
                                        },
                                        enabled = playbackDurationMs > 0L,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics {
                                                contentDescription = audioProgressCd
                                                stateDescription =
                                                    "${detailsFormatPlaybackTime(playbackPositionMs)} of ${detailsFormatPlaybackTime(playbackDurationMs)}"
                                            }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = detailsFormatPlaybackTime(playbackPositionMs),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = detailsFormatPlaybackTime(playbackDurationMs),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            
                // TEXT CONTENT Section (if text exists)
                if (!item.reminderText.isNullOrBlank()) {
                    SectionCard(title = stringResource(R.string.ae_msg_label)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = item.reminderText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // SCHEDULE Section (tappable, replaces "Settings")
                var showRecurrenceSheet by remember { mutableStateOf(false) }
            
                SectionCard(title = stringResource(R.string.ae_section_schedule)) {
                    val doubleTapEdit = stringResource(R.string.det_double_tap_edit)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecurrenceSheet = true }
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                stateDescription = doubleTapEdit
                            },
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DetailInfoRow(
                            icon = Icons.Default.Refresh,
                            label = stringResource(R.string.ae_repeat),
                            value = recurrenceSummary
                        )
                        DetailInfoRow(
                            icon = Icons.Default.NotificationsActive,
                            label = stringResource(R.string.ae_time),
                            value = java.text.SimpleDateFormat(
                                "h:mm a",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(item.nextTriggerAt))
                        )
                        if (recurrenceModel.endRule.type != EndRuleType.NEVER) {
                            val endRuleText = when (recurrenceModel.endRule.type) {
                                EndRuleType.UNTIL_DATE -> {
                                    val dateStr = java.text.SimpleDateFormat(
                                        "MMM d, yyyy 'at' h:mm a",
                                        java.util.Locale.getDefault()
                                    ).format(java.util.Date(recurrenceModel.endRule.endDateMillis ?: 0L))
                                    stringResource(R.string.det_ends_by, dateStr)
                                }
                                EndRuleType.AFTER_OCCURRENCES -> pluralStringResource(
                                    R.plurals.det_ends_after,
                                    recurrenceModel.endRule.count ?: 0,
                                    recurrenceModel.endRule.count ?: 0
                                )
                                else -> ""
                            }
                            DetailInfoRow(
                                icon = Icons.Default.EventBusy,
                                label = stringResource(R.string.det_end_rule),
                                value = endRuleText
                            )
                        }
                        DetailInfoRow(
                            icon = Icons.Default.NotificationsActive,
                            label = stringResource(R.string.det_missed_handling),
                            value = when (recurrenceModel.missedPolicy) {
                                MissedPolicy.FIRE_ON_RESUME -> stringResource(R.string.det_missed_fire)
                                MissedPolicy.SKIP_TO_NEXT -> stringResource(R.string.det_missed_skip)
                            }
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.det_tap_edit_schedule),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            
                if (showRecurrenceSheet) {
                    com.ghostgramlabs.speakalert.ui.addedit.RecurrenceSelectionSheet(
                        initialType = item.recurrenceType,
                        initialJson = item.recurrenceJson,
                        minEndDateTimeMillis = item.nextTriggerAt,
                        onRecurrenceSelected = { model ->
                            viewModel.updateRecurrence(model)
                            showRecurrenceSheet = false
                        },
                        onDismiss = { showRecurrenceSheet = false }
                    )
                }
            
                Spacer(modifier = Modifier.height(4.dp))
            
                // ACTIONS - Context-aware buttons
                val now = System.currentTimeMillis()
                val isFiringOrMissed = remember(item) {
                    val oneHourAgo = now - 3600_000
                    // Active if: currently snoozed, fired in the last hour, or scheduled time has passed but not completed
                    item.snoozeUntil != null || 
                    (item.lastFiredAt != null && item.lastFiredAt > oneHourAgo) ||
                    (item.nextTriggerAt < now && !item.isCompleted)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show button if: Completed (to undo), or Active (Firing/Missed/Snoozed)
                    // This hiding logic now applies to BOTH one-time and recurring for consistency
                    if (item.isCompleted || isFiringOrMissed) {
                        PrimaryActionButton(
                            text = when {
                                item.isCompleted -> stringResource(R.string.det_mark_undone)
                                isRecurring -> stringResource(R.string.action_dismiss)
                                else -> stringResource(R.string.det_mark_done)
                            },
                            icon = if (item.isCompleted) Icons.Filled.Refresh else Icons.Filled.Done,
                            onClick = { 
                                if (item.isCompleted && item.nextTriggerAt < now) {
                                    showPastActionSheet = true
                                } else if (isRecurring && !item.isCompleted) {
                                    viewModel.dismissReminder(context)
                                    navigateBack()
                                } else {
                                    viewModel.toggleDone()
                                    navigateBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Helper text for future reminders
                    if (!item.isCompleted && !isFiringOrMissed) {
                        Text(
                            text = stringResource(R.string.det_scheduled_for, DateUtils.formatDateTime(item.nextTriggerAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showRescheduleDatePicker && pendingRescheduleTime != null) {
        val cancelDateFlow = {
            showRescheduleDatePicker = false
            pendingRescheduleTime = null
        }
        val applyPickedDate: (Long) -> Unit = { selectedDate ->
            if (detailsIsDateTodayOrFuture(selectedDate)) {
                pendingRescheduleTime = detailsMergeDateWithCurrentTime(
                    pendingRescheduleTime ?: System.currentTimeMillis(),
                    selectedDate
                )
                showRescheduleDatePicker = false
                showRescheduleTimePicker = true
            } else {
                Toast.makeText(context, context.getString(R.string.err_date_future), Toast.LENGTH_SHORT).show()
            }
        }
        if (shouldUseSystemDateTimePickers()) {
            SystemDatePickerDialog(
                initialSelectedDateMillisUtc = detailsUtcStartOfTodayMillis(
                    pendingRescheduleTime ?: System.currentTimeMillis()
                ),
                onDismiss = cancelDateFlow,
                onConfirm = applyPickedDate,
            )
        } else {
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = detailsUtcStartOfTodayMillis(
                    pendingRescheduleTime ?: System.currentTimeMillis()
                )
            )
            DatePickerDialog(
                onDismissRequest = cancelDateFlow,
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedDate = dateState.selectedDateMillis
                            if (selectedDate != null) {
                                applyPickedDate(selectedDate)
                            }
                        },
                        enabled = dateState.selectedDateMillis != null
                    ) {
                        Text(stringResource(R.string.action_apply))
                    }
                },
                dismissButton = {
                    TextButton(onClick = cancelDateFlow) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            ) {
                DatePicker(state = dateState)
            }
        }
    }

    if (showRescheduleTimePicker && pendingRescheduleTime != null) {
        val cal = remember(pendingRescheduleTime) {
            java.util.Calendar.getInstance().apply {
                timeInMillis = pendingRescheduleTime ?: System.currentTimeMillis()
            }
        }
        val cancelTimeFlow = {
            showRescheduleTimePicker = false
            pendingRescheduleTime = null
        }
        val applyPickedTime: (Int, Int) -> Unit = { hour, minute ->
            val selectedTime = detailsMergeTimeWithCurrentDate(
                pendingRescheduleTime ?: System.currentTimeMillis(),
                hour,
                minute
            )
            if (selectedTime <= System.currentTimeMillis()) {
                Toast.makeText(context, context.getString(R.string.err_time_future), Toast.LENGTH_SHORT).show()
            } else {
                pendingRescheduleTime = selectedTime
                showRescheduleTimePicker = false
                showRescheduleConfirmation = true
            }
        }
        if (shouldUseSystemDateTimePickers()) {
            SystemTimePickerDialog(
                initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(java.util.Calendar.MINUTE),
                is24Hour = android.text.format.DateFormat.is24HourFormat(context),
                onDismiss = cancelTimeFlow,
                onConfirm = applyPickedTime,
            )
        } else {
            val timeState = rememberTimePickerState(
                initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(java.util.Calendar.MINUTE),
                is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            )
            AlertDialog(
                onDismissRequest = cancelTimeFlow,
                title = { Text(stringResource(R.string.time_picker_title)) },
                text = { TimePicker(state = timeState) },
                confirmButton = {
                    Button(
                        onClick = {
                            applyPickedTime(timeState.hour, timeState.minute)
                        }
                    ) {
                        Text(stringResource(R.string.action_apply))
                    }
                },
                dismissButton = {
                    TextButton(onClick = cancelTimeFlow) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
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
                    stringResource(R.string.det_delete_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isRecurring) stringResource(R.string.det_delete_recurring)
                    else stringResource(R.string.det_delete_onetime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteReminder()
                        Toast.makeText(context, context.getString(R.string.home_snackbar_deleted), Toast.LENGTH_SHORT).show()
                        navigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
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
                val now = System.currentTimeMillis()
                pendingRescheduleTime = item.nextTriggerAt.takeIf { it > now } ?: now
                showRescheduleDatePicker = true
            },
            onPlayNow = {
                showPastActionSheet = false
                viewModel.playAudio(context)
                isPlaying = true
                isSeekingPlayback = false
                playbackPositionMs = 0L
                playbackDurationMs = 0L
                playbackSliderValue = 0f
            },
            onCancel = { showPastActionSheet = false },
            onDismiss = { showPastActionSheet = false }
        )
    }

    if (showRescheduleConfirmation && pendingRescheduleTime != null) {
        val formattedTime = DateUtils.formatDateTime(pendingRescheduleTime!!)
        AlertDialog(
            onDismissRequest = { showRescheduleConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.EventBusy, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.det_reschedule_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.det_reschedule_to, formattedTime),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reschedule(pendingRescheduleTime!!)
                        showRescheduleConfirmation = false
                        navigateBack()
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRescheduleConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun detailsMergeDateWithCurrentTime(currentTime: Long, selectedDateMillis: Long): Long {
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

private fun detailsMergeTimeWithCurrentDate(currentTime: Long, hour: Int, minute: Int): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = currentTime
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun detailsUtcStartOfTodayMillis(time: Long = System.currentTimeMillis()): Long {
    return java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = time
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun detailsIsDateTodayOrFuture(selectedDateMillis: Long): Boolean {
    return selectedDateMillis >= detailsUtcStartOfTodayMillis()
}

private fun detailsStartOfDayMillis(time: Long = System.currentTimeMillis()): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = time
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun detailsFormatPlaybackTime(timeMs: Long): String {
    if (timeMs <= 0L) return "0:00"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
}

private fun isPlaybackNotificationActive(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return notificationManager.activeNotifications.any {
        it.id == com.ghostgramlabs.speakalert.service.ReminderPlaybackService.NOTIFICATION_ID
    }
}
