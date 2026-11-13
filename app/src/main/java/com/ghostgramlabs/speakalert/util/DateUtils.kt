package com.ghostgramlabs.speakalert.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.ghostgramlabs.speakalert.R

object DateUtils {

    /**
     * Computes the calendar-day difference between two timestamps.
     * Returns positive if target is after now, negative if before.
     * Uses floor-division on midnight-aligned days to handle year boundaries correctly.
     */
    fun dayDifference(nowMillis: Long, targetMillis: Long): Int {
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

    /**
     * Which parts of a date to show. The order and punctuation come from the app language
     * ("Fri, Nov 13, 2026" in English, "Fr., 13. Nov. 2026" in German); [fallback] is the English
     * layout, used when the platform cannot supply one (plain JVM unit tests).
     */
    enum class DateParts(internal val skeleton: String, internal val fallback: String) {
        WEEKDAY_MONTH_DAY("EEEMMMd", "EEE, MMM d"),
        WEEKDAY_MONTH_DAY_YEAR("yyyyMMMEEEd", "EEE, MMM d, yyyy"),
        MONTH_DAY("MMMd", "MMM d"),
        MONTH_DAY_YEAR("yyyyMMMd", "MMM d, yyyy")
    }

    /** Asks the platform for a language's layout of a skeleton. Replaceable in unit tests. */
    internal var bestPattern: (Locale, String) -> String? = { locale, skeleton ->
        runCatching { android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton) }.getOrNull()
    }

    /** The [SimpleDateFormat] pattern showing [parts] the way [locale] writes dates. */
    fun datePattern(parts: DateParts, locale: Locale = Locale.getDefault()): String =
        bestPattern(locale, parts.skeleton)?.takeIf { it.isNotBlank() } ?: parts.fallback

    /** Formats only the date of [timestamp], in the app language's own order. */
    fun formatDate(timestamp: Long, parts: DateParts): String =
        SimpleDateFormat(datePattern(parts), Locale.getDefault()).format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String =
        "${formatDate(timestamp, DateParts.WEEKDAY_MONTH_DAY)}, ${formatTimeOnly(timestamp)}"

    fun formatRelativeTime(timestamp: Long): String {
        // e.g. "Today, 10:00 AM" or "Tomorrow, 10:00 AM"
        val timeFormat = SimpleDateFormat(com.ghostgramlabs.speakalert.util.TimeFormat.timePattern, Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestamp))
        
        return when (dayDifference(System.currentTimeMillis(), timestamp)) {
            0 -> "${relativeDay(R.string.date_today)}, $timeStr"
            1 -> "${relativeDay(R.string.date_tomorrow)}, $timeStr"
            -1 -> "${relativeDay(R.string.date_yesterday)}, $timeStr"
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
        
        val timeFormat = SimpleDateFormat(com.ghostgramlabs.speakalert.util.TimeFormat.timePattern, Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestamp))
        
        val dayDiff = dayDifference(now, timestamp)
        
        return when (dayDiff) {
            0 -> "${relativeDay(R.string.date_today)} • $timeStr"
            1 -> "${relativeDay(R.string.date_tomorrow)} • $timeStr"
            -1 -> "${relativeDay(R.string.date_yesterday)} • $timeStr"
            else -> {
                if (dayDiff in 2..6) {
                    // Within next 7 days — show day name
                    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    "${dayFormat.format(Date(timestamp))} • $timeStr"
                } else if (nowYear != targetYear) {
                    // Different year — include year
                    "${formatDate(timestamp, DateParts.MONTH_DAY_YEAR)} • $timeStr"
                } else {
                    // Same year — month and day
                    "${formatDate(timestamp, DateParts.MONTH_DAY)} • $timeStr"
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
            0 -> relativeDay(R.string.date_today)
            1 -> relativeDay(R.string.date_tomorrow)
            -1 -> relativeDay(R.string.date_yesterday)
            else -> {
                if (nowYear != targetYear) {
                    formatDate(timestamp, DateParts.MONTH_DAY_YEAR)
                } else {
                    formatDate(timestamp, DateParts.MONTH_DAY)
                }
            }
        }
    }

    fun formatTimeOnly(timestamp: Long): String {
        return SimpleDateFormat(com.ghostgramlabs.speakalert.util.TimeFormat.timePattern, Locale.getDefault()).format(Date(timestamp))
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

    /**
     * Application context for the three relative day names.
     *
     * These are rendered from reminder cards, the alert screen, notifications and the pause
     * banner, so threading a Context through every caller would touch far more than it is worth.
     * They were plain English literals before, which left "Today" sitting inside otherwise
     * translated sentences. Resolved per call so a per-app language change takes effect at once,
     * and falling back to English rather than crashing if init has not run yet.
     */
    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private fun relativeDay(resId: Int): String {
        val context = appContext ?: return when (resId) {
            R.string.date_tomorrow -> "Tomorrow"
            R.string.date_yesterday -> "Yesterday"
            else -> "Today"
        }
        return AppLocale.localizedContext(context).getString(resId)
    }
}
