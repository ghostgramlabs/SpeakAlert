package com.ghostgramlabs.speakalert.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.EndRuleType
import com.ghostgramlabs.speakalert.domain.models.MissedPolicy
import com.ghostgramlabs.speakalert.domain.models.MonthlyVariant
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.domain.models.TimeUnit
import com.ghostgramlabs.speakalert.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Localized, Composable equivalents of the string-building helpers in [DateUtils] and
 * [RecurrenceUtils]. Those domain utilities stay in English (they are covered by unit tests and
 * used off the main thread / by widgets); the UI uses these versions so recurrence summaries and
 * relative date labels are shown in the selected language.
 */

@Composable
fun localizedDateLabel(timestamp: Long): String {
    val nowYear = Calendar.getInstance().get(Calendar.YEAR)
    val targetYear = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
    return when (DateUtils.dayDifference(System.currentTimeMillis(), timestamp)) {
        0 -> stringResource(R.string.date_today)
        1 -> stringResource(R.string.date_tomorrow)
        -1 -> stringResource(R.string.date_yesterday)
        else -> {
            val pattern = if (nowYear != targetYear) "MMM d, yyyy" else "MMM d"
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
        }
    }
}

@Composable
fun localizedRecurrenceSummary(
    type: RecurrenceType,
    json: String?,
    nextTriggerAt: Long,
    includeEndRule: Boolean = false,
    includeMissedPolicy: Boolean = false,
    includeTime: Boolean = true
): String {
    val timeStr = if (includeTime) {
        " • " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nextTriggerAt))
    } else {
        ""
    }

    if (type == RecurrenceType.NONE) {
        return stringResource(R.string.rec_onetime) + " • " + localizedDateLabel(nextTriggerAt)
    }

    val dayNames = listOf(
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat),
        stringResource(R.string.day_sun)
    )

    val model = RecurrenceUtils.fromJson(type, json) ?: when (type) {
        RecurrenceType.YEARLY -> RecurrenceModel.Yearly()
        else -> RecurrenceModel.Daily()
    }

    val sb = StringBuilder()
    when (model) {
        is RecurrenceModel.Daily -> sb.append(stringResource(R.string.rec_daily))
        is RecurrenceModel.Weekly -> {
            val days = model.daysOfWeek.sorted().joinToString(", ") { dayNames.getOrElse(it - 1) { "" } }
            sb.append(stringResource(R.string.rec_weekly)).append(" • ").append(days)
        }
        is RecurrenceModel.Monthly -> {
            sb.append(stringResource(R.string.rec_monthly)).append(" • ")
            if (model.variant == MonthlyVariant.LAST_DAY) {
                sb.append(stringResource(R.string.rs_last_day))
            } else {
                val days = if (model.daysOfMonth.isEmpty()) listOf(1) else model.daysOfMonth.sorted()
                sb.append(days.joinToString(", "))
            }
        }
        is RecurrenceModel.Yearly -> {
            val yearlyDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(nextTriggerAt))
            sb.append(stringResource(R.string.rec_yearly)).append(" • ").append(yearlyDate)
        }
        is RecurrenceModel.Custom -> {
            val unit = summaryUnitLabel(model.unit, model.interval)
            sb.append(stringResource(R.string.rec_every, model.interval, unit))
        }
    }

    sb.append(timeStr)

    if (includeEndRule) {
        when (model.endRule.type) {
            EndRuleType.NEVER -> sb.append("\n").append(stringResource(R.string.rec_ends_never))
            EndRuleType.UNTIL_DATE -> {
                val endDate = model.endRule.endDateMillis ?: 0L
                val dateStr = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    .format(Date(endDate))
                sb.append("\n").append(stringResource(R.string.det_ends_by, dateStr))
            }
            EndRuleType.AFTER_OCCURRENCES -> {
                val count = model.endRule.count ?: 0
                sb.append("\n").append(pluralStringResource(R.plurals.rec_ends_times, count, count))
            }
        }
    }

    if (includeMissedPolicy) {
        when (model.missedPolicy) {
            MissedPolicy.FIRE_ON_RESUME -> sb.append("\n").append(stringResource(R.string.rec_if_off_fire))
            MissedPolicy.SKIP_TO_NEXT -> sb.append("\n").append(stringResource(R.string.rec_if_off_skip))
        }
    }

    return sb.toString()
}

@Composable
private fun summaryUnitLabel(unit: TimeUnit, count: Int): String {
    val label = when (unit) {
        TimeUnit.MINUTES -> pluralStringResource(R.plurals.unit_minutes, count)
        TimeUnit.HOURS -> pluralStringResource(R.plurals.unit_hours, count)
        TimeUnit.DAYS -> pluralStringResource(R.plurals.unit_days, count)
        TimeUnit.WEEKS -> pluralStringResource(R.plurals.unit_weeks, count)
        TimeUnit.MONTHS -> pluralStringResource(R.plurals.unit_months, count)
        TimeUnit.YEARS -> pluralStringResource(R.plurals.unit_years, count)
    }
    return label.lowercase(Locale.getDefault())
}
