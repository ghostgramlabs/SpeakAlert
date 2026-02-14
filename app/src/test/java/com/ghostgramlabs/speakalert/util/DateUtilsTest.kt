package com.ghostgramlabs.speakalert.util

import org.junit.Assert.assertEquals
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
}
