# SpeakAlert - Offline Voice Reminders

An offline-first Android application for creating and scheduling voice reminders. Built with **Jetpack Compose**, **Room**, **AlarmManager**, and **Clean Architecture** (MVVM).

## Features

- **Voice-First Creation**: Quickly record audio reminders.
- **Scheduled Alarms**: Exact notifications using `AlarmManager`.
- **Recurrence**: Support for Daily, Weekly, Monthly, and Custom intervals.
- **Offline Storage**: All data (database + audio files) stored locally.
- **Playback**: Listen to your voice notes directly from the app.
- **Clean UI**: Material 3 Design with Dark Mode support.

## Architecture

The app follows MVVM with Clean Architecture:

- **Data Layer**: `Room` for persistence, `AudioRecorder` for file handling.
- **Domain Layer**: `RecurrenceUtils` for complex scheduling logic.
- **UI Layer**: Jetpack Compose Screens and ViewModels.
- **Alarm Layer**: BroadcastReceivers for reliable alarm handling and boot persistence.

## Project Structure

- `data`: Room Database, Entities, and Repository.
- `domain`: Recurrence logic and models.
- `ui`: Compose screens (Home, Add/Edit, Details, Settings).
- `alarm`: Scheduler, Receivers, NotificationHelper.
- `audio`: Wrappers for `MediaRecorder` and `MediaPlayer`.

## Permissions

The app requests the following permissions:
- **Record Audio**: To record voice notes.
- **Post Notifications** (Android 13+): To show reminder alerts.
- **Schedule Exact Alarms** (Android 12+): To ensure reminders fire at the precise time.

## Setup & Running

1.  Open the project in **Android Studio**.
2.  Sync Gradle with project files.
3.  Run on an emulator or physical device (Min SDK 26).

## Notes

- **Exact Alarms**: On Android 12+, if permission is revoked, the app gracefully falls back or prompts the user in Settings.
- **Recurrence**: Recurrence logic handles complex rules like "Every Monday and Friday" or "Every 3 Days".
- **Audio**: Stored in `context.filesDir/reminders/` as `.m4a` files.

## Tests

Unit tests are included for `RecurrenceUtils` to verify scheduling logic (Daily, Weekly, Monthly handling).
Run tests via: `./gradlew test`
