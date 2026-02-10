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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ReminderCard(
    title: String,
    badgeTime: String,
    dateLabel: String,
    recurrenceSummary: String?,
    recurrenceIcon: ImageVector?,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Time Badge - Hero element
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val timeParts = badgeTime.split(" ")
                        val timeOnly = timeParts.firstOrNull() ?: badgeTime
                        val amPm = timeParts.lastOrNull()?.takeIf { 
                            it.uppercase() in listOf("AM", "PM") 
                        } ?: ""

                        Text(
                            text = timeOnly,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (amPm.isNotEmpty()) {
                            Text(
                                text = amPm.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp) // Align text with top of time badge
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Subtitle
                val subtitleParts = listOfNotNull(
                    dateLabel.takeIf { it.isNotEmpty() },
                    recurrenceSummary?.takeIf { it.isNotEmpty() }
                )
                
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        text = subtitleParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 6,
                        overflow = TextOverflow.Clip // Ensure it wraps fully
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                // Metadata Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip 1: Type (Voice/Text) - Use primary container for cohesion
                    MetadataChip(
                        icon = if (hasAudio) Icons.Filled.Mic else null,
                        text = if (hasAudio) "Voice" else "Text",
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Chip 2: Recurrence (only show icon if recurring)
                    if (recurrenceSummary != null) {
                        // Icon-only recurring indicator
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
            
            // Actions
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(top = 4.dp) // Align buttons with top
            ) {
                // Play/Stop Button
                if (hasAudio || (hasText && isTextToSpeechEnabled)) {
                    IconButton(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isPlaying) onStopClick() else onPlayClick() 
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Edit
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) }
                        )
                        
                        // Mark as Done (for all active reminders)
                        if (!isCompleted) {
                             DropdownMenuItem(
                                text = { 
                                    Text(if (recurrenceSummary != null) "Mark this done" else "Mark as Done") 
                                },
                                onClick = {
                                    showMenu = false
                                    onCompleteClick()
                                },
                                leadingIcon = { Icon(Icons.Filled.Check, null) }
                            )
                        }

                        // Delete / Stop Recurring
                        val deleteLabel = if (recurrenceSummary != null) "Stop recurring" else "Delete"
                        DropdownMenuItem(
                            text = { Text(deleteLabel) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            )
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
