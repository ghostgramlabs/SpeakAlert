package com.ghostgramlabs.speakalert.domain

import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.domain.models.MissedPolicy

/**
 * The one place that decides what happens to a late reminder.
 *
 * SKIP_TO_NEXT is also the model default, so a stored SKIP usually means "never chosen" rather
 * than "chosen". It only wins when the app-wide default is SKIP too; otherwise the app default
 * applies. The alarm receiver, the boot worker and the details screen all go through here, so
 * what the details screen shows is what actually happens.
 */
object MissedPolicyResolver {

    fun parse(raw: String): MissedPolicy = when (raw) {
        "FIRE_ON_RESUME", "FIRE" -> MissedPolicy.FIRE_ON_RESUME
        else -> MissedPolicy.SKIP_TO_NEXT
    }

    fun effective(
        recurrenceType: RecurrenceType,
        recurrenceJson: String?,
        storedPolicy: MissedPolicy,
        settingsDefault: MissedPolicy
    ): MissedPolicy {
        fun MissedPolicy.chosen() = this != MissedPolicy.SKIP_TO_NEXT || settingsDefault == MissedPolicy.SKIP_TO_NEXT
        val recurrencePolicy = RecurrenceUtils.fromJson(recurrenceType, recurrenceJson)?.missedPolicy
        return recurrencePolicy?.takeIf { it.chosen() }
            ?: storedPolicy.takeIf { it.chosen() }
            ?: settingsDefault
    }
}
