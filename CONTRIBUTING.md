# Contributing to SpeakAlert

Thanks for helping improve SpeakAlert. Bug reports, translations, and code changes are all
welcome. Everyone taking part is expected to follow the [code of conduct](CODE_OF_CONDUCT.md).

## Reporting a bug

Open an issue with:

- What you did, what you expected, and what happened instead
- Your phone model, Android version, and SpeakAlert version (Android Settings > Apps > SpeakAlert)
- Whether the reminder was voice, text-to-speech, or tone-only, and whether it repeats

Missed or late alarms are often caused by the phone's battery manager. Mention whether
SpeakAlert is exempt from battery optimisation on your device.

## Suggesting a feature

Open an issue describing the problem you want solved before writing code, so we can agree
on the approach first.

## Translations

Strings live in `app/src/main/res/values/strings.xml` (English) and
`app/src/main/res/values-<language>/strings.xml`. To add a language, copy the English file
into a new `values-<code>` folder and translate the values, keeping the `name` attributes and
any `%1$s`-style placeholders unchanged. To fix an existing translation, edit that file
directly.

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Keep each pull request to one change, and explain what it does and why.
3. Match the style of the surrounding code (Kotlin, Jetpack Compose, MVVM).
4. Run the unit tests and make sure they pass:

   ```bash
   ./gradlew testDebugUnitTest
   ```

5. Add or update tests when you change scheduling, recurrence, or other logic.
6. For changes to alarms, notifications, or playback, test on a real device and say which
   device and Android version you used.
7. Add a line under "Unreleased" in [CHANGELOG.md](CHANGELOG.md) for anything users will
   notice, and update [ARCHITECTURE.md](ARCHITECTURE.md) or the README if you change how the app
   is structured, its permissions, or its build steps.

CI runs the unit tests and a debug build on every pull request. Read
[ARCHITECTURE.md](ARCHITECTURE.md) first if you're new to the code; it walks through how a
reminder is scheduled, fired and played.

Do not commit build output, keystores, `local.properties`, or IDE settings; `.gitignore`
already excludes them.

## License of contributions

By submitting a pull request, you agree that your contribution is licensed under the
[Apache License 2.0](LICENSE), the same license as the project.
