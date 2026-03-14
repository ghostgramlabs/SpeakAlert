package com.ghostgramlabs.speakalert.alarm

import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderAlertPayloadTest {

    @Test
    fun `follow up payload uses follow up question instead of original audio`() {
        val reminder = ReminderEntity(
            id = 1L,
            title = "Water plants",
            reminderText = "Back patio plants",
            audioPath = "/storage/emulated/0/recordings/reminder.m4a",
            nextTriggerAt = System.currentTimeMillis()
        )

        val payload = buildReminderAlertPayload(
            reminder = reminder,
            isFollowUpTrigger = true,
            hasPlayableAudio = true,
            hasAudioConfigured = true
        )

        assertEquals("Water plants", payload.title)
        assertEquals("Did you complete Water plants?", payload.message)
        assertNull(payload.playbackAudioPath)
        assertEquals("Did you complete Water plants?", payload.playbackText)
        assertFalse(payload.autoplayOnTap)
        assertEquals(true, payload.isFollowUpAlert)
    }
}
