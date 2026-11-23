package com.ghostgramlabs.speakalert.util

import android.content.Context

/**
 * The first day of the week in date pickers ("Week starts on"). 0 follows the phone's region;
 * otherwise a java.util.Calendar day constant. Kept in SharedPreferences rather than DataStore
 * because the date picker dialogs read it synchronously when they open.
 */
internal object WeekStartPreferences {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("week_start", Context.MODE_PRIVATE)

    fun firstDayOfWeek(context: Context): Int = prefs(context).getInt("first_day", 0)

    fun setFirstDayOfWeek(context: Context, day: Int) {
        prefs(context).edit().putInt("first_day", day).apply()
    }
}
