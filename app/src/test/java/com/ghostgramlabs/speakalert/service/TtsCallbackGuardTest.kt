package com.ghostgramlabs.speakalert.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsCallbackGuardTest {
    @Test fun `old completion cannot stop a replacement recording`() = runTest {
        val events = mutableListOf<String>()
        val guard = TtsCallbackGuard(this, { events.add("done") }, { events.add("error") })
        val speech = guard.begin()
        guard.complete(speech)
        guard.invalidate() // Recording replaces speech before its queued callback runs.
        runCurrent()
        assertEquals(emptyList<String>(), events)
    }

    @Test fun `old error and completion cannot affect a newer speech request`() = runTest {
        val events = mutableListOf<String>()
        val guard = TtsCallbackGuard(this, { events.add("done") }, { events.add("error") })
        val old = guard.begin()
        val current = guard.begin()
        guard.complete(old, failed = true)
        guard.complete(old)
        runCurrent()
        assertEquals(emptyList<String>(), events)
        guard.complete(current)
        runCurrent()
        assertEquals(listOf("done"), events)
    }

    @Test fun `current failure terminates once and duplicate callbacks do nothing`() = runTest {
        val events = mutableListOf<String>()
        val guard = TtsCallbackGuard(this, { events.add("done") }, { events.add("error") })
        val id = guard.begin()
        guard.complete(id, failed = true)
        guard.complete(id)
        guard.complete(null, failed = true)
        runCurrent()
        assertEquals(listOf("error"), events)
    }
}
