package com.ghostgramlabs.speakalert.domain

import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.domain.models.*
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.*
import com.ghostgramlabs.speakalert.util.DateUtils
import com.google.gson.Gson
import java.util.Calendar

object RecurrenceUtils {

    private val gson = Gson()

    fun fromJson(type: RecurrenceType, json: String?): RecurrenceModel? {
        com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: fromJson type=$type json=$json")
        if (json.isNullOrBlank()) {
             if (type == RecurrenceType.DAILY) return RecurrenceModel.Daily()
             com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: JSON is blank for type=$type, returning null")
             return null
        }
        return try {
            val model = when (type) {
                RecurrenceType.DAILY -> gson.fromJson(json, RecurrenceModel.Daily::class.java)
                RecurrenceType.WEEKLY -> gson.fromJson(json, RecurrenceModel.Weekly::class.java)
                RecurrenceType.MONTHLY -> gson.fromJson(json, RecurrenceModel.Monthly::class.java)
                RecurrenceType.CUSTOM -> gson.fromJson(json, RecurrenceModel.Custom::class.java)
                else -> null
            }
            if (model == null) {
                com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Parsed model is NULL for $json")
            }
            model
        } catch (e: Exception) {
            com.ghostgramlabs.speakalert.util.FileLogger.logError("Recurrence", "Json parse failed: $json", e)
            e.printStackTrace()
            null
        }
    }

    fun toJson(model: RecurrenceModel): String {
        val json = gson.toJson(model)
        com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: toJson($model) -> $json")
        return json
    }

    fun computeNextTrigger(reminder: ReminderEntity, fromTime: Long = System.currentTimeMillis()): Long? {
        if (reminder.recurrenceType == RecurrenceType.NONE) return null

        val model = fromJson(reminder.recurrenceType, reminder.recurrenceJson) ?: run {
            com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: computeNextTrigger failed - invalid JSON for ${reminder.recurrenceType}")
            // Fallback for Daily if JSON missing
            if (reminder.recurrenceType == RecurrenceType.DAILY) RecurrenceModel.Daily() else null
        } ?: return null

        // Calculate next basic trigger
        var nextTrigger = when (model) {
            is RecurrenceModel.Daily -> computeNextDaily(reminder.nextTriggerAt, fromTime)
            is RecurrenceModel.Weekly -> computeNextWeekly(reminder.nextTriggerAt, model, fromTime)
            is RecurrenceModel.Monthly -> computeNextMonthly(reminder.nextTriggerAt, model, fromTime)
            is RecurrenceModel.Custom -> computeNextCustom(reminder.nextTriggerAt, model, fromTime)
        }
        
        com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Computed raw next=$nextTrigger for $model")

        // CRITICAL FIX: Evaluate the new candidate against the end rules
        if (nextTrigger != null && isTimeEnded(model, nextTrigger)) {
            com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Candidate $nextTrigger exceeded END RULE. Ending recurrence.")
            return null
        }
        
        // SAFETY: Ensure nextTrigger is strictly > fromTime (or at least > now).
        // If the calculation returns something in the past/present (due to loose math or exact match),
        // we must advance it again to avoid infinite alarm loops.
        // This handles the case where "Every 1 Minute" calculation might land on "Now" exactly.
        
        var attempts = 0
        while (nextTrigger != null && nextTrigger <= fromTime && attempts < 50) {
             com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Result $nextTrigger <= $fromTime. Advancing again...")
             nextTrigger = when (model) {
                is RecurrenceModel.Daily -> computeNextDaily(nextTrigger, fromTime + 1000)
                is RecurrenceModel.Weekly -> computeNextWeekly(nextTrigger, model, fromTime + 1000)
                is RecurrenceModel.Monthly -> computeNextMonthly(nextTrigger, model, fromTime + 1000)
                is RecurrenceModel.Custom -> computeNextCustom(nextTrigger, model, fromTime + 1000)
            }
            
            if (nextTrigger != null && isTimeEnded(model, nextTrigger)) {
                com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Advanced candidate $nextTrigger exceeded END RULE.")
                return null
            }
            attempts++
        }
        
        if (nextTrigger == null || nextTrigger <= fromTime) {
             com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: FAILED to find future time. End or default.")
             return if (nextTrigger == null) null else System.currentTimeMillis() + 60000
        }
        
        return nextTrigger
    }

    private fun isTimeEnded(model: RecurrenceModel, candidateTime: Long): Boolean {
        val end = model.endRule
        return when (end.type) {
            EndRuleType.NEVER -> false
            EndRuleType.UNTIL_DATE -> candidateTime > (end.endDateMillis ?: Long.MAX_VALUE)
            EndRuleType.AFTER_OCCURRENCES -> (end.count ?: 0) <= 0
        }
    }

    private fun isEnded(model: RecurrenceModel, reminder: ReminderEntity): Boolean {
        return isTimeEnded(model, System.currentTimeMillis())
    }

    // --- DAILY ---
    private fun computeNextDaily(baseTrigger: Long, fromTime: Long): Long {
        val targetCal = Calendar.getInstance().apply { timeInMillis = baseTrigger }
        val nowCal = Calendar.getInstance().apply { timeInMillis = fromTime }

        val candidate = nowCal.clone() as Calendar
        candidate.set(Calendar.HOUR_OF_DAY, targetCal.get(Calendar.HOUR_OF_DAY))
        candidate.set(Calendar.MINUTE, targetCal.get(Calendar.MINUTE))
        candidate.set(Calendar.SECOND, 0)
        candidate.set(Calendar.MILLISECOND, 0)

        // If today's slot is past, move to tomorrow
        if (candidate.timeInMillis <= fromTime) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }

    // --- WEEKLY ---
    private fun computeNextWeekly(baseTrigger: Long, rule: RecurrenceModel.Weekly, fromTime: Long): Long {
        val targetCal = Calendar.getInstance().apply { timeInMillis = baseTrigger }
        val nowCal = Calendar.getInstance().apply { timeInMillis = fromTime }
        
        // Check next 7 days
        for (i in 0..7) {
            val candidate = nowCal.clone() as Calendar
            if (i > 0) candidate.add(Calendar.DAY_OF_YEAR, i)
            
            candidate.set(Calendar.HOUR_OF_DAY, targetCal.get(Calendar.HOUR_OF_DAY))
            candidate.set(Calendar.MINUTE, targetCal.get(Calendar.MINUTE))
            candidate.set(Calendar.SECOND, 0)
            candidate.set(Calendar.MILLISECOND, 0)

            // Convert Calendar Day (Sun=1) to Model Day (Mon=1, Sun=7)??
            // Or just stick to Calendar constants if Model uses them. 
            // Model says "1=Monday".
            // Calendar: Sun=1, Mon=2.
            // Map:
            val calDay = candidate.get(Calendar.DAY_OF_WEEK)
            val modelDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
            
            if (rule.daysOfWeek.contains(modelDay)) {
                if (candidate.timeInMillis > fromTime) {
                    return candidate.timeInMillis
                }
            }
        }
        // Failsafe
        return fromTime + 86400000L
    }

    // --- MONTHLY ---
    private fun computeNextMonthly(baseTrigger: Long, rule: RecurrenceModel.Monthly, fromTime: Long): Long {
        val targetCal = Calendar.getInstance().apply { timeInMillis = baseTrigger }
        // targetCal provides the TIME. rule provides the DATE logic.
        
        var searchCal = Calendar.getInstance().apply { timeInMillis = fromTime }
        searchCal.set(Calendar.HOUR_OF_DAY, targetCal.get(Calendar.HOUR_OF_DAY))
        searchCal.set(Calendar.MINUTE, targetCal.get(Calendar.MINUTE))
        searchCal.set(Calendar.SECOND, 0)
        searchCal.set(Calendar.MILLISECOND, 0)

        // Check up to 24 months ahead (to be safe)
        for (monthOffset in 0..24) {
            val temp = searchCal.clone() as Calendar
            temp.set(Calendar.DAY_OF_MONTH, 1) // Reset to 1st
            temp.add(Calendar.MONTH, monthOffset)
            
            val maxDayInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            if (rule.variant == MonthlyVariant.LAST_DAY) {
                // Last day of month
                temp.set(Calendar.DAY_OF_MONTH, maxDayInMonth)
                if (temp.timeInMillis > fromTime) {
                    return temp.timeInMillis
                }
            } else {
                // DAY_OF_MONTH - check all specified days in sorted order
                val days = if (rule.daysOfMonth.isEmpty()) {
                    // Fallback: use the original trigger day
                    setOf(targetCal.get(Calendar.DAY_OF_MONTH))
                } else {
                    rule.daysOfMonth
                }
                
                for (day in days.sorted()) {
                    val clampedDay = if (day > maxDayInMonth) maxDayInMonth else day
                    temp.set(Calendar.DAY_OF_MONTH, clampedDay)
                    
                    if (temp.timeInMillis > fromTime) {
                        return temp.timeInMillis
                    }
                }
            }
        }
        
        return fromTime + 86400000L // Fallback: +1 day
    }

    // --- CUSTOM ---
    private fun computeNextCustom(baseTrigger: Long, rule: RecurrenceModel.Custom, fromTime: Long): Long {
        com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Custom calc. Base=${java.util.Date(baseTrigger)}, From=${java.util.Date(fromTime)}, Rule=$rule")
        
        if (rule.unit == TimeUnit.MONTHS || rule.unit == TimeUnit.DAYS || rule.unit == TimeUnit.WEEKS) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = baseTrigger
            
            // For DAYS/WEEKS/MONTHS we want to preserve the wall-clock time
            // So we use Calendar addition until we pass fromTime
            
            val field = when (rule.unit) {
                TimeUnit.DAYS -> Calendar.DAY_OF_YEAR
                TimeUnit.WEEKS -> Calendar.WEEK_OF_YEAR
                TimeUnit.MONTHS -> Calendar.MONTH
                else -> Calendar.DAY_OF_YEAR // Should not happen
            }
            
            // Safety check for invalid interval to prevent infinite loop
            val safeInterval = if (rule.interval < 1) 1 else rule.interval
            
            while (cal.timeInMillis <= fromTime) {
                cal.add(field, safeInterval)
            }
            com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Custom (Cal) result=${java.util.Date(cal.timeInMillis)}")
            return cal.timeInMillis
        } else {
            // Hours / Minutes - Millis math is fine (and often desired for strict interval)
            val intervalMillis = when (rule.unit) {
                TimeUnit.MINUTES -> rule.interval * 60 * 1000L
                TimeUnit.HOURS -> rule.interval * 3600 * 1000L
                else -> 0L // Handled above
            }
            
            // Safety check
            if (intervalMillis <= 0) {
                 com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Invalid interval millis $intervalMillis")
                 return fromTime + 60000 // Failsafe 1 min
            }
            
            var nextTime = baseTrigger
            if (nextTime > fromTime) {
                com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Base is future, keeping ${java.util.Date(nextTime)}")
                return nextTime
            }
            
            val diff = fromTime - nextTime
            if (diff >= 0) {
                 val jumps = (diff / intervalMillis) + 1
                 nextTime += jumps * intervalMillis
            }
            com.ghostgramlabs.speakalert.util.FileLogger.log("Recurrence: Custom (Millis) result=${java.util.Date(nextTime)}")
            return nextTime
        }
    }

    /**
     * Generates a human-readable summary of the recurrence.
     */
    fun getRecurrenceSummary(
        type: RecurrenceType,
        json: String?,
        nextTriggerAt: Long,
        includeEndRule: Boolean = false,
        includeMissedPolicy: Boolean = false
    ): String {
        val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(nextTriggerAt))
        
        if (type == RecurrenceType.NONE) {
            val relativeDate = DateUtils.formatRelativeTime(nextTriggerAt)
            return "One-time • $relativeDate"
        }

        val model = fromJson(type, json) ?: RecurrenceModel.Daily() // Default fallback
        val sb = StringBuilder()

        when (model) {
            is RecurrenceModel.Daily -> {
                sb.append("Daily")
            }
            is RecurrenceModel.Weekly -> {
                val daysStr = model.daysOfWeek.sorted().joinToString(", ") { day ->
                    when (day) {
                        1 -> "Mon"
                        2 -> "Tue"
                        3 -> "Wed"
                        4 -> "Thu"
                        5 -> "Fri"
                        6 -> "Sat"
                        7 -> "Sun"
                        else -> ""
                    }
                }
                sb.append("Weekly • $daysStr")
            }
            is RecurrenceModel.Monthly -> {
                sb.append("Monthly • ")
                if (model.variant == MonthlyVariant.LAST_DAY) {
                    sb.append("Last day")
                } else {
                    val days = if (model.daysOfMonth.isEmpty()) listOf(1) else model.daysOfMonth.sorted()
                    val daysStr = days.joinToString(", ") { d ->
                        val suffix = when {
                            d in 11..13 -> "th"
                            d % 10 == 1 -> "st"
                            d % 10 == 2 -> "nd"
                            d % 10 == 3 -> "rd"
                            else -> "th"
                        }
                        "$d$suffix"
                    }
                    sb.append(daysStr)
                }
            }
            is RecurrenceModel.Custom -> {
                val unitStr = when (model.unit) {
                    TimeUnit.MINUTES -> if (model.interval == 1) "minute" else "minutes"
                    TimeUnit.HOURS -> if (model.interval == 1) "hour" else "hours"
                    TimeUnit.DAYS -> if (model.interval == 1) "day" else "days"
                    TimeUnit.WEEKS -> if (model.interval == 1) "week" else "weeks"
                    TimeUnit.MONTHS -> if (model.interval == 1) "month" else "months"
                }
                sb.append("Every ${model.interval} $unitStr")
            }
        }

        sb.append(" • $timeStr")

        if (includeEndRule) {
             when (model.endRule.type) {
                 EndRuleType.NEVER -> sb.append("\nEnds: Never")
                 EndRuleType.UNTIL_DATE -> {
                     val endDate = model.endRule.endDateMillis ?: 0L
                     val dateStr = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(endDate))
                     sb.append("\nEnds on $dateStr")
                 }
                 EndRuleType.AFTER_OCCURRENCES -> sb.append("\nEnds after ${model.endRule.count ?: 0} times")
             }
        }

        if (includeMissedPolicy) {
            when (model.missedPolicy) {
                MissedPolicy.FIRE_ON_RESUME -> sb.append("\nIf missed: Fire when back")
                MissedPolicy.SKIP_TO_NEXT -> sb.append("\nIf missed: Skip missed")
            }
        }

        return sb.toString()
    }
    
    fun updateForNextOccurrence(reminder: ReminderEntity): ReminderEntity {
        if (reminder.recurrenceType == RecurrenceType.NONE) return reminder

        val model = fromJson(reminder.recurrenceType, reminder.recurrenceJson) ?: return reminder
        
        val end = model.endRule
        val newModel = if (end.type == EndRuleType.AFTER_OCCURRENCES) {
            val count = end.count ?: 0
            val newCount = count - 1
            // Ensure count doesn't go below 0, though isEnded handles it
            val finalCount = if (newCount < 0) 0 else newCount
            val newEnd = end.copy(count = finalCount)
            when (model) {
                is RecurrenceModel.Daily -> model.copy(endRule = newEnd)
                is RecurrenceModel.Weekly -> model.copy(endRule = newEnd)
                is RecurrenceModel.Monthly -> model.copy(endRule = newEnd)
                is RecurrenceModel.Custom -> model.copy(endRule = newEnd)
            }
        } else {
            model
        }

        return if (newModel != model) {
            reminder.copy(recurrenceJson = toJson(newModel))
        } else {
            reminder
        }
    }
}
