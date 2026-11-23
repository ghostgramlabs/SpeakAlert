# Emulator functional audit — 2026-10-24

Tested SpeakAlert **2.0.45 (66), debug**, on **Pixel 5 AVD, Android 16 / API 36, x86_64**. The emulator initially had 2.0.44; the current checkout was built and installed before the functional checks. Production source was not changed.

## Confirmed bugs

### 1. Existing reminder edits disappear when the activity is recreated — high

**Reproduction:** Open an existing reminder from Home, tap Edit, change its message without saving, then recreate the activity (for example, a configuration change). The entered message is replaced with the saved message. Also reproduced when entering from a reminder-ID intent, as used by notifications.

**Expected:** Unsaved edits remain in the editor through activity recreation.

**Evidence:** `homeEditDraftSurvivesActivityRecreation` and `editDraftSurvivesActivityRecreation` both verify the new message exists before recreation and fail to find it afterward. These use `ActivityScenario.recreate()`, not process death.

**Cause:** `AddEditReminderScreen.kt:104` runs `LaunchedEffect(reminderId)` again on recreation. `AddEditViewModel.loadReminder()` at line 180 unconditionally reloads the database row into the retained editor state through `setSavedDraft()`.

**Suggested fix:** Initialize a particular editor only once; do not reload an already-loaded reminder over a dirty draft. Separately consider saved-state support for process death.

### 2. New drafts lose their unsaved-change protection after recreation — high

**Reproduction:** Home → Add Reminder → type a message → recreate the activity → tap the toolbar Back arrow. The editor exits without the “Save reminder?” prompt. In the Home-path test, the text is verified to still exist immediately before pressing Back. The quick-add intent path also fails the prompt check.

**Expected:** Back offers Save / Keep editing / Discard whenever an unsaved draft exists.

**Evidence:** `homeNewDraftRemainsDirtyAfterActivityRecreation` and `newDraftRemainsDirtyAfterActivityRecreation` fail because the confirmation is missing. The ordinary edit-and-discard flow without recreation passes.

**Cause:** `applyDefaultsForNewReminder()` (`AddEditViewModel.kt:170`) runs again and calls `setSavedDraft()` on the current draft. That makes unsaved text the new clean baseline. The quick-add navigation effect can additionally rerun on recreation.

**Suggested fix:** Apply defaults only to a genuinely uninitialized draft; preserve its original baseline and dirty state through recreation.

### 3. Saving an edit overwrites the original creation time — medium

**Reproduction:** Open a previously created reminder, change only its message, and save. Inspect the stored `createdAt`: it now contains the edit time, not the original creation time.

**Expected:** Creation time remains unchanged when editing a reminder.

**Evidence:** `editRetainsOriginalCreationTimestamp` seeds a reminder created a day earlier, edits it through the UI, waits for the database update, and fails its original-timestamp assertion. One run changed `1792769373998` to `1792855775387`.

**Cause:** `saveReminder()` constructs a new `ReminderEntity` at `AddEditViewModel.kt:640` without supplying `createdAt`; the entity defaults it to `System.currentTimeMillis()` (`ReminderEntity.kt:16`).

**Impact:** Original creation metadata is lost; audio-only reminders using the Creation time fallback title can change their generated title after editing.

**Suggested fix:** Preserve immutable metadata from the existing row. Review other omitted fields individually; schedule-reset fields should not automatically be treated as bugs.

## Coverage and evidence

- **JVM:** 239 tests passed, zero failures/errors/skips in the fresh successful run. This covers recurrence calculations, missed reminders, rescheduling, recording validation, playback decisions, settings, localization helpers, and backup logic at the unit level.
- **Lint:** zero errors, 219 warnings, 9 informational findings. Warnings are not all functional defects.
- **Emulator:** **30 tests run: 25 passed, 5 failed**, with all five failures reproducing the three bugs above (two entry paths each for the two draft bugs). All 15 pre-existing tests passed in the final consolidated run. Existing tests plus 15 added audit tests exercise real Room persistence, Compose navigation, Android media players, and AlarmManager delivery. See `build/qa-audit/final-emulator-suite.txt`.

| Function | Observed result / scope |
| --- | --- |
| Create typed reminder | UI save persists a future, incomplete reminder |
| Edit and save | Content persists; creation-time bug above |
| Discard edits | Stored reminder remains unchanged |
| Unsaved drafts / recreation | Bugs 1 and 2 above |
| Daily, weekly, monthly, yearly, custom repeats | Each selected through UI and saved; next occurrence calculation succeeds. Weekly uses Monday; monthly uses Last Day |
| Scheduled delivery | Actual alarm scheduled seven seconds ahead posts a notification and updates `lastFiredAt` |
| Recorded, imported, and TTS playback | Existing emulator tests observe active Android alarm audio players and verify Done stops playback and persists completion |
| Tone playback | Done, Snooze, delete, reschedule, missed-reminder recovery, and recurring dismissal stop the relevant tone; resolving another reminder preserves the active tone |
| Delete confirmation | Row survives Cancel; original test initially failed but passed on targeted rerun. The audit check waits for the dialog explicitly |
| Settings playback modes | Tone/voice mode controls and preference preservation exercised |
| Full-screen alert presentation | Message prominence checked by existing emulator test |
| Backup / restore | Text, schedule, looping, follow-up interval, and audio bytes survive a ZIP round trip; duplicate import skipped. Uses a local URI and binary audio fixture, not the Android document-picker UI |

## Test environment notes

The first Gradle invocation reused previously passing JVM results. A forced fresh run exposed a **host Mockito/Byte Buddy dynamic-agent attachment failure**, producing 115 setup-related failures. Loading the already-installed Byte Buddy agent at JVM startup resolved it; all 239 tests then passed. The temporary local Gradle init script is `build/qa-audit/test-agent.init.gradle`; no application/build configuration was changed for this workaround.

The Android test runner's initial managed run removed its installed app afterward; the current debug app was reinstalled for subsequent checks. Test-created QA reminder rows are cleaned up. The added tests snapshot and restore their changed DataStore settings. Existing tests can retain their own onboarding/battery guidance choices.

Added reproduction tests: `app/src/androidTest/java/com/ghostgramlabs/speakalert/EmulatorFunctionalAuditTest.kt`. Assertions describe the correct expected behavior, so the confirmed-bug cases intentionally remain failing until fixed.

Useful local artifacts:

- `build/qa-audit/final-emulator-suite.txt`
- `build/qa-audit/functional-audit.txt`
- `build/qa-audit/targeted-recheck.txt`
- `build/qa-audit/home-edit-recheck.txt`
- `build/qa-audit/unit-tests-with-agent.txt`
- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/lint-results-debug.html`

## Limits

This is a broad functional audit, not a claim that every feature, state combination, or supported Android version was verified. No physical-device sound-quality check was performed. Wear OS pairing, Bluetooth/earpiece routing, incoming-call behavior, DND access, OEM battery restrictions, long idle/overnight delivery, reboot recovery, widget placement/refresh, all language layouts, large-font accessibility, every recurrence end-rule UI, permission-denial permutations, and document-picker import/export still need dedicated device/UI checks. Existing unit coverage does not replace those checks.
