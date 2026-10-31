package com.ghostgramlabs.speakalert.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoopGapTest {
    @Test
    fun `no pause replays immediately`() = runTest {
        var replays = 0
        val gap = LoopGap(this) { 0 }
        gap.schedule { replays++ }
        runCurrent()
        assertEquals(1, replays)
    }

    @Test
    fun `replay waits for configured pause`() = runTest {
        var replays = 0
        val gap = LoopGap(this) { 10 }
        gap.schedule { replays++ }
        advanceTimeBy(9_999)
        runCurrent()
        assertEquals(0, replays)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, replays)
    }

    @Test
    fun `cancel during pause drops the replay`() = runTest {
        var replays = 0
        val gap = LoopGap(this) { 10 }
        gap.schedule { replays++ }
        advanceTimeBy(5_000)
        gap.cancel()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(0, replays)
    }

    @Test
    fun `rescheduling replaces the pending replay`() = runTest {
        val replays = mutableListOf<String>()
        val gap = LoopGap(this) { 10 }
        gap.schedule { replays += "old" }
        advanceTimeBy(5_000)
        gap.schedule { replays += "new" }
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(listOf("new"), replays)
    }
}
