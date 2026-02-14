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
                    title = "🎤 Creating Reminders",
                    content = "• Voice: Tap the Mic to record. It's the fastest way to set a reminder!\n• Type: Prefer text? Type your message. If 'Text-to-Speech' is on in Settings, the app will read it aloud for you.\n• Label: Give your reminder a short name (like 'Meds' or 'Gym') to find it easily in your list."
                )
                
                HelpSection(
                    title = "🔁 Recurrence (Repeating Alarms)",
                    content = "• Daily/Weekly/Monthly: Set it once and forget it! The app calculates the exact next trigger time automatically.\n• Custom: Need something unique? Set intervals like 'Every 3 hours' or 'Every 2 weeks'.\n• Mark Done: For repeating tasks, this only clears the *current* alarm. It will ring again at the next scheduled time."
                )
                
                HelpSection(
                    title = "🔄 Loop Playback (Anti-Skip)",
                    content = "• Problem: Regular alarms might stop too quickly. \n• Solution: Enable 'Loop Playback' to make the audio repeat until you manually dismiss it.\n• Auto-Stop: In Playback Settings, you can set a safety timeout (e.g., 10 minutes) so your battery doesn't drain if you're not near your phone."
                )
                
                HelpSection(
                    title = "💤 Snooze & Quiet Hours",
                    content = "• Smart Snooze: Need a few more minutes? Tap Snooze. You can change the default duration in Settings.\n• Quiet Hours: Block all voice alerts during specific times (like 11 PM to 7 AM) so you can sleep undisturbed."
                )

                HelpSection(
                    title = "📥 Missed Inbox",
                    content = "If your phone was off or in 'Quiet Hours' when an alarm was supposed to fire, it will appear in the 'Missed' tab. You can review them anytime and clear the list once you've seen them."
                )

                HelpSection(
                    title = "🚀 Reliability Tips",
                    content = "• Permissions: Ensure 'Post Notifications' and 'Exact Alarms' are granted to prevent missed alerts.\n• Battery: If your phone kills background apps, check our 'Permissions' card in Settings to ensure full reliability."
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
