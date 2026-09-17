package com.ghostgramlabs.speakalert.data.repository

import android.content.Context
import com.ghostgramlabs.speakalert.data.database.MissedReminderDao
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * The missed list is read in full by Home, the widget and the review check, and every quiet-hours,
 * paused or genuinely missed reminder adds to it. Nothing trimmed it, so a nightly reminder under
 * a quiet-hours window grew the table by a row a day indefinitely.
 */
class MissedReminderRetentionTest {

    /** Stands in for Room, keeping the rows in a list so the trim can be observed directly. */
    private class FakeDao : MissedReminderDao {
        val rows = mutableListOf<MissedReminderEntity>()
        private var nextId = 1L

        override fun getAllMissedReminders(): Flow<List<MissedReminderEntity>> =
            flowOf(rows.sortedByDescending { it.detectedTime })

        override suspend fun insert(missedReminder: MissedReminderEntity) {
            rows.add(missedReminder.copy(id = nextId++))
        }

        override suspend fun delete(missedReminder: MissedReminderEntity) {
            rows.removeAll { it.id == missedReminder.id }
        }

        override suspend fun deleteById(id: Long) {
            rows.removeAll { it.id == id }
        }

        override suspend fun deleteByReminderId(reminderId: Long) {
            rows.removeAll { it.reminderId == reminderId }
        }

        override suspend fun deleteByReminderIdAndScheduledTime(reminderId: Long, scheduledTime: Long) {
            rows.removeAll { it.reminderId == reminderId && it.scheduledTime == scheduledTime }
        }

        override suspend fun trimToMostRecent(keep: Int) {
            val survivors = rows.sortedByDescending { it.detectedTime }.take(keep).map { it.id }.toSet()
            rows.removeAll { it.id !in survivors }
        }

        override suspend fun count(): Int = rows.size
    }

    private lateinit var dao: FakeDao
    private lateinit var repository: MissedReminderRepository

    @Before
    fun setUp() {
        dao = FakeDao()
        // The repository nudges the widget after every write; the refresh itself is queued off
        // this thread, so the mock only has to answer for the context it passes along.
        val context: Context = mock()
        whenever(context.applicationContext).thenReturn(context)
        repository = MissedReminderRepositoryImpl(context, dao)
    }

    private fun entry(day: Int) = MissedReminderEntity(
        reminderId = day.toLong(),
        title = "Reminder $day",
        scheduledTime = day * 86_400_000L,
        detectedTime = day * 86_400_000L,
        reminderText = null
    )

    @Test
    fun `a year of nightly misses does not grow without bound`() = runTest {
        repeat(365) { day -> repository.insertMissedReminder(entry(day)) }

        assertEquals(
            MissedReminderRepositoryImpl.MAX_MISSED_ENTRIES,
            dao.rows.size
        )
    }

    @Test
    fun `the entries kept are the most recent ones`() = runTest {
        repeat(150) { day -> repository.insertMissedReminder(entry(day)) }

        val oldest = dao.rows.minOf { it.detectedTime }
        val newest = dao.rows.maxOf { it.detectedTime }
        assertEquals("the newest miss must survive", 149 * 86_400_000L, newest)
        assertTrue(
            "the oldest survivor should be recent, not from the start (was $oldest)",
            oldest >= 50 * 86_400_000L
        )
    }

    @Test
    fun `a short list is left alone`() = runTest {
        repeat(5) { day -> repository.insertMissedReminder(entry(day)) }

        assertEquals(5, dao.rows.size)
    }
}
