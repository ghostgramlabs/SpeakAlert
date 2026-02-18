# SpeakAlert - Offline Voice Reminders

SpeakAlert is an offline-first Android reminder app for voice and text alerts. It is built with Jetpack Compose, Room, AlarmManager, and MVVM Clean Architecture.

## App Usage Guide

### 1. Create a reminder
1. Tap the floating `Mic` button on Home.
2. Record a voice reminder, or type reminder text.
3. Add an optional title.
4. Pick date/time.
5. Choose repeat mode:
   - `Does not repeat`
   - `Daily`
   - `Weekly`
   - `Monthly`
   - `Custom` (for example: every 2 days, every 3 hours)
6. Save.

### 1A. One-time reminder completion
- A one-time reminder is not completed just because it fired.
- It stays active until you mark it `Done` or `Dismiss`.
- `Snooze` keeps it active and reschedules it to the snooze time.

### 2. Understand Home tabs
- `Today`: reminders scheduled for today (including overdue items for today).
- `Upcoming`: future reminders after today.
- `Missed`: reminders not delivered at the scheduled moment (for example device off, quiet hours, or late handling).
- `Done`: completed reminders.

### 3. Use reminder cards
- Tap a card to open details.
- Use `...` for actions such as edit, mark done, stop recurring, or delete.
- Use the inline `Play reminder` / `Stop playback` button on cards that can play audio or TTS.

### 4. Missed reminders workflow
- Open `Missed` tab to review missed items.
- Tap `Play` to fire now.
- Tap `Stop` to stop current playback.
- Use `Dismiss` on a single item or `Dismiss All` for bulk clear.

### 5. Recurring reminders behavior
- The selected date/time is the first occurrence start point.
- Recurring reminders automatically compute and schedule the next occurrence after each trigger.
- `Custom` intervals support minutes, hours, days, weeks, and months.
- End rules are supported:
  - Never
  - Until date
  - After number of occurrences
- Marking a recurring reminder as done clears only the current occurrence, then continues to next schedule.

### 6. Playback and alert controls
In `Settings > Playback`:
- `Auto-play audio`
- `Only when unlocked`
- `Text-to-Speech`
- `Tone-only mode` (alarm tone at fire time, manual Play Voice / Play TTS still available)
- `Volume`
- `Loop duration` (including infinite)
- `Default snooze`
- `Quiet hours`

Tone-only notes:
- Tone-only mode still follows quiet hours.
- In tone-only mode, volume is controlled by system alarm/notification volume.

### 6A. Tone-only mode (more reliable alerts)
On some phones, voice or text-to-speech reminders may not always play reliably due to system settings or battery optimizations.

To reduce missed audio alerts, enable `Tone-only mode`.

What it does:
- Plays a clear alarm tone instead of voice/TTS.
- Starts quickly and works more consistently across devices.

Good to know:
- You can still tap `Play Voice` or `Play TTS` from the notification.
- Tone stops on `Dismiss`, `Snooze`, `Play Voice`, `Play TTS`, or loop timeout.

When to use this:
- If you notice delays in voice playback.
- If reminders are sometimes silent.
- If you prefer a simple and reliable alert.

### 7. Device restart behavior
- Future reminders are rescheduled automatically after reboot.
- Past-due reminders are handled as missed flow and surfaced through notification/missed list behavior.

## Key Features

- Voice-first and text reminder creation
- Exact alarm scheduling with AlarmManager
- Daily, weekly, monthly, and custom recurrence
- Missed reminder inbox
- Local-only storage (database + audio files)
- Material 3 UI

## Permissions

- `RECORD_AUDIO`: record voice reminders
- `POST_NOTIFICATIONS` (Android 13+): show reminder alerts
- `SCHEDULE_EXACT_ALARM` (Android 12+): precise reminder timing

## Developer Setup

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run on emulator/device (Min SDK 26).

## Test

Run unit tests:

```bash
./gradlew test
```
