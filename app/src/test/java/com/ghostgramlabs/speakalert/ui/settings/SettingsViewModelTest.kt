package com.ghostgramlabs.speakalert.ui.settings

import com.ghostgramlabs.speakalert.alarm.AlarmScheduler
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var alarmScheduler: AlarmScheduler
    
    private lateinit var viewModel: SettingsViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        
        settingsRepository = mock()
        reminderRepository = mock()
        alarmScheduler = mock()
        
        // Mock required flows
        whenever(settingsRepository.autoPlayEnabled).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.autoPlayOnUnlockOnly).thenReturn(MutableStateFlow(false))
        whenever(settingsRepository.speakTextIfNoVoice).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.toneOnlyMode).thenReturn(MutableStateFlow(false))
        whenever(settingsRepository.themeMode).thenReturn(MutableStateFlow(0))
        whenever(settingsRepository.fullScreenAlertEnabled).thenReturn(MutableStateFlow(false))
        whenever(settingsRepository.showVoiceRecordingSection).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.showAudioFileSection).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.showTypedReminderSection).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.showShortLabelSection).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.defaultSnoozeDuration).thenReturn(MutableStateFlow(5))
        whenever(settingsRepository.quietTimeEnabled).thenReturn(MutableStateFlow(false))
        whenever(settingsRepository.quietTimeStartHour).thenReturn(MutableStateFlow(22))
        whenever(settingsRepository.quietTimeStartMinute).thenReturn(MutableStateFlow(0))
        whenever(settingsRepository.quietTimeEndHour).thenReturn(MutableStateFlow(7))
        whenever(settingsRepository.quietTimeEndMinute).thenReturn(MutableStateFlow(0))
        whenever(settingsRepository.debugLoggingEnabled).thenReturn(MutableStateFlow(false))
        whenever(settingsRepository.appVolume).thenReturn(MutableStateFlow(1.0f))
        whenever(settingsRepository.loopTimeoutMinutes).thenReturn(MutableStateFlow(10))

        viewModel = SettingsViewModel(settingsRepository, reminderRepository, alarmScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `themeMode state reflects repository value`() = runTest {
        // Arrange
        val themeFlow = MutableStateFlow(0)
        whenever(settingsRepository.themeMode).thenReturn(themeFlow)
        
        // Re-init VM to pick up the new flow (though in init it should be fine)
        viewModel = SettingsViewModel(settingsRepository, reminderRepository, alarmScheduler)
        // Start collection in background to keep WhileSubscribed flow active
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themeMode.collect()
        }
        
        advanceUntilIdle()

        // Assert initial
        assertEquals(0, viewModel.themeMode.value)

        // Act
        themeFlow.value = 2 // Dark
        advanceUntilIdle()

        // Assert updated
        assertEquals(2, viewModel.themeMode.value)
    }

    @Test
    fun `setThemeMode calls repository`() = runTest {
        // Act
        viewModel.setThemeMode(1) // Light
        advanceUntilIdle()

        // Assert
        verify(settingsRepository).setThemeMode(1)
    }

    @Test
    fun `setters forward values to settings repository`() = runTest {
        viewModel.setAutoPlayEnabled(false)
        viewModel.setAutoPlayOnUnlockOnly(true)
        viewModel.setDefaultSnoozeDuration(10)
        viewModel.setSpeakTextIfNoVoice(false)
        viewModel.setToneOnlyMode(true)
        viewModel.setFullScreenAlertEnabled(true)
        viewModel.setShowVoiceRecordingSection(false)
        viewModel.setShowAudioFileSection(false)
        viewModel.setShowTypedReminderSection(false)
        viewModel.setShowShortLabelSection(false)
        viewModel.setAppVolume(0.7f)
        viewModel.setLoopTimeoutMinutes(2)
        viewModel.setQuietTimeEnabled(true)
        viewModel.setQuietTimeStart(21, 30)
        viewModel.setQuietTimeEnd(6, 45)
        advanceUntilIdle()

        verify(settingsRepository).setAutoPlayEnabled(false)
        verify(settingsRepository).setAutoPlayOnUnlockOnly(true)
        verify(settingsRepository).setDefaultSnoozeDuration(10)
        verify(settingsRepository).setSpeakTextIfNoVoice(false)
        verify(settingsRepository).setToneOnlyMode(true)
        verify(settingsRepository).setFullScreenAlertEnabled(true)
        verify(settingsRepository).setShowVoiceRecordingSection(false)
        verify(settingsRepository).setShowAudioFileSection(false)
        verify(settingsRepository).setShowTypedReminderSection(false)
        verify(settingsRepository).setShowShortLabelSection(false)
        verify(settingsRepository).setAppVolume(0.7f)
        verify(settingsRepository).setLoopTimeoutMinutes(2)
        verify(settingsRepository).setQuietTimeEnabled(true)
        verify(settingsRepository).setQuietTimeStart(21, 30)
        verify(settingsRepository).setQuietTimeEnd(6, 45)
    }

    @Test
    fun `scheduleTestReminder inserts and schedules one-time reminder`() = runTest {
        whenever(reminderRepository.insertReminder(org.mockito.kotlin.any())).thenReturn(123L)

        viewModel.scheduleTestReminder()
        advanceUntilIdle()

        val insertCaptor = argumentCaptor<com.ghostgramlabs.speakalert.data.model.ReminderEntity>()
        verify(reminderRepository).insertReminder(insertCaptor.capture())
        val inserted = insertCaptor.firstValue
        assertEquals("Test Reminder", inserted.title)
        assertTrue((inserted.reminderText ?: "").contains("test reminder", ignoreCase = true))
        assertTrue(inserted.nextTriggerAt > System.currentTimeMillis())
        assertEquals(com.ghostgramlabs.speakalert.domain.models.RecurrenceType.NONE, inserted.recurrenceType)

        val scheduleCaptor = argumentCaptor<com.ghostgramlabs.speakalert.data.model.ReminderEntity>()
        verify(alarmScheduler).schedule(scheduleCaptor.capture(), org.mockito.kotlin.any())
        assertEquals(123L, scheduleCaptor.firstValue.id)
    }
}
