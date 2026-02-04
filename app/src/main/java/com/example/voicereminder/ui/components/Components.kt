package com.example.voicereminder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ReminderCard(
    title: String,
    badgeTime: String,
    dateLabel: String, // e.g. "Today" or "Feb 12"
    formattedFullDate: String? = null,
    recurrenceSummary: String?,
    recurrenceIcon: ImageVector?,
    hasAudio: Boolean,
    hasText: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Badge - Hero element on the left
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display badgeTime (e.g. "8:30") and AM/PM logic
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
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Secondary Text: Date + Recurrence
                Row(verticalAlignment = Alignment.CenterVertically) {
                     // ALWAYS Show Date first (e.g. "Today" or "Tomorrow")
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (recurrenceSummary != null) {
                         Text(
                             text = " • $recurrenceSummary",
                             style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             maxLines = 1,
                             overflow = TextOverflow.Ellipsis
                         )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Metadata Row (Media Type)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Media type badge
                    if (hasAudio || hasText) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasAudio) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Voice",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                } else if (hasText) {
                                    Text(
                                        text = "📝 Text",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Recurrence badge
                    if (recurrenceSummary != null && recurrenceIcon != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = recurrenceIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Repeat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play/Stop Button
                if (hasAudio || hasText) {
                    IconButton(
                        onClick = { if (isPlaying) onStopClick() else onPlayClick() }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp).background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = CircleShape
                            ).padding(6.dp)
                        )
                    }
                }
            }
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
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
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
                 
                 // Visual Indicator
                 Box(contentAlignment = Alignment.Center) {
                     // Ripple effect
                     Box(
                         modifier = Modifier
                             .size(100.dp)
                             .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale, alpha = 0.3f)
                             .background(MaterialTheme.colorScheme.error, CircleShape)
                     )
                     IconButton(
                         onClick = onStopClick,
                         modifier = Modifier
                             .size(80.dp)
                             .background(MaterialTheme.colorScheme.error, CircleShape)
                     ) {
                         Icon(
                             Icons.Filled.Stop, 
                             null, 
                             tint = Color.White, 
                             modifier = Modifier.size(40.dp)
                         )
                     }
                 }
                 Spacer(modifier = Modifier.height(16.dp))
                 Text("Tap to Stop", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                 
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
                         Icon(Icons.Filled.Stop, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                     }
                     
                     Slider(
                         value = playbackProgress,
                         onValueChange = onSeek,
                         modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
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
                             Text("Recording Saved", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                             Text("Ready to save", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         }
                         
                         Row {
                             FilledIconButton(
                                 onClick = onPlayClick,
                                 colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                             ) {
                                 Icon(Icons.Filled.PlayArrow, null)
                             }
                             Spacer(modifier = Modifier.width(8.dp))
                             OutlinedIconButton(
                                 onClick = onRecordClick,
                                 border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                             ) {
                                  Icon(Icons.Filled.Mic, null, tint = MaterialTheme.colorScheme.primary)
                             }
                         }
                     }
                 } else {
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Box(
                             contentAlignment = Alignment.Center,
                             modifier = Modifier
                                 .size(80.dp)
                                 .clickable(onClick = onRecordClick)
                                 .background(MaterialTheme.colorScheme.primary, CircleShape)
                         ) {
                             Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(36.dp))
                         }
                         Spacer(modifier = Modifier.height(12.dp))
                         Text("Tap to Record", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                     }
                 }
             }
        }
    }
}
