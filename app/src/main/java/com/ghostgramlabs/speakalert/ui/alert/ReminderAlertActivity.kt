package com.ghostgramlabs.speakalert.ui.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.alarm.ToneAlertPlayer
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.service.ReminderPlaybackService
import com.ghostgramlabs.speakalert.ui.theme.VoiceReminderTheme
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.DateUtils

class ReminderAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALERT_TITLE = "alertTitle"
        const val EXTRA_ALERT_MESSAGE = "alertMessage"
        const val EXTRA_PLAYBACK_AUDIO_PATH = "playbackAudioPath"
        const val EXTRA_PLAYBACK_TEXT = "playbackText"
        const val EXTRA_IS_FOLLOW_UP_ALERT = "isFollowUpAlert"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val reminderId = intent.getLongExtra("reminderId", -1L)
        val alertTitleOverride = intent.getStringExtra(EXTRA_ALERT_TITLE)
        val alertMessageOverride = intent.getStringExtra(EXTRA_ALERT_MESSAGE)
        val playbackAudioPathOverride = intent.getStringExtra(EXTRA_PLAYBACK_AUDIO_PATH)
        val playbackTextOverride = intent.getStringExtra(EXTRA_PLAYBACK_TEXT)
        val isFollowUpAlert = intent.getBooleanExtra(EXTRA_IS_FOLLOW_UP_ALERT, false)
        if (reminderId == -1L) {
            finish()
            return
        }

        setContent {
            val app = applicationContext as VoiceReminderApp
            val themeMode by app.container.settingsRepository.themeMode.collectAsState(initial = 0)
            val isDarkTheme = when (themeMode) {
                1 -> false // Light
                2 -> true  // Dark
                else -> isSystemInDarkTheme() // System
            }

            VoiceReminderTheme(darkTheme = isDarkTheme) {
                BackHandler(enabled = true) {
                    // Keep the reminder visible until the user explicitly chooses an action.
                }
                val reminder by produceState<ReminderEntity?>(initialValue = null, reminderId) {
                    value = app.container.reminderRepository.getReminder(reminderId)
                    if (value == null) finish()
                }
                reminder?.let {
                    ReminderAlertContent(
                        reminder = it,
                        onPlayAgain = {
                            sendPlayAction(
                                reminderId = it.id,
                                title = alertTitleOverride ?: it.title ?: APP_DISPLAY_NAME,
                                audioPath = playbackAudioPathOverride,
                                reminderText = playbackTextOverride
                            )
                        },
                        onStopPlayback = {
                            stopPlayback()
                        },
                        onDone = {
                            sendReminderAction("ACTION_DONE", it.id)
                            finish()
                        },
                        onSnoozeFive = {
                            sendReminderAction("ACTION_SNOOZE", it.id, 5)
                            finish()
                        },
                        onSnoozeTen = {
                            sendReminderAction("ACTION_SNOOZE", it.id, 10)
                            finish()
                        },
                        titleOverride = alertTitleOverride,
                        messageOverride = alertMessageOverride,
                        playbackAudioPath = playbackAudioPathOverride,
                        playbackText = playbackTextOverride,
                        isFollowUpAlert = isFollowUpAlert
                    )
                }
            }
        }
    }

    private fun sendReminderAction(action: String, reminderId: Long, snoozeMinutesOverride: Int? = null) {
        sendBroadcast(
            Intent(this, com.ghostgramlabs.speakalert.alarm.ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra("reminderId", reminderId)
                if (snoozeMinutesOverride != null) {
                    putExtra("snoozeMinutesOverride", snoozeMinutesOverride)
                }
            }
        )
    }

    private fun sendPlayAction(
        reminderId: Long,
        title: String,
        audioPath: String?,
        reminderText: String?
    ) {
        sendBroadcast(
            Intent(this, com.ghostgramlabs.speakalert.alarm.ReminderActionReceiver::class.java).apply {
                action = "ACTION_PLAY"
                putExtra("reminderId", reminderId)
                putExtra("title", title)
                audioPath?.let { putExtra("audioPath", it) }
                reminderText?.let { putExtra("reminderText", it) }
            }
        )
    }

    private fun stopPlayback() {
        ReminderPlaybackService.stop(this)
        ToneAlertPlayer.stop()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderAlertContent(
    reminder: ReminderEntity,
    onPlayAgain: () -> Unit,
    onStopPlayback: () -> Unit,
    onDone: () -> Unit,
    onSnoozeFive: () -> Unit,
    onSnoozeTen: () -> Unit,
    titleOverride: String?,
    messageOverride: String?,
    playbackAudioPath: String?,
    playbackText: String?,
    isFollowUpAlert: Boolean
) {
    val context = LocalContext.current
    var isPlaying by remember(reminder.id) { mutableStateOf(false) }

    DisposableEffect(context, reminder.id) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != "ACTION_PLAYBACK_STATUS") return
                val activeReminderId = intent.getLongExtra("reminderId", -1L)
                val playing = intent.getBooleanExtra("isPlaying", false)
                isPlaying = activeReminderId == reminder.id && playing
            }
        }
        val filter = IntentFilter("ACTION_PLAYBACK_STATUS")
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

    val isFollowUp = isFollowUpAlert || (reminder.followUpCheckMinutes > 0 && reminder.pendingFollowUpAt != null)
    val headline = titleOverride
        ?: reminder.title
        ?: reminder.reminderText
        ?: APP_DISPLAY_NAME
    val bodyText = messageOverride
        ?: reminder.reminderText
        ?: if (isFollowUp) {
            "Did you complete this reminder?"
        } else {
            "Reminder is active"
        }
    val scheduledText = DateUtils.formatSmartDate(reminder.nextTriggerAt)
    val canPlayAgain = !playbackAudioPath.isNullOrBlank() || !playbackText.isNullOrBlank()
    val sourceLabel = when {
        isFollowUp -> "Follow-up check"
        !playbackAudioPath.isNullOrBlank() -> "Voice reminder"
        !playbackText.isNullOrBlank() -> "Text reminder"
        else -> "Alert"
    }
    val statusLabel = when {
        isPlaying -> "Playing now"
        isFollowUp -> "Needs response"
        else -> "Awaiting action"
    }
    val pulse = rememberInfiniteTransition(label = "alert_pulse")
    val outerScale by pulse.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_scale"
    )
    val outerAlpha by pulse.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 96.dp, y = (-48).dp)
                    .size(220.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-74).dp, y = 74.dp)
                    .size(190.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                        )
                    ) {
                        Text(
                            text = if (isFollowUp) "Follow-up reminder" else APP_DISPLAY_NAME,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(104.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .graphicsLayer {
                                        scaleX = outerScale
                                        scaleY = outerScale
                                    }
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = outerAlpha),
                                        shape = CircleShape
                                    )
                            )
                            Surface(
                                modifier = Modifier.size(78.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isFollowUp) "Follow up on this reminder" else "Reminder on screen",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = headline,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!bodyText.equals(headline, ignoreCase = true)) {
                                Text(
                                    text = bodyText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AlertTag(text = sourceLabel)
                            AlertTag(text = statusLabel, highlighted = isPlaying)
                            if (isFollowUp) {
                                AlertTag(text = "Follow-up", highlighted = true)
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AlertMetaRow(
                                    icon = Icons.Filled.Schedule,
                                    label = "Scheduled for",
                                    value = scheduledText
                                )
                                AlertMetaRow(
                                    icon = if (isPlaying) Icons.Filled.Stop else Icons.Filled.NotificationsActive,
                                    label = "Alert status",
                                    value = if (isPlaying) {
                                        "Audio is playing. Use Silence to stop it."
                                    } else {
                                        "Waiting for Done or Snooze."
                                    }
                                )
                                if (reminder.followUpCheckMinutes > 0) {
                                    AlertMetaRow(
                                        icon = Icons.Filled.NotificationsActive,
                                        label = "Follow-up",
                                        value = "Repeats every ${reminder.followUpCheckMinutes} minutes until done."
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onDone,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mark Done",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isPlaying = false
                                        onStopPlayback()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPlaying) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
                                        }
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Stop,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPlaying) "Silence now" else "Silence",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (canPlayAgain) {
                                    FilledTonalButton(
                                        onClick = onPlayAgain,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isPlaying) "Replay" else "Play again",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SnoozeActionButton(
                                    label = "Snooze 5m",
                                    onClick = onSnoozeFive,
                                    modifier = Modifier.weight(1f)
                                )
                                SnoozeActionButton(
                                    label = "Snooze 10m",
                                    onClick = onSnoozeTen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "Silence stops playback. Mark Done or Snooze clears the lock-screen alert.",
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
    }
}

@Composable
private fun AlertTag(
    text: String,
    highlighted: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)
            }
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AlertMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun SnoozeActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
