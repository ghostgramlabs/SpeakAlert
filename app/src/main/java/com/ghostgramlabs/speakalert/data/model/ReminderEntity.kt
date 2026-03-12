package com.ghostgramlabs.speakalert.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import com.ghostgramlabs.speakalert.domain.models.MissedPolicy

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String? = null,
    val reminderText: String? = null,
    val transcript: String? = null,
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val nextTriggerAt: Long,
    val lastFiredAt: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    
    // Recurrence
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    // JSON string for specific rules e.g. {"days":[2,4],"hour":10,"minute":30}
    val recurrenceJson: String? = null, 
    
    // Snooze
    val snoozeUntil: Long? = null,
    
    // Missed Policy (applies to both one-time and recurring)
    // Default matches RecurrenceModel defaults which is what the receiver actually reads
    val missedPolicy: MissedPolicy = MissedPolicy.SKIP_TO_NEXT,
    
    // Loop Playback - when enabled, audio/TTS plays repeatedly until stopped
    val loopPlayback: Boolean = false,

    // Optional one-time follow-up check after the reminder fires.
    val followUpCheckMinutes: Int = 0,

    // Internal marker for a scheduled follow-up that reuses the normal alarm path.
    val pendingFollowUpAt: Long? = null
)
