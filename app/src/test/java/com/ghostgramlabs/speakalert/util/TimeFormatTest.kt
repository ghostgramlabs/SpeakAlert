package com.ghostgramlabs.speakalert.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TimeFormatTest {

    private fun at(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @After
    fun reset() = TimeFormat.setForTesting(false)

    @Test
    fun `24-hour mode renders afternoon times on a 24-hour clock`() {
        TimeFormat.setForTesting(true)
        assertEquals("HH:mm", TimeFormat.timePattern)
        assertEquals("17:30", TimeFormat.formatTime(at(17, 30)))
        assertEquals("00:05", TimeFormat.formatTime(at(0, 5)))
    }

    @Test
    fun `12-hour mode keeps the meridiem suffix`() {
        TimeFormat.setForTesting(false)
        assertEquals("h:mm a", TimeFormat.timePattern)
        assertEquals("5:30", TimeFormat.formatTime(at(17, 30)).substringBefore(" "))
    }

    @Test
    fun `composite patterns follow the same setting`() {
        TimeFormat.setForTesting(true)
        assertEquals("Jan 1, 17:30", TimeFormat.formatDateTime(
            Calendar.getInstance().apply {
                set(2026, Calendar.JANUARY, 1, 17, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis,
            datePattern = "MMM d"
        ))
    }
}
