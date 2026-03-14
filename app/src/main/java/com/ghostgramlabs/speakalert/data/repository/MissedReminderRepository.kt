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
}
