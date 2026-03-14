package com.ghostgramlabs.speakalert.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissedReminderDao {
    @Query("SELECT * FROM missed_reminders ORDER BY detectedTime DESC")
    fun getAllMissedReminders(): Flow<List<MissedReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(missedReminder: MissedReminderEntity)

    @Delete
    suspend fun delete(missedReminder: MissedReminderEntity)

    @Query("DELETE FROM missed_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM missed_reminders WHERE reminderId = :reminderId")
    suspend fun deleteByReminderId(reminderId: Long)

    @Query("DELETE FROM missed_reminders WHERE reminderId = :reminderId AND scheduledTime = :scheduledTime")
    suspend fun deleteByReminderIdAndScheduledTime(reminderId: Long, scheduledTime: Long)
}
