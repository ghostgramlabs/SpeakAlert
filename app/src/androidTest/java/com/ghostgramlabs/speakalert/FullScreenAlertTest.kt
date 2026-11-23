package com.ghostgramlabs.speakalert

import android.app.KeyguardManager
import android.app.NotificationManager
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.ghostgramlabs.speakalert.alarm.NotificationHelper
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.dataStore
import com.ghostgramlabs.speakalert.ui.alert.ReminderAlertActivity
import com.ghostgramlabs.speakalert.util.FullScreenIntentSupport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

/**
 * End-to-end full-screen alert: a real alarm fires through AlarmManager and the system decides
 * whether to launch the full-screen intent. Covers screen off, swipe keyguard, screen on, and
 * the setting turned off.
 */
@FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
class FullScreenAlertTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private val repo get() = app.container.reminderRepository
    private val settings get() = app.container.settingsRepository
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val power = app.getSystemService(PowerManager::class.java)
    private val keyguard = app.getSystemService(KeyguardManager::class.java)
    private val notifications = app.getSystemService(NotificationManager::class.java)
    private val message = "QA-fullscreen take the tablets"
    private lateinit var originalSettings: androidx.datastore.preferences.core.Preferences
    private var lockWasDisabled = true
    private var reminder: ReminderEntity? = null
    private val pin = "1234"
    private var pinSet = false
    private var snoozeWasEnabled = true
    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun awaitAlert() {
        assertTrue("Alert activity should take over the screen", waitFor(15_000) { alertActivity() != null })
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.alert_mark_done)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitText(value: String, present: Boolean = true) =
        compose.waitUntil(15_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() == present }

    private fun snoozeFromAlert(minutes: Int) {
        sleep()
        val row = fireAlarm()
        awaitAlert()
        val before = System.currentTimeMillis()
        compose.onNodeWithText(text(R.string.alert_snooze_minutes, minutes)).assertIsDisplayed().performClick()
        assertTrue("Snooze should close the alert", waitFor(10_000) { alertActivity() == null })
        assertTrue("Snooze should clear the notification",
            waitFor(10_000) { notifications.activeNotifications.none { it.id == row.id.toInt() } })
        assertTrue("Snooze should be stored", waitFor(10_000) { runBlocking { repo.getReminder(row.id)?.snoozeUntil } != null })
        val saved = runBlocking { repo.getReminder(row.id)!! }
        val delta = saved.snoozeUntil!! - before
        assertTrue("Snooze should ring in about $minutes minutes, got ${delta / 1000}s",
            delta in (minutes - 1) * 60_000L..minutes * 60_000L + 5_000)
        assertEquals("Snooze must not move the reminder's own time", row.nextTriggerAt, saved.nextTriggerAt)
        assertFalse(saved.isCompleted)
    }

    private fun sh(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }

    private fun alertActivity(): ReminderAlertActivity? {
        var found: ReminderAlertActivity? = null
        instrumentation.runOnMainSync {
            found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<ReminderAlertActivity>().firstOrNull()
        }
        return found
    }

    private fun waitFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(250)
        }
        return condition()
    }

    private fun shot(name: String) {
        Thread.sleep(800)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        File(app.getExternalFilesDir(null), "fullscreen_$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun wake() {
        sh("input keyevent KEYCODE_WAKEUP")
        sh("wm dismiss-keyguard")
        waitFor(5_000) { power.isInteractive && !keyguard.isKeyguardLocked }
    }

    /** Unlocks with the PIN before removing it; clearing a PIN from a locked device strands the keyguard. */
    private fun removePin() {
        sh("input keyevent KEYCODE_WAKEUP")
        sh("wm dismiss-keyguard")
        Thread.sleep(1_500)
        sh("input text $pin")
        sh("input keyevent KEYCODE_ENTER")
        waitFor(8_000) { !keyguard.isKeyguardLocked }
        sh("locksettings clear --old $pin")
        pinSet = false
    }

    private fun sleep() {
        sh("input keyevent KEYCODE_SLEEP")
        assertTrue("Screen should turn off", waitFor(8_000) { !power.isInteractive })
    }

    /** Schedules a real alarm a few seconds out and waits for its notification. */
    private fun fireAlarm(text: String = message): ReminderEntity {
        val row = ReminderEntity(title = "QA-fullscreen", reminderText = text,
            nextTriggerAt = System.currentTimeMillis() + 7_000)
        val saved = row.copy(id = runBlocking { repo.insertReminder(row) })
        reminder = saved
        app.container.alarmScheduler.schedule(saved)
        assertTrue("Alarm should post its notification",
            waitFor(30_000) { notifications.activeNotifications.any { it.id == saved.id.toInt() } })
        return saved
    }

    @Before fun setup(): Unit = runBlocking {
        originalSettings = app.dataStore.data.first()
        // A run that died (emulator crash) leaves its reminder behind, and a missed one covers Home.
        repo.getAllRemindersStream().first().filter { it.title == "QA-fullscreen" }.forEach {
            app.container.alarmScheduler.cancel(it)
            repo.deleteReminder(it)
            app.container.missedReminderRepository.deleteMissedReminderByReminderId(it.id)
        }
        lockWasDisabled = sh("locksettings get-disabled").trim() == "true"
        snoozeWasEnabled = com.ghostgramlabs.speakalert.util.SnoozePreferences.snoozeEnabled(app)
        com.ghostgramlabs.speakalert.util.SnoozePreferences.setSnoozeEnabled(app, true)
        settings.setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
        settings.setBatteryOptimizationPromptShown(true)
        settings.dismissBatteryGuidance()
        settings.setAutoPlayEnabled(false)
        settings.setFullScreenAlertEnabled(true)
        wake()
        sh("input keyevent KEYCODE_HOME")
    }

    @After fun cleanup() = runBlocking {
        alertActivity()?.let { activity -> instrumentation.runOnMainSync { activity.finish() } }
        reminder?.let {
            app.container.alarmScheduler.cancel(it)
            NotificationHelper.cancelAlertNotification(app, it.id)
            repo.getReminder(it.id)?.let { row -> repo.deleteReminder(row) }
            app.container.missedReminderRepository.deleteMissedReminderByReminderId(it.id)
        }
        com.ghostgramlabs.speakalert.service.ReminderPlaybackService.stop(app)
        if (pinSet) removePin()
        wake()
        sh("appops set ${app.packageName} USE_FULL_SCREEN_INTENT default")
        com.ghostgramlabs.speakalert.util.SnoozePreferences.setSnoozeEnabled(app, snoozeWasEnabled)
        sh("locksettings set-disabled $lockWasDisabled")
        wake()
        app.dataStore.updateData { originalSettings }
        Unit
    }

    @Test fun systemAllowsFullScreenIntent() {
        assertTrue("System must allow full-screen intents", FullScreenIntentSupport.canUseFullScreenIntent(app))
    }

    @Test fun screenOffAlarmWakesScreenAndShowsAlertThenDoneDismisses() {
        sleep()
        val row = fireAlarm()
        assertTrue("Alert activity should take over the screen", waitFor(15_000) { alertActivity() != null })
        assertTrue("Alert should turn the screen on", waitFor(5_000) { power.isInteractive })
        compose.waitUntil(10_000) { compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.alert_mark_done)).assertIsDisplayed()
        shot("screen_off")
        compose.onNodeWithText(app.getString(R.string.alert_mark_done)).performClick()
        assertTrue("Done should close the alert", waitFor(10_000) { alertActivity() == null })
        assertTrue("Done should clear the notification",
            waitFor(10_000) { notifications.activeNotifications.none { it.id == row.id.toInt() } })
    }

    @Test fun keyguardAlarmShowsAlertOverLockScreen() {
        sh("locksettings set-disabled false")
        sleep()
        sh("input keyevent KEYCODE_WAKEUP")
        assertTrue("Swipe keyguard should be showing", waitFor(8_000) { keyguard.isKeyguardLocked })
        fireAlarm()
        assertTrue("Alert activity should show over the lock screen", waitFor(15_000) { alertActivity() != null })
        assertTrue("Keyguard should still be locked underneath", keyguard.isKeyguardLocked)
        compose.waitUntil(10_000) { compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(message).assertIsDisplayed()
        shot("keyguard")
    }

    @Test fun screenOnUnlockedAlarmDoesNotTakeOverScreen() {
        assertTrue(power.isInteractive)
        assertFalse(keyguard.isKeyguardLocked)
        fireAlarm()
        assertFalse("Alert must not take over an unlocked, lit screen", waitFor(6_000) { alertActivity() != null })
        shot("screen_on")
    }

    @Test fun spokenAlertShowsPlayingThenSilenceThenPlayAgain() {
        val service = com.ghostgramlabs.speakalert.service.ReminderPlaybackService
        runBlocking {
            settings.setAutoPlayEnabled(true)
            settings.setAutoPlayOnUnlockOnly(false)
            settings.setSpeakTextIfNoVoice(true)
            settings.setToneOnlyMode(false)
            settings.setPrivatePlaybackEnabled(false)
        }
        val long = "QA-fullscreen " + (1..40).joinToString(" ") { "please remember to take tablet number $it" }
        sleep()
        val row = fireAlarm(long)
        awaitAlert()
        awaitText(text(R.string.alert_playing_now))
        compose.onNodeWithText(text(R.string.alert_silence_now)).assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.alert_play_again)).assertCountEquals(0)
        assertEquals(row.id, service.playingReminderId)
        shot("playing")
        compose.onNodeWithText(text(R.string.alert_silence_now)).performClick()
        awaitText(text(R.string.alert_play_again))
        compose.onAllNodesWithText(text(R.string.alert_silence_now)).assertCountEquals(0)
        assertTrue("Silence should stop playback", waitFor(5_000) { service.playingReminderId != row.id })
        assertNotNull("Silence must leave the alert on screen", alertActivity())
        shot("silenced")
        compose.onNodeWithText(text(R.string.alert_play_again)).performClick()
        awaitText(text(R.string.alert_playing_now))
        assertTrue("Play again should restart playback", waitFor(5_000) { service.playingReminderId == row.id })
        compose.onNodeWithText(text(R.string.alert_mark_done)).performClick()
        assertTrue("Done should close the alert", waitFor(10_000) { alertActivity() == null })
        assertTrue("Done should stop playback", waitFor(10_000) { service.playingReminderId != row.id })
    }

    @Test fun snoozeFiveFromAlertSchedulesFiveMinutes() = snoozeFromAlert(5)

    @Test fun snoozeTenFromAlertSchedulesTenMinutes() = snoozeFromAlert(10)

    @Test fun snoozeDisabledHidesSnoozeButtonsOnAlert() {
        com.ghostgramlabs.speakalert.util.SnoozePreferences.setSnoozeEnabled(app, false)
        sleep()
        fireAlarm()
        awaitAlert()
        compose.onAllNodesWithText(text(R.string.alert_snooze_minutes, 5)).assertCountEquals(0)
        compose.onAllNodesWithText(text(R.string.alert_snooze_minutes, 10)).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.alert_mark_done)).assertIsDisplayed()
        shot("no_snooze")
    }

    // Named to sort last: it changes the device credential.
    @Test fun zPinLockAlarmShowsAlertAndDoneWorksWithoutUnlocking() {
        sh("locksettings set-disabled false")
        sh("locksettings set-pin $pin")
        pinSet = true
        sleep()
        sh("input keyevent KEYCODE_WAKEUP")
        assertTrue("PIN keyguard should be showing", waitFor(8_000) { keyguard.isKeyguardLocked })
        assertTrue(keyguard.isDeviceSecure)
        sleep()
        val row = fireAlarm()
        awaitAlert()
        assertTrue("Alert should turn the screen on", waitFor(5_000) { power.isInteractive })
        assertTrue("Device should stay locked under the alert", keyguard.isKeyguardLocked)
        compose.onNodeWithText(message).assertIsDisplayed()
        shot("pin_lock")
        compose.onNodeWithText(text(R.string.alert_mark_done)).performClick()
        assertTrue("Done should close the alert", waitFor(10_000) { alertActivity() == null })
        assertTrue("Done should clear the notification",
            waitFor(10_000) { notifications.activeNotifications.none { it.id == row.id.toInt() } })
        assertTrue("Done must not unlock the device", keyguard.isKeyguardLocked)
    }

    @Test fun revokedFullScreenAccessFallsBackToNotification() {
        sh("appops set ${app.packageName} USE_FULL_SCREEN_INTENT deny")
        assertTrue("Revocation should be visible to the app",
            waitFor(5_000) { !FullScreenIntentSupport.canUseFullScreenIntent(app) })
        sleep()
        fireAlarm()
        assertFalse("Alert must not take over without system access", waitFor(6_000) { alertActivity() != null })
    }

    @Test fun settingOffAlarmDoesNotTakeOverScreen() {
        runBlocking { settings.setFullScreenAlertEnabled(false) }
        sleep()
        fireAlarm()
        assertFalse("Alert must stay a normal notification when the setting is off",
            waitFor(6_000) { alertActivity() != null })
    }
}
