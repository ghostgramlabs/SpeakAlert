package com.ghostgramlabs.speakalert.ui.addedit

import android.content.Context
import com.ghostgramlabs.speakalert.MainDispatcherRule
import com.ghostgramlabs.speakalert.alarm.AlarmScheduler
import com.ghostgramlabs.speakalert.audio.AudioPlayer
import com.ghostgramlabs.speakalert.audio.AudioRecorder
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.RecurrenceModel
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: ReminderRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Context
    private lateinit var recorder: AudioRecorder
    private lateinit var player: AudioPlayer
    
    private lateinit var viewModel: AddEditViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        repository = mock()
        scheduler = mock()
        settingsRepository = mock()
        context = mock()
        recorder = mock()
        player = mock()
        
        // Mock Context filesDir and cacheDir
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        whenever(context.filesDir).thenReturn(filesDir)
        whenever(context.cacheDir).thenReturn(cacheDir)
        
        // Mock Settings
        whenever(settingsRepository.appVolume).thenReturn(MutableStateFlow(1.0f))
        whenever(settingsRepository.speakTextIfNoVoice).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.defaultMissedPolicy).thenReturn(MutableStateFlow("SKIP_TO_NEXT"))
        
        viewModel = AddEditViewModel(
            repository,
            scheduler,
            settingsRepository,
            context,
            recorder,
            player
        )
    }

    @Test
    fun `saveReminder with valid text saves to repository and shedules alarm`() = runTest {
        // Arrange
        val title = "Buy milk"
        val text = "Don't forget the almond milk"
        viewModel.updateTitle(title)
        viewModel.updateReminderText(text)
        
        // Mock insert to return ID 1
        whenever(repository.insertReminder(any())).thenReturn(1L)

        // Act
        viewModel.saveReminder()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value.saveCompleted)
        assertFalse(viewModel.uiState.value.showError)
        
        // Verify Repository Insert
        val captor = argumentCaptor<ReminderEntity>()
        verify(repository).insertReminder(captor.capture())
        assertEquals(title, captor.firstValue.title)
        assertEquals(text, captor.firstValue.reminderText)
        
        // Verify Scheduler
        verify(scheduler).schedule(any(), any())
    }

    @Test
    fun `saveReminder with empty title generates smart title from text`() = runTest {
        // Arrange
        val text = "Call Mom"
        viewModel.updateReminderText(text) // Title is empty
        whenever(repository.insertReminder(any())).thenReturn(1L)

        // Act
        viewModel.saveReminder()
        advanceUntilIdle()

        // Assert
        val captor = argumentCaptor<ReminderEntity>()
        verify(repository).insertReminder(captor.capture())
        // New behavior: VM saves null title if blank, UI layer handles fallback to "Reminder at..." or time
        assertNull(captor.firstValue.title)
    }

    @Test
    fun `saveReminder with no text and no audio shows error`() = runTest {
        // Act
        viewModel.saveReminder() // Initial state is empty
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value.showError)
        assertFalse(viewModel.uiState.value.saveCompleted)
        verify(repository, Mockito.never()).insertReminder(any())
    }
    
    @Test
    fun `startRecording updates state and starts recorder`() = runTest {
        // Act
        viewModel.startRecording()
        // Do NOT advanceUntilIdle, as that finishes the 5-minute timer instantly
        // Just advance a little to let the launch start
        advanceTimeBy(100) 
        
        // Assert
        assertTrue(viewModel.uiState.value.isRecording)
        verify(recorder).start(any())
    }
    
    @Test
    fun `stopRecording updates state and stops recorder`() = runTest {
        // Arrange
        viewModel.startRecording()
        advanceTimeBy(100) 
        
        // Act
        viewModel.stopRecording()
        advanceTimeBy(100)
        
        // Assert
        assertFalse(viewModel.uiState.value.isRecording)
        verify(recorder).stop()
        assertNotNull(viewModel.uiState.value.recordedAudioPath)
    }
    
    @Test
    fun `loadReminder populates ui state`() = runTest {
        // Arrange
        val reminder = ReminderEntity(
            id = 1,
            title = "Existing Task",
            reminderText = "Notes",
            nextTriggerAt = 1000L,
            recurrenceType = RecurrenceType.DAILY
        )
        whenever(repository.getReminder(1)).thenReturn(reminder)
        
        // Act
        viewModel.loadReminder(1)
        advanceUntilIdle()
        
        // Assert
        assertEquals("Existing Task", viewModel.uiState.value.title)
        assertEquals("Notes", viewModel.uiState.value.reminderText)
        assertEquals(RecurrenceType.DAILY, viewModel.uiState.value.recurrenceType)
    }

    @Test
    fun `playRecording calls player play`() = runTest {
        // Arrange
        val audioFile = File.createTempFile("test", ".aac")
        val reminder = ReminderEntity(id = 1, audioPath = audioFile.absolutePath, nextTriggerAt = 1000L)
        whenever(repository.getReminder(1)).thenReturn(reminder)
        viewModel.loadReminder(1)
        advanceUntilIdle()

        // Act
        viewModel.playRecording()
        // IMPORTANT: Use advanceTimeBy, NOT advanceUntilIdle, because playRecording starts an infinite loop
        advanceTimeBy(100)

        // Assert
        verify(player).playFile(any())
        assertTrue(viewModel.uiState.value.isPlaying)
        
        // Cleanup loop
        viewModel.stopPlayback()
    }

    @Test
    fun `stopPlayback calls player stop`() = runTest {
        // Arrange
        val audioFile = File.createTempFile("test", ".aac")
        val reminder = ReminderEntity(id = 1, audioPath = audioFile.absolutePath, nextTriggerAt = 1000L)
        whenever(repository.getReminder(1)).thenReturn(reminder)
        viewModel.loadReminder(1)
        advanceUntilIdle()
        
        viewModel.playRecording()
        advanceTimeBy(100) // Start the loop

        // Act
        viewModel.stopPlayback()
        advanceUntilIdle() // Now it's safe to idle as loop should be cancelled

        // Assert
        // Stop is called twice: once in playRecording (to clear previous) and once manually
        verify(player, Mockito.times(2)).stop()
        assertFalse(viewModel.uiState.value.isPlaying)
    }
    
    @Test
    fun `seekTo calls player seekTo`() = runTest {
        // Arrange
        val audioFile = File.createTempFile("test", ".aac")
        val reminder = ReminderEntity(id = 1, audioPath = audioFile.absolutePath, nextTriggerAt = 1000L)
        whenever(repository.getReminder(1)).thenReturn(reminder)
        whenever(player.getDuration()).thenReturn(10000) // 10 seconds
        
        viewModel.loadReminder(1)
        advanceUntilIdle()
        viewModel.playRecording()
        advanceTimeBy(100) // Start the loop

        // Act
        viewModel.seekTo(0.5f) // 50%
        
        // Assert
        verify(player).seekTo(5000) // 50% of 10000 is 5000
        
        // Cleanup
        viewModel.stopPlayback()
    }

    @Test
    fun `saveReminder recurring keeps selected first trigger when it matches the rule`() = runTest {
        // Arrange
        val selected = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 5)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        viewModel.updateReminderText("Daily standup")
        viewModel.setTriggerTime(selected)
        viewModel.setRecurrence(
            RecurrenceType.DAILY,
            RecurrenceUtils.toJson(RecurrenceModel.Daily())
        )
        whenever(repository.insertReminder(any())).thenReturn(1L)

        // Act
        viewModel.saveReminder()
        advanceUntilIdle()

        // Assert
        val captor = argumentCaptor<ReminderEntity>()
        verify(repository).insertReminder(captor.capture())
        assertEquals(selected, captor.firstValue.nextTriggerAt)
    }
}
