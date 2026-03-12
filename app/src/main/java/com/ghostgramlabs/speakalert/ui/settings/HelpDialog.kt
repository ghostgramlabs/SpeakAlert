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
                    content = "- Voice: Tap the Mic to record quickly.\n- Audio File: Pick an audio file from your device.\n- Text (TTS): Type reminder text.\n- The text you enter is shown in reminder screens/notifications.\n- If Voice or Audio File is selected, text is optional (display only).\n- If no audio is selected and TTS is enabled, text is spoken aloud.\n- Title: Add a short label to identify reminders faster.\n- Schedule: Pick date/time, repeat options, follow-up check, and save."
                )

                HelpSection(
                    title = "One-time reminders",
                    content = "- A one-time reminder is not completed just because it fired.\n- It stays active until you choose Done or Dismiss.\n- Snooze moves it to the snooze time and keeps it active."
                )

                HelpSection(
                    title = "Home tabs",
                    content = "- Today: Reminders scheduled for today.\n- Upcoming: Future reminders after today.\n- Missed: Reminders that were not delivered at their scheduled moment.\n- Done: Completed reminders."
                )

                HelpSection(
                    title = "Widgets",
                    content = "- Quick Reminder widget: Opens Add Reminder directly.\n- Upcoming widget: Shows upcoming reminders and missed reminders.\n- Tap a widget item to open the related reminder in the app.\n- Widget content auto-refreshes after add/edit/delete and missed updates."
                )

                HelpSection(
                    title = "Missed reminder popup",
                    content = "- On app open, missed reminders can appear in a quick recovery popup.\n- Play missed reminder: Plays the latest missed reminder immediately.\n- Close: Hides the popup and keeps missed reminders in the Missed tab."
                )

                HelpSection(
                    title = "Card quick actions",
                    content = "- Tap a card to open details.\n- Use the Play reminder or Stop playback button on the card.\n- Use the menu (three dots) for Edit, Mark done, Stop recurring, or Delete."
                )

                HelpSection(
                    title = "Notification actions",
                    content = "- Play Reminder: Plays available reminder audio (voice, audio file, or TTS).\n- Done: Marks the current occurrence as done.\n- Snooze: Delays by your configured snooze duration.\n- Swipe away: Same as Done - marks current occurrence done and stops active playback.\n- For repeating reminders, Done and swipe clear only the current occurrence. The next scheduled occurrence continues."
                )

                HelpSection(
                    title = "Full-screen reminder alert",
                    content = "- Optional full-screen alert can appear over lock screen when reminders fire.\n- Works with voice, audio file, and TTS reminder modes.\n- If Android full-screen permission is off, app falls back to standard notification."
                )

                HelpSection(
                    title = "Repeating reminders",
                    content = "- Repeat modes: Daily, Weekly, Monthly, Yearly, or Custom.\n- The selected date/time is treated as the first occurrence.\n- Custom supports intervals such as every 2 days or every 3 hours.\n- Mark done on a repeating reminder clears only the current occurrence."
                )

                HelpSection(
                    title = "Playback settings",
                    content = "- Auto-play audio: Starts playback automatically when reminder fires.\n- Text-to-Speech: Speaks typed reminders when no voice/audio file is selected.\n- Tone-only mode: Plays an alarm tone at fire time and keeps playback actions manual.\n- Loop duration: Controls auto-stop timeout."
                )

                HelpSection(
                    title = "Follow-up check",
                    content = "- Follow-up asks again if a reminder is not marked done.\n- You can select preset minutes or set a custom minute value.\n- Follow-up runs after the active reminder or snooze cycle."
                )

                HelpSection(
                    title = "Tone-only mode (more reliable alerts)",
                    content = "- On some phones, voice or text-to-speech playback may be less reliable due to system settings or battery optimization.\n- Tone-only mode plays a clear alarm tone instead of voice/TTS.\n- You can still tap Play Reminder from the notification.\n- Tone stop conditions: Done, Snooze, Play Reminder, or loop timeout.\n- Use this mode if voice playback is delayed, sometimes silent, or if you prefer a simple reliable alert."
                )

                HelpSection(
                    title = "Snooze and quiet hours",
                    content = "- Snooze delays the reminder by your configured default duration.\n- Quiet hours silences reminder playback between selected start and end times.\n- Tone-only mode also follows quiet hours."
                )

                HelpSection(
                    title = "After device restart",
                    content = "- Future reminders are rescheduled automatically.\n- Past-due reminders are handled through missed reminder flow.\n- If playback cannot start immediately, notification fallback is used."
                )

                HelpSection(
                    title = "Wear OS support",
                    content = "- SpeakAlert reminders can appear on connected Wear OS watches.\n- Phone app notifications and reminder channel must be enabled.\n- Notification sync must be enabled in your watch companion app (Wear OS / Galaxy Wearable)."
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
