package com.example.voicereminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missed_reminders")
data class MissedReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long,
    val title: String,
    val scheduledTime: Long,
    val detectedTime: Long = System.currentTimeMillis()
)
