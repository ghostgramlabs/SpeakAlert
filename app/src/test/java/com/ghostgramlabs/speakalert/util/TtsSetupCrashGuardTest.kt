package com.ghostgramlabs.speakalert.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsSetupCrashGuardTest {
    private fun frame(cls: String, method: String) = StackTraceElement(cls, method, "X.java", 1)

    @Test
    fun `recognizes the framework speech setup failure`() {
        // Shape of the Android 10 report: AsyncTask wraps the engine's exception.
        val cause = IllegalStateException("Task was cancelled.").apply {
            stackTrace = arrayOf(
                frame("android.speech.tts.ITextToSpeechService\$Stub\$Proxy", "getDefaultVoiceNameFor"),
                frame("android.speech.tts.TextToSpeech\$Connection\$SetupConnectionAsyncTask", "doInBackground"),
                frame("android.os.AsyncTask\$3", "call")
            )
        }
        val wrapper = RuntimeException("An error occurred while executing doInBackground()", cause).apply {
            stackTrace = arrayOf(frame("android.os.AsyncTask\$4", "done"))
        }
        assertTrue(TtsSetupCrashGuard.isTtsSetupFailure(wrapper))
    }

    @Test
    fun `leaves every other crash alone`() {
        val ours = IllegalStateException("boom").apply {
            stackTrace = arrayOf(
                frame("com.ghostgramlabs.speakalert.service.ReminderPlaybackService", "speakNow"),
                frame("android.os.AsyncTask\$3", "call")
            )
        }
        assertFalse(TtsSetupCrashGuard.isTtsSetupFailure(RuntimeException(ours)))
        assertFalse(TtsSetupCrashGuard.isTtsSetupFailure(NullPointerException()))
    }
}
