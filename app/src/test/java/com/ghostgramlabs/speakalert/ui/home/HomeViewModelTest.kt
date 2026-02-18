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
import org.mockito.kotlin.mock
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
}
