package com.ghostgramlabs.speakalert.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReminderPlaybackStartTest {
    // Android framework calls are mocked; these check launch acceptance, not audible playback.
    private fun withAndroidMocks(test: (Context) -> Unit) {
        mockStatic(Looper::class.java).use {
            mockConstruction(Handler::class.java).use {
                mockConstruction(Intent::class.java).use {
                    test(mock())
                }
            }
        }
    }

    @Test
    fun `accepted launch returns true`() = withAndroidMocks { context ->
        whenever(context.startForegroundService(any())).thenReturn(mock<ComponentName>())
        assertTrue(ReminderPlaybackService.start(context, 1, "Reminder", "/voice.m4a", null))
        verify(context).startForegroundService(any())
    }

    @Test
    fun `rejected launch returns false without crashing caller`() = withAndroidMocks { context ->
        whenever(context.startForegroundService(any())).thenThrow(IllegalStateException("Background start rejected"))
        assertFalse(ReminderPlaybackService.start(context, 1, "Reminder", null, "Take a break"))
    }

    @Test
    fun `missing service returns false`() = withAndroidMocks { context ->
        whenever(context.startForegroundService(any())).thenReturn(null)
        assertFalse(ReminderPlaybackService.start(context, 1, "Reminder", "/voice.m4a", null))
    }
}
