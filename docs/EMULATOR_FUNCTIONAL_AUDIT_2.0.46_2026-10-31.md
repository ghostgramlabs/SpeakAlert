# SpeakAlert 2.0.46 functional audit — 2026-10-31

Scope: changes after 2.0.45 (`1fcaa4c`) through `eb76ae0`, version 2.0.46 (67). Tested checkout includes the pre-existing, untracked `EmulatorFunctionalAuditTest.kt`. No production code changes were made during this audit.

Environment: Pixel 5 AVD, Android 16 / API 36, x86_64. Existing 2.0.45 emulator installation upgraded in place. Debug build; release signing/minification and physical OEM behavior are outside this run.

## Result

**Not fully passing: three confirmed defects remain.** All six new release-specific emulator checks pass. The three defects were documented in the previous 2.0.45 audit and reproduced again on 2.0.46; they are not established as new regressions introduced by this release.

| Check | Result |
| --- | --- |
| Debug app and instrumentation APK build | Passed |
| Fresh JVM suite (`--rerun-tasks`) | 257 passed; zero failures, errors, or skips |
| Full emulator suite | 36 executed: 31 passed, 5 failed |
| New 2.0.46 emulator checks | 6 passed |
| Lint | Zero errors; 238 warnings, 9 informational findings |
| Crash buffer | No SpeakAlert crash recorded; one emulator native crash-dump-helper message predates the test run |

The five failing assertions map to the three defects below. The lint warnings are not counted as confirmed functional defects. The build used the existing local Byte Buddy startup-agent init script for Mockito compatibility. Build completed successfully in 13m 40s.

## Confirmed defects

### 1. Unsaved edits to existing reminders are lost on activity recreation — high

Reproduce: Home → existing reminder → Edit → change message without saving → recreate the activity, such as through a configuration change. The edited message reverts to the saved value. Reproduces both from Home and the reminder-ID intent entry path.

Expected: The current unsaved edit survives activity recreation.

Evidence: `editDraftSurvivesActivityRecreation` and `homeEditDraftSurvivesActivityRecreation` verify the draft before recreation and fail to find it afterward.

Cause: `AddEditViewModel.kt:182` reloads the database row through `setSavedDraft()` whenever `loadReminder()` runs. The screen launches that load again after recreation, overwriting the retained draft.

Suggested correction: Initialize each editor once and preserve dirty draft state through recreation; handle process restoration separately.

### 2. New drafts lose the unsaved-change warning after recreation — high

Reproduce: Home → Add → type a message → recreate activity → toolbar Back. The editor exits without the “Save reminder?” confirmation. Also reproduced through the quick-add intent. The Home-path test confirms that the typed text remains present before Back.

Expected: Back offers Save / Keep editing / Discard while the draft has unsaved changes.

Evidence: `homeNewDraftRemainsDirtyAfterActivityRecreation` and `newDraftRemainsDirtyAfterActivityRecreation` both fail to find the confirmation. Ordinary edit/discard without recreation passes.

Cause: `AddEditViewModel.kt:172` reapplies new-reminder defaults and calls `setSavedDraft()` on the current draft, changing its clean baseline. Quick-add navigation also needs lifecycle-safe initialization.

Suggested correction: Apply defaults only to an uninitialized draft and retain the original clean baseline.

### 3. Saving an edit overwrites the original creation timestamp — medium

Reproduce: Open a reminder created earlier → change its message → Save. Its stored `createdAt` becomes the edit time.

Expected: Editing preserves the original creation timestamp.

Evidence: `editRetainsOriginalCreationTimestamp` failed with expected `1793396297426`, actual `1793482700888` (approximately one day later).

Cause: `AddEditViewModel.kt:642` constructs a new `ReminderEntity` without preserving `createdAt`; the entity supplies the current time by default.

Impact: Creation metadata is lost. Audio reminders using the creation-time fallback title can receive a different generated title after editing.

Suggested correction: Preserve immutable metadata from the existing row when saving edits.

## Functional coverage

| Function | Observed result and scope |
| --- | --- |
| Upgrade 2.0.45 → 2.0.46 | APK replacement succeeded; installed version verified as 2.0.46 / 67; app data retained |
| Monthly weekday rule (new) | UI selects last Thursday, saves DAY_OF_WEEK / -1 / Thursday, and persists a trigger on the last Thursday. JVM tests additionally cover first/second/fourth weekdays, February, month advancement, invalid rules and summaries |
| Loop-pause setting (new) | UI selects 10 seconds and persists it through activity recreation |
| Recorded playback gap (new) | Actual Android alarm player stops after a one-second WAV, stays quiet during a five-second gap, restarts, and does not restart when stopped during the next gap |
| Done/Delete colors (new) | Blue Done and red Delete selected in UI and persisted through recreation; JVM tests check color selection/fallback. Every palette/theme visual combination was not inspected |
| Clear notification on Home (new) | Finished alert clears when persistence is disabled; persistent alert remains when enabled. Test uses an occurrence older than the deliberate ten-second startup guard |
| Clear notification on edit (new) | Editing and saving clears an alert even with persistence enabled |
| Speech-engine changes | Normal TTS playback and Done-stop pass; JVM crash-guard tests pass. The specific Oppo Android 10 setup failure and never-initializing engine were not reproduced |
| Create / edit / discard / delete | Text creation, content save, ordinary discard, delete confirmation and cancellation pass; exceptions are the three defects above |
| Daily / weekly / monthly / yearly / custom | UI selections save and yield a future/next recurrence; monthly legacy test uses Last Day |
| Actual scheduled alarm | Seven-second future alarm posts a notification and records lastFiredAt |
| Tone lifecycle | Done from Home/details/notification, Snooze, delete, reschedule, missed-reminder recovery, and recurring dismissal stop the relevant tone; resolving another reminder preserves it |
| Recorded / imported / spoken audio | Real Android playback observed; Done stops it and persists completion |
| Settings | Voice/tone control visibility and preference preservation pass |
| Full-screen alert | Existing message-prominence check passes |
| Backup / restore | Text, schedule, loop flag, follow-up interval and audio bytes survive ZIP round trip; duplicate import skipped |
## Evidence

- Build, fresh JVM tests and lint: `build/qa-2.0.46/build-tests.txt`.
- Full instrumentation log: `build/qa-2.0.46/emulator-suite.txt`.
- Per-test results: `build/qa-2.0.46/emulator-results.csv`.
- Crash buffer: `build/qa-2.0.46/crash-log.txt`.
- JVM HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Lint HTML report: `app/build/reports/lint-results-debug.html`.
- Release-specific test source: `app/src/androidTest/java/com/ghostgramlabs/speakalert/Release246AuditTest.kt`.
- Existing audit source: `app/src/androidTest/java/com/ghostgramlabs/speakalert/EmulatorFunctionalAuditTest.kt`.

## Coverage limits

A passing emulator test is not a guarantee of every device/state combination. Physical Oppo Android 10 speech-engine failure, Bluetooth/earpiece routing, Wear OS, incoming calls, OEM battery restrictions, overnight doze delivery, actual reboot recovery, all locale/font/theme layouts, widget placement, Android file-picker interaction, and permission denial permutations require additional dedicated coverage. Backup tests use local URIs; playback tests inspect Android player state, not perceived speaker sound quality. Monthly rule unit tests cover more calendar combinations than the single UI example. Test assertions use correct expected behavior and remain failing when a defect reproduces.

