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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var repository: ReminderRepository
    private lateinit var missedRepository: MissedReminderRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var settingsRepository: SettingsRepository
    
    private lateinit var viewModel: HomeViewModel
    
    private val remindersFlow = MutableStateFlow<List<ReminderEntity>>(emptyList())
    private val missedFlow = MutableStateFlow<List<MissedReminderEntity>>(emptyList())
    private val ttsEnabledFlow = MutableStateFlow(true)
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        
        repository = mock()
        missedRepository = mock()
        scheduler = mock()
        settingsRepository = mock()
        
        // Default Mock Flows
        whenever(settingsRepository.speakTextIfNoVoice).thenReturn(ttsEnabledFlow)
        whenever(repository.getAllRemindersStream()).thenReturn(remindersFlow)
        whenever(missedRepository.allMissedReminders).thenReturn(missedFlow)

        viewModel = HomeViewModel(repository, missedRepository, scheduler, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState separates today and upcoming reminders correctly`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val todayReminder = ReminderEntity(id = 1, title = "Today", nextTriggerAt = now + 1000) // 1 sec future
        val upcomingReminder = ReminderEntity(id = 2, title = "Tomorrow", nextTriggerAt = now + 86400000) // +1 day
        
        remindersFlow.value = listOf(todayReminder, upcomingReminder)

        // Start collection in background
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // Act
        // Create new VM to trigger init flow? Flow is hot, just collecting.
        // We need to wait for flow collection. 
        // With stateIn, initial value is empty, so we must advance.
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(1, state.todayReminders.size)
        assertEquals("Today", state.todayReminders[0].title)
        assertEquals(1, state.upcomingReminders.size)
        assertEquals("Tomorrow", state.upcomingReminders[0].title)
    }

    @Test
    fun `uiState shows overdue reminders in today list`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val overdueReminder = ReminderEntity(id = 1, title = "Overdue", nextTriggerAt = now - 3600000) // -1 hour
        
        remindersFlow.value = listOf(overdueReminder)
        
        // Start collection in background
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(1, state.todayReminders.size)
        assertEquals("Overdue", state.todayReminders[0].title)
    }

    @Test
    fun `uiState keeps same-day future reminder in today even after earlier fire`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val firedTodayFutureReminder = ReminderEntity(
            id = 1,
            title = "Hourly",
            nextTriggerAt = now + 30 * 60 * 1000, // still today and future
            lastFiredAt = now - 5 * 60 * 1000
        )

        remindersFlow.value = listOf(firedTodayFutureReminder)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(1, state.todayReminders.size)
        assertEquals("Hourly", state.todayReminders[0].title)
        assertTrue(state.upcomingReminders.isEmpty())
    }

    @Test
    fun `uiState hides one-time reminder from active lists when missed entry exists`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val oneTimeReminder = ReminderEntity(
            id = 42,
            title = "One-time missed",
            recurrenceType = RecurrenceType.NONE,
            nextTriggerAt = now - 5 * 60 * 1000,
            isCompleted = false
        )
        val missedEntry = MissedReminderEntity(
            id = 1,
            reminderId = 42,
            title = "One-time missed",
            scheduledTime = oneTimeReminder.nextTriggerAt,
            detectedTime = now
        )
        remindersFlow.value = listOf(oneTimeReminder)
        missedFlow.value = listOf(missedEntry)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.todayReminders.isEmpty())
        assertTrue(state.upcomingReminders.isEmpty())
        assertEquals(1, state.missedReminders.size)
        assertEquals(42L, state.missedReminders.first().reminderId)
    }

     @Test
    fun `markAsDone completes one-time reminder`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(id = 1, recurrenceType = RecurrenceType.NONE, isCompleted = false, nextTriggerAt = now)
        
        // Act
        viewModel.completeReminder(reminder)
        advanceUntilIdle()

        // Assert
        verify(repository).updateReminder(org.mockito.kotlin.check {
            assertTrue(it.isCompleted)
            assertNotNull(it.completedAt)
        })
        verify(missedRepository).deleteMissedReminderByReminderId(reminder.id)
    }
    
    @Test
    fun `markTodayAsDone advances recurring reminder`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(id = 1, recurrenceType = RecurrenceType.DAILY, isCompleted = false, nextTriggerAt = now)
        
        // Act
        viewModel.markTodayAsDone(reminder)
        advanceUntilIdle()

        // Assert
        verify(repository).updateReminder(org.mockito.kotlin.check {
            assertFalse(it.isCompleted) // Should stay active
            assertNotNull(it.lastFiredAt) // But marked as handled
            // Verify next trigger advanced?
            assertTrue(it.nextTriggerAt > now)
        })
    }

    @Test
    fun `deleteReminder cancels alarm and deletes from repo`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(id = 1, nextTriggerAt = now)
        
        // Act
        viewModel.deleteReminder(reminder)
        advanceUntilIdle()

        // Assert
        verify(scheduler).cancel(reminder)
        verify(repository).deleteReminder(reminder)
    }

    @Test
    fun `dismissMissedReminder deletes from missed repository by ID`() = runTest {
        // Arrange
        val missed = MissedReminderEntity(id = 1, reminderId = 10, title = "Missed", scheduledTime = 1000, detectedTime = 2000)
        
        // Act
        viewModel.dismissMissedReminder(missed)
        advanceUntilIdle()

        // Assert
        verify(missedRepository).deleteMissedReminderById(missed.id)
    }

    @Test
    fun `markMissedRemindersDone removes stale missed item when reminder is missing`() = runTest {
        val missed = MissedReminderEntity(id = 8, reminderId = 99, title = "Missing", scheduledTime = 1000, detectedTime = 2000)
        whenever(repository.getReminder(99)).thenReturn(null)

        viewModel.markMissedRemindersDone(listOf(missed))
        advanceUntilIdle()

        verify(missedRepository).deleteMissedReminderById(8)
        verify(repository, never()).updateReminder(any())
    }

    @Test
    fun `markMissedRemindersDone marks one-time reminder as completed`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 20,
            recurrenceType = RecurrenceType.NONE,
            nextTriggerAt = now - 5_000,
            isCompleted = false,
            snoozeUntil = now + 60_000,
            pendingFollowUpAt = now + 120_000
        )
        val missed = MissedReminderEntity(id = 3, reminderId = 20, title = "Missed one-time", scheduledTime = reminder.nextTriggerAt, detectedTime = now)
        whenever(repository.getReminder(20)).thenReturn(reminder)

        viewModel.markMissedRemindersDone(listOf(missed))
        advanceUntilIdle()

        val updatedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(updatedCaptor.capture())
        assertTrue(updatedCaptor.firstValue.isCompleted)
        assertNotNull(updatedCaptor.firstValue.completedAt)
        assertNull(updatedCaptor.firstValue.snoozeUntil)
        assertNull(updatedCaptor.firstValue.pendingFollowUpAt)
        verify(scheduler).cancel(reminder)
        verify(missedRepository).deleteMissedReminderById(missed.id)
    }

    @Test
    fun `markMissedRemindersDone keeps recurring future reminder active and clears transient fields`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 30,
            recurrenceType = RecurrenceType.DAILY,
            nextTriggerAt = now + 86_400_000,
            snoozeUntil = now + 120_000,
            pendingFollowUpAt = now + 240_000
        )
        val missed = MissedReminderEntity(id = 4, reminderId = 30, title = "Future recurring", scheduledTime = now, detectedTime = now)
        whenever(repository.getReminder(30)).thenReturn(reminder)

        viewModel.markMissedRemindersDone(listOf(missed))
        advanceUntilIdle()

        val updatedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(updatedCaptor.capture())
        assertEquals(reminder.nextTriggerAt, updatedCaptor.firstValue.nextTriggerAt)
        assertFalse(updatedCaptor.firstValue.isCompleted)
        assertNull(updatedCaptor.firstValue.snoozeUntil)
        assertNull(updatedCaptor.firstValue.pendingFollowUpAt)
        verify(scheduler, never()).schedule(any(), any())
        verify(scheduler, never()).cancel(any())
        verify(missedRepository).deleteMissedReminderById(missed.id)
    }

    @Test
    fun `markMissedRemindersDone advances recurring past reminder`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 31,
            recurrenceType = RecurrenceType.DAILY,
            nextTriggerAt = now - 60_000
        )
        val missed = MissedReminderEntity(id = 5, reminderId = 31, title = "Past recurring", scheduledTime = reminder.nextTriggerAt, detectedTime = now)
        whenever(repository.getReminder(31)).thenReturn(reminder)

        viewModel.markMissedRemindersDone(listOf(missed))
        advanceUntilIdle()

        val updatedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(updatedCaptor.capture())
        assertTrue(updatedCaptor.firstValue.nextTriggerAt > now)
        assertNotNull(updatedCaptor.firstValue.lastFiredAt)
        verify(scheduler).schedule(any(), any())
        verify(missedRepository).deleteMissedReminderById(missed.id)
    }

    @Test
    fun `markMissedRemindersDone completes recurring reminder when next trigger cannot be computed`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 32,
            recurrenceType = RecurrenceType.CUSTOM,
            recurrenceJson = "{broken_json",
            nextTriggerAt = now - 60_000
        )
        val missed = MissedReminderEntity(id = 6, reminderId = 32, title = "Broken recurring", scheduledTime = reminder.nextTriggerAt, detectedTime = now)
        whenever(repository.getReminder(32)).thenReturn(reminder)

        viewModel.markMissedRemindersDone(listOf(missed))
        advanceUntilIdle()

        val updatedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(updatedCaptor.capture())
        assertTrue(updatedCaptor.firstValue.isCompleted)
        assertNotNull(updatedCaptor.firstValue.completedAt)
        verify(scheduler).cancel(any())
        verify(missedRepository).deleteMissedReminderById(missed.id)
    }

    @Test
    fun `remindAgainForMissed updates one-time reminder trigger time`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 40,
            recurrenceType = RecurrenceType.NONE,
            nextTriggerAt = now - 5_000,
            isCompleted = true,
            completedAt = now - 2_000
        )
        val missed = MissedReminderEntity(id = 9, reminderId = 40, title = "Missed one-time", scheduledTime = reminder.nextTriggerAt, detectedTime = now)
        whenever(repository.getReminder(40)).thenReturn(reminder)
        whenever(settingsRepository.defaultSnoozeDuration).thenReturn(MutableStateFlow(5))

        viewModel.remindAgainForMissed(listOf(missed))
        advanceUntilIdle()

        val updatedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(updatedCaptor.capture())
        val updated = updatedCaptor.firstValue
        assertFalse(updated.isCompleted)
        assertNull(updated.completedAt)
        assertNull(updated.snoozeUntil)
        assertTrue(updated.nextTriggerAt > now)
        assertEquals(0L, updated.nextTriggerAt % 60_000L)
        verify(scheduler).schedule(any(), any())
        verify(missedRepository).deleteMissedReminderById(missed.id)
    }

    @Test
    fun `remindAgainForMissed uses snoozeUntil for recurring reminder`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 41,
            recurrenceType = RecurrenceType.DAILY,
            nextTriggerAt = now + 86_400_000,
            isCompleted = true,
            completedAt = now - 1_000
        )
        val missed = MissedReminderEntity(id = 10, reminderId = 41, title = "Missed recurring", scheduledTime = now - 5_000, detectedTime = now)
        whenever(repository.getReminder(41)).thenReturn(reminder)
        whenever(settingsRepository.defaultSnoozeDuration).thenReturn(MutableStateFlow(10))

        viewModel.remindAgainForMissed(listOf(missed))
        advanceUntilIdle()

        val updatedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(updatedCaptor.capture())
        val updated = updatedCaptor.firstValue
        assertEquals(reminder.nextTriggerAt, updated.nextTriggerAt)
        assertNotNull(updated.snoozeUntil)
        assertTrue(updated.snoozeUntil!! > now)
        assertFalse(updated.isCompleted)
        verify(scheduler).schedule(any(), any())
        verify(missedRepository).deleteMissedReminderById(missed.id)
    }

    @Test
    fun `remindAgainForMissed removes stale missed when reminder missing`() = runTest {
        val missed = MissedReminderEntity(id = 11, reminderId = 404, title = "Missing", scheduledTime = 1000, detectedTime = 2000)
        whenever(repository.getReminder(404)).thenReturn(null)
        whenever(settingsRepository.defaultSnoozeDuration).thenReturn(MutableStateFlow(5))

        viewModel.remindAgainForMissed(listOf(missed))
        advanceUntilIdle()

        verify(missedRepository).deleteMissedReminderById(missed.id)
        verify(repository, never()).updateReminder(any())
    }

    @Test
    fun `undoDelete with past force time restores in future with safe defaults`() = runTest {
        val now = System.currentTimeMillis()
        val deleted = ReminderEntity(
            id = 50,
            title = "Undo target",
            nextTriggerAt = now + 120_000,
            isCompleted = true,
            lastFiredAt = now - 10_000,
            snoozeUntil = now + 60_000,
            pendingFollowUpAt = now + 90_000
        )
        viewModel.deleteReminder(deleted)
        advanceUntilIdle()

        viewModel.undoDelete(now - 1_000)
        advanceUntilIdle()

        val insertedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).insertReminder(insertedCaptor.capture())
        val restored = insertedCaptor.firstValue
        assertTrue(restored.nextTriggerAt > now)
        assertFalse(restored.isCompleted)
        assertNull(restored.lastFiredAt)
        assertNull(restored.snoozeUntil)
        assertNull(restored.pendingFollowUpAt)
        verify(scheduler).schedule(any(), any())
    }

    @Test
    fun `restoreReminder with past time falls back to future and schedules`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 60,
            title = "Restore",
            nextTriggerAt = now - 10_000,
            isCompleted = true,
            completedAt = now - 5_000,
            lastFiredAt = now - 1_000
        )

        viewModel.restoreReminder(reminder, now - 1_000)
        advanceUntilIdle()

        val updatedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(updatedCaptor.capture())
        val updated = updatedCaptor.firstValue
        assertFalse(updated.isCompleted)
        assertNull(updated.completedAt)
        assertNull(updated.lastFiredAt)
        assertNull(updated.snoozeUntil)
        assertNull(updated.pendingFollowUpAt)
        assertTrue(updated.nextTriggerAt > now)
        verify(scheduler).schedule(any(), any())
    }

    @Test
    fun `moveToMissed derives title from reminder text when app label placeholder is used`() = runTest {
        val reminderText = "Take medicine after lunch and drink water immediately today please"
        val reminder = ReminderEntity(
            id = 70,
            title = "SpeakAlert",
            reminderText = reminderText,
            nextTriggerAt = System.currentTimeMillis() - 1_000,
            isCompleted = true
        )

        viewModel.moveToMissed(reminder)
        advanceUntilIdle()

        val missedCaptor = argumentCaptor<MissedReminderEntity>()
        verify(missedRepository).insertMissedReminder(missedCaptor.capture())
        assertEquals("Take medicine after lunch and drink water immediately", missedCaptor.firstValue.title)

        val updatedCaptor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(updatedCaptor.capture())
        assertFalse(updatedCaptor.firstValue.isCompleted)
        assertNull(updatedCaptor.firstValue.completedAt)
    }

    @Test
    fun `moveToMissed falls back to Reminder title when no title and no text`() = runTest {
        val reminder = ReminderEntity(
            id = 71,
            title = "   ",
            reminderText = null,
            nextTriggerAt = System.currentTimeMillis() - 1_000
        )

        viewModel.moveToMissed(reminder)
        advanceUntilIdle()

        val missedCaptor = argumentCaptor<MissedReminderEntity>()
        verify(missedRepository).insertMissedReminder(missedCaptor.capture())
        assertEquals("Reminder", missedCaptor.firstValue.title)
    }
}
