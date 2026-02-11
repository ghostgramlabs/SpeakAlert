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
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
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
        whenever(settingsRepository.speakTextIfNoVoice).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.themeMode).thenReturn(MutableStateFlow(0))
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
}
