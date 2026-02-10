package com.ghostgramlabs.speakalert.util

import android.util.Log
// import com.ghostgramlabs.speakalert.BuildConfig

object DebugLogger {
    private const val TAG = "VoiceReminderQA"
    // Toggle this to false before release
    var isEnabled = true 

    fun logAlarmScheduled(reminderId: Long, triggerAt: Long, recurrence: String?) {
        if (!isEnabled) return
        val triggerStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(triggerAt))
        Log.d(TAG, "ALARM_SCHEDULED | ID=$reminderId | At=$triggerStr | Recurrence=$recurrence")
    }

    fun logAlarmFired(reminderId: Long, expectedTrigger: Long, lateByMs: Long) {
        if (!isEnabled) return
        val nowStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        Log.d(TAG, "ALARM_FIRED | ID=$reminderId | Now=$nowStr | LateBy=${lateByMs}ms")
    }

    fun logAutoplayDecision(reminderId: Long, allowed: Boolean, reason: String) {
        if (!isEnabled) return
        Log.d(TAG, "AUTOPLAY | ID=$reminderId | Allowed=$allowed | Reason=$reason")
    }

    fun logPlayback(type: String, action: String, details: String) {
        if (!isEnabled) return
        Log.d(TAG, "PLAYBACK | Type=$type | Action=$action | $details")
    }

    fun logRecurrenceComputed(reminderId: Long, current: Long, next: Long?) {
        if (!isEnabled) return
        val nextStr = if (next != null) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(next)) else "NULL"
        Log.d(TAG, "RECURRENCE | ID=$reminderId | From=$current | Next=$nextStr")
    }
}
