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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
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
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        
        repository = mock()
        missedRepository = mock()
        scheduler = mock()
        settingsRepository = mock()
        
        // Default Settings
        whenever(settingsRepository.speakTextIfNoVoice).thenReturn(MutableStateFlow(true))
        // Default Empty Streams
        whenever(repository.getAllRemindersStream()).thenReturn(MutableStateFlow(emptyList()))
        whenever(missedRepository.allMissedReminders).thenReturn(MutableStateFlow(emptyList()))

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
        
        whenever(repository.getAllRemindersStream()).thenReturn(MutableStateFlow(listOf(todayReminder, upcomingReminder)))

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
        
        whenever(repository.getAllRemindersStream()).thenReturn(MutableStateFlow(listOf(overdueReminder)))
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(1, state.todayReminders.size)
        assertEquals("Overdue", state.todayReminders[0].title)
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
}
