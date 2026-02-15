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
                    title = "📱 Organizing & Tabs",
                    content = "• Today: Everything happening right now or later today.\n• Upcoming: Plan ahead! See all your future reminders in one list.\n• Missed: If your phone was off or silenced, check here for alerts you might have skipped.\n• Done: A history of your completed tasks. You can always restore or reschedule them from here."
                )

                HelpSection(
                    title = "⚡ Card Quick Actions",
                    content = "Save time! Tap the Play/Stop icon directly on any reminder card to hear it instantly. Use the 'Check' icon (or the ⋮ menu) to mark a reminder as done without opening it."
                )
                
                HelpSection(
                    title = "🔁 Recurrence (Repeating Reminders)",
                    content = "• Daily/Weekly/Monthly: Set it once and forget it!\n• Custom: Set intervals like 'Every 3 hours' or 'Every 2 weeks'.\n• Mark Done: For repeating tasks, this only clears the *current* occurrence. It will ring again at the next scheduled time."
                )
                
                HelpSection(
                    title = "🔄 Loop Playback (Anti-Skip)",
                    content = "• Problem: Regular notifications might stop too quickly. \n• Solution: Enable 'Loop Playback' to make the audio repeat until you manually dismiss it.\n• Auto-Stop: Set a safety timeout (e.g., 10 minutes) in Settings so your battery doesn't drain if you're not near your phone."
                )
                
                HelpSection(
                    title = "💤 Snooze & Quiet Hours",
                    content = "• Smart Snooze: Need a few more minutes? Tap Snooze. You can change the default duration in Settings.\n• Quiet Hours: Block all voice alerts during specific times (like 11 PM to 7 AM) so you can sleep undisturbed."
                )

                HelpSection(
                    title = "🔌 Device Power & Restart",
                    content = "• Reliability: Your reminders are safe even if your phone restarts. SpeakAlert automatically sets them back up for you.\n• After Restart: Reminders that were due while your phone was off will appear as a notification and be added to your Missed list. Tap 'Play' on the notification to hear them.\n• Future Reminders: Any upcoming reminders will continue to work normally after a restart, including auto-play."
                )

                HelpSection(
                    title = "⚡ Reminders after device restart",
                    content = "• If your phone restarts near a reminder time, audio might not play automatically.\n• A notification will appear instead.\n• Tap the notification to play or reschedule."
                )

                HelpSection(
                    title = "📣 Why Voice Reminders?",
                    content = "Unlike a standard alert that just beeps, SpeakAlert gives you context. By hearing your own voice or personalized text, you know exactly what needs your attention without even unlocking your phone."
                )

                HelpSection(
                    title = "🚀 Reliability Tips",
                    content = "• Permissions: For the most accurate timing, ensure 'Post Notifications' and 'Exact Timing' (Reminders) are granted in system settings.\n• Battery: To prevent missed alerts, ensure SpeakAlert is set to 'Unrestricted' or 'Not Optimized' in your phone's battery settings."
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
