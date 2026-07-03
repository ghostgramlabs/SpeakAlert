package com.ghostgramlabs.speakalert.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * In-app language selection backed by AppCompat per-app locales (works on API 26+; uses the
 * platform LocaleManager on Android 13+). Selecting a language recreates activities automatically
 * and the choice is persisted by AppCompat.
 *
 * Language display names are intentionally shown in their own script so they're recognizable
 * regardless of the current UI language.
 */
object AppLocale {

    /** (tag, native display name). Empty tag = follow the system language. */
    val supported: List<Pair<String, String>> = listOf(
        "" to "System",
        "en" to "English",
        "es" to "Español",
        "hi" to "हिन्दी",
        "ar" to "العربية"
    )

    /** Current selected primary language tag, or "" when following the system. */
    fun currentTag(): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isBlank()) return ""
        return tags.substringBefore(',').substringBefore('-')
    }

    fun set(tag: String) {
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /**
     * Applies the persisted app language to an activity's base context. Android 13+ does this
     * automatically; this makes the in-app picker also work (including RTL) on Android 12 and
     * below, where a plain ComponentActivity would otherwise ignore it. Call from
     * [android.app.Activity.attachBaseContext].
     */
    fun wrapContext(base: Context): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return base
        val locale = locales[0] ?: return base
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
