package com.ghostgramlabs.speakalert.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "App Guide",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HelpSection(
                    title = "⚡ Getting Started",
                    content = "Tap the large microphone icon to create a reminder. Record a voice or type a message to be read aloud. Type an optional title to identify your reminders at a glance."
                )
                
                HelpSection(
                    title = "🔊 Audio & Playback",
                    content = "• Auto-play: If on, your reminder speaks as soon as it fires.\n• Only when unlocked: Prevents sound while your phone is in your pocket or locked.\n• Text-to-Speech: If you didn't record a voice, the app will read your text message aloud."
                )
                
                HelpSection(
                    title = "🔁 Loop & Timeout",
                    content = "Use 'Loop timeout' to set how long a reminder repeats (default 10m). Use 'Infinite Loop' (∞) if you want it to keep playing until you stop it."
                )

                HelpSection(
                    title = "💤 Snooze & Timing",
                    content = "• Snooze: Tap to postpone an alarm. Default is 5m, but you can pick a custom time.\n• Quiet Hours: Block all voice alerts during specific times (e.g., at night)."
                )
                
                HelpSection(
                    title = "🔄 Recurring Tasks",
                    content = "• 'Mark Done': Finishes just this occurrence. It will ring again at the next cycle.\n• 'Stop recurring': Totally deletes the reminder and future alarms."
                )

                HelpSection(
                    title = "🔔 Missed & Inbox",
                    content = "Missed reminders are kept in the 'Missed' tab. This happens if your phone was off, set to Quiet Hours, or in Do Not Disturb mode when the alarm was supposed to fire."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun HelpSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
