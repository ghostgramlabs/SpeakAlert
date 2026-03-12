package com.ghostgramlabs.speakalert.domain

import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.domain.models.RecurrenceEndRule
import com.ghostgramlabs.speakalert.domain.models.EndRuleType
import com.ghostgramlabs.speakalert.domain.models.MonthlyVariant
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.domain.models.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, daysOfMonth = setOf(31))
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
        // Use a fixed date so the test is stable around DST transitions.
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val triggerTime = now
        
        val model = RecurrenceModel.Custom(interval = 2, unit = TimeUnit.DAYS)
        val json = RecurrenceUtils.toJson(model)
        
        val reminder = ReminderEntity(
            nextTriggerAt = triggerTime,
            recurrenceType = RecurrenceType.CUSTOM,
            recurrenceJson = json
        )
        
        val nextTrigger = RecurrenceUtils.computeNextTrigger(reminder, now)
        
        val expected = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 2)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        assertEquals(expected, nextTrigger)
    }

    @Test
    fun testGetRecurrenceSummary() {
        val now = System.currentTimeMillis()
        val model = RecurrenceModel.Daily()
        val json = RecurrenceUtils.toJson(model)

        // Test with time included (default)
        val summaryWithTime = RecurrenceUtils.getRecurrenceSummary(
            RecurrenceType.DAILY,
            json,
            now,
            includeTime = true
        )
        assertTrue("Summary should contain time separator", summaryWithTime.contains("•"))
        assertTrue("Summary should start with Daily", summaryWithTime.startsWith("Daily"))

        // Test without time
        val summaryNoTime = RecurrenceUtils.getRecurrenceSummary(
            RecurrenceType.DAILY,
            json,
            now,
            includeTime = false
        )
        assertEquals("Daily", summaryNoTime)
        
        // Test Monthly
        val monthlyModel = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, daysOfMonth = setOf(1, 15))
        val monthlyJson = RecurrenceUtils.toJson(monthlyModel)
        val monthlySummary = RecurrenceUtils.getRecurrenceSummary(
            RecurrenceType.MONTHLY,
            monthlyJson,
            now,
            includeTime = false
        )
        // Order of days might vary if set is unordered, but usually sorted in implementation
        assertTrue(monthlySummary.contains("Monthly"))
        assertTrue(monthlySummary.contains("1st"))
        assertTrue(monthlySummary.contains("15th"))
    }
    @Test
    fun testWeeklyMultiDayCrossWeek() {
        val monday = Calendar.getInstance().apply {
            set(2023, Calendar.OCTOBER, 30, 10, 0, 0) // Monday
            set(Calendar.MILLISECOND, 0)
        }
        val model = RecurrenceModel.Weekly(daysOfWeek = setOf(1, 3, 5))
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(nextTriggerAt = monday.timeInMillis, recurrenceType = RecurrenceType.WEEKLY, recurrenceJson = json)

        // From Monday 10:00:01
        var next = RecurrenceUtils.computeNextTrigger(reminder, monday.timeInMillis + 1000)
        val wednesday = Calendar.getInstance().apply { set(2023, Calendar.NOVEMBER, 1, 10, 0, 0); set(Calendar.MILLISECOND, 0) }
        assertEquals("Next should be Wed", wednesday.timeInMillis, next)

        val friday = Calendar.getInstance().apply { set(2023, Calendar.NOVEMBER, 3, 10, 0, 0); set(Calendar.MILLISECOND, 0) }
        next = RecurrenceUtils.computeNextTrigger(reminder, friday.timeInMillis + 1000)
        val nextMon = Calendar.getInstance().apply { set(2023, Calendar.NOVEMBER, 6, 10, 0, 0); set(Calendar.MILLISECOND, 0) }
        assertEquals("Next should be Mon", nextMon.timeInMillis, next)
    }

    @Test
    fun testMonthlyLeapYear() {
        val feb29 = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 29, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, daysOfMonth = setOf(29))
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(nextTriggerAt = feb29.timeInMillis, recurrenceType = RecurrenceType.MONTHLY, recurrenceJson = json)

        val next = RecurrenceUtils.computeNextTrigger(reminder, feb29.timeInMillis)
        val expected = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 29, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Next should be Mar 29", expected.timeInMillis, next)
    }

    @Test
    fun testYearlyNextYearSameDate() {
        val base = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 10, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val model = RecurrenceModel.Yearly()
        val reminder = ReminderEntity(
            nextTriggerAt = base.timeInMillis,
            recurrenceType = RecurrenceType.YEARLY,
            recurrenceJson = RecurrenceUtils.toJson(model)
        )

        val next = RecurrenceUtils.computeNextTrigger(reminder, base.timeInMillis)
        val expected = Calendar.getInstance().apply {
            set(2027, Calendar.MARCH, 10, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals("Yearly should move to same date next year", expected.timeInMillis, next)
    }

    @Test
    fun testYearlyLeapDayClampsToFeb28OnNonLeapYear() {
        val leapDay = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 29, 9, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val model = RecurrenceModel.Yearly()
        val reminder = ReminderEntity(
            nextTriggerAt = leapDay.timeInMillis,
            recurrenceType = RecurrenceType.YEARLY,
            recurrenceJson = RecurrenceUtils.toJson(model)
        )

        val next = RecurrenceUtils.computeNextTrigger(reminder, leapDay.timeInMillis)
        val expected = Calendar.getInstance().apply {
            set(2025, Calendar.FEBRUARY, 28, 9, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertEquals("Feb 29 yearly should clamp to Feb 28 on non-leap years", expected.timeInMillis, next)
    }

    @Test
    fun testMonthlyClampingApril31To30() {
        val mar31 = Calendar.getInstance().apply {
            set(2023, Calendar.MARCH, 31, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, daysOfMonth = setOf(31))
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(nextTriggerAt = mar31.timeInMillis, recurrenceType = RecurrenceType.MONTHLY, recurrenceJson = json)

        val next = RecurrenceUtils.computeNextTrigger(reminder, mar31.timeInMillis)
        val expected = Calendar.getInstance().apply {
            set(2023, Calendar.APRIL, 30, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Apr 31 should clamp to Apr 30", expected.timeInMillis, next)
    }

    @Test
    fun testCustomEvery3Weeks() {
        val sun = Calendar.getInstance().apply {
            set(2023, Calendar.OCTOBER, 22, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val model = RecurrenceModel.Custom(interval = 3, unit = TimeUnit.WEEKS)
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(nextTriggerAt = sun.timeInMillis, recurrenceType = RecurrenceType.CUSTOM, recurrenceJson = json)

        val next = RecurrenceUtils.computeNextTrigger(reminder, sun.timeInMillis)
        
        // Use Calendar for expectation to match wall-clock preservation logic
        val expectedCal = sun.clone() as Calendar
        expectedCal.add(Calendar.WEEK_OF_YEAR, 3)
        
        assertEquals("Should preserve 10:00 AM wall clock time across DST", expectedCal.timeInMillis, next)
    }

    @Test
    fun testEndRuleAfterOccurrences() {
        val now = System.currentTimeMillis()
        val model = RecurrenceModel.Daily(endRule = RecurrenceEndRule(EndRuleType.AFTER_OCCURRENCES, count = 0))
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(nextTriggerAt = now, recurrenceType = RecurrenceType.DAILY, recurrenceJson = json)

        val next = RecurrenceUtils.computeNextTrigger(reminder, now - 1000)
        assertEquals(null, next)
    }

    @Test
    fun testEndRuleUntilDate() {
        val now = System.currentTimeMillis()
        val model = RecurrenceModel.Daily(endRule = RecurrenceEndRule(EndRuleType.UNTIL_DATE, endDateMillis = now))
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(nextTriggerAt = now, recurrenceType = RecurrenceType.DAILY, recurrenceJson = json)

        val next = RecurrenceUtils.computeNextTrigger(reminder, now)
        assertEquals(null, next)
    }

    @Test
    fun testAlignmentOnSaveExactMatch() {
        val now = 1698746400000L // 2023-10-31 10:00:00
        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, daysOfMonth = setOf(31))
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(nextTriggerAt = now, recurrenceType = RecurrenceType.MONTHLY, recurrenceJson = json)

        val aligned = RecurrenceUtils.computeNextTrigger(reminder, now - 1)
        assertEquals(now, aligned)
    }

    @Test
    fun testMonthlyTodayFutureSuccess() {
        // Today is 11th. We want it to trigger today at 19:00.
        // But let's simulate the issue where current time (fromTime) is 18:39:45
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 18, 39, 45)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Reminder is set for 19:00 (with 0 seconds for now)
        val triggerTime = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 19, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, daysOfMonth = setOf(11))
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(
            nextTriggerAt = triggerTime,
            recurrenceType = RecurrenceType.MONTHLY,
            recurrenceJson = json
        )

        val next = RecurrenceUtils.computeNextTrigger(reminder, now.timeInMillis)
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 19, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        assertEquals("Should trigger today at 19:00", expected.timeInMillis, next)
    }

    @Test
    fun testMonthlyTodayFutureWithSecondsBug() {
        // Today is 11th. Current time is 18:30:00.
        // BUT the baseTrigger (reminder.nextTriggerAt) was saved with seconds, e.g. 19:00:30.
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 18, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val baseTrigger = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 19, 0, 30) // 30 seconds
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, daysOfMonth = setOf(11))
        val json = RecurrenceUtils.toJson(model)
        val reminder = ReminderEntity(
            nextTriggerAt = baseTrigger,
            recurrenceType = RecurrenceType.MONTHLY,
            recurrenceJson = json
        )

        val next = RecurrenceUtils.computeNextTrigger(reminder, now.timeInMillis)
        
        // Expected should be today 19:00:00 (Normalized)
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 19, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        assertEquals("Should trigger today at 19:00:00", expected.timeInMillis, next)
    }

    @Test
    fun testDailyTodayFutureSuccess() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val base = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 19, 0, 30)
            set(Calendar.MILLISECOND, 500)
        }.timeInMillis
        
        val reminder = ReminderEntity(nextTriggerAt = base, recurrenceType = RecurrenceType.DAILY)
        val next = RecurrenceUtils.computeNextTrigger(reminder, now)
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 19, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        assertEquals("Daily should trigger today at 19:00:00", expected, next)
    }

    @Test
    fun testWeeklyTodayFutureSuccess() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 18, 0, 0) // Feb 11 2026 is Wednesday
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val base = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 19, 0, 30)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val model = RecurrenceModel.Weekly(daysOfWeek = setOf(3)) // Wednesday
        val reminder = ReminderEntity(nextTriggerAt = base, recurrenceType = RecurrenceType.WEEKLY, recurrenceJson = RecurrenceUtils.toJson(model))
        
        val next = RecurrenceUtils.computeNextTrigger(reminder, now)
        
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 19, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        assertEquals("Weekly should trigger today at 19:00:00", expected, next)
    }

    @Test
    fun testEndRuleUntilDateInclusivity() {
        // Test that reminder triggers ON the end date
        val endDay = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val triggerTime = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val model = RecurrenceModel.Daily(
            endRule = RecurrenceEndRule(type = EndRuleType.UNTIL_DATE, endDateMillis = endDay)
        )
        
        val reminder = ReminderEntity(
            nextTriggerAt = triggerTime, 
            recurrenceType = RecurrenceType.DAILY,
            recurrenceJson = RecurrenceUtils.toJson(model)
        )
        
        val next = RecurrenceUtils.computeNextTrigger(reminder, triggerTime - 1)
        assertEquals("Should trigger on the end date itself", triggerTime, next)
        
        // Should NOT trigger after the end date
        val DayAfter = triggerTime + 86400000L
        val nextAfter = RecurrenceUtils.computeNextTrigger(reminder, DayAfter - 1)
        assertEquals("Should be null after the end date", null, nextAfter)
    }
    @Test
    fun testMarkDoneEarlyAdvancesToNextOccurrence() {
        // Setup: Today 10:00 AM (Scheduled)
        val scheduledCal = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val scheduledTime = scheduledCal.timeInMillis
        
        // Actually it's 9:00 AM now (Early)
        val nowCal = scheduledCal.clone() as Calendar
        nowCal.add(Calendar.HOUR_OF_DAY, -1)
        val now = nowCal.timeInMillis
        
        val reminder = ReminderEntity(
            nextTriggerAt = scheduledTime,
            recurrenceType = RecurrenceType.DAILY
        )
        
        // CalculationBase from ViewModel fix: maxOf(now, scheduledTime)
        val calculationBase = maxOf(now, scheduledTime)
        val nextTrigger = RecurrenceUtils.computeNextTrigger(reminder, calculationBase)
        
        // Expect: Tomorrow 10:00 AM
        val expectedCal = scheduledCal.clone() as Calendar
        expectedCal.add(Calendar.DAY_OF_YEAR, 1)
        val expected = expectedCal.timeInMillis
        
        assertEquals("Should advance to tomorrow when marked done early", expected, nextTrigger)
    }

    @Test
    fun testMarkDoneExactTimeAdvancesToNextOccurrence() {
        // Setup: Exactly at scheduled time
        val scheduledCal = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 11, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val scheduledTime = scheduledCal.timeInMillis
        
        val reminder = ReminderEntity(
            nextTriggerAt = scheduledTime,
            recurrenceType = RecurrenceType.DAILY
        )
        
        // CalculationBase = scheduledTime
        val nextTrigger = RecurrenceUtils.computeNextTrigger(reminder, scheduledTime)
        
        // Expect: Tomorrow 10:00 AM
        val expectedCal = scheduledCal.clone() as Calendar
        expectedCal.add(Calendar.DAY_OF_YEAR, 1)
        
        assertEquals("Should advance to tomorrow even at exact time", expectedCal.timeInMillis, nextTrigger)
    }
}
