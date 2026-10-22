package com.ghostgramlabs.speakalert.util

import android.content.Context

/**
 * Whether reminders are set as alarm clocks. Kept in SharedPreferences rather than DataStore
 * because alarms are scheduled synchronously, including from boot and alarm receivers.
 */
internal object AlarmDeliveryPreferences {
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("alarm_delivery", Context.MODE_PRIVATE)

    fun useAlarmClock(context: Context): Boolean = prefs(context).getBoolean("alarm_clock", false)

    fun setUseAlarmClock(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("alarm_clock", enabled).apply()
    }
}
