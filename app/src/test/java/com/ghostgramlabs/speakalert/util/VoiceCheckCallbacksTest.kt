package com.ghostgramlabs.speakalert.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VoiceCheckCallbacksTest {
    @Test fun `late callbacks cannot complete the next probe`() = runBlocking {
        val callbacks = VoiceCheckCallbacks()
        callbacks.begin("timed-out")
        callbacks.clear()
        val current = callbacks.begin("current")
        callbacks.complete("timed-out", true)
        callbacks.complete("timed-out", false)
        callbacks.complete(null, true)
        assertFalse(current.isCompleted)
        callbacks.complete("current", false)
        assertFalse(current.await())
    }

    @Test fun `completion is accepted once and cleared probes ignore callbacks`() = runBlocking {
        val callbacks = VoiceCheckCallbacks()
        val first = callbacks.begin("first")
        callbacks.complete("first", true)
        callbacks.complete("first", false)
        assertTrue(first.await())
        val cancelled = callbacks.begin("cancelled")
        callbacks.clear()
        callbacks.complete("cancelled", true)
        assertFalse(cancelled.isCompleted)
    }
}
