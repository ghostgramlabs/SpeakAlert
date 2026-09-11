package com.ghostgramlabs.speakalert.data.repository

/** Usage means a normal reminder notification was delivered, not that audio was heard. */
internal data class ReviewUsage(
    val firstDeliveryAt: Long = 0,
    val lastDeliveryAt: Long = 0,
    val deliveryDays: Int = 0,
    val lastPromptAt: Long = 0,
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
            (lastPromptAt == 0L || (now >= lastPromptAt && now - lastPromptAt >= 60 * DAY))

    companion object {
        const val DAY = 86_400_000L
    }
}
