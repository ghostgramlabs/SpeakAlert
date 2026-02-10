package com.ghostgramlabs.speakalert.domain.models

enum class MonthlyVariant {
    DAY_OF_MONTH,
    LAST_DAY
}

enum class TimeUnit {
    MINUTES, HOURS, DAYS, WEEKS, MONTHS
}

enum class EndRuleType {
    NEVER, UNTIL_DATE, AFTER_OCCURRENCES
}

data class RecurrenceEndRule(
    val type: EndRuleType = EndRuleType.NEVER,
    val endDateMillis: Long? = null,
    val count: Int? = null
)

enum class MissedPolicy {
    FIRE_ON_RESUME,
    SKIP_TO_NEXT
}

/**
 * Holds the detailed configuration for a recurrence.
 * This object is serialized to JSON in the database.
 */
sealed class RecurrenceModel {
    abstract val endRule: RecurrenceEndRule
    abstract val missedPolicy: MissedPolicy

    data class Daily(
        override val endRule: RecurrenceEndRule = RecurrenceEndRule(),
        override val missedPolicy: MissedPolicy = MissedPolicy.SKIP_TO_NEXT
    ) : RecurrenceModel()

    data class Weekly(
        val daysOfWeek: Set<Int>, // 1 = Monday, 7 = Sunday
        override val endRule: RecurrenceEndRule = RecurrenceEndRule(),
        override val missedPolicy: MissedPolicy = MissedPolicy.SKIP_TO_NEXT
    ) : RecurrenceModel()

    data class Monthly(
        val variant: MonthlyVariant,
        val daysOfMonth: Set<Int> = emptySet(), // 1-31, used if variant is DAY_OF_MONTH
        override val endRule: RecurrenceEndRule = RecurrenceEndRule(),
        override val missedPolicy: MissedPolicy = MissedPolicy.SKIP_TO_NEXT
    ) : RecurrenceModel()

    data class Custom(
        val interval: Int,
        val unit: TimeUnit,
        override val endRule: RecurrenceEndRule = RecurrenceEndRule(),
        override val missedPolicy: MissedPolicy = MissedPolicy.SKIP_TO_NEXT
    ) : RecurrenceModel()
}
