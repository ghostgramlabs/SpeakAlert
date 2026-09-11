package com.ghostgramlabs.speakalert.alarm

import com.ghostgramlabs.speakalert.data.model.ReminderEntity

// Cancellation cannot retract a broadcast that has already been delivered.
internal fun shouldIgnoreReminderAlarm(
    reminder: ReminderEntity,
    scheduledTime: Long,
    isFollowUpTrigger: Boolean
): Boolean = reminder.isCompleted ||
    (isFollowUpTrigger && reminder.pendingFollowUpAt != scheduledTime)
