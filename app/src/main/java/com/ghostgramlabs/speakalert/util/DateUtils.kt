package com.ghostgramlabs.speakalert.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        // e.g. "Today, 10:00 AM" or "Tomorrow, 10:00 AM"
        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val nowYear = nowCal.get(Calendar.YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)
        val targetYear = targetCal.get(Calendar.YEAR)
        
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestamp))
        
        return if (nowYear == targetYear) {
            when (targetDay) {
                nowDay -> "Today, $timeStr"
                nowDay + 1 -> "Tomorrow, $timeStr"
                nowDay - 1 -> "Yesterday, $timeStr"
                else -> formatDateTime(timestamp)
            }
        } else {
            formatDateTime(timestamp)
        }
    }
    
    fun formatSmartDate(timestamp: Long): String {
        // Requested format:
        // Today • 6:30 PM
        // Tomorrow • 9:00 AM
        // Mon • 7:00 PM
        // Mar 15 • 8:30 AM
        
        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val nowYear = nowCal.get(Calendar.YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)
        val targetYear = targetCal.get(Calendar.YEAR)
        
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestamp))
        
        if (nowYear == targetYear) {
            return when (targetDay) {
                nowDay -> "Today • $timeStr"
                nowDay + 1 -> "Tomorrow • $timeStr"
                nowDay - 1 -> "Yesterday • $timeStr"
                else -> {
                    // Start of week logic? Or just "Mon", "Tue"? 
                    // Let's check if it's within the next 7 days for simplified Day Name
                    if (targetDay > nowDay && targetDay < nowDay + 7) {
                        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                        "${dayFormat.format(Date(timestamp))} • $timeStr"
                    } else {
                        // Mar 15 • 8:30 AM
                         val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                        "${dateFormat.format(Date(timestamp))} • $timeStr"
                    }
                }
            }
        } else {
             // Formats date with year if different year? Original request didn't specify, but "Mar 15 • 8:30 AM" implies no year. 
             // We'll stick to MMM d unless it's very far.
             val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            return "${dateFormat.format(Date(timestamp))} • $timeStr"
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
        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val nowYear = nowCal.get(Calendar.YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)
        val targetYear = targetCal.get(Calendar.YEAR)
        
        if (nowYear == targetYear) {
            return when (targetDay) {
                nowDay -> "Today"
                nowDay + 1 -> "Tomorrow"
                nowDay - 1 -> "Yesterday"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
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
