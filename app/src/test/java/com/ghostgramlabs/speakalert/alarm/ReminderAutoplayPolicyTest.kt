package com.ghostgramlabs.speakalert.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderAutoplayPolicyTest {

    @Test
    fun `text only reminder requires automatic spoken text setting`() {
        val result = shouldAutoPlayReminder(
            autoPlayEnabled = true,
            inCall = false,
            unlockedOnly = false,
            isLocked = false,
            playbackAudioPath = null,
            playbackText = "Take medicine",
            isFollowUpTrigger = false,
            speakTextIfNoVoice = false,
            bootBlocked = false,
            toneOnlyMode = false
        )

        assertFalse(result)
    }

    @Test
    fun `follow up reminder can autoplay even when text fallback is off`() {
        val result = shouldAutoPlayReminder(
            autoPlayEnabled = true,
            inCall = false,
            unlockedOnly = false,
            isLocked = false,
            playbackAudioPath = null,
            playbackText = "Did you complete Water plants?",
            isFollowUpTrigger = true,
            speakTextIfNoVoice = false,
            bootBlocked = false,
            toneOnlyMode = false
        )

        assertTrue(result)
    }

    @Test
    fun `follow up reminder still respects tone only and boot restrictions`() {
        val toneOnlyBlocked = shouldAutoPlayReminder(
            autoPlayEnabled = true,
            inCall = false,
            unlockedOnly = false,
            isLocked = false,
            playbackAudioPath = null,
            playbackText = "Did you complete Water plants?",
            isFollowUpTrigger = true,
            speakTextIfNoVoice = true,
            bootBlocked = false,
            toneOnlyMode = true
        )
        val bootBlocked = shouldAutoPlayReminder(
            autoPlayEnabled = true,
            inCall = false,
            unlockedOnly = false,
            isLocked = false,
            playbackAudioPath = null,
            playbackText = "Did you complete Water plants?",
            isFollowUpTrigger = true,
            speakTextIfNoVoice = true,
            bootBlocked = true,
            toneOnlyMode = false
        )

        assertFalse(toneOnlyBlocked)
        assertFalse(bootBlocked)
    }
}
