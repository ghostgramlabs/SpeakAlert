# Changelog

User-facing changes to SpeakAlert, newest first. Version numbers match the Google Play release
(`versionName` in `app/build.gradle.kts`).

## 2.0.42 - 2026-10-21

- New "Extra-reliable reminders" option in Settings > Reliability and the Battery Optimization
  Guide. Off by default; for phones whose battery saver holds reminders back until the app is
  opened. Reminders are then set like alarms, so some phones show a clock icon or an "alarm
  set" message; Home and Settings say when it is on and which reminder it applies to next.
- The Battery Optimization Guide has steps for Infinix, Tecno and itel phones.

## 2.0.41 - 2026-10-20

- SpeakAlert is now open source under the Apache License 2.0.
- The full-screen alert shows the reminder itself as the large, coloured headline; the app name
  is now a small label above it.
- Edit, Done and Delete buttons at the bottom of each reminder card. Deleting asks first and can
  still be undone.
- Reminder cards are more compact: "One-time" and "Daily" are no longer repeated in a separate
  box.
- The battery notice on Home is calmer and can be dismissed with "Keep current setting".
  Settings still shows whether background battery access is restricted.
- Fixed: the alert tone or spoken reminder kept playing after the reminder was marked done,
  deleted or rescheduled.
- Fixed: marking a repeating reminder done while it was ringing or snoozed also skipped its
  next occurrence.
- The Today tab says "Nothing due today" instead of "No reminders yet" when reminders are
  scheduled for later.

## 2.0.40 - 2026-10-06

- Eight new languages: Portuguese (Brazil), Russian, Vietnamese, Bengali, Telugu, Tamil, Greek
  and Malayalam, for 12 in total. Buttons now grow to fit longer translations.
- Reschedule a missed reminder from the Missed tab.
- Fixed: sound kept playing after the notification was swiped away.
- Fixed: a reminder played by itself after its time was changed.
- Fixed: a voice note kept playing while it was being edited.
- Fixed: backing up with no reminders produced an empty file.
- Fixed: wrapped chips in the Custom repeat sheet.

## 2.0.39 - 2026-09-30

- Refreshed app icon.
- Edit a reminder's date and time directly from its details screen.
- Voice reminders save immediately instead of waiting for voice enhancement.
- A reminder's notification clears once it is handled in the app.
- Back closes bottom sheets on Android 16.
- Sheets and dialogs are opaque, so the screen behind no longer shows through.
- Fixed: the notification permission prompt replaced the battery-optimisation request on first
  run.
- Fixed: a crash on devices with no file or ringtone picker; the app now explains instead.
- Fixed: freezes caused by the speech engine running on the main thread.
- Fixed: a crash in the time picker.

## 2.0.37 - 2026-09-17

- Home explains why reminders are not firing, and lets you pause reminders for a while.
- The Missed list no longer grows forever.
- Finished translations that still contained English text.

## 2.0.36 - 2026-09-16

- The full-screen alert reliably reaches the lock screen, with a rebuilt design.
- You can save while still recording, and voice normalisation no longer amplifies background
  noise.
- Configurable title for reminders saved without a name.
- 24-hour time setting that applies across the app and widgets.
- Dismissing a missed entry also resolves its reminder.
- Fixed several freezes in widgets and recording previews.

## 2.0.35 - 2026-09-10

- Reminder reliability and first-run onboarding improvements.
- Fixed silent voice recordings.

## 2.0.34 - 2026-08-13

- Android 16 support.

## Earlier versions

Releases before 2.0.34 are recorded in the git history.
