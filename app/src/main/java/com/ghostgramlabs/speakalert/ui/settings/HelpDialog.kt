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
                    title = "Creating reminders",
                    content = "- Voice: Tap the Mic to record quickly.\n- Text: Type reminder text if you do not want to record audio.\n- Title: Add a short label to identify reminders faster.\n- Schedule: Set date/time and save."
                )

                HelpSection(
                    title = "Home tabs",
                    content = "- Today: Reminders scheduled for today.\n- Upcoming: Future reminders after today.\n- Missed: Reminders that were not delivered at their scheduled moment.\n- Done: Completed reminders."
                )

                HelpSection(
                    title = "Card quick actions",
                    content = "- Tap a card to open details.\n- Use the Play reminder or Stop playback button on the card.\n- Use the menu (three dots) for Edit, Mark done, Stop recurring, or Delete."
                )

                HelpSection(
                    title = "Repeating reminders",
                    content = "- Repeat modes: Daily, Weekly, Monthly, or Custom.\n- Custom supports intervals such as every 2 days or every 3 hours.\n- Mark done on a repeating reminder clears only the current occurrence."
                )

                HelpSection(
                    title = "Playback settings",
                    content = "- Auto-play audio: Start playback automatically when reminder fires.\n- Text-to-Speech: Speak typed reminders when no voice note is available.\n- Loop duration: Keep playback repeating until stop or timeout."
                )

                HelpSection(
                    title = "Snooze and quiet hours",
                    content = "- Snooze delays the reminder by your configured default duration.\n- Quiet hours silences reminders between selected start and end times."
                )

                HelpSection(
                    title = "After device restart",
                    content = "- Future reminders are rescheduled automatically.\n- Past-due reminders are handled through missed reminder flow.\n- If playback cannot start immediately, notification fallback is used."
                )

                HelpSection(
                    title = "Why voice reminders",
                    content = "Voice reminders give context instantly, so you know exactly what to do without reading long text."
                )

                HelpSection(
                    title = "Reliability tips",
                    content = "- Keep Notifications and Exact Alarms permissions enabled.\n- Exclude the app from aggressive battery optimization for better delivery reliability."
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
