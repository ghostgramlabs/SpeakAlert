package com.ghostgramlabs.speakalert.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackTimeoutTest {
    @Test
    fun `loop stops at configured timeout`() = runTest {
        val stops = mutableListOf<Int>()
        val timeout = PlaybackTimeout(this, { 1 }, stops::add)
        timeout.restart(true)
        advanceTimeBy(59_999)
        assertTrue(stops.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(1), stops)
    }

    @Test
    fun `non looping replacement cancels old timeout`() = runTest {
        val stops = mutableListOf<Int>()
        val timeout = PlaybackTimeout(this, { 1 }, stops::add)
        timeout.restart(true)
        advanceTimeBy(30_000)
        timeout.restart(false)
        advanceTimeBy(120_000)
        runCurrent()
        assertTrue(stops.isEmpty())
    }

    @Test
    fun `never ending replacement cancels old timeout`() = runTest {
        var minutes = 1
        val stops = mutableListOf<Int>()
        val timeout = PlaybackTimeout(this, { minutes }, stops::add)
        timeout.restart(true)
        advanceTimeBy(30_000)
        minutes = 0
        timeout.restart(true)
        advanceTimeBy(120_000)
        runCurrent()
        assertTrue(stops.isEmpty())
    }

    @Test
    fun `new loop gets its full duration`() = runTest {
        val stops = mutableListOf<Int>()
        val timeout = PlaybackTimeout(this, { 1 }, stops::add)
        timeout.restart(true)
        advanceTimeBy(30_000)
        timeout.restart(true)
        advanceTimeBy(30_000)
        runCurrent()
        assertTrue(stops.isEmpty())
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(listOf(1), stops)
    }

    @Test
    fun `replaced pending settings read cannot install an old timer`() = runTest {
        val setting = CompletableDeferred<Int>()
        val stops = mutableListOf<Int>()
        val timeout = PlaybackTimeout(this, { setting.await() }, stops::add)
        timeout.restart(true)
        runCurrent()
        timeout.restart(false)
        setting.complete(1)
        advanceTimeBy(120_000)
        runCurrent()
        assertTrue(stops.isEmpty())
    }

    @Test
    fun `service cleanup cancels timeout`() = runTest {
        val stops = mutableListOf<Int>()
        val timeout = PlaybackTimeout(this, { 1 }, stops::add)
        timeout.restart(true)
        runCurrent()
        timeout.cancel()
        advanceTimeBy(120_000)
        runCurrent()
        assertTrue(stops.isEmpty())
    }
}
