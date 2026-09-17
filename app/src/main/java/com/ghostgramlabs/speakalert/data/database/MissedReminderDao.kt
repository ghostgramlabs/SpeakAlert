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

    /**
     * Drop everything but the newest [keep] entries.
     *
     * Nothing removed these in bulk before, and every quiet-hours, paused or genuinely missed
     * reminder writes one. A nightly reminder under a quiet-hours window therefore added a row a
     * day forever, and the whole table is read into memory by Home, the widget and the review
     * check. The oldest entries are also the least useful: a miss from three months ago is not
     * something anyone is going to act on.
     */
    @Query(
        """
        DELETE FROM missed_reminders
        WHERE id NOT IN (
            SELECT id FROM missed_reminders ORDER BY detectedTime DESC LIMIT :keep
        )
        """
    )
    suspend fun trimToMostRecent(keep: Int)

    @Query("SELECT COUNT(*) FROM missed_reminders")
    suspend fun count(): Int
}
