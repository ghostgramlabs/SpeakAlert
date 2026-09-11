package com.ghostgramlabs.speakalert.alarm

import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderAlarmValidityTest {
    private val reminder = ReminderEntity(id = 1, nextTriggerAt = 10_000L)

    @Test
    fun `done rejects already delivered regular and follow-up alarms`() {
        val completed = reminder.copy(isCompleted = true, pendingFollowUpAt = 20_000L)
        assertTrue(shouldIgnoreReminderAlarm(completed, 10_000L, false))
        assertTrue(shouldIgnoreReminderAlarm(completed, 20_000L, true))
    }

    @Test
    fun `skipped occurrence rejects cleared or replaced follow-up`() {
        assertTrue(shouldIgnoreReminderAlarm(reminder, 20_000L, true))
        assertTrue(shouldIgnoreReminderAlarm(reminder.copy(pendingFollowUpAt = 30_000L), 20_000L, true))
    }

    @Test
    fun `active regular snooze and current follow-up alarms remain eligible`() {
        assertFalse(shouldIgnoreReminderAlarm(reminder, 10_000L, false))
        assertFalse(shouldIgnoreReminderAlarm(reminder.copy(snoozeUntil = 15_000L), 15_000L, false))
        assertFalse(shouldIgnoreReminderAlarm(reminder.copy(pendingFollowUpAt = 20_000L), 20_000L, true))
    }
}
