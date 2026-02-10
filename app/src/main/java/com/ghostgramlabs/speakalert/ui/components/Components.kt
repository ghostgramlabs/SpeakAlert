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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.MonthlyVariant
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType

// ─── Monthly Day Grid ─────────────────────────────────────────────────────────
@Composable
fun MonthlyDayGrid(
    selectedDays: Set<Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Days of month",
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
                                .clip(RoundedCornerShape(6.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(6.dp)
                                        )
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            RoundedCornerShape(6.dp)
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
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            )
                        } else {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                        }
                    )
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
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
            Text(
                text = "One-time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RecurrenceType.DAILY -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "🔁  Repeats every day",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                )
            }
        }
        RecurrenceType.WEEKLY -> {
            val model = RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)
            if (model is RecurrenceModel.Weekly) {
                WeekdayChips(selectedDays = model.daysOfWeek, modifier = modifier)
            } else {
                Text(
                    text = "Weekly",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RecurrenceType.MONTHLY -> {
            val model = RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)
            if (model is RecurrenceModel.Monthly) {
                if (model.variant == MonthlyVariant.LAST_DAY) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "📅  Last day of each month",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                        )
                    }
                } else {
                    MonthlyDayGrid(
                        selectedDays = model.daysOfMonth,
                        modifier = modifier
                    )
                }
            } else {
                Text(
                    text = "Monthly",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RecurrenceType.CUSTOM -> {
            val model = RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)
            val summaryText = if (model is RecurrenceModel.Custom) {
                val unitStr = when (model.unit) {
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.MINUTES -> if (model.interval == 1) "minute" else "minutes"
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.HOURS -> if (model.interval == 1) "hour" else "hours"
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.DAYS -> if (model.interval == 1) "day" else "days"
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.WEEKS -> if (model.interval == 1) "week" else "weeks"
                    com.ghostgramlabs.speakalert.domain.models.TimeUnit.MONTHS -> if (model.interval == 1) "month" else "months"
                }
                "⏱  Every ${model.interval} $unitStr"
            } else {
                "Custom interval"
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                )
            }
        }
    }
}

// ─── Redesigned Reminder Card ─────────────────────────────────────────────────
@Composable
fun ReminderCard(
    title: String,
    badgeTime: String,
    dateLabel: String,
    recurrenceSummary: String?,
    recurrenceIcon: ImageVector?,
    recurrenceType: RecurrenceType = RecurrenceType.NONE,
    recurrenceJson: String? = null,
    hasAudio: Boolean,
    hasText: Boolean,
    isTextToSpeechEnabled: Boolean,
    isPlaying: Boolean,
    isCompleted: Boolean = false,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val borderColor = if (isPlaying) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isPlaying) 2.dp else 1.dp
    var showMenu by remember { mutableStateOf(false) }

    // Build a concise subtitle: "Today • Monthly" or "Upcoming • Daily"
    val recurrenceLabel = when (recurrenceType) {
        RecurrenceType.NONE -> "One-time"
        RecurrenceType.DAILY -> "Daily"
        RecurrenceType.WEEKLY -> "Weekly"
        RecurrenceType.MONTHLY -> "Monthly"
        RecurrenceType.CUSTOM -> "Custom"
    }
    val subtitleLine = listOfNotNull(
        dateLabel.takeIf { it.isNotEmpty() },
        recurrenceLabel
    ).joinToString(" • ")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { 
                contentDescription = buildString {
                    append("Reminder: $title")
                    if (dateLabel.isNotEmpty()) append(", $dateLabel")
                    append(", at $badgeTime")
                    if (hasAudio) append(", has voice note")
                    if (recurrenceSummary != null) append(", recurring $recurrenceSummary")
                    if (isCompleted) append(", completed")
                    if (isPlaying) append(", currently playing")
                    append(". Double tap to play or view details.")
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            borderWidth, 
            if (isPlaying) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── ROW 1: Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
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
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = subtitleLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Actions Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Overflow Menu at top
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { showMenu = false; onEditClick() },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) }
                            )
                            if (!isCompleted) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(if (recurrenceSummary != null) "Mark this done" else "Mark as Done") 
                                    },
                                    onClick = { showMenu = false; onCompleteClick() },
                                    leadingIcon = { Icon(Icons.Filled.Check, null) }
                                )
                            }
                            val deleteLabel = if (recurrenceSummary != null) "Stop recurring" else "Delete"
                            DropdownMenuItem(
                                text = { Text(deleteLabel) },
                                onClick = { showMenu = false; onDeleteClick() },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                    
                    // Play/Stop Button below menu
                    if (hasAudio || (hasText && isTextToSpeechEnabled)) {
                        IconButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isPlaying) onStopClick() else onPlayClick() 
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop" else "Play",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── ROW 2: Recurrence Details (full width) ─────────────────────
            if (recurrenceType != RecurrenceType.NONE) {
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
            }
            
            // ── FOOTER: Type chip + recurrence icon ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = if (recurrenceType == RecurrenceType.NONE) 0.dp else 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetadataChip(
                    icon = if (hasAudio) Icons.Filled.Mic else null,
                    text = if (hasAudio) "Voice" else "Text",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                if (recurrenceType != RecurrenceType.NONE) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Recurring",
                            modifier = Modifier.padding(6.dp).size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataChip(
    icon: ImageVector?,
    text: String,
    color: Color,
    onColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = onColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
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
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
    TextButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
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

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth().animateContentSize()
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
                     "Recording...", 
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
                             contentDescription = "Real-time sound level visualization"
                         }
                 )
                 
                 Spacer(modifier = Modifier.height(24.dp))
                 
                 // Visual Indicator with Progress Ring
                 Box(contentAlignment = Alignment.Center) {
                     // Progress ring
                     CircularProgressIndicator(
                         progress = recordingElapsedSeconds.toFloat() / maxRecordingSeconds.toFloat(),
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
                             contentDescription = "Stop Recording", 
                             tint = Color.White, 
                             modifier = Modifier.size(32.dp)
                         )
                     }
                 }
                 Spacer(modifier = Modifier.height(16.dp))
                 Text(
                     text = "Tap to Stop", 
                     style = MaterialTheme.typography.bodySmall, 
                     color = MaterialTheme.colorScheme.onErrorContainer,
                     modifier = Modifier.semantics { contentDescription = "Double tap to stop the recording" }
                 )
                 
             } else if (isPlaying) {
                 // Playing state
                 Text("Playing Audio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 Spacer(modifier = Modifier.height(16.dp))
                 
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     IconButton(
                         onClick = onStopPlaybackClick,
                         modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                     ) {
                         Icon(Icons.Filled.Stop, "Stop Playback", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                     }
                     
                     Slider(
                         value = playbackProgress,
                         onValueChange = onSeek,
                         modifier = Modifier
                             .weight(1f)
                             .padding(horizontal = 16.dp)
                             .semantics { contentDescription = "Playback progress" }
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
                            Text("Voice recorded", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                             Text("Text is optional", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         }
                         
                         Row {
                             FilledIconButton(
                                 onClick = onPlayClick,
                                 colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                             ) {
                                 Icon(Icons.Filled.PlayArrow, "Play Recording")
                             }
                             Spacer(modifier = Modifier.width(8.dp))
                             OutlinedIconButton(
                                 onClick = onRecordClick,
                                 border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                             ) {
                                  Icon(Icons.Filled.Mic, "Restart Recording", tint = MaterialTheme.colorScheme.primary)
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
                                 .background(MaterialTheme.colorScheme.primary, CircleShape)
                                 .semantics { 
                                     role = androidx.compose.ui.semantics.Role.Button
                                     contentDescription = "Start Voice Recording"
                                 }
                         ) {
                             Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(36.dp))
                         }
                         Spacer(modifier = Modifier.height(12.dp))
                         Text(
                             "Record a voice — or type a message to be spoken",
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
