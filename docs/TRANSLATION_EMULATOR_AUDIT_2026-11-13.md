# Translation emulator audit — 2026-11-13

## Environment and scope

- App: SpeakAlert 2.0.46, debug build from the current working tree.
- Device: emulator-5554, Android 16 / API 36, x86_64, 1080 × 2340, font scale 1.0.
- Languages: German (`de`), French (`fr`), Indonesian (`id`, Android resources `values-in`).
- Built and installed both the app and instrumentation APK. Used the installed Gradle 8.11.1 distribution offline.
- This checks the changed localization flows on one emulator, not every translated sentence, screen size, Android version, or physical-device audio behavior.

## Results

The four checks in `TranslationEmulatorAuditTest` passed in each language (12 checks):

1. Settings, the three new language picker entries, voice-enhancement settings, and opening Help.
2. Custom recurrence intervals of 1 and 2, and localized end-date text.
3. Platform date patterns place the day before the month; app resources and default locale match; translated status and plural resources resolve.
4. Upcoming reminder cards, details, inline date picker, and the shared date/time dialog.

The existing `Release246AuditTest` passed all six checks in German: monthly last-Thursday persistence, loop-pause/card-color settings, three notification-cleanup cases, and playback-loop stop behavior.

The JVM suites passed: `AppLocaleTest` (7), `DateUtilsTest` (9), and `TranslationParityTest` (5), totaling 21 tests.

## Issues found and fixed during visual review

- The Home card used a separate lowercase conversion, producing German `Intervall: 2 tage` even though the editor and details were correct. Preserved German noun capitalization in `Components.kt` and added a card-level assertion for `Intervall: 2 Tage`.
- The localized full date made the Home card's end-date chip truncate the time. Allowed two lines and added text-layout checks for hidden/ellipsized text. The German screenshot now shows the complete date and time.

After these fixes, the complete German translation suite passed again. The affected card/details/picker test also passed again in French and Indonesian, including the assertions that the end-date chip does not truncate its text. The emulator was returned to its original German locale with the updated app open.

## Evidence and reproduction

Logs (generated build artifacts):

- `build/translation-audit-de.txt`: original 10-test German translation/release run.
- `build/translation-audit-fr.txt`, `build/translation-audit-id.txt`: four translation checks per language.
- `build/translation-audit-de-final.txt`: four German checks after the card fixes.
- `build/translation-audit-fr-final.txt`, `build/translation-audit-id-final.txt`: affected-screen rechecks after the card fixes.
- `build/translation-audit-screens/files/translation_<locale>_<screen>.png`: screenshots of settings, language picker, recording settings, Help, editor, custom intervals, end date, card, details, and date pickers. Screenshots of scrolling screens show the captured viewport, not the entire page.

Run the audit after building and installing both debug APKs:

```powershell
adb -s emulator-5554 shell cmd locale set-app-locales com.ghostgramlabs.speakalert --locales de
adb -s emulator-5554 shell am instrument -w -e class com.ghostgramlabs.speakalert.TranslationEmulatorAuditTest com.ghostgramlabs.speakalert.test/androidx.test.runner.AndroidJUnitRunner
```

Repeat with `fr` and `id`. The tests restore their settings and remove their temporary reminder. The app locale is controlled outside the tests and should be restored after the run (original locale here: `de`).
