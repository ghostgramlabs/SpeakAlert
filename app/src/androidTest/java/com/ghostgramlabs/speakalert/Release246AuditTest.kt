package com.ghostgramlabs.speakalert

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.ghostgramlabs.speakalert.data.backup.ReminderBackupManager
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Exploratory regression checks. Failing assertions document bugs, not accepted behavior. */
class Release246AuditTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private val repo get() = app.container.reminderRepository
    private val prefix = "QA-246-"
    private lateinit var originalSettings: androidx.datastore.preferences.core.Preferences
    private fun rows() = runBlocking { repo.getAllRemindersStream().first().filter { it.title?.startsWith(prefix) == true || it.reminderText?.startsWith(prefix) == true } }

    @Before fun setup(): Unit = runBlocking {
        originalSettings = app.dataStore.data.first()
        app.container.settingsRepository.apply {
            setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
            setBatteryOptimizationPromptShown(true)
            dismissBatteryGuidance()
            setShowTypedReminderSection(true)
            setShowShortLabelSection(true)
        }
    }

    @After fun cleanup() = runBlocking {
        rows().forEach {
            app.container.alarmScheduler.cancel(it)
            repo.deleteReminder(it)
            app.container.missedReminderRepository.deleteMissedReminderByReminderId(it.id)
        }
        app.dataStore.updateData { originalSettings }
        Unit
    }

    private fun seed(): ReminderEntity = runBlocking {
        val r = ReminderEntity(title = prefix + "original", reminderText = prefix + "message",
            createdAt = System.currentTimeMillis() - 86_400_000,
            nextTriggerAt = System.currentTimeMillis() + 3_600_000)
        r.copy(id = repo.insertReminder(r))
    }

    private fun editor(r: ReminderEntity, fromHome: Boolean = false): ActivityScenario<MainActivity> {
        val intent = Intent(app, MainActivity::class.java)
        if (!fromHome) intent.putExtra("reminderId", r.id)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        if (fromHome) {
            compose.waitUntil(10_000) { compose.onAllNodesWithText(r.title!!).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText(app.getString(R.string.action_edit)) and hasAnyAncestor(hasContentDescription(r.title!!, substring = true)))
                .performScrollTo().performClick()
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription(app.getString(R.string.action_edit)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(app.getString(R.string.action_edit)).performClick()
        compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo().assertExists()
        return scenario
    }

    @Test fun monthlyLastThursdayPersistsThroughEditor() {
        val r = seed()
        editor(r).use {
            compose.onNodeWithText(app.getString(R.string.ae_repeat)).performScrollTo().performClick()
            compose.onAllNodesWithText(app.getString(R.string.rec_monthly)).onLast().performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.rs_day_of_week)).performScrollTo().performClick()
            compose.onNodeWithText(app.resources.getStringArray(R.array.rs_ordinal_labels).last()).performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.day_thu)).performScrollTo().performClick()
            compose.onAllNodesWithText(app.getString(R.string.action_save)).onLast().performClick()
            compose.onNodeWithText(app.getString(R.string.ae_save_reminder)).performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.getReminder(r.id)?.recurrenceType == com.ghostgramlabs.speakalert.domain.models.RecurrenceType.MONTHLY } }
            val saved = runBlocking { repo.getReminder(r.id)!! }
            val model = com.ghostgramlabs.speakalert.domain.RecurrenceUtils.fromJson(saved.recurrenceType, saved.recurrenceJson) as com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.Monthly
            assertEquals(com.ghostgramlabs.speakalert.domain.models.MonthlyVariant.DAY_OF_WEEK, model.variant)
            assertEquals(-1, model.weekOfMonth)
            assertEquals(4, model.dayOfWeek)
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = saved.nextTriggerAt }
            assertEquals(java.util.Calendar.THURSDAY, cal.get(java.util.Calendar.DAY_OF_WEEK))
            assertTrue(cal.get(java.util.Calendar.DAY_OF_MONTH) + 7 > cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        }
    }

    @Test fun loopPauseAndCardColorsCanBeChangedThroughSettings() {
        val settings = app.container.settingsRepository
        runBlocking { settings.setToneOnlyMode(false) }
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription(app.getString(R.string.home_cd_settings)).performClick()
            compose.onNodeWithText(app.getString(R.string.set_loop_gap)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(app.getString(R.string.set_seconds_short, 10)).performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { settings.loopGapSeconds.first() == 10 } }
            compose.onNodeWithText(app.getString(R.string.set_appearance)).performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.set_card_colors)).performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithContentDescription(app.getString(R.string.color_blue)).onFirst().performScrollTo().performClick()
            compose.onAllNodesWithContentDescription(app.getString(R.string.color_red)).onLast().performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { settings.cardDoneColor.first() == "blue" && settings.cardDeleteColor.first() == "red" } }
            scenario.recreate()
            assertEquals(10, runBlocking { settings.loopGapSeconds.first() })
            assertEquals("blue", runBlocking { settings.cardDoneColor.first() })
            assertEquals("red", runBlocking { settings.cardDeleteColor.first() })
        }
    }

    private fun notificationCase(persist: Boolean, edit: Boolean) {
        val r = seed().copy(lastFiredAt = System.currentTimeMillis() - 60_000)
        runBlocking { repo.updateReminder(r); app.container.settingsRepository.setPersistUntilDone(persist) }
        val manager = app.getSystemService(android.app.NotificationManager::class.java)
        try {
            assertTrue(com.ghostgramlabs.speakalert.alarm.NotificationHelper(app).showNotification(r.id, r.title, r.reminderText, silentAlert = true, persistUntilDone = persist))
            compose.waitUntil(5_000) { manager.activeNotifications.any { it.id == r.id.toInt() } }
            if (edit) {
                editor(r).use {
                    compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performTextReplacement(prefix + "changed")
                    compose.onNodeWithText(app.getString(R.string.ae_save_reminder)).performScrollTo().performClick()
                    compose.waitUntil(5_000) { manager.activeNotifications.none { it.id == r.id.toInt() } }
                }
            } else {
                ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
                    compose.waitForIdle()
                    if (persist) assertTrue(manager.activeNotifications.any { it.id == r.id.toInt() })
                    else compose.waitUntil(5_000) { manager.activeNotifications.none { it.id == r.id.toInt() } }
                }
            }
        } finally { manager.cancel(r.id.toInt()) }
    }
    @Test fun openingHomeClearsFinishedAlert() = notificationCase(false, false)
    @Test fun openingHomeKeepsPersistentAlert() = notificationCase(true, false)
    @Test fun savingEditClearsPersistentAlert() = notificationCase(true, true)
    private fun activePlayers(): Int {
        val fd = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys audio")
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }
            .lineSequence().count { it.contains("AudioPlaybackConfiguration piid:") && it.contains("state:started") && it.contains("usage=USAGE_ALARM") }
    }

    @Test fun recordedLoopPausesRestartsAndStopCancelsPendingReplay() {
        val r = seed()
        val audio = File(app.filesDir, "qa246-loop.wav")
        val count = 16000
        val data = java.nio.ByteBuffer.allocate(44 + count * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + count * 2).put("WAVEfmt ".toByteArray())
        data.putInt(16).putShort(1.toShort()).putShort(1.toShort()).putInt(16000).putInt(32000).putShort(2.toShort()).putShort(16.toShort())
        data.put("data".toByteArray()).putInt(count * 2)
        repeat(count) { data.putShort((kotlin.math.sin(it * 2 * Math.PI * 440 / 16000) * 4000).toInt().toShort()) }
        audio.writeBytes(data.array())
        runBlocking { app.container.settingsRepository.setLoopGapSeconds(5) }
        try {
            ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
                val baseline = activePlayers()
                assertTrue(com.ghostgramlabs.speakalert.service.ReminderPlaybackService.start(app, r.id, r.title, audio.absolutePath, null, loop = true))
                compose.waitUntil(15_000) { activePlayers() > baseline }
                compose.waitUntil(5_000) { activePlayers() == baseline }
                val gapStart = android.os.SystemClock.elapsedRealtime()
                Thread.sleep(2500)
                assertEquals("Audio must stay quiet during the 5 second gap", baseline, activePlayers())
                compose.waitUntil(7_000) { activePlayers() > baseline }
                assertTrue("Loop must not restart before its pause", android.os.SystemClock.elapsedRealtime() - gapStart >= 4000)
                compose.waitUntil(5_000) { activePlayers() == baseline }
                com.ghostgramlabs.speakalert.service.ReminderPlaybackService.stop(app, r.id)
                Thread.sleep(6500)
                assertEquals("Stop during pause must cancel replay", baseline, activePlayers())
                assertNotEquals(r.id, com.ghostgramlabs.speakalert.service.ReminderPlaybackService.playingReminderId)
            }
        } finally {
            app.stopService(Intent(app, com.ghostgramlabs.speakalert.service.ReminderPlaybackService::class.java))
            audio.delete()
        }
    }
}
