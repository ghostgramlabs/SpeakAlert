package com.ghostgramlabs.speakalert.util

import android.app.Activity
import android.content.Context
import com.ghostgramlabs.speakalert.R

/**
 * Synchronous mirror of the DataStore theme setting. Activities need the right window theme
 * before the first frame is drawn, and DataStore reads are async — reading this mirror in
 * onCreate lets a forced Light/Dark theme apply without a wrong-color flash on cold start.
 */
object ThemePrefs {

    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    private const val PREFS = "theme_prefs"
    private const val KEY_MODE = "theme_mode"

    /** Call whenever the user changes the theme setting. */
    fun cache(context: Context, mode: Int) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MODE, mode).apply()
    }

    fun cached(context: Context): Int = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, MODE_SYSTEM)
    }.getOrDefault(MODE_SYSTEM)

    /**
     * Applies the forced Light/Dark window theme, if any. Must run in onCreate before setContent.
     * When following the system, the manifest theme (with its values-night variant) already
     * matches, so nothing needs to change.
     */
    fun applyWindowTheme(activity: Activity) {
        when (cached(activity)) {
            MODE_LIGHT -> activity.setTheme(R.style.Theme_SpeakAlert_Light)
            MODE_DARK -> activity.setTheme(R.style.Theme_SpeakAlert_Dark)
        }
    }
}
