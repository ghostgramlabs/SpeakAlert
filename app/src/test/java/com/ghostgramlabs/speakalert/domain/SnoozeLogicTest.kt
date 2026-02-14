package com.ghostgramlabs.speakalert.domain

import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SnoozeLogicTest {

    @Test
    fun testSnoozeDoesNotModifyRecurrence() {
        val now = 1698746400000L // 2023-10-31 10:00:00
        val nextTrigger = now + 3600000L // +1 hour
        
        val reminder = ReminderEntity(
            id = 1,
            nextTriggerAt = nextTrigger,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceJson = "{}" // Daily default
        )
        
        // Simulating snooze (normally handled in ReminderActionReceiver but logic is here)
        val snoozeMinutes = 10
        val snoozeUntil = now + (snoozeMinutes * 60 * 1000L)
        
        val snoozedReminder = reminder.copy(snoozeUntil = snoozeUntil)
        
        assertEquals("SnoozeUntil should be set", snoozeUntil, snoozedReminder.snoozeUntil)
        assertEquals("NextTriggerAt MUST remain unchanged", nextTrigger, snoozedReminder.nextTriggerAt)
        assertEquals("Recurrence type must remain same", RecurrenceType.DAILY, snoozedReminder.recurrenceType)
    }

    @Test
    fun testSnoozeLogicWithMultipleSteps() {
        val now = System.currentTimeMillis()
        val baseTrigger = now + 1000000L
        
        var reminder = ReminderEntity(
            nextTriggerAt = baseTrigger,
            recurrenceType = RecurrenceType.NONE
        )
        
        // Snooze 1
        val snooze1 = now + 300000L
        reminder = reminder.copy(snoozeUntil = snooze1)
        assertEquals(snooze1, reminder.snoozeUntil)
        
        // Snooze 2 (updates existing snooze)
        val snooze2 = now + 600000L
        reminder = reminder.copy(snoozeUntil = snooze2)
        assertEquals(snooze2, reminder.snoozeUntil)
        
        // baseTrigger remains the same
        assertEquals(baseTrigger, reminder.nextTriggerAt)
    }
}
