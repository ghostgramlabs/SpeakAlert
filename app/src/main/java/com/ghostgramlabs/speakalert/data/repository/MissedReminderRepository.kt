package com.ghostgramlabs.speakalert.data.repository

import android.content.Context
import com.ghostgramlabs.speakalert.data.database.MissedReminderDao
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import com.ghostgramlabs.speakalert.widget.SpeakAlertWidgetUpdater
import kotlinx.coroutines.flow.Flow

interface MissedReminderRepository {
    val allMissedReminders: Flow<List<MissedReminderEntity>>
    suspend fun insertMissedReminder(missedReminder: MissedReminderEntity)
    suspend fun deleteMissedReminder(missedReminder: MissedReminderEntity)
    suspend fun deleteMissedReminderById(id: Long)
    suspend fun deleteMissedReminderByReminderId(reminderId: Long)
}

class MissedReminderRepositoryImpl(
    private val context: Context,
    private val missedReminderDao: MissedReminderDao
) : MissedReminderRepository {
    override val allMissedReminders: Flow<List<MissedReminderEntity>> = missedReminderDao.getAllMissedReminders()

    override suspend fun insertMissedReminder(missedReminder: MissedReminderEntity) {
        // Prevent the same missed occurrence from piling up after reboot/time-change recovery.
        missedReminderDao.deleteByReminderIdAndScheduledTime(
            missedReminder.reminderId,
            missedReminder.scheduledTime
        )
        missedReminderDao.insert(missedReminder)
        // Trimmed here rather than on a schedule: this is the only place the table grows, so the
        // cap cannot be outrun, and the delete is a no-op once the list is already short enough.
        missedReminderDao.trimToMostRecent(MAX_MISSED_ENTRIES)
        SpeakAlertWidgetUpdater.requestUpdate(context)
    }

    override suspend fun deleteMissedReminder(missedReminder: MissedReminderEntity) {
        missedReminderDao.delete(missedReminder)
        SpeakAlertWidgetUpdater.requestUpdate(context)
    }

    override suspend fun deleteMissedReminderById(id: Long) {
        missedReminderDao.deleteById(id)
        SpeakAlertWidgetUpdater.requestUpdate(context)
    }

    override suspend fun deleteMissedReminderByReminderId(reminderId: Long) {
        missedReminderDao.deleteByReminderId(reminderId)
        SpeakAlertWidgetUpdater.requestUpdate(context)
    }

    companion object {
        /**
         * How many missed entries to keep.
         *
         * Generous enough that nobody loses a miss they were actually going to act on - a month
         * of several a day - while still bounding a table that four different readers load in
         * full.
         */
        const val MAX_MISSED_ENTRIES = 100
    }
}
