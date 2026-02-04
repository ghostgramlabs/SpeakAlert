package com.example.voicereminder.domain

import com.example.voicereminder.data.model.ReminderEntity
import com.example.voicereminder.domain.models.MonthlyVariant
import com.example.voicereminder.domain.models.RecurrenceModel
import com.example.voicereminder.domain.models.RecurrenceType
import com.example.voicereminder.domain.models.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RecurrenceUtilsTest {

    @Test
    fun testMonthly31stClampsToFeb28() {
        // Setup: Jan 31st
        val jan31 = Calendar.getInstance().apply {
            set(2023, Calendar.JANUARY, 31, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerTime = jan31.timeInMillis
        
        // Rule: Monthly on day 31
        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, dayOfMonth = 31)
        val json = RecurrenceUtils.toJson(model)
        
        val reminder = ReminderEntity(
            nextTriggerAt = triggerTime,
            recurrenceType = RecurrenceType.MONTHLY,
            recurrenceJson = json
        )
        
        // Calculate next from Jan 31
        val nextTrigger = RecurrenceUtils.computeNextTrigger(reminder, jan31.timeInMillis)
        
        // Expect: Feb 28 (2023 is not leap)
        val expected = Calendar.getInstance().apply {
            set(2023, Calendar.FEBRUARY, 28, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        assertEquals("Should trigger on Feb 28", expected.timeInMillis, nextTrigger)
    }

    @Test
    fun testMonthlyLastDay() {
        // Setup: Feb 28 2023 (Last Day)
        val feb28 = Calendar.getInstance().apply {
            set(2023, Calendar.FEBRUARY, 28, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerTime = feb28.timeInMillis
        
        // Rule: Monthly Last Day
        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.LAST_DAY)
        val json = RecurrenceUtils.toJson(model)
        
        val reminder = ReminderEntity(
            nextTriggerAt = triggerTime,
            recurrenceType = RecurrenceType.MONTHLY,
            recurrenceJson = json
        )
        
        // Calculate next from Feb 28
        val nextTrigger = RecurrenceUtils.computeNextTrigger(reminder, feb28.timeInMillis)
        
        // Expect: Mar 31
        val expected = Calendar.getInstance().apply {
            set(2023, Calendar.MARCH, 31, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        assertEquals("Should trigger on Mar 31", expected.timeInMillis, nextTrigger)
    }

    @Test
    fun testCustomEvery2Days() {
        // Setup: Today 10:00
        val now = System.currentTimeMillis()
        val triggerTime = now // Fired now
        
        val model = RecurrenceModel.Custom(interval = 2, unit = TimeUnit.DAYS)
        val json = RecurrenceUtils.toJson(model)
        
        val reminder = ReminderEntity(
            nextTriggerAt = triggerTime,
            recurrenceType = RecurrenceType.CUSTOM,
            recurrenceJson = json
        )
        
        val nextTrigger = RecurrenceUtils.computeNextTrigger(reminder, now)
        
        // Expect: Now + 2 days
        val expected = now + 2L * 24 * 3600 * 1000
        
        // Allow small diff due to Calendar implementations but math should be exact
        assertEquals(expected, nextTrigger)
    }
}
