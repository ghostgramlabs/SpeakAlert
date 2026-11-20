package com.ghostgramlabs.speakalert.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun testNormalizeToMinute() {
        // Create a timestamp with specific seconds and milliseconds
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 15)
            set(Calendar.MILLISECOND, 500)
        }
        val input = cal.timeInMillis

        val result = DateUtils.normalizeToMinute(input)
        
        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        
        assertEquals("Hour should remain same", 10, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute should remain same", 30, resultCal.get(Calendar.MINUTE))
        assertEquals("Second should be normalized to 0", 0, resultCal.get(Calendar.SECOND))
        assertEquals("Millisecond should be normalized to 0", 0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `formatRelativeTime labels current timestamp as today`() {
        val now = System.currentTimeMillis()
        val today = DateUtils.formatRelativeTime(now)
        assertTrue(today.startsWith("Today,"))
    }

    @Test
    fun `formatDateLabel handles current and distant dates`() {
        val now = System.currentTimeMillis()
        val future = (Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 20) }).timeInMillis
        assertEquals("Today", DateUtils.formatDateLabel(now))
        assertTrue(DateUtils.formatDateLabel(future).isNotBlank())
    }

    @Test
    fun `isToday and isUpcoming behave correctly`() {
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = baseCal.timeInMillis
        val tomorrow = (baseCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val yesterday = (baseCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis

        assertTrue(DateUtils.isToday(now))
        assertTrue(DateUtils.isUpcoming(tomorrow))
        assertFalse(DateUtils.isUpcoming(yesterday))
    }

    @Test
    fun `formatSmartDate for today contains Today and time`() {
        val now = System.currentTimeMillis()
        val formatted = DateUtils.formatSmartDate(now)
        assertTrue(formatted.contains("Today"))
        assertTrue(formatted.contains(":"))
    }

    @Test
    fun `formatDateTime and formatTimeOnly return non-empty values`() {
        val ts = System.currentTimeMillis()
        assertTrue(DateUtils.formatDateTime(ts).isNotBlank())
        assertTrue(DateUtils.formatTimeOnly(ts).isNotBlank())
    }

    private fun withPlatformPattern(pattern: (java.util.Locale, String) -> String?, block: () -> Unit) {
        val savedPattern = DateUtils.bestPattern
        val savedLocale = java.util.Locale.getDefault()
        DateUtils.bestPattern = pattern
        try {
            block()
        } finally {
            DateUtils.bestPattern = savedPattern
            java.util.Locale.setDefault(savedLocale)
        }
    }

    private val nov13 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 13, 9, 5, 0) }.timeInMillis

    @Test
    fun `date order comes from the language when the platform supplies one`() {
        withPlatformPattern({ _, skeleton -> if (skeleton == "yyyyMMMd") "d. MMM yyyy" else null }) {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("d. MMM yyyy", DateUtils.datePattern(DateUtils.DateParts.MONTH_DAY_YEAR))
            assertTrue(DateUtils.formatDate(nov13, DateUtils.DateParts.MONTH_DAY_YEAR).startsWith("13. "))
        }
    }

    @Test
    fun `date falls back to the English layout without a platform pattern`() {
        for (missing in listOf<String?>(null, "", "  ")) {
            withPlatformPattern({ _, _ -> missing }) {
                java.util.Locale.setDefault(java.util.Locale.US)
                assertEquals("Fri, Nov 13, 2026", DateUtils.formatDate(nov13, DateUtils.DateParts.WEEKDAY_MONTH_DAY_YEAR))
                assertEquals("Nov 13", DateUtils.formatDate(nov13, DateUtils.DateParts.MONTH_DAY))
            }
        }
    }

    @Test
    fun `date and time keep the date first and the time last`() {
        withPlatformPattern({ _, _ -> "EEE, d. MMM" }) {
            java.util.Locale.setDefault(java.util.Locale.US)
            val text = DateUtils.formatDateTime(nov13)
            assertTrue(text, text.startsWith("Fri, 13. Nov, "))
            assertTrue(text, text.endsWith(DateUtils.formatTimeOnly(nov13)))
        }
    }
}
