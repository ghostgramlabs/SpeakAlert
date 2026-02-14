package com.ghostgramlabs.speakalert.ui.details

import android.content.Context
import com.ghostgramlabs.speakalert.alarm.AlarmScheduler
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderDetailsViewModelTest {

    private lateinit var repository: ReminderRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Context
    private lateinit var player: com.ghostgramlabs.speakalert.audio.AudioPlayer
    
    private lateinit var viewModel: ReminderDetailsViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        
        repository = mock()
        scheduler = mock()
        settingsRepository = mock()
        context = mock()
        player = mock()
        
        // Default TTS setting
        whenever(settingsRepository.speakTextIfNoVoice).thenReturn(MutableStateFlow(true))
        
        viewModel = ReminderDetailsViewModel(repository, scheduler, settingsRepository, context, player)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadReminder updates ui state`() = runTest {
        // Arrange
        val reminder = ReminderEntity(id = 1, title = "Test", nextTriggerAt = 1000L)
        whenever(repository.getReminder(1)).thenReturn(reminder)
        
        // Act
        viewModel.loadReminder(1)
        advanceUntilIdle()
        
        // Assert
        val state = viewModel.reminder.value
        assertTrue(state != null)
        assertTrue(state?.title == "Test")
    }
    
    @Test
    fun `deleteReminder cancels alarm and deletes from repo`() = runTest {
        // Arrange
        val reminder = ReminderEntity(id = 1, nextTriggerAt = 1000L)
        whenever(repository.getReminder(1)).thenReturn(reminder)
        viewModel.loadReminder(1)
        advanceUntilIdle()
        
        // Act
        viewModel.deleteReminder()
        advanceUntilIdle()
        
        // Assert
        verify(scheduler).cancel(reminder)
        verify(repository).deleteReminder(reminder)
    }
    
    @Test
    fun `toggleDone updates completion status and schedules or cancels alarm`() = runTest {
        // Arrange
        val reminder = ReminderEntity(id = 1, isCompleted = false, nextTriggerAt = 1000L)
        whenever(repository.getReminder(1)).thenReturn(reminder)
        viewModel.loadReminder(1)
        advanceUntilIdle()
        
        // Act - Mark Done
        viewModel.toggleDone()
        advanceUntilIdle()
        
        // Assert - Completed
        verify(scheduler).cancel(reminder) // Cancel old
        val capturedUpdate1 = org.mockito.kotlin.argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(capturedUpdate1.capture())
        assertTrue(capturedUpdate1.firstValue.isCompleted)
        
        // Arrange for Undone
        whenever(repository.getReminder(1)).thenReturn(capturedUpdate1.firstValue)
        
        // Act - Mark Undone
        viewModel.toggleDone()
        advanceUntilIdle()
        
        // Assert - Active
        // Note: verify call count? 
        // We can check if it scheduled the updated entity
        // Since we are using mock(), all calls are recorded. 
        // But verifying second updateReminder call:
        // verify(repository, times(2)).updateReminder(...)
        // Let's just check scheduler call
        verify(scheduler).schedule(any(), any())
    }
}
