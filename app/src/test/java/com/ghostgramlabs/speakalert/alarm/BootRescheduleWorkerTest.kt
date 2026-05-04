package com.ghostgramlabs.speakalert.alarm

import android.content.Intent
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.impl.utils.SerialExecutorImpl
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.data.AppContainer
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.MissedReminderRepository
import com.ghostgramlabs.speakalert.data.repository.ReminderRepository
import com.ghostgramlabs.speakalert.data.repository.SettingsRepository
import com.ghostgramlabs.speakalert.util.FileLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import com.ghostgramlabs.speakalert.MainDispatcherRule

import org.junit.Rule
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
class BootRescheduleWorkerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: VoiceReminderApp
    private lateinit var container: AppContainer
    private lateinit var repository: ReminderRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var missedRepository: MissedReminderRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var serialExecutor: SerialExecutor
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var workerParams: WorkerParameters

    private lateinit var fileLoggerMock: MockedStatic<FileLogger>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        fileLoggerMock = mockStatic(FileLogger::class.java)

        app = mock()
        container = mock()
        repository = mock()
        settingsRepository = mock()
        missedRepository = mock()
        scheduler = mock()
        val directExecutor = Executor { runnable -> runnable.run() }
        serialExecutor = SerialExecutorImpl(directExecutor)
        taskExecutor = mock()
        workerParams = mock()
        
        whenever(app.applicationContext).thenReturn(app)
        whenever(app.container).thenReturn(container)
        whenever(container.reminderRepository).thenReturn(repository)
        whenever(container.settingsRepository).thenReturn(settingsRepository)
        whenever(container.missedReminderRepository).thenReturn(missedRepository)
        whenever(container.alarmScheduler).thenReturn(scheduler)
        whenever(settingsRepository.defaultMissedPolicy).thenReturn(MutableStateFlow("FIRE_ON_RESUME"))
        whenever(settingsRepository.toneOnlyMode).thenReturn(MutableStateFlow(false))
        whenever(settingsRepository.toneOnlyAlertToneUri).thenReturn(MutableStateFlow(null))
        whenever(settingsRepository.loopTimeoutMinutes).thenReturn(MutableStateFlow(10))
        whenever(settingsRepository.dndBypassEnabled).thenReturn(MutableStateFlow(true))
        whenever(taskExecutor.serialTaskExecutor).thenReturn(serialExecutor)
        whenever(taskExecutor.mainThreadExecutor).thenReturn(directExecutor)
        whenever(workerParams.taskExecutor).thenReturn(taskExecutor)
    }

    @After
    fun tearDown() {
        fileLoggerMock.close()
    }

    @Test
    fun `doWork suppresses missed reminder notification during boot restart`() = runTest {
        val reminder = ReminderEntity(
            id = 11L,
            title = "Water plants",
            reminderText = "Back patio",
            nextTriggerAt = System.currentTimeMillis() - 60_000L
        )
        whenever(repository.getAllActiveReminders()).thenReturn(listOf(reminder))
        whenever(workerParams.inputData).thenReturn(
            workDataOf(BootRescheduleWorker.KEY_TRIGGER_ACTION to Intent.ACTION_BOOT_COMPLETED)
        )

        mockConstruction(NotificationHelper::class.java).use { notificationMocks ->
            val worker = BootRescheduleWorker(app, workerParams)

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            assertTrue(notificationMocks.constructed().isNotEmpty())
            verify(notificationMocks.constructed().single(), never()).showNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
            verify(missedRepository).insertMissedReminder(any())
            verify(repository).updateReminder(any())
            verify(scheduler).cancel(any())
        }
    }

    @Test
    fun `doWork suppresses overdue follow up notification during boot restart`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 22L,
            title = "Daily walk",
            nextTriggerAt = now + 3_600_000L,
            pendingFollowUpAt = now - 60_000L,
            followUpCheckMinutes = 10
        )
        whenever(repository.getAllActiveReminders()).thenReturn(listOf(reminder))
        whenever(workerParams.inputData).thenReturn(
            workDataOf(BootRescheduleWorker.KEY_TRIGGER_ACTION to Intent.ACTION_BOOT_COMPLETED)
        )

        mockConstruction(NotificationHelper::class.java).use { notificationMocks ->
            val worker = BootRescheduleWorker(app, workerParams)

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            assertTrue(notificationMocks.constructed().isNotEmpty())
            verify(notificationMocks.constructed().single(), never()).showNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
            verify(scheduler).schedule(eq(reminder), eq(true))
            verify(repository).updateReminder(any())
        }
    }

    @Test
    fun `doWork keeps rescheduled reminder fields when clearing expired follow up during boot`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 33L,
            title = "Daily walk",
            nextTriggerAt = now - 60_000L,
            recurrenceType = com.ghostgramlabs.speakalert.domain.models.RecurrenceType.DAILY,
            pendingFollowUpAt = now - 30_000L,
            followUpCheckMinutes = 10
        )
        whenever(repository.getAllActiveReminders()).thenReturn(listOf(reminder))
        whenever(workerParams.inputData).thenReturn(
            workDataOf(BootRescheduleWorker.KEY_TRIGGER_ACTION to Intent.ACTION_BOOT_COMPLETED)
        )

        mockConstruction(NotificationHelper::class.java).use {
            val worker = BootRescheduleWorker(app, workerParams)

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            val reminderCaptor = argumentCaptor<ReminderEntity>()
            verify(repository, atLeastOnce()).updateReminder(reminderCaptor.capture())

            val finalUpdate = reminderCaptor.allValues.last()
            assertTrue(finalUpdate.lastFiredAt != null)
            assertTrue(finalUpdate.pendingFollowUpAt == null)
            assertTrue(finalUpdate.nextTriggerAt > now)
            assertTrue(!finalUpdate.isCompleted)
        }
    }

    @Test
    fun `doWork uses follow up question for overdue follow up notification`() = runTest {
        val now = System.currentTimeMillis()
        val reminder = ReminderEntity(
            id = 44L,
            title = "Daily walk",
            audioPath = "/tmp/original-audio.mp3",
            reminderText = "Go outside",
            nextTriggerAt = now + 3_600_000L,
            pendingFollowUpAt = now - 60_000L,
            followUpCheckMinutes = 10
        )
        whenever(repository.getAllActiveReminders()).thenReturn(listOf(reminder))
        whenever(workerParams.inputData).thenReturn(workDataOf())

        mockConstruction(NotificationHelper::class.java).use { notificationMocks ->
            val worker = BootRescheduleWorker(app, workerParams)

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            verify(notificationMocks.constructed().single()).showNotification(
                eq(reminder.id),
                eq("Daily walk"),
                eq("Did you complete Daily walk?"),
                isNull(),
                eq("Did you complete Daily walk?"),
                eq(false),
                eq(false),
                eq(false),
                eq(true),
                eq(true)
            )
        }
    }
}
