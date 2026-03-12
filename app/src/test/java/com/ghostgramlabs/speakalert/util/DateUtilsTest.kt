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
}
