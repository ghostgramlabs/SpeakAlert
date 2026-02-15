package com.ghostgramlabs.speakalert.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.data.AppContainer
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.MissedReminderRepository
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import com.ghostgramlabs.speakalert.service.ReminderPlaybackService
import com.ghostgramlabs.speakalert.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderAlarmReceiverTest {

    private lateinit var context: Context
    private lateinit var intent: Intent
    private lateinit var app: VoiceReminderApp
    private lateinit var container: AppContainer
    private lateinit var repository: ReminderRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var missedRepository: MissedReminderRepository
    private lateinit var scheduler: AlarmScheduler
    
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var audioManager: AudioManager

    // Static mocks
    private lateinit var reminderPlaybackServiceMock: MockedStatic<ReminderPlaybackService>
    private lateinit var fileLoggerMock: MockedStatic<FileLogger>

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        
        // Setup static mocks
        reminderPlaybackServiceMock = mockStatic(ReminderPlaybackService::class.java)
        fileLoggerMock = mockStatic(FileLogger::class.java)

        context = mock()
        intent = mock()
        app = mock()
        container = mock()
        repository = mock()
        settingsRepository = mock()
        missedRepository = mock()
        scheduler = mock()
        keyguardManager = mock()
        audioManager = mock()
        
        whenever(context.applicationContext).thenReturn(app)
        whenever(app.container).thenReturn(container)
        whenever(container.reminderRepository).thenReturn(repository)
        whenever(container.settingsRepository).thenReturn(settingsRepository)
        whenever(container.missedReminderRepository).thenReturn(missedRepository)
        whenever(container.alarmScheduler).thenReturn(scheduler)
        
        doReturn(keyguardManager).`when`(context).getSystemService(Context.KEYGUARD_SERVICE)
        doReturn(audioManager).`when`(context).getSystemService(Context.AUDIO_SERVICE)

        val notificationManager = mock<android.app.NotificationManager>()
        doReturn(notificationManager).`when`(context).getSystemService(Context.NOTIFICATION_SERVICE)
        whenever(notificationManager.currentInterruptionFilter).thenReturn(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)

        // Default: Keyguard unlocked, Audio normal
        whenever(keyguardManager.isKeyguardLocked).thenReturn(false)
        whenever(audioManager.mode).thenReturn(AudioManager.MODE_NORMAL)

        whenever(intent.getLongExtra("reminderId", -1L)).thenReturn(1L)
        
        // Default Mock Flows for Settings
        whenever(settingsRepository.quietTimeEnabled).thenReturn(MutableStateFlow(false))
        whenever(settingsRepository.autoPlayEnabled).thenReturn(MutableStateFlow(true)) // Auto-play enabled by default
        whenever(settingsRepository.autoPlayOnUnlockOnly).thenReturn(MutableStateFlow(false))
        whenever(settingsRepository.speakTextIfNoVoice).thenReturn(MutableStateFlow(true))
        
        // Quiet time defaults
        whenever(settingsRepository.quietTimeStartHour).thenReturn(MutableStateFlow(22))
        whenever(settingsRepository.quietTimeStartMinute).thenReturn(MutableStateFlow(0))
        whenever(settingsRepository.quietTimeEndHour).thenReturn(MutableStateFlow(7))
        whenever(settingsRepository.quietTimeEndMinute).thenReturn(MutableStateFlow(0))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reminderPlaybackServiceMock.close()
        fileLoggerMock.close()
    }

    /*
    @Test
    fun `when isBootReschedule is TRUE, FGS start is BLOCKED and notification is shown`() = runTest {
         // ... (existing code)
    }

    @Test
    fun `when isBootReschedule is FALSE, FGS start is ALLOWED`() = runTest {
        // ... (existing code)
    }
    */
}
