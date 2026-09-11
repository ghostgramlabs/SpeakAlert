package com.ghostgramlabs.speakalert.ui.home

import com.ghostgramlabs.speakalert.alarm.AlarmScheduler
import com.ghostgramlabs.speakalert.data.model.MissedReminderEntity
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.MissedReminderRepository
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * A dismissed missed reminder must not stay active and past due, or BootRescheduleWorker
 * (which selects `isCompleted = 0 AND nextTriggerAt > 0`) re-detects it and re-notifies
 * after every reboot.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DismissMissedReminderTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ReminderRepository
    private lateinit var missedRepository: MissedReminderRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        missedRepository = mock()
        scheduler = mock()
        val settings: SettingsRepository = mock()
        whenever(settings.speakTextIfNoVoice).thenReturn(MutableStateFlow(true))
        whenever(repository.getAllRemindersStream()).thenReturn(MutableStateFlow(emptyList()))
        whenever(missedRepository.allMissedReminders).thenReturn(MutableStateFlow(emptyList()))
        viewModel = HomeViewModel(repository, missedRepository, scheduler, settings)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `dismissing a missed one-time reminder completes it so it is not re-detected`() = runTest {
        val pastDue = System.currentTimeMillis() - 3_600_000L
        val reminder = ReminderEntity(
            id = 1L,
            title = "Pay bill",
            nextTriggerAt = pastDue,
            isCompleted = false,
            recurrenceType = RecurrenceType.NONE
        )
        whenever(repository.getReminder(1L)).thenReturn(reminder)

        viewModel.dismissMissedReminder(
            MissedReminderEntity(id = 99L, reminderId = 1L, title = "Pay bill", scheduledTime = pastDue)
        )
        advanceUntilIdle()

        verify(missedRepository).deleteMissedReminderById(99L)
        verify(scheduler).cancel(reminder)

        val saved = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(saved.capture())
        assertTrue("dismissed one-time reminder must be completed", saved.firstValue.isCompleted)
    }

    @Test
    fun `dismissing a missed recurring reminder advances it to a future occurrence`() = runTest {
        val now = System.currentTimeMillis()
        val pastDue = now - 3_600_000L
        val reminder = ReminderEntity(
            id = 2L,
            title = "Pills",
            nextTriggerAt = pastDue,
            isCompleted = false,
            recurrenceType = RecurrenceType.DAILY
        )
        whenever(repository.getReminder(2L)).thenReturn(reminder)

        viewModel.dismissMissedReminder(
            MissedReminderEntity(id = 77L, reminderId = 2L, title = "Pills", scheduledTime = pastDue)
        )
        advanceUntilIdle()

        verify(missedRepository).deleteMissedReminderById(77L)

        val saved = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(saved.capture())
        val updated = saved.firstValue
        assertEquals("recurring reminder must stay active", false, updated.isCompleted)
        assertTrue(
            "next trigger must be in the future, got ${updated.nextTriggerAt} vs $now",
            updated.nextTriggerAt > now
        )
    }
}
