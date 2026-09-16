package com.ghostgramlabs.speakalert.data.repository

import org.junit.Assert.*
import org.junit.Test

class ReviewPromptPolicyTest {
    private val day = ReviewUsage.DAY
    private val start = 100 * day
    private val experienced = ReviewUsage()
        .recordDelivery(start)
        .recordDelivery(start + day)
        .recordDelivery(start + 2 * day)

    @Test fun `fresh installs and repeated app opens cannot qualify`() {
        assertFalse(ReviewUsage().eligible(start + 100 * day))
    }

    @Test fun `many deliveries on one day do not qualify`() {
        var usage = ReviewUsage()
        repeat(100) { usage = usage.recordDelivery(start + it * 60_000L) }
        assertEquals(1, usage.deliveryDays)
        assertFalse(usage.eligible(start + 7 * day))
    }

    @Test fun `three delivery days still require a week of experience`() {
        assertFalse(experienced.eligible(start + 7 * day - 1))
        assertTrue(experienced.eligible(start + 7 * day))
    }

    @Test fun `recent deliveries postpone the prompt even after a week`() {
        val now = start + 8 * day
        val usage = experienced.recordDelivery(now)
        assertFalse(usage.eligible(now + 599_999L))
        assertTrue(usage.eligible(now + 600_000L))
    }

    @Test fun `dismissal cooldown lasts sixty days`() {
        val promptedAt = start + 8 * day
        val usage = experienced.copy(lastPromptAt = promptedAt)
        assertFalse(usage.eligible(promptedAt + 60 * day - 1))
        assertTrue(usage.eligible(promptedAt + 60 * day))
    }

    @Test fun `legacy no thanks or rate now decision remains respected`() {
        assertFalse(experienced.copy(decided = true).eligible(start + 500 * day))
    }

    @Test fun `opening the store listing pauses rather than ends the prompt`() {
        val ratedAt = start + 8 * day
        val usage = experienced.copy(lastPromptAt = ratedAt, ratedAt = ratedAt)
        val cooldown = ReviewUsage.RATED_COOLDOWN_DAYS * day
        assertFalse(usage.eligible(ratedAt + cooldown - 1))
        assertTrue(usage.eligible(ratedAt + cooldown))
    }

    @Test fun `rating cooldown outlasts the ordinary dismissal cooldown`() {
        val ratedAt = start + 8 * day
        val usage = experienced.copy(lastPromptAt = ratedAt, ratedAt = ratedAt)
        // 60-day dismissal window has passed, but the rating pause has not.
        assertFalse(usage.eligible(ratedAt + 61 * day))
    }

    @Test fun `a clock moved backwards cannot shorten the rating pause`() {
        val usage = experienced.copy(ratedAt = start + 20 * day)
        assertFalse(usage.eligible(start + 10 * day))
    }

    @Test fun `only a sounding pinned or fresh alert counts as active`() {
        val now = start + 8 * day
        val stale = AlertNotification(postedAt = now - day, ongoing = false)
        val pinned = AlertNotification(postedAt = now - day, ongoing = true)
        val fresh = AlertNotification(postedAt = now - 60_000L, ongoing = false)

        assertFalse(hasActiveAlert(emptyList(), now))
        // The case that used to block the prompt for everyone: yesterday's un-swiped reminder.
        assertFalse(hasActiveAlert(listOf(stale), now))
        assertTrue(hasActiveAlert(listOf(pinned), now))
        assertTrue(hasActiveAlert(listOf(fresh), now))
        assertTrue(hasActiveAlert(listOf(stale, fresh), now))
    }

    @Test fun `a notification posted in the future does not count as active`() {
        val now = start + 8 * day
        assertFalse(hasActiveAlert(listOf(AlertNotification(now + day, ongoing = false)), now))
    }

    @Test fun `only recent misses suppress the prompt`() {
        val now = start + 30 * day
        assertFalse(hasRecentMiss(emptyList(), now))
        assertFalse(hasRecentMiss(listOf(now - 10 * day), now))
        assertTrue(hasRecentMiss(listOf(now - 60_000L), now))
        assertTrue(hasRecentMiss(listOf(now - day), now))
        assertFalse(hasRecentMiss(listOf(now - day - 1), now))
    }

    @Test fun `clock moving backwards cannot bypass age or cooldown`() {
        assertFalse(experienced.eligible(start - day))
        assertFalse(experienced.copy(lastPromptAt = start + 20 * day).eligible(start + 10 * day))
        assertEquals(experienced, experienced.recordDelivery(start))
    }

    @Test fun `duplicate delivery and later usage preserve first experience`() {
        assertEquals(experienced, experienced.recordDelivery(start + 2 * day))
        val later = experienced.recordDelivery(start + 10 * day)
        assertEquals(start, later.firstDeliveryAt)
        assertEquals(3, later.deliveryDays)
    }
}
