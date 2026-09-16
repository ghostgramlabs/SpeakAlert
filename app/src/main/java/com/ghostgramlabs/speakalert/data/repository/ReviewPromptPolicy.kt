package com.ghostgramlabs.speakalert.data.repository

/** Usage means a normal reminder notification was delivered, not that audio was heard. */
internal data class ReviewUsage(
    val firstDeliveryAt: Long = 0,
    val lastDeliveryAt: Long = 0,
    val deliveryDays: Int = 0,
    val lastPromptAt: Long = 0,
    /**
     * When the user last chose to rate. Opening the store listing is an intent, not a completed
     * review - people get distracted or come back - so it earns a long pause rather than a
     * permanent stop. Only an explicit "No thanks" sets [decided].
     */
    val ratedAt: Long = 0,
    val decided: Boolean = false
) {
    fun recordDelivery(now: Long): ReviewUsage {
        if (now <= lastDeliveryAt) return this
        // UTC day buckets keep repeated alarms on the same day from inflating experience.
        val newDay = lastDeliveryAt == 0L || now / DAY > lastDeliveryAt / DAY
        return copy(
            firstDeliveryAt = firstDeliveryAt.takeIf { it > 0 } ?: now,
            lastDeliveryAt = now,
            deliveryDays = if (newDay) (deliveryDays + 1).coerceAtMost(3) else deliveryDays
        )
    }

    fun eligible(now: Long): Boolean =
        !decided && deliveryDays >= 3 && firstDeliveryAt > 0 &&
            now >= firstDeliveryAt && now - firstDeliveryAt >= 7 * DAY &&
            now >= lastDeliveryAt && now - lastDeliveryAt >= 10 * 60_000L &&
            (lastPromptAt == 0L || (now >= lastPromptAt && now - lastPromptAt >= 60 * DAY)) &&
            (ratedAt == 0L || (now >= ratedAt && now - ratedAt >= RATED_COOLDOWN_DAYS * DAY))

    companion object {
        const val DAY = 86_400_000L
        /** Long enough that someone who did review is not asked again about the same app. */
        const val RATED_COOLDOWN_DAYS = 180L
    }
}

/**
 * One reminder notification currently in the shade. Reminder alerts deliberately stay posted until
 * the user acts on them ([NotificationHelper] sets autoCancel false), so their mere presence says
 * nothing about whether the user is busy right now.
 */
internal data class AlertNotification(val postedAt: Long, val ongoing: Boolean)

/**
 * True when a reminder is competing for attention: still sounding/pinned, or posted moments ago.
 * A day-old notification the user simply never swiped away does not count.
 */
internal fun hasActiveAlert(
    notifications: List<AlertNotification>,
    now: Long,
    recentWindowMs: Long = 10 * 60_000L
): Boolean = notifications.any { it.ongoing || (now - it.postedAt) in 0..recentWindowMs }

/**
 * True when something was missed recently enough that the user may still be annoyed by it.
 * Old entries left sitting in the Missed tab are not a reason to never ask again.
 */
internal fun hasRecentMiss(
    detectedTimes: List<Long>,
    now: Long,
    windowMs: Long = ReviewUsage.DAY
): Boolean = detectedTimes.any { (now - it) in 0..windowMs }
