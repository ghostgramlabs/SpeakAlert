package com.ghostgramlabs.speakalert

import android.content.Intent
import android.media.Ringtone
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ghostgramlabs.speakalert.alarm.NotificationHelper
import com.ghostgramlabs.speakalert.alarm.SystemReminderAlertNotifications
import com.ghostgramlabs.speakalert.alarm.ToneAlertPlayer
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.ui.home.HomeViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real ringtone + Room + screen navigation regression coverage on an Android device. */
@RunWith(AndroidJUnit4::class)
class ActiveToneResolutionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private val container get() = app.container
    private val ids = mutableListOf<Long>()

    private fun seed(): ReminderEntity = runBlocking {
        val reminder = ReminderEntity(title = "Tone resolution regression", nextTriggerAt = System.currentTimeMillis() - 1000)
        val id = container.reminderRepository.insertReminder(reminder)
        ids.add(id)
        reminder.copy(id = id)
    }

    private fun startTone(id: Long): Ringtone {
        ToneAlertPlayer.start(app, 1, reminderId = id)
        val field = ToneAlertPlayer::class.java.getDeclaredField("ringtone").apply { isAccessible = true }
        val tone = field.get(ToneAlertPlayer) as Ringtone
        assertTrue("Test must begin with a playing ringtone", tone.isPlaying)
        return tone
    }

    @After fun cleanup() = runBlocking {
        ToneAlertPlayer.stop()
        ids.forEach { id ->
            container.reminderRepository.getReminder(id)?.let {
                container.alarmScheduler.cancel(it)
                container.reminderRepository.deleteReminder(it)
            }
            NotificationHelper.cancelAlertNotification(app, id)
            container.missedReminderRepository.deleteMissedReminderByReminderId(id)
        }
    }

    @Test fun detailsDonePersistsAfterNavigationAndStopsTone() {
        val reminder = seed()
        val tone = startTone(reminder.id)
        ActivityScenario.launch<MainActivity>(
            Intent(app, MainActivity::class.java).putExtra("reminderId", reminder.id)
        ).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(app.getString(R.string.det_mark_done)).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(app.getString(R.string.det_mark_done)).performClick()
            compose.waitUntil(10_000) {
                runBlocking { container.reminderRepository.getReminder(reminder.id)?.isCompleted == true } &&
                    !tone.isPlaying
            }
            assertTrue(runBlocking { container.reminderRepository.getReminder(reminder.id)!!.isCompleted })
            assertFalse(tone.isPlaying)
        }
    }

    @Test fun homeDoneStopsToneAndPersists() {
        val reminder = seed()
        val tone = startTone(reminder.id)
        val vm = HomeViewModel(container.reminderRepository, container.missedReminderRepository,
            container.alarmScheduler, container.settingsRepository,
            SystemReminderAlertNotifications(app, container.settingsRepository))
        vm.completeReminder(reminder)
        compose.waitUntil(10_000) {
            runBlocking { container.reminderRepository.getReminder(reminder.id)?.isCompleted == true } &&
                !tone.isPlaying
        }
    }

    @Test fun resolvingDifferentReminderDoesNotStopActiveTone() {
        val active = seed()
        val other = seed()
        val tone = startTone(active.id)
        SystemReminderAlertNotifications(app, container.settingsRepository).onResolved(other.id)
        assertTrue(tone.isPlaying)
        SystemReminderAlertNotifications(app, container.settingsRepository).onResolved(active.id)
        assertFalse(tone.isPlaying)
    }

    @Test fun notificationDoneStopsToneAndPersists() {
        val reminder = seed()
        val tone = startTone(reminder.id)
        app.sendBroadcast(Intent(app, com.ghostgramlabs.speakalert.alarm.ReminderActionReceiver::class.java)
            .setAction("ACTION_DONE").putExtra("reminderId", reminder.id))
        compose.waitUntil(10_000) {
            runBlocking { container.reminderRepository.getReminder(reminder.id)?.isCompleted == true } &&
                !tone.isPlaying
        }
    }

    @Test fun notificationSnoozeStopsToneAndKeepsReminderPending() {
        val reminder = seed()
        val tone = startTone(reminder.id)
        app.sendBroadcast(Intent(app, com.ghostgramlabs.speakalert.alarm.ReminderActionReceiver::class.java)
            .setAction("ACTION_SNOOZE").putExtra("reminderId", reminder.id))
        compose.waitUntil(10_000) {
            runBlocking {
                container.reminderRepository.getReminder(reminder.id)?.let {
                    !it.isCompleted && (it.snoozeUntil ?: it.nextTriggerAt) > System.currentTimeMillis()
                } == true
            } && !tone.isPlaying
        }
    }

    private fun details(reminder: ReminderEntity) =
        com.ghostgramlabs.speakalert.ui.details.ReminderDetailsViewModel(
            container.reminderRepository, container.alarmScheduler, container.settingsRepository, app,
            alertNotifications = SystemReminderAlertNotifications(app, container.settingsRepository),
            missedRepository = container.missedReminderRepository
        ).also { vm ->
            vm.loadReminder(reminder.id)
            compose.waitUntil(10_000) { vm.reminder.value != null }
        }

    @Test fun detailsRescheduleStopsToneAndClearsOldSnooze() {
        val reminder = seed().copy(snoozeUntil = System.currentTimeMillis() + 60_000)
        runBlocking { container.reminderRepository.updateReminder(reminder) }
        val tone = startTone(reminder.id)
        val vm = details(reminder)
        val newTime = System.currentTimeMillis() + 600_000
        vm.reschedule(newTime)
        compose.waitUntil(10_000) {
            runBlocking {
                container.reminderRepository.getReminder(reminder.id)?.let {
                    it.nextTriggerAt == newTime && it.snoozeUntil == null && !it.isCompleted
                } == true
            } && !tone.isPlaying
        }
    }

    @Test fun missedRemindAgainStopsToneAndSchedulesLater() {
        val reminder = seed()
        val missed = runBlocking {
            container.missedReminderRepository.insertMissedReminder(
                com.ghostgramlabs.speakalert.data.model.MissedReminderEntity(
                    reminderId = reminder.id, title = reminder.title.orEmpty(), scheduledTime = reminder.nextTriggerAt
                )
            )
            container.missedReminderRepository.allMissedReminders.first().first { it.reminderId == reminder.id }
        }
        val tone = startTone(reminder.id)
        val vm = HomeViewModel(container.reminderRepository, container.missedReminderRepository,
            container.alarmScheduler, container.settingsRepository,
            SystemReminderAlertNotifications(app, container.settingsRepository))
        vm.remindAgainForMissed(listOf(missed))
        compose.waitUntil(10_000) {
            runBlocking {
                container.reminderRepository.getReminder(reminder.id)?.nextTriggerAt ?: 0
            } > System.currentTimeMillis() && !tone.isPlaying
        }
    }

    @Test fun detailsDeleteStopsToneAndDeletesReminder() {
        val reminder = seed()
        val tone = startTone(reminder.id)
        details(reminder).deleteReminder()
        compose.waitUntil(10_000) {
            runBlocking { container.reminderRepository.getReminder(reminder.id) == null } && !tone.isPlaying
        }
    }

    @Test fun recurringDismissStopsToneAndPreservesNextOccurrence() {
        val reminder = seed().copy(
            recurrenceType = com.ghostgramlabs.speakalert.domain.models.RecurrenceType.DAILY,
            nextTriggerAt = System.currentTimeMillis() + 86_400_000,
            pendingFollowUpAt = System.currentTimeMillis() + 60_000
        )
        runBlocking { container.reminderRepository.updateReminder(reminder) }
        val tone = startTone(reminder.id)
        details(reminder).dismissReminder(app)
        compose.waitUntil(10_000) {
            runBlocking {
                container.reminderRepository.getReminder(reminder.id)?.let {
                    !it.isCompleted && it.pendingFollowUpAt == null && it.nextTriggerAt == reminder.nextTriggerAt
                } == true
            } && !tone.isPlaying
        }
    }
}
