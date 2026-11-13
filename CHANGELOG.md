# Changelog

User-facing changes to SpeakAlert, newest first. Version numbers match the Google Play release
(`versionName` in `app/build.gradle.kts`).

## Unreleased

- New "Week starts on" choice in Settings, Language & appearance: System, Mon, Sun or Sat. A
  chosen day sets the first column of the date picker. Material's date picker cannot be told
  its first weekday, so with a day chosen the app shows the phone's own date picker instead;
  "System" keeps the Material picker and the region's week start.
- The phone's own date picker, used for a chosen week start and for languages with their own
  digits, now uses the app's color in light and dark theme instead of the platform teal.
- Fixed the date picker opening on the wrong day in the evening in time zones west of UTC (and
  early morning east of it) on the reminder details screen, Home's reschedule, and the combined
  date and time dialog. On details and Home it also refused today's date at those hours.
- New "Allow snooze" switch in Settings, Snooze & follow-up, on by default. Turned off, reminder
  notifications, the speaking notification and the lock-screen alert no longer offer Snooze, so
  Done is the only way past a reminder, and the snooze length setting is hidden. Asked for by a
  user who kept snoozing instead of doing the task.

## 2.0.46 - 2026-10-31

- The "What's new" sheet lists the four changes below that users asked for.
- Fixed a crash on some phones (seen on an Oppo with Android 10) when a reminder stopped while
  the phone's speech engine was still starting up. The engine now connects only for spoken
  reminders, is never shut down mid-connect, and a spoken reminder whose engine never starts
  gives up after 20 seconds instead of waiting silently.
- Choose your own colors for the Done and Delete (Stop recurring) buttons on Home's reminder
  cards: Settings, Language & appearance, Reminder card buttons. Eight colors plus the app
  default, each with a readable shade in dark theme, and a preview as you pick.
- A reminder's notification now clears when you open the app after it rings, and when you edit
  and save the reminder. Before, it only cleared when you tapped the notification itself. One that
  is still sounding stays, so its Silence and Stop buttons remain. With "Keep notification until
  done" on, opening the app leaves it as before; saving an edit still clears it, since the edit
  replaces the time it was about.
- Monthly repeats can now fall on a weekday by its place in the month: the first, second, third,
  fourth or last Monday to Sunday, such as "Last Thursday of each month". Choose Repeat, then
  Monthly, then "Day of week". It starts from the reminder's own date (Saturday the 31st offers
  the last Saturday), and the reminder card shows the rule.
- New "Pause between repeats" setting, under "Stop continuous playback after". A looping
  reminder can now wait 5, 10 or 30 seconds, or a custom time up to 10 minutes, before it plays
  again, so a short message like "Meeting" becomes a periodic nudge instead of nonstop speech.
  The stop time still ends the loop. The default stays back-to-back; tones are not affected.

## 2.0.45 - 2026-10-24

- The "What's new" sheet describes what changed since 2.0.40, the last production release:
  Reminder check, Extra-reliable reminders, easier Settings, card buttons, and sound that stops
  when it should.
- Settings is easier to search. Every collapsed section now shows a short line saying what is
  inside, such as "App language, theme, time format", so a setting can be found without opening
  each card.
- The app language moved up, right below Reminder check, in a section renamed "Language &
  appearance" with a globe icon that is recognizable in any language. Language comes before the
  theme inside it.
- The battery prompt's buttons now say what they do: "Allow background use" (it opens Android's
  "allow in background" question) and "No thanks". The text matches the Allow choice on that
  screen and says where to change it later. In Settings, the battery line uses the same "Fix"
  button as the other checks, and the guide link reads "Reminders still late? Steps for your
  phone".
- In Reminder check, a long button label (such as "Open settings" in Malayalam) now sits under
  its line instead of squeezing the text to a word per line.
- A repeating reminder that rings late while the phone is on (battery saver, doze, a clock
  change) now also appears in the Missed tab, as it already did after a restart. Before, it only
  showed a "Missed" notification, and swiping that away lost it.
- A reminder's details now show the missed-reminder handling that actually applies. Some showed
  "Remind only at the next exact time" while the app alerted on resume.
- A repeating reminder that ends after a number of times no longer shows "0 occurrences" once
  it has finished.
- A tone-only alarm offers Silence again: first in the notification, and as "Silence now" on the
  lock-screen alert. Before, a reminder with typed text showed Replay instead, and the alert
  screen showed "Play again" while the tone was still ringing.
- The lock-screen alert also shows "Silence now" when the voice started playing a moment before
  the screen appeared; it used to miss that and offer "Play again".
- A snoozed reminder's alert shows "Snoozed" with the time it is ringing, instead of the original
  time, which made it look late.
- Tapping the "Loop playback" label in the reminder form now turns it on; only the small switch
  did. The same fix applies to Extra-reliable reminders in the battery guide.
- The audio playback notification has one Stop button (it had two, one always in English) and no
  longer shows Previous and Next.
- Widgets: each has its own name and a real preview in the widget picker, the Quick add button's
  plus sign matches the button, and "Missed", "Today" and "Tomorrow" in the reminders widget
  follow the app language. "Tomorrow" is also correct across New Year.
- Help, Quick start and What's new text no longer point to the old section names (Playback,
  Reliability, Appearance). Updated copy is translated across all 12 supported languages.

## 2.0.44 - 2026-10-22

- Settings is regrouped so it is easier to follow. A new "Reminder check" at the top lists, in
  plain words, everything that decides whether reminders ring on time: notifications, exact
  alarms, battery, alarm volume, and lock-screen and Do Not Disturb access when those are in
  use. Required permissions are separate from recommendations and optional features, so
  battery restrictions alone do not show a red failure summary. "When a reminder rings"
  starts with a sound-mode choice and shows the relevant controls while preserving preferences.
  Continuous playback duration and persistent notifications have clearer labels; less-used
  sections start collapsed. Updated copy is translated across all 12 supported languages.
- The alarm-volume line appears only when the volume is low, with a Raise button and how to turn
  it up in the phone's sound settings.
- The battery notice says the same thing everywhere: "Reminders may be late or missed" on Home
  and at first launch, "Battery saver may hold reminders back" in Settings.
- Closing the first-launch battery prompt by tapping outside it no longer counts as an answer;
  it is asked again on the next launch.

## 2.0.43 - 2026-10-22

- Public release of 2.0.42, which went to closed testing only. No further changes.

## 2.0.42 - 2026-10-21 (closed testing)

- New "Extra-reliable reminders" option in Settings > Reliability and the Battery Optimization
  Guide. Off by default; for phones whose battery saver holds reminders back until the app is
  opened. Reminders are then set like alarms, so some phones show a clock icon or an "alarm
  set" message; Home and Settings say when it is on and which reminder it applies to next.
- The Battery Optimization Guide has steps for Infinix, Tecno and itel phones.
- Settings shows the phone's alarm volume, which reminders play at, and warns when it is low,
  with a button to raise it.

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
