package com.ghostgramlabs.speakalert.util

import android.content.Context

/** Synchronous startup mirror; -1 means follow the device, absence means not migrated yet. */
internal object ClockPreferences {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("clock_preferences", Context.MODE_PRIVATE)

    fun hasValue(context: Context) = prefs(context).contains("mode")
    fun read(context: Context): Boolean? = when (prefs(context).getInt("mode", -1)) {
        0 -> false
        1 -> true
        else -> null
    }

    fun write(context: Context, override: Boolean?) {
        prefs(context).edit().putInt("mode", when (override) {
            true -> 1
            false -> 0
            null -> -1
        }).apply()
    }
}
