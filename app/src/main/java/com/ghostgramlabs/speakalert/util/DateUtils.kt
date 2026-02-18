package com.ghostgramlabs.speakalert.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    /**
     * Computes the calendar-day difference between two timestamps.
     * Returns positive if target is after now, negative if before.
     * Uses floor-division on midnight-aligned days to handle year boundaries correctly.
     */
    private fun dayDifference(nowMillis: Long, targetMillis: Long): Int {
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = targetMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = targetCal.timeInMillis - nowCal.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        // e.g. "Today, 10:00 AM" or "Tomorrow, 10:00 AM"
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestamp))
        
        return when (dayDifference(System.currentTimeMillis(), timestamp)) {
            0 -> "Today, $timeStr"
            1 -> "Tomorrow, $timeStr"
            -1 -> "Yesterday, $timeStr"
            else -> formatDateTime(timestamp)
        }
    }
    
    fun formatSmartDate(timestamp: Long): String {
        // Requested format:
        // Today • 6:30 PM
        // Tomorrow • 9:00 AM
        // Mon • 7:00 PM
        // Mar 15 • 8:30 AM
        
        val now = System.currentTimeMillis()
        val nowYear = Calendar.getInstance().get(Calendar.YEAR)
        val targetYear = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
        
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestamp))
        
        val dayDiff = dayDifference(now, timestamp)
        
        return when (dayDiff) {
            0 -> "Today • $timeStr"
            1 -> "Tomorrow • $timeStr"
            -1 -> "Yesterday • $timeStr"
            else -> {
                if (dayDiff in 2..6) {
                    // Within next 7 days — show day name
                    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    "${dayFormat.format(Date(timestamp))} • $timeStr"
                } else if (nowYear != targetYear) {
                    // Different year — include year
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    "${dateFormat.format(Date(timestamp))} • $timeStr"
                } else {
                    // Same year — month and day
                    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    "${dateFormat.format(Date(timestamp))} • $timeStr"
                }
            }
        }
    }

    fun isToday(timestamp: Long): Boolean {
        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
               nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    }

    // Helper for "Upcoming" logic (e.g. tomorrow onwards)
    fun isUpcoming(timestamp: Long): Boolean {
        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        if (targetCal.get(Calendar.YEAR) > nowCal.get(Calendar.YEAR)) return true
        if (targetCal.get(Calendar.YEAR) < nowCal.get(Calendar.YEAR)) return false
        
        return targetCal.get(Calendar.DAY_OF_YEAR) > nowCal.get(Calendar.DAY_OF_YEAR)
    }

    fun formatDateLabel(timestamp: Long): String {
        val nowYear = Calendar.getInstance().get(Calendar.YEAR)
        val targetYear = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
        
        return when (dayDifference(System.currentTimeMillis(), timestamp)) {
            0 -> "Today"
            1 -> "Tomorrow"
            -1 -> "Yesterday"
            else -> {
                if (nowYear != targetYear) {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
                } else {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }
    }

    fun formatTimeOnly(timestamp: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Normalizes a timestamp to the top of the minute (0 seconds, 0 milliseconds).
     */
    fun normalizeToMinute(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
