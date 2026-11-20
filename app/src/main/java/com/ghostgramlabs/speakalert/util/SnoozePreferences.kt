package com.ghostgramlabs.speakalert.util

import android.content.Context

/**
 * Whether a ringing reminder offers Snooze ("Allow snooze"). Some people asked to turn it off
 * so the only way past a reminder is Done. Kept in SharedPreferences rather than DataStore
 * because the notifications and the alert screen that read it are built synchronously.
 */
internal object SnoozePreferences {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("snooze", Context.MODE_PRIVATE)

    fun snoozeEnabled(context: Context): Boolean = prefs(context).getBoolean("enabled", true)

    fun setSnoozeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled", enabled).apply()
    }
}
