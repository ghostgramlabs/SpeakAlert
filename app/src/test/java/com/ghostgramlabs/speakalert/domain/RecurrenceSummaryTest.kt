package com.ghostgramlabs.speakalert.domain

import com.ghostgramlabs.speakalert.domain.models.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurrenceSummaryTest {

    @Test
    fun testOneTime() {
        // Mock a future time (e.g. tomorrow)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)

        // Relative day text can vary by runtime timezone/day boundary, keep this assertion stable.
        val summary = RecurrenceUtils.getRecurrenceSummary(RecurrenceType.NONE, null, cal.timeInMillis)
        println("One-time summary: $summary")
        assertTrue(summary.startsWith("One-time"))
        assertTrue(summary.contains("10:00 AM"))
    }

    @Test
    fun testDaily() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 7)
        cal.set(Calendar.MINUTE, 30)
        val summary = RecurrenceUtils.getRecurrenceSummary(RecurrenceType.DAILY, null, cal.timeInMillis)
        assertEquals("Daily • 7:30 AM", summary)
    }

    @Test
    fun testWeekly() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 0)

        // Mon, Wed, Fri
        val model = RecurrenceModel.Weekly(daysOfWeek = setOf(1, 3, 5))
        val json = RecurrenceUtils.toJson(model)

        val summary = RecurrenceUtils.getRecurrenceSummary(RecurrenceType.WEEKLY, json, cal.timeInMillis)
        assertEquals("Weekly • Mon, Wed, Fri • 8:00 AM", summary)
    }

    @Test
    fun testMonthlyDay() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 21) // 9 PM
        cal.set(Calendar.MINUTE, 0)

        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.DAY_OF_MONTH, daysOfMonth = setOf(31))
        val json = RecurrenceUtils.toJson(model)

        val summary = RecurrenceUtils.getRecurrenceSummary(RecurrenceType.MONTHLY, json, cal.timeInMillis)
        assertEquals("Monthly • 31st • 9:00 PM", summary)
    }

    @Test
    fun testMonthlyLastDay() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)

        val model = RecurrenceModel.Monthly(variant = MonthlyVariant.LAST_DAY)
        val json = RecurrenceUtils.toJson(model)

        val summary = RecurrenceUtils.getRecurrenceSummary(RecurrenceType.MONTHLY, json, cal.timeInMillis)
        assertEquals("Monthly • Last day • 9:00 AM", summary)
    }

    @Test
    fun testCustom() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 7)
        cal.set(Calendar.MINUTE, 0)

        val model = RecurrenceModel.Custom(interval = 2, unit = TimeUnit.DAYS)
        val json = RecurrenceUtils.toJson(model)

        val summary = RecurrenceUtils.getRecurrenceSummary(RecurrenceType.CUSTOM, json, cal.timeInMillis)
        assertEquals("Every 2 days • 7:00 AM", summary)
    }

    @Test
    fun testYearly() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, 2026)
        cal.set(Calendar.MONTH, Calendar.MARCH)
        cal.set(Calendar.DAY_OF_MONTH, 10)
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val model = RecurrenceModel.Yearly()
        val json = RecurrenceUtils.toJson(model)
        val summary = RecurrenceUtils.getRecurrenceSummary(RecurrenceType.YEARLY, json, cal.timeInMillis)

        assertTrue(summary.startsWith("Yearly"))
        assertTrue(summary.contains("Mar 10"))
        assertTrue(summary.contains("8:00 AM"))
    }

    @Test
    fun testCustomYears() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 7)
        cal.set(Calendar.MINUTE, 0)

        val model = RecurrenceModel.Custom(interval = 2, unit = TimeUnit.YEARS)
        val json = RecurrenceUtils.toJson(model)

        val summary = RecurrenceUtils.getRecurrenceSummary(RecurrenceType.CUSTOM, json, cal.timeInMillis)
        assertTrue(summary.startsWith("Every 2 years"))
        assertTrue(summary.contains("7:00 AM"))
    }

    @Test
    fun testWithEndRuleAndPolicy() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)

        val model = RecurrenceModel.Daily(
            endRule = RecurrenceEndRule(type = EndRuleType.AFTER_OCCURRENCES, count = 10),
            missedPolicy = MissedPolicy.SKIP_TO_NEXT
        )
        val json = RecurrenceUtils.toJson(model)

        val summary = RecurrenceUtils.getRecurrenceSummary(
            RecurrenceType.DAILY,
            json,
            cal.timeInMillis,
            includeEndRule = true,
            includeMissedPolicy = true
        )

        val expected = "Daily • 10:00 AM\nEnds after 10 times\nIf device off: Remind at exact time only"
        assertEquals(expected, summary)
    }
}
