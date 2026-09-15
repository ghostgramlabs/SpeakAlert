# Reminder reliability verification

Run automated checks on the candidate build:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

Unit tests cover application decisions and mocked Android calls. They do not verify audible
playback, device power management, or real speech engines. The checks below remain a release
gate on physical devices; record the phone model, Android version, and app version for each run.

## Physical-device checks

Use both a recorded-only reminder and a text-only reminder. Start with autoplay enabled,
audible alarm volume, Quiet Time off, and the app's requested notification/alarm permissions.

| Scenario | Expected result |
| --- | --- |
| Normal alert, app closed and screen locked | Notification and voice at the scheduled time, subject to playback settings. |
| Upcoming → three-dot menu → mark one occurrence Done, twice | Each selected occurrence is skipped. The next regular occurrence still speaks. Use an actual future-day reminder to exercise Upcoming. |
| Complete a one-time reminder, then restore/reschedule it | The restored reminder retains its original audio/text and speaks. |
| Mark an occurrence Done while a follow-up is pending | The old follow-up does not fire. The next regular occurrence still works. |
| Replace looping playback with a non-looping reminder before its timeout | The old timeout does not stop the new reminder. |
| Change the loop timeout to Never, then start replacement looping playback | The previous timer does not stop the replacement. Stop it manually after verification. |
| Speak reminder A, then reminder B; use Snooze on B's playback notification | B is snoozed, not A. |
| Speech engine unavailable; play a recorded reminder | Speech initialization failure does not stop the recording. |
| Quiet Time, call in progress, lock-screen playback restriction, private route | Playback follows the selected settings; verify speaker, Bluetooth, and earpiece separately. |
| Restart the phone before a reminder and leave it locked | Rescheduling works. Check the existing missed-reminder and boot playback policies separately from normal on-time playback. |

Short recurrence intervals are useful for iteration. Also run daily reminders overnight and
after extended idle time. If possible, include the reporting customer's phone model and the
oldest and newest supported Android versions.

## When a notification appears without voice

Record its exact notification text and whether Play/Replay works. Record the scheduled and
actual delivery times, audio source type, playback settings, and connected audio devices.
Use a debug build with Settings → Developer → Debug logging enabled to collect app logs, or
capture Android logcat. The standard release build does not expose that debug control.

Distinguish a rejected service launch from a launch accepted by Android followed by a decoder,
speech-engine, routing, or volume problem. Launch acceptance alone is not proof of sound.

## First-run guide

- Fresh home launch: Quick start appears before notification permission and battery prompts.
- Upgrade: existing users receive release notes, not the first-run guide.
- Follow all five steps, use Back, rotate the phone, and verify the current step is retained.
- On the final step, Review Settings opens Settings from the first-run guide and Home Help;
  from Settings Help, it closes the guide and reveals Settings without duplicating the screen.
- Increase system font size and verify that the page body scrolls and navigation stays reachable.
- Close the guide, reopen the app, and verify it does not automatically repeat on that version.
- Open Help from Home and App guide from Settings; Quick start can be replayed from either.
- Verify English, Spanish, Hindi, and Arabic, including right-to-left navigation placement.
- Launch from a notification or widget: skip the introduction, preserve the requested destination,
  and keep notification permission available when needed.
- Use the guide to create and hear a real two-minute reminder before relying on daily reminders.

## Review request timing

- Automatic requests require normal reminder notifications delivered on three distinct UTC days,
  at least seven days since the first tracked delivery, and ten minutes since the latest one.
  Usage starts tracking with this update; old app-open counts do not qualify anyone.
- Upcoming > Mark done, failed notifications, snoozes, and follow-up checks must not add usage days.
- A qualifying plain Home launch can show the honest review request. Notification/widget launches,
  onboarding/release-notes sessions, active notifications, missed-reminder recovery, and full-screen
  permission recovery must not be interrupted. Check Android notification permission prompts too.
- Maybe later, swipe-down, and back dismissal all wait at least 60 days before another request.
- No thanks and Rate now both stop automatic requests, including decisions saved before this update.
  Rate now opens the store; this does not establish that a review was submitted.
- Settings > Rate remains available whenever the user chooses to review.
- Automated policy and DataStore tests exercise age, distinct days, clock rollback, cooldown,
  concurrent claims, and persisted opt-out. Real-device presentation still needs manual verification.
- Review and feature requests are available to everyone without a satisfaction question.
- Suggest a feature opens an editable draft to ghostgramlabs@gmail.com with a feature-request
  subject. Settings > Feature requests & support opens the general support draft.
- Without an email app, show the fallback message and leave the selectable address accessible.
- Verify email address order in Arabic and scroll access to all review-sheet actions with large text.

## Reported device check for 2.0.35 (55)

The app owner reported that one reminder repeating every two minutes worked correctly.
This confirms that basic recurring-reminder scenario only; the specific Upcoming > Mark done
reproduction and the remaining device checklist above are not confirmed by that report.

## Additional regression review

- Confirmed and corrected a 2.0.35 regression: the completed-reminder guard rejected an
  explicit snooze of the final occurrence of a finite repeating schedule. A targeted test
  failed before the fix; the guard now permits only the matching persisted snooze time.
  Done still clears that snooze and blocks its queued broadcast.
- Introduction selection now waits for preferences to load after activity recreation, so an
  open What's New sheet cannot briefly turn into Quick Start during restoration.
- Device checks: snooze the last occurrence of a count-limited reminder; then mark it Done.
  Rotate while What's New is open and confirm that its content stays consistent.

## Missed-entry, clock-format, and playback replacement fixes

- Reschedule a missed one-time reminder, then dismiss its old Missed entry: the new reminder
  remains scheduled. Repeat with an already-advanced recurring reminder and an active snooze.
- Dismiss an actually missed snooze: the regular recurring alarm is restored, and its old
  snooze/follow-up alarms are cancelled.
- Save a clock format opposite to the phone setting, stop the app process, and launch again:
  the first displayed clock uses the saved format. Existing DataStore-only preferences migrate
  once into the startup mirror; migration has a 500 ms bound to protect service startup.
- Without an explicit app clock preference, change the phone clock format while the app is alive:
  visible times, the Settings switch, and widgets update. An explicit app preference wins.
- Start recorded playback, then a spoken-text reminder: recorded playback stops. Start another
  reminder before the old speech completion/error callback arrives: the newer playback continues.
- These changes retain version 2.0.35 (55). Rebuilt artifacts replace the earlier local artifacts.

## Unnamed reminder title preference

- Settings > Appearance > Unnamed reminder title offers Creation time, Reminder type, and No title.
- Upgrade an existing installation: Creation time preserves the previous Home/Details fallback.
- Fresh install: Reminder type is selected; finishing Quick Start and later upgrades retain it.
- Choose No title: an audio-only reminder has no generated title line in Home, Details, or its
  actions sheet. Its time, schedule, playback controls, and user-entered labels/messages remain.
- Switch choices and restart: the selected option persists. Check English, Spanish, Hindi,
  Arabic, and large text. This preference does not modify stored reminder data or alarm behavior.
