package com.ghostgramlabs.speakalert.data.repository

import com.ghostgramlabs.speakalert.data.database.ReminderDao
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllRemindersStream(): Flow<List<ReminderEntity>>
    fun getActiveRemindersStream(): Flow<List<ReminderEntity>>
    fun getCompletedRemindersStream(): Flow<List<ReminderEntity>>
    suspend fun getReminder(id: Long): ReminderEntity?
    suspend fun getAllActiveReminders(): List<ReminderEntity>
    suspend fun insertReminder(reminder: ReminderEntity): Long
    suspend fun updateReminder(reminder: ReminderEntity)
    suspend fun deleteReminder(reminder: ReminderEntity)
}

class OfflineReminderRepository(private val reminderDao: ReminderDao) : ReminderRepository {
    override fun getAllRemindersStream(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    override fun getActiveRemindersStream(): Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()
    override fun getCompletedRemindersStream(): Flow<List<ReminderEntity>> = reminderDao.getCompletedReminders()
    override suspend fun getReminder(id: Long): ReminderEntity? = reminderDao.getReminderById(id)
    override suspend fun getAllActiveReminders(): List<ReminderEntity> = reminderDao.getAllActiveRemindersOnce()
    override suspend fun insertReminder(reminder: ReminderEntity): Long = reminderDao.insertReminder(reminder)
    override suspend fun updateReminder(reminder: ReminderEntity) = reminderDao.updateReminder(reminder)
    override suspend fun deleteReminder(reminder: ReminderEntity) = reminderDao.deleteReminder(reminder)
}
