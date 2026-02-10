package com.ghostgramlabs.speakalert.data.repository

import com.ghostgramlabs.speakalert.data.database.MissedReminderDao
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import kotlinx.coroutines.flow.Flow

interface MissedReminderRepository {
    val allMissedReminders: Flow<List<MissedReminderEntity>>
    suspend fun insertMissedReminder(missedReminder: MissedReminderEntity)
    suspend fun deleteMissedReminder(missedReminder: MissedReminderEntity)
    suspend fun deleteMissedReminderById(id: Long)
    suspend fun deleteMissedReminderByReminderId(reminderId: Long)
}

class MissedReminderRepositoryImpl(private val missedReminderDao: MissedReminderDao) : MissedReminderRepository {
    override val allMissedReminders: Flow<List<MissedReminderEntity>> = missedReminderDao.getAllMissedReminders()

    override suspend fun insertMissedReminder(missedReminder: MissedReminderEntity) = missedReminderDao.insert(missedReminder)
    override suspend fun deleteMissedReminder(missedReminder: MissedReminderEntity) = missedReminderDao.delete(missedReminder)
    override suspend fun deleteMissedReminderById(id: Long) = missedReminderDao.deleteById(id)
    override suspend fun deleteMissedReminderByReminderId(reminderId: Long) = missedReminderDao.deleteByReminderId(reminderId)
}
