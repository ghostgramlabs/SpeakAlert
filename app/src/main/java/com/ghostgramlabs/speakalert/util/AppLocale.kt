package com.ghostgramlabs.speakalert.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language selection.
 *
 * The app is built entirely on Jetpack Compose with plain [android.app.ComponentActivity]s (no
 * AppCompat activities/theme), so the AppCompat per-app-locale APIs cannot be used here: they only
 * take effect for AppCompatActivity.
 *
 * On Android 13+ the platform [android.app.LocaleManager] is the single source of truth: it
 * persists the choice, applies it to every context (activities, services, receivers), updates the
 * JVM default locale, and recreates activities on change. This also keeps the picker in sync when
 * the user changes the app language from system Settings (locales_config.xml).
 *
 * On Android 12 and below we persist the chosen tag ourselves and apply it by overriding the
 * activity's base-context [Configuration] in [wrapContext].
 *
 * Language display names are intentionally shown in their own script so they're recognizable
 * regardless of the current UI language.
 */
object AppLocale {

    private const val PREFS = "app_locale"
    private const val KEY_TAG = "language_tag"

    /**
     * JVM default locale captured before any override, used to undo [Locale.setDefault] when the
     * user switches back to "System" (pre-13 only; the platform manages the default on 13+).
     */
    private val systemDefaultLocale: Locale = Locale.getDefault()

    /** (tag, native display name). Empty tag = follow the system language. */
    val supported: List<Pair<String, String>> = listOf(
        "" to "System",
        "en" to "English",
        "es" to "Español",
        "hi" to "हिन्दी",
        "ar" to "العربية"
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Current selected primary language tag, or "" when following the system. */
    fun currentTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
            val tags = localeManager?.applicationLocales?.toLanguageTags().orEmpty()
            return if (tags.isBlank()) "" else tags.substringBefore(',').substringBefore('-')
        }
        return prefs(context).getString(KEY_TAG, "").orEmpty()
    }

    /**
     * Persists [tag] and applies it. On Android 13+ the platform recreates activities itself; on
     * older versions we recreate [activity] so the new locale is applied via [wrapContext].
     */
    fun set(activity: Activity, tag: String) {
        if (tag == currentTag(activity)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = activity.getSystemService(android.app.LocaleManager::class.java)
            localeManager?.applicationLocales = if (tag.isBlank()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(tag)
            }
            return
        }

        prefs(activity).edit().putString(KEY_TAG, tag).apply()
        activity.recreate()
    }

    /**
     * Applies the persisted app language to an activity's base context (including RTL layout
     * direction). Call from [android.app.Activity.attachBaseContext]. No-op on Android 13+, where
     * the platform localizes contexts itself.
     *
     * Also syncs the JVM default locale so date/number formatting done inside the UI matches the
     * chosen language. Only appropriate on the main/UI thread — background code should use
     * [localizedContext], which does not mutate the process-wide default.
     */
    fun wrapContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = selectedLocale(base)
        if (locale == null) {
            // Back on "System": undo any earlier override so formatting follows the device again.
            Locale.setDefault(systemDefaultLocale)
            return base
        }
        Locale.setDefault(locale)
        return base.withLocale(locale)
    }

    /**
     * Returns a context whose resources resolve to the chosen app language, without touching the
     * process-wide default locale. Use this to localize text built off the UI thread (notifications,
     * services, receivers). Returns [base] unchanged when following the system language and on
     * Android 13+, where the platform localizes every context itself.
     */
    fun localizedContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = selectedLocale(base) ?: return base
        return base.withLocale(locale)
    }

    private fun selectedLocale(context: Context): Locale? {
        // Treat unreadable prefs as "follow the system" — never fail a caller over localization.
        val tag = runCatching { prefs(context).getString(KEY_TAG, "").orEmpty() }.getOrDefault("")
        return if (tag.isBlank()) null else Locale.forLanguageTag(tag)
    }

    private fun Context.withLocale(locale: Locale): Context {
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return createConfigurationContext(config)
    }

    /** Unwraps a Compose/theme [ContextWrapper] chain to the hosting [Activity], if any. */
    fun activityFrom(context: Context): Activity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
