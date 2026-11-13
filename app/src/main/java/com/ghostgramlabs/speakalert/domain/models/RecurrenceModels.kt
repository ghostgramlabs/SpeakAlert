package com.ghostgramlabs.speakalert.domain.models

enum class MonthlyVariant {
    DAY_OF_MONTH,
    LAST_DAY,
    /** A weekday by its position in the month, such as the last Thursday. */
    DAY_OF_WEEK
}

/** [RecurrenceModel.Monthly.weekOfMonth] value for the last such weekday of the month. */
const val LAST_WEEK_OF_MONTH = -1

enum class TimeUnit {
    MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS
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
        // Used if variant is DAY_OF_WEEK: 1-4 = first..fourth, LAST_WEEK_OF_MONTH = last
        val weekOfMonth: Int = 0,
        val dayOfWeek: Int = 0, // 1 = Monday, 7 = Sunday, as in Weekly
        override val endRule: RecurrenceEndRule = RecurrenceEndRule(),
        override val missedPolicy: MissedPolicy = MissedPolicy.SKIP_TO_NEXT
    ) : RecurrenceModel()

    data class Yearly(
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
