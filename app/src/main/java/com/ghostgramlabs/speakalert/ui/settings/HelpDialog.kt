package com.ghostgramlabs.speakalert.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "App guide",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Everything important about creating, playing, repeating, and recovering reminders in $APP_DISPLAY_NAME.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HelpSection(
                    title = "Creating reminders",
                    content = "- Voice first: Tap the Mic to record a quick spoken reminder.\n- Audio file: Pick an existing audio file from your device.\n- Reminder message: Shown in the app and notifications.\n- Voice notes and audio files take priority over spoken typed text.\n- Typed reminders are spoken automatically only for text-only reminders when automatic spoken text is enabled.\n- Short label: Optional name for easier scanning in lists.\n- Schedule: Pick date, time, repeat, follow-up check, and save."
                )
                HelpSection(
                    title = "Playback and preview",
                    content = "- Add Reminder lets you preview recorded voice and selected audio before saving.\n- Reminder details lets you play, stop, and seek through voice notes and audio files.\n- Text-only reminders can still be spoken manually from the app or notification.\n- Tone-only mode uses the reminder tone instead of spoken playback.\n- You can choose a tone-only alert sound in Settings, and $APP_DISPLAY_NAME falls back to the default alarm tone if that sound becomes unavailable."
                )
                HelpSection(
                    title = "Home and quick actions",
                    content = "- Today, Upcoming, Missed, and Done tabs group reminders by state.\n- Tap a card to open full reminder details.\n- Use the copy button to duplicate an existing reminder.\n- Card actions include Play reminder, Done, and the three-dot sheet for Edit, Mark done, Stop recurring, or Delete."
                )
                HelpSection(
                    title = "Repeating reminders",
                    content = "- Repeat modes: Daily, Weekly, Monthly, Yearly, or Custom.\n- The selected date and time become the first occurrence.\n- Repeats can end forever, by exact date and time, or after a number of occurrences.\n- Marking done on a repeating reminder clears only the current occurrence."
                )
                HelpSection(
                    title = "Notifications and full-screen alerts",
                    content = "- Reminder notifications support Play reminder or Speak reminder, plus Done and Snooze.\n- Swiping away an active reminder marks the current occurrence done.\n- Lock-screen full-screen alert can appear when enabled in Settings."
                )
                HelpSection(
                    title = "Missed and restored reminders",
                    content = "- Missed reminders stay in the Missed tab until you review them.\n- On app open, a missed reminder recovery sheet may offer quick playback of the latest missed reminder.\n- Past reminders can be restored, rescheduled, or replayed from their action sheets."
                )
                HelpSection(
                    title = "Playback settings",
                    content = "- Auto-play reminder audio starts voice note or audio file playback when a reminder fires.\n- Speak typed reminders automatically controls spoken text for text-only reminders only.\n- Manual playback still works even if automatic spoken text is off.\n- Play through earpiece can switch near-ear playback to the earpiece when supported.\n- Loop playback keeps audio repeating until you stop it or the loop timeout is reached.\n- Follow-up check asks again until the reminder is marked done.\n- Quiet hours can silence reminder playback during selected times."
                )
                HelpSection(
                    title = "Tone-only mode and reliability",
                    content = "- Tone-only mode uses a clear alarm tone instead of voice or TTS.\n- You can choose a custom tone-only alert sound, or keep the default alarm tone.\n- Alert even during DND lets $APP_DISPLAY_NAME try to play important reminders while Do Not Disturb is active.\n- When Alert even during DND is off, reminders blocked by DND go to Missed instead of playing.\n- This setting does not turn your phone's DND mode on or off. Android DND access is required before notification bypass becomes active.\n- When enabled, reminders use alarm-priority audio. Some phone routines can still block alarms if the system mode silences alarm audio.\n- If the selected sound is missing later, $APP_DISPLAY_NAME automatically falls back to the default alarm tone.\n- Use tone-only mode if your phone delays, suppresses, or stops spoken playback.\n- Settings > Reliability includes the Battery Optimization Guide for phones with aggressive background restrictions.\n- Keep notifications, exact alarms, and battery settings enabled for the best reminder reliability."
                )
                HelpSection(
                    title = "Widgets and Wear OS",
                    content = "- Quick Reminder widget opens Add Reminder directly.\n- Upcoming widget shows upcoming reminders and missed reminders.\n- Wear OS support depends on phone notifications and watch sync being enabled."
                )
                HelpSection(
                    title = "After device restart",
                    content = "- Future reminders are rescheduled automatically after restart.\n- Past-due reminders go through the missed reminder flow instead of firing unexpectedly on boot."
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
