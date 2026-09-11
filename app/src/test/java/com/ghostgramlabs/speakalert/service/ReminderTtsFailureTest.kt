package com.ghostgramlabs.speakalert.service

import android.speech.tts.TextToSpeech
import org.junit.Test
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReminderTtsFailureTest {
    @Test
    fun `speech request stops recorded playback before creating its notification`() {
        val service = service(usingTts = true)
        val player = mock<androidx.media3.exoplayer.ExoPlayer>()
        ReminderPlaybackService::class.java.getDeclaredField("player").apply {
            isAccessible = true
            set(service, player)
        }
        // The Android notification path is unavailable in a JVM test and is caught by
        // speakTts. The previous player must already be stopped before reaching it.
        ReminderPlaybackService::class.java.getDeclaredMethod(
            "speakTts", String::class.java, String::class.java, java.lang.Long.TYPE
        ).apply {
            isAccessible = true
            invoke(service, "Take a break", "Reminder", 1L)
        }
        verify(player).stop()
    }
    private fun service(usingTts: Boolean): ReminderPlaybackService {
        // Exercise the real callback without constructing Android players or a speech engine.
        val service = mock<ReminderPlaybackService>(defaultAnswer = CALLS_REAL_METHODS)
        doNothing().whenever(service).stopSelf()
        ReminderPlaybackService::class.java.getDeclaredField("isTtsMode").apply {
            isAccessible = true
            setBoolean(service, usingTts)
        }
        return service
    }

    @Test
    fun `speech engine failure does not stop recorded playback`() {
        val service = service(usingTts = false)
        service.onInit(TextToSpeech.ERROR)
        verify(service, never()).stopSelf()
    }

    @Test
    fun `speech engine failure ends waiting speech request`() {
        val service = service(usingTts = true)
        service.onInit(TextToSpeech.ERROR)
        verify(service).stopSelf()
    }

    @Test
    fun `speech requested after failed initialization does not wait forever`() {
        val service = service(usingTts = false)
        service.onInit(TextToSpeech.ERROR)
        ReminderPlaybackService::class.java.getDeclaredMethod(
            "speakTts", String::class.java, String::class.java, java.lang.Long.TYPE
        ).apply {
            isAccessible = true
            invoke(service, "Take a break", "Reminder", 1L)
        }
        verify(service).stopSelf()
    }
}
