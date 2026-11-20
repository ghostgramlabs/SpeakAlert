package com.ghostgramlabs.speakalert.alarm

import android.app.Notification
import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertChannelHealthTest {

    @Test
    fun `tone-only alerts are checked on the tone channels only`() {
        assertEquals(
            listOf(NotificationHelper.TONE_ONLY_CHANNEL_ID, NotificationHelper.TONE_ONLY_DND_BYPASS_CHANNEL_ID),
            AlertChannelHealth.channelsInUse(toneOnlyMode = true, dndBypassEnabled = true, fullScreenAlertEnabled = true)
        )
        assertEquals(
            listOf(NotificationHelper.TONE_ONLY_CHANNEL_ID),
            AlertChannelHealth.channelsInUse(toneOnlyMode = true, dndBypassEnabled = false, fullScreenAlertEnabled = false)
        )
    }

    @Test
    fun `voice alerts include the silent full-screen channels only when full-screen alerts are on`() {
        assertEquals(
            listOf(NotificationHelper.CHANNEL_ID),
            AlertChannelHealth.channelsInUse(toneOnlyMode = false, dndBypassEnabled = false, fullScreenAlertEnabled = false)
        )
        assertEquals(
            listOf(
                NotificationHelper.CHANNEL_ID,
                NotificationHelper.DND_BYPASS_CHANNEL_ID,
                NotificationHelper.SILENT_CHANNEL_ID,
                NotificationHelper.SILENT_DND_BYPASS_CHANNEL_ID
            ),
            AlertChannelHealth.channelsInUse(toneOnlyMode = false, dndBypassEnabled = true, fullScreenAlertEnabled = true)
        )
    }

    @Test
    fun `channel settings map to the problem the user sees`() {
        val public = Notification.VISIBILITY_PUBLIC
        assertNull(AlertChannelHealth.problemOf(NotificationManager.IMPORTANCE_HIGH, public))
        assertNull(AlertChannelHealth.problemOf(NotificationManager.IMPORTANCE_UNSPECIFIED, public))
        assertEquals(AlertChannelProblem.BLOCKED, AlertChannelHealth.problemOf(NotificationManager.IMPORTANCE_NONE, public))
        assertEquals(AlertChannelProblem.NO_POPUP, AlertChannelHealth.problemOf(NotificationManager.IMPORTANCE_DEFAULT, public))
        assertEquals(AlertChannelProblem.NO_POPUP, AlertChannelHealth.problemOf(NotificationManager.IMPORTANCE_LOW, public))
        assertEquals(
            AlertChannelProblem.HIDDEN_ON_LOCK_SCREEN,
            AlertChannelHealth.problemOf(NotificationManager.IMPORTANCE_HIGH, Notification.VISIBILITY_SECRET)
        )
        // Switched off outranks everything else on the same channel.
        assertEquals(
            AlertChannelProblem.BLOCKED,
            AlertChannelHealth.problemOf(NotificationManager.IMPORTANCE_NONE, Notification.VISIBILITY_SECRET)
        )
    }
}
