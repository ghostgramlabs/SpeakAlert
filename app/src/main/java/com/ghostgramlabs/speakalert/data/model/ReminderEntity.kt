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
    val missedPolicy: MissedPolicy = MissedPolicy.FIRE_ON_RESUME,
    
    // Loop Playback - when enabled, audio/TTS plays repeatedly until stopped
    val loopPlayback: Boolean = false
)
