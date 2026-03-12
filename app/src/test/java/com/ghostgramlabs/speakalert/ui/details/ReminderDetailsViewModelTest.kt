package com.ghostgramlabs.speakalert.ui.details

import android.content.Context
import com.ghostgramlabs.speakalert.alarm.AlarmScheduler
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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

    @Test
    fun `toggleDone uncompletes recurring past reminder and moves trigger to future`() = runTest {
        val now = System.currentTimeMillis()
        val recurringCompleted = ReminderEntity(
            id = 2,
            isCompleted = true,
            recurrenceType = RecurrenceType.DAILY,
            nextTriggerAt = now - 60_000
        )
        whenever(repository.getReminder(2)).thenReturn(recurringCompleted)
        viewModel.loadReminder(2)
        advanceUntilIdle()

        viewModel.toggleDone()
        advanceUntilIdle()

        val captor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(captor.capture())
        val updated = captor.firstValue
        assertFalse(updated.isCompleted)
        assertNull(updated.completedAt)
        assertTrue(updated.nextTriggerAt > now)
        verify(scheduler).schedule(any(), any())
    }

    @Test
    fun `reschedule with past timestamp falls back to future and clears completion fields`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 3,
            isCompleted = true,
            completedAt = now - 1_000,
            lastFiredAt = now - 2_000,
            nextTriggerAt = now - 10_000,
            pendingFollowUpAt = now + 5_000
        )
        whenever(repository.getReminder(3)).thenReturn(reminder)
        viewModel.loadReminder(3)
        advanceUntilIdle()

        viewModel.reschedule(now - 500)
        advanceUntilIdle()

        val captor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(captor.capture())
        val updated = captor.firstValue
        assertFalse(updated.isCompleted)
        assertNull(updated.completedAt)
        assertNull(updated.lastFiredAt)
        assertNull(updated.pendingFollowUpAt)
        assertTrue(updated.nextTriggerAt > now)
        verify(scheduler).schedule(any(), any())
    }

    @Test
    fun `markAsMissed updates reminder without scheduling`() = runTest {
        val reminder = ReminderEntity(
            id = 4,
            isCompleted = true,
            completedAt = System.currentTimeMillis() - 1_000,
            nextTriggerAt = System.currentTimeMillis() - 60_000
        )
        whenever(repository.getReminder(4)).thenReturn(reminder)
        viewModel.loadReminder(4)
        advanceUntilIdle()

        viewModel.markAsMissed(playAudio = false)
        advanceUntilIdle()

        val captor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(captor.capture())
        assertFalse(captor.firstValue.isCompleted)
        assertNull(captor.firstValue.completedAt)
        verify(scheduler, never()).schedule(any(), any())
    }

    @Test
    fun `markAsMissed with playAudio true plays local reminder file`() = runTest {
        val file = kotlin.io.path.createTempFile(prefix = "reminder", suffix = ".m4a").toFile()
        val reminder = ReminderEntity(
            id = 5,
            audioPath = file.absolutePath,
            nextTriggerAt = System.currentTimeMillis() - 60_000
        )
        whenever(repository.getReminder(5)).thenReturn(reminder)
        viewModel.loadReminder(5)
        advanceUntilIdle()

        viewModel.markAsMissed(playAudio = true)
        advanceUntilIdle()

        verify(player).playFile(any())
        file.delete()
    }

    @Test
    fun `updateRecurrence null switches to one-time and keeps trigger`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 6,
            nextTriggerAt = now + 60_000,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceJson = RecurrenceUtils.toJson(RecurrenceModel.Daily())
        )
        whenever(repository.getReminder(6)).thenReturn(reminder)
        viewModel.loadReminder(6)
        advanceUntilIdle()

        viewModel.updateRecurrence(null)
        advanceUntilIdle()

        val captor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(captor.capture())
        val updated = captor.firstValue
        assertEquals(RecurrenceType.NONE, updated.recurrenceType)
        assertNull(updated.recurrenceJson)
        assertEquals(reminder.nextTriggerAt, updated.nextTriggerAt)
        verify(scheduler).cancel(reminder)
        verify(scheduler).schedule(any(), any())
    }

    @Test
    fun `updateRecurrence daily computes next future trigger and schedules`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 7,
            nextTriggerAt = now - 60_000,
            recurrenceType = RecurrenceType.NONE
        )
        whenever(repository.getReminder(7)).thenReturn(reminder)
        viewModel.loadReminder(7)
        advanceUntilIdle()

        viewModel.updateRecurrence(RecurrenceModel.Daily())
        advanceUntilIdle()

        val captor = argumentCaptor<ReminderEntity>()
        verify(repository).updateReminder(captor.capture())
        val updated = captor.firstValue
        assertEquals(RecurrenceType.DAILY, updated.recurrenceType)
        assertNotNull(updated.recurrenceJson)
        assertTrue(updated.nextTriggerAt > now)
        assertFalse(updated.isCompleted)
        verify(scheduler).schedule(any(), any())
    }
}
