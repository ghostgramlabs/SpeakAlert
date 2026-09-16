package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.EndRuleType
import com.ghostgramlabs.speakalert.domain.models.MonthlyVariant
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.util.normalizeLocalizedDigitsOrNull
import com.ghostgramlabs.speakalert.util.sanitizeUnitFloat
import com.ghostgramlabs.speakalert.util.toLocalizedIntOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Monthly Day Grid ─────────────────────────────────────────────────────────
@Composable
fun MonthlyDayGrid(
    selectedDays: Set<Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.card_days_of_month),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        // 7 columns, days 1–31 → 5 rows
        for (rowStart in 1..31 step 7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (day in rowStart until rowStart + 7) {
                    if (day <= 31) {
                        val isSelected = day in selectedDays
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(12.dp)
                                        )
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                                            RoundedCornerShape(12.dp)
                                        )
                                    }
                                )
                        ) {
                            Text(
                                text = "$day",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Empty spacer for grid alignment
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
            if (rowStart + 7 <= 31) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ─── Weekday Chips ────────────────────────────────────────────────────────────
@Composable
fun WeekdayChips(
    selectedDays: Set<Int>, // 1=Mon, 7=Sun
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        dayLabels.forEachIndexed { index, label ->
            val dayNum = index + 1 // 1=Mon … 7=Sun
            val isSelected = dayNum in selectedDays
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                                RoundedCornerShape(12.dp)
                            )
                        }
                    )
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RecurrenceInfoCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

// ─── Recurrence Details Row ───────────────────────────────────────────────────
@Composable
fun RecurrenceDetailsRow(
    recurrenceType: RecurrenceType,
    recurrenceJson: String?,
    modifier: Modifier = Modifier
) {
    when (recurrenceType) {
        RecurrenceType.NONE -> {
            RecurrenceInfoCard(text = stringResource(R.string.card_recur_onetime), modifier = modifier)
        }
        RecurrenceType.DAILY -> {
            RecurrenceInfoCard(text = stringResource(R.string.card_recur_daily), modifier = modifier)
        }
        RecurrenceType.WEEKLY -> {
            val model = RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)
            if (model is RecurrenceModel.Weekly) {
                WeekdayChips(selectedDays = model.daysOfWeek, modifier = modifier)
            } else {
                Text(
                    text = stringResource(R.string.rec_weekly),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = modifier
                )
            }
        }
        RecurrenceType.MONTHLY -> {
            val model = RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)
            if (model is RecurrenceModel.Monthly) {
                if (model.variant == MonthlyVariant.LAST_DAY) {
                    RecurrenceInfoCard(text = stringResource(R.string.card_recur_last_day), modifier = modifier)
                } else {
                    MonthlyDayGrid(
                        selectedDays = model.daysOfMonth,
                        modifier = modifier
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.rec_monthly),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = modifier
                )
            }
        }
        RecurrenceType.CUSTOM -> {
            val model = RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)
            val summaryText = if (model is RecurrenceModel.Custom) {
                val unitStr = when (model.unit) {
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.MINUTES ->
                        pluralStringResource(R.plurals.unit_minutes, model.interval)
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.HOURS ->
                        pluralStringResource(R.plurals.unit_hours, model.interval)
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.DAYS ->
                        pluralStringResource(R.plurals.unit_days, model.interval)
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.WEEKS ->
                        pluralStringResource(R.plurals.unit_weeks, model.interval)
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.MONTHS ->
                        pluralStringResource(R.plurals.unit_months, model.interval)
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.YEARS ->
                        pluralStringResource(R.plurals.unit_years, model.interval)
                }.lowercase(Locale.getDefault())
                stringResource(R.string.rec_every, model.interval, unitStr)
            } else {
                stringResource(R.string.rs_custom_interval)
            }
            RecurrenceInfoCard(text = summaryText, modifier = modifier)
        }
        RecurrenceType.YEARLY -> {
            RecurrenceInfoCard(text = stringResource(R.string.card_recur_yearly), modifier = modifier)
        }
    }
}

@Composable
private fun RecurrenceEndRuleChip(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.EventBusy,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun buildRecurrenceEndRuleText(
    recurrenceType: RecurrenceType,
    recurrenceJson: String?
): RecurrenceEndRuleDisplay? {
    val model = remember(recurrenceType, recurrenceJson) {
        RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)
    } ?: return null
    return when (model.endRule.type) {
        EndRuleType.NEVER -> null
        EndRuleType.UNTIL_DATE -> {
            val endDate = model.endRule.endDateMillis ?: return null
            val compactFormatter = SimpleDateFormat("MMM d ''yy • ${com.ghostgramlabs.speakalert.util.TimeFormat.timePattern}", Locale.getDefault())
            val fullFormatter = SimpleDateFormat("MMM d, yyyy 'at' ${com.ghostgramlabs.speakalert.util.TimeFormat.timePattern}", Locale.getDefault())
            RecurrenceEndRuleDisplay(
                text = compactFormatter.format(Date(endDate)),
                contentDescription = stringResource(R.string.det_ends_by, fullFormatter.format(Date(endDate)))
            )
        }
        EndRuleType.AFTER_OCCURRENCES -> {
            val count = model.endRule.count ?: 0
            RecurrenceEndRuleDisplay(
                text = pluralStringResource(R.plurals.card_occurrences, count, count),
                contentDescription = pluralStringResource(R.plurals.det_ends_after, count, count)
            )
        }
    }
}

private data class RecurrenceEndRuleDisplay(
    val text: String,
    val contentDescription: String
)

// ─── Redesigned Reminder Card ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    title: String,
    badgeTime: String,
    dateLabel: String,
    recurrenceSummary: String?,
    recurrenceType: RecurrenceType = RecurrenceType.NONE,
    recurrenceJson: String? = null,
    hasAudio: Boolean,
    hasText: Boolean,
    isTextToSpeechEnabled: Boolean,
    hasCustomAudioFile: Boolean = false,
    isPlaying: Boolean,
    isCompleted: Boolean = false,
    loopEnabled: Boolean = false,
    followUpCheckMinutes: Int = 0,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDuplicateClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    // Pulse animation for playing state
    val pulseAlpha = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "playPulse")
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "borderPulse"
        ).value
    } else 1f
    val borderColor = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    }
    val borderWidth = if (isPlaying) 2.dp else 1.dp

    // Build a concise subtitle: "Today • Monthly" or "Upcoming • Daily"
    val recurrenceLabel = when (recurrenceType) {
        RecurrenceType.NONE -> stringResource(R.string.rec_onetime)
        RecurrenceType.DAILY -> stringResource(R.string.rec_daily)
        RecurrenceType.WEEKLY -> stringResource(R.string.rec_weekly)
        RecurrenceType.MONTHLY -> stringResource(R.string.rec_monthly)
        RecurrenceType.YEARLY -> stringResource(R.string.rec_yearly)
        RecurrenceType.CUSTOM -> stringResource(R.string.rec_custom)
    }
    val subtitleLine = listOfNotNull(
        dateLabel.takeIf { it.isNotEmpty() },
        recurrenceLabel
    ).joinToString(" • ")
    val recurrenceEndRuleText = buildRecurrenceEndRuleText(recurrenceType, recurrenceJson)

    // Resolved up front: the semantics lambda below is not composable.
    val stateCompleted = stringResource(R.string.state_completed)
    val statePlaying = stringResource(R.string.state_playing)
    val stateActive = stringResource(R.string.state_active)
    val cdReminder = stringResource(R.string.a11y_card_reminder, title)
    val cdAt = stringResource(R.string.a11y_card_at, badgeTime)
    val cdAudioFile = stringResource(R.string.a11y_card_has_audio_file)
    val cdVoiceNote = stringResource(R.string.a11y_card_has_voice)
    val cdRecurring = recurrenceSummary?.let { stringResource(R.string.a11y_card_recurring, it) }
    val cdFollowUp = if (followUpCheckMinutes > 0) {
        stringResource(R.string.card_cd_followup, followUpCheckMinutes)
    } else {
        null
    }
    val cdCompleted = stringResource(R.string.a11y_card_completed)
    val cdPlaying = stringResource(R.string.a11y_card_playing)
    val cdOpenHint = stringResource(R.string.a11y_card_open_hint)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { 
                role = Role.Button
                stateDescription = when {
                    isCompleted -> stateCompleted
                    isPlaying -> statePlaying
                    else -> stateActive
                }
                contentDescription = buildList {
                    add(cdReminder)
                    if (dateLabel.isNotEmpty()) add(dateLabel)
                    add(cdAt)
                    if (hasAudio) add(if (hasCustomAudioFile) cdAudioFile else cdVoiceNote)
                    cdRecurring?.let { add(it) }
                    cdFollowUp?.let { add(it) }
                    if (isCompleted) add(cdCompleted)
                    if (isPlaying) add(cdPlaying)
                }.joinToString(", ") + ". " + cdOpenHint
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 3.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── ROW 1: Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp, top = 18.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time Badge
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val timeParts = badgeTime.split(" ")
                            val timeOnly = timeParts.firstOrNull() ?: badgeTime
                            val amPm = timeParts.lastOrNull()?.takeIf { 
                                it.uppercase() in listOf("AM", "PM") 
                            } ?: ""

                            Text(
                                text = timeOnly,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (amPm.isNotEmpty()) {
                                Text(
                                    text = amPm.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Title + Subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp)
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    
                    Text(
                        text = subtitleLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Quick actions
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDuplicateClick != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDuplicateClick()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.card_cd_duplicate),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.card_cd_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── ROW 2: Schedule Details (full width) ───────────────────────
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            
            RecurrenceDetailsRow(
                recurrenceType = recurrenceType,
                recurrenceJson = recurrenceJson,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            if (hasAudio || (hasText && isTextToSpeechEnabled)) {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isPlaying) onStopClick() else onPlayClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isPlaying) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (isPlaying) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(if (isPlaying) R.string.sheet_stop_playback else R.string.rrd_play))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // ── FOOTER: Type chip + recurrence icon ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetadataChip(
                    icon = when {
                        hasCustomAudioFile -> Icons.Filled.Folder
                        hasAudio -> Icons.Filled.Mic
                        else -> null
                    },
                    text = when {
                        hasCustomAudioFile -> stringResource(R.string.card_type_audio_file)
                        hasAudio -> stringResource(R.string.card_type_voice)
                        else -> stringResource(R.string.card_type_text)
                    },
                    iconContentDescription = when {
                        hasCustomAudioFile -> stringResource(R.string.card_custom_audio_file)
                        hasAudio -> stringResource(R.string.card_voice_recording)
                        else -> stringResource(R.string.card_text_note)
                    },
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    onColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (recurrenceEndRuleText != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RecurrenceEndRuleChip(
                            text = recurrenceEndRuleText.text,
                            contentDescription = recurrenceEndRuleText.contentDescription,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recurrenceType != RecurrenceType.NONE) {
                        MetadataChip(
                            icon = Icons.Filled.Repeat,
                            text = stringResource(R.string.card_chip_recurring),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            onColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = stringResource(R.string.card_recur_onetime),
                                modifier = Modifier.padding(6.dp).size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (loopEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.88f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AllInclusive,
                                contentDescription = stringResource(R.string.card_cd_looping),
                                modifier = Modifier.padding(6.dp).size(14.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (followUpCheckMinutes > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .semantics {
                                        contentDescription = cdFollowUp.orEmpty()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${followUpCheckMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.sheet_reminder_actions),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ActionSheetRow(
                    icon = Icons.Filled.Edit,
                    label = stringResource(R.string.ae_header_edit),
                    onClick = {
                        showMenu = false
                        onEditClick()
                    },
                    emphasize = true
                )

                if (isPlaying && (hasAudio || (hasText && isTextToSpeechEnabled))) {
                    ActionSheetRow(
                        icon = Icons.Filled.Stop,
                        label = stringResource(R.string.sheet_stop_playback),
                        onClick = {
                            showMenu = false
                            onStopClick()
                        }
                    )
                }

                if (!isCompleted) {
                    ActionSheetRow(
                        icon = Icons.Filled.Check,
                        label = stringResource(if (recurrenceSummary != null) R.string.sheet_mark_this_done else R.string.det_mark_done),
                        onClick = {
                            showMenu = false
                            onCompleteClick()
                        }
                    )
                }

                ActionSheetRow(
                    icon = Icons.Filled.Delete,
                    label = stringResource(if (recurrenceSummary != null) R.string.rcd_stop_recurring else R.string.sheet_delete_reminder),
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    },
                    isDestructive = true
                )
            }
        }
    }
}

@Composable
fun MetadataChip(
    icon: ImageVector?,
    text: String,
    color: Color,
    onColor: Color,
    // Passed in explicitly: deriving it by comparing translated text would break per language.
    iconContentDescription: String? = null
) {
    Surface(
        shape = CircleShape,
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    modifier = Modifier.size(13.dp),
                    tint = onColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = onColor,
                maxLines = 1
            )
        }
    }
}


@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        )
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ActionSheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    emphasize: Boolean = false,
    isDestructive: Boolean = false
) {
    val containerColor = when {
        isDestructive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        emphasize -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }
    val contentColor = when {
        isDestructive -> MaterialTheme.colorScheme.error
        emphasize -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val destructiveState = stringResource(R.string.a11y_destructive)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = if (subLabel.isNullOrBlank()) {
                    label
                } else {
                    "$label. $subLabel"
                }
                if (isDestructive) {
                    stateDescription = destructiveState
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isDestructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                emphasize -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                }
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
                if (!subLabel.isNullOrBlank()) {
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDestructive) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FollowUpDurationPicker(
    currentMinutes: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<Int> = listOf(0, 5, 10, 15),
    customDefault: Int = 20,
    maxMinutes: Int = 240
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    val isCustom = currentMinutes > 0 && currentMinutes !in presets

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { minutes ->
            FilterChip(
                selected = currentMinutes == minutes,
                onClick = { onChange(minutes) },
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
            selected = isCustom,
            onClick = { showCustomDialog = true },
            label = {
                Text(
                    text = if (isCustom) "${currentMinutes}m" else stringResource(R.string.rec_custom),
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

    if (showCustomDialog) {
        CustomFollowUpDurationDialog(
            initialValue = if (isCustom) currentMinutes else customDefault,
            maxMinutes = maxMinutes,
            onDismiss = { showCustomDialog = false },
            onSave = { minutes ->
                onChange(minutes)
                showCustomDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFollowUpDurationDialog(
    initialValue: Int,
    maxMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember(initialValue) {
        mutableStateOf(initialValue.coerceAtLeast(1).toString())
    }
    val parsedValue = value.toLocalizedIntOrNull()
    val isValid = parsedValue != null && parsedValue in 1..maxMinutes
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.fu_custom_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.fu_custom_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    val normalized = newValue.normalizeLocalizedDigitsOrNull()
                    if (normalized != null && normalized.length <= maxMinutes.toString().length) {
                        value = normalized
                    }
                },
                label = { Text(stringResource(R.string.set_minutes)) },
                supportingText = { Text(stringResource(R.string.fu_allowed_range, maxMinutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = value.isNotEmpty() && !isValid,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onSave((parsedValue ?: 10).coerceIn(1, maxMinutes))
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

@Composable
fun VoiceRecorderCard(
    isRecording: Boolean,
    isPlaying: Boolean = false,
    hasRecording: Boolean,
    onRecordClick: () -> Unit,
    onStopClick: () -> Unit,
    onPlayClick: () -> Unit,
    onStopPlaybackClick: () -> Unit = {},
    playbackProgress: Float = 0f,
    onSeek: (Float) -> Unit = {},
    recordingElapsedSeconds: Int = 0,
    currentAmplitude: Int = 0,
    maxRecordingSeconds: Int = 300, 
    modifier: Modifier = Modifier
) {
    val safePlaybackProgress = playbackProgress.sanitizeUnitFloat()
    val safeRecordingProgress = if (maxRecordingSeconds > 0) {
        (recordingElapsedSeconds.toFloat() / maxRecordingSeconds.toFloat()).sanitizeUnitFloat()
    } else {
        0f
    }

    // Pulsing animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Resolved up front: the semantics lambdas below are not composable.
    val stateRecording = stringResource(R.string.a11y_recording_in_progress)
    val statePlayingPreview = stringResource(R.string.a11y_playing_preview)
    val stateVoiceRecorded = stringResource(R.string.a11y_voice_recorded)
    val stateNoVoice = stringResource(R.string.a11y_no_voice_recorded)
    val cdDoubleTapStop = stringResource(R.string.vr_cd_double_tap_stop)
    val cdLevel = stringResource(R.string.vr_cd_level)
    val cdStartRecording = stringResource(R.string.vr_cd_start)
    val cdPreviewProgress = stringResource(R.string.vr_cd_preview_progress)
    val cdPreviewPercent = stringResource(
        R.string.a11y_percent,
        (safePlaybackProgress * 100).toInt()
    )

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isRecording) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics {
                stateDescription = when {
                    isRecording -> stateRecording
                    isPlaying -> statePlayingPreview
                    hasRecording -> stateVoiceRecorded
                    else -> stateNoVoice
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
             if (isRecording) {
                 // Recording state
                 val minutes = recordingElapsedSeconds / 60
                 val seconds = recordingElapsedSeconds % 60
                 val maxMinutes = maxRecordingSeconds / 60
                 val remainingSeconds = maxRecordingSeconds - recordingElapsedSeconds
                 val remainingMins = remainingSeconds / 60
                 val remainingSecs = remainingSeconds % 60
                 
                 Text(
                     stringResource(R.string.vr_recording_ellipsis), 
                     style = MaterialTheme.typography.titleLarge, 
                     color = MaterialTheme.colorScheme.onErrorContainer
                 )
                 
                 // Show elapsed time
                 Text(
                     String.format("%d:%02d / %d:00", minutes, seconds, maxMinutes),
                     style = MaterialTheme.typography.headlineMedium,
                     color = MaterialTheme.colorScheme.onErrorContainer,
                     fontWeight = FontWeight.Bold
                 )
                 
                 // Warning when close to limit
                 if (remainingSeconds <= 30) {
                     Text(
                         "⚠ ${remainingMins}:${String.format("%02d", remainingSecs)} remaining",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.error
                     )
                 }
                 
                 Spacer(modifier = Modifier.height(16.dp))
                 
                 // Real-time Waveform
                 VoiceWaveform(
                     currentAmplitude = currentAmplitude,
                     isRecording = true,
                     modifier = Modifier
                         .height(60.dp)
                         .fillMaxWidth()
                         .padding(horizontal = 32.dp)
                         .semantics {
                             contentDescription = cdLevel
                         }
                 )
                 
                 Spacer(modifier = Modifier.height(24.dp))
                 
                 // Visual Indicator with Progress Ring
                 Box(contentAlignment = Alignment.Center) {
                     // Progress ring
                     CircularProgressIndicator(
                         progress = safeRecordingProgress,
                         modifier = Modifier.size(100.dp),
                         color = MaterialTheme.colorScheme.error,
                         strokeWidth = 4.dp
                     )
                     
                     // Ripple effect
                     Box(
                         modifier = Modifier
                             .size(80.dp)
                             .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale, alpha = 0.3f)
                             .background(MaterialTheme.colorScheme.error, CircleShape)
                     )
                     
                     IconButton(
                         onClick = onStopClick,
                         modifier = Modifier
                             .size(64.dp)
                             .background(MaterialTheme.colorScheme.error, CircleShape)
                     ) {
                         Icon(
                             Icons.Filled.Stop, 
                             contentDescription = stringResource(R.string.vr_cd_stop),
                             tint = Color.White, 
                             modifier = Modifier.size(32.dp)
                         )
                     }
                 }
                 Spacer(modifier = Modifier.height(16.dp))
                 Text(
                     text = stringResource(R.string.vr_tap_to_stop),
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onErrorContainer,
                     modifier = Modifier.semantics { contentDescription = cdDoubleTapStop }
                 )

             } else if (isPlaying) {
                 // Playing state
                 Text(stringResource(R.string.vr_playing_audio), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 Spacer(modifier = Modifier.height(16.dp))
                 
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     IconButton(
                         onClick = onStopPlaybackClick,
                         modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                     ) {
                         Icon(Icons.Filled.Stop, stringResource(R.string.sheet_stop_playback), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                     }
                     
                     Slider(
                         value = safePlaybackProgress,
                         onValueChange = onSeek,
                         modifier = Modifier
                             .weight(1f)
                             .padding(horizontal = 16.dp)
                             .semantics {
                                 contentDescription = cdPreviewProgress
                                 stateDescription = cdPreviewPercent
                             }
                     )
                 }
                 
             } else {
                 // Idle State
                 if (hasRecording) {
                     Row(
                         verticalAlignment = Alignment.CenterVertically, 
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween
                     ) {
                         Column {
                            Text(stringResource(R.string.vr_voice_recorded), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.vr_text_optional), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         }
                         
                         Row {
                             FilledIconButton(
                                 onClick = onPlayClick,
                                 colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                             ) {
                                 Icon(Icons.Filled.PlayArrow, stringResource(R.string.vr_play_recording))
                             }
                             Spacer(modifier = Modifier.width(8.dp))
                             OutlinedIconButton(
                                 onClick = onRecordClick,
                                 border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                             ) {
                                  Icon(Icons.Filled.Mic, stringResource(R.string.vr_cd_restart), tint = MaterialTheme.colorScheme.primary)
                             }
                         }
                     }
                 } else {
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Box(
                             contentAlignment = Alignment.Center,
                             modifier = Modifier
                                 .size(80.dp)
                                  .clip(CircleShape)
                                 .clickable(onClick = onRecordClick)
                                 .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                 .semantics { 
                                     role = androidx.compose.ui.semantics.Role.Button
                                     contentDescription = cdStartRecording
                                 }
                         ) {
                             Icon(
                                 Icons.Filled.Mic,
                                 null,
                                 tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                 modifier = Modifier.size(36.dp)
                             )
                         }
                         Spacer(modifier = Modifier.height(12.dp))
                         Text(
                             "Tap to record a voice reminder",
                             style = MaterialTheme.typography.titleMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             textAlign = androidx.compose.ui.text.style.TextAlign.Center
                         )
                     }
                 }
             }
        }
    }
}
@Composable
fun VoiceWaveform(
    currentAmplitude: Int,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 30
    val amplitudes = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0.1f) } } }
    
    LaunchedEffect(currentAmplitude) {
        if (isRecording) {
            // Normalize amplitude (MediaRecorder.getMaxAmplitude() returns 0-32767)
            val normalized = (currentAmplitude.toFloat() / 32767f).coerceIn(0.1f, 1f)
            // Shift amplitudes left
            for (i in 0 until barCount - 1) {
                amplitudes[i] = amplitudes[i + 1]
            }
            amplitudes[barCount - 1] = normalized
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        amplitudes.forEachIndexed { index, amplitude ->
            val animatedHeight by animateFloatAsState(
                targetValue = amplitude,
                animationSpec = tween(100),
                label = "height"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(
                            alpha = if (index == barCount - 1) 1f else 0.4f + (index.toFloat() / barCount) * 0.4f
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
