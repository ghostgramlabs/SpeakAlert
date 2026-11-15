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
class EmulatorFunctionalAuditTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private val repo get() = app.container.reminderRepository
    private val prefix = "QA-audit-"
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
        compose.waitForIdle()
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

    @Test fun editRetainsOriginalCreationTimestamp() {
        val r = seed()
        editor(r).use {
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performTextReplacement(prefix + "edited")
            compose.onNodeWithText(app.getString(R.string.ae_save_reminder)).performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.getReminder(r.id)?.reminderText == prefix + "edited" } }
            assertEquals("Editing content must preserve creation time", r.createdAt, runBlocking { repo.getReminder(r.id)!!.createdAt })
        }
    }

    @Test fun editDraftSurvivesActivityRecreation() {
        val r = seed()
        editor(r).use { scenario ->
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performTextReplacement(prefix + "unsaved edit")
            compose.onNodeWithText(prefix + "unsaved edit").assertExists()
            scenario.recreate()
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo()
            compose.onNodeWithText(prefix + "unsaved edit").assertExists()
        }
    }

    @Test fun newDraftRemainsDirtyAfterActivityRecreation() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java).putExtra("openAddEdit", true)).use { scenario ->
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo().performTextInput(prefix + "new draft")
            scenario.recreate()
            compose.onNodeWithContentDescription(app.getString(R.string.action_back)).performClick()
            compose.onNodeWithText(app.getString(R.string.ae_unsaved_title)).assertExists()
        }
    }

    @Test fun homeEditDraftSurvivesActivityRecreation() {
        val r = seed()
        editor(r, fromHome = true).use { scenario ->
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performTextReplacement(prefix + "home draft")
            compose.onNodeWithText(prefix + "home draft").assertExists()
            scenario.recreate()
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo()
            compose.onNodeWithText(prefix + "home draft").assertExists()
        }
    }

    @Test fun homeNewDraftRemainsDirtyAfterActivityRecreation() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription(app.getString(R.string.home_cd_add)).performClick()
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo().performTextInput(prefix + "home new draft")
            scenario.recreate()
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo()
            compose.onNodeWithText(prefix + "home new draft").assertExists()
            compose.onNodeWithContentDescription(app.getString(R.string.action_back)).performClick()
            compose.onNodeWithText(app.getString(R.string.ae_unsaved_title)).assertExists()
        }
    }

    private fun repeatOption(label: Int, type: com.ghostgramlabs.speakalert.domain.models.RecurrenceType, submenu: Boolean = false) {
        val r = seed()
        editor(r).use {
            compose.onNodeWithText(app.getString(R.string.ae_repeat)).performScrollTo().performClick()
            compose.onAllNodesWithText(app.getString(label)).onLast().performScrollTo().performClick()
            if (label == R.string.rec_weekly) compose.onNodeWithText(app.getString(R.string.day_mon)).performScrollTo().performClick()
            if (label == R.string.rec_monthly) compose.onNodeWithText(app.getString(R.string.rs_last_day)).performScrollTo().performClick()
            if (submenu) compose.onAllNodesWithText(app.getString(R.string.action_save)).onLast().assertIsEnabled().performClick()
            compose.onNodeWithText(app.getString(R.string.ae_save_reminder)).performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.getReminder(r.id)?.recurrenceType == type } }
            val saved = runBlocking { repo.getReminder(r.id)!! }
            assertTrue(saved.nextTriggerAt > System.currentTimeMillis())
            assertNotNull(com.ghostgramlabs.speakalert.domain.RecurrenceUtils.computeNextTrigger(saved, saved.nextTriggerAt))
        }
    }

    @Test fun dailyRepeatCanBeSaved() = repeatOption(R.string.rec_daily, com.ghostgramlabs.speakalert.domain.models.RecurrenceType.DAILY)
    @Test fun weeklyRepeatCanBeSaved() = repeatOption(R.string.rec_weekly, com.ghostgramlabs.speakalert.domain.models.RecurrenceType.WEEKLY, true)
    @Test fun monthlyRepeatCanBeSaved() = repeatOption(R.string.rec_monthly, com.ghostgramlabs.speakalert.domain.models.RecurrenceType.MONTHLY, true)
    @Test fun yearlyRepeatCanBeSaved() = repeatOption(R.string.rec_yearly, com.ghostgramlabs.speakalert.domain.models.RecurrenceType.YEARLY)
    @Test fun customRepeatCanBeSaved() = repeatOption(R.string.rec_custom, com.ghostgramlabs.speakalert.domain.models.RecurrenceType.CUSTOM, true)

    @Test fun scheduledAlarmActuallyPostsNotification() {
        val r = seed().copy(nextTriggerAt = System.currentTimeMillis() + 7_000)
        val settings = app.container.settingsRepository
        val originalAuto = runBlocking { settings.autoPlayEnabled.first() }
        try {
            runBlocking { settings.setAutoPlayEnabled(false); repo.updateReminder(r) }
            app.container.alarmScheduler.schedule(r)
            val manager = app.getSystemService(android.app.NotificationManager::class.java)
            compose.waitUntil(25_000) { manager.activeNotifications.any { it.id == r.id.toInt() } }
            assertNotNull(runBlocking { repo.getReminder(r.id)!!.lastFiredAt })
        } finally {
            runBlocking { settings.setAutoPlayEnabled(originalAuto) }
            com.ghostgramlabs.speakalert.alarm.NotificationHelper.cancelAlertNotification(app, r.id)
        }
    }

    @Test fun createTextReminderThroughUi() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java).putExtra("openAddEdit", true)).use {
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo().performTextInput(prefix + "new reminder")
            compose.onNodeWithText(app.getString(R.string.ae_save_reminder)).performScrollTo().performClick()
            compose.waitUntil(10_000) { rows().any { it.reminderText == prefix + "new reminder" } }
            val saved = rows().single { it.reminderText == prefix + "new reminder" }
            assertTrue(saved.nextTriggerAt > System.currentTimeMillis())
            assertFalse(saved.isCompleted)
        }
    }

    @Test fun discardEditDoesNotChangeStoredReminder() {
        val r = seed()
        editor(r).use {
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performTextReplacement(prefix + "discard me")
            compose.onNodeWithContentDescription(app.getString(R.string.action_back)).performClick()
            compose.onNodeWithText(app.getString(R.string.ae_unsaved_title)).assertIsDisplayed()
            compose.onNodeWithText(app.getString(R.string.action_discard)).performClick()
            assertEquals(r, runBlocking { repo.getReminder(r.id) })
        }
    }

    @Test fun homeDeleteRequiresConfirmationAndCancelPreservesRow() {
        val r = seed()
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.waitUntil(10_000) { compose.onAllNodesWithText(r.title!!).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText(app.getString(R.string.action_delete)) and hasAnyAncestor(hasContentDescription(r.title!!, substring = true)))
                .performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText(app.getString(R.string.det_delete_title)).fetchSemanticsNodes().isNotEmpty() }
            assertNotNull(runBlocking { repo.getReminder(r.id) })
            compose.onNodeWithText(app.getString(R.string.action_cancel)).performClick()
            assertNotNull(runBlocking { repo.getReminder(r.id) })
        }
    }

    @Test fun backupRoundTripPreservesTextAndAudioAndSkipsDuplicates(): Unit = runBlocking {
        val r = seed()
        val audio = File(app.filesDir, "qa-audit-audio.bin").apply { writeBytes(ByteArray(1024) { (it % 127).toByte() }) }
        val archive = File(app.cacheDir, "qa-audit-backup.zip")
        try {
            val source = r.copy(audioPath = audio.absolutePath, loopPlayback = true, followUpCheckMinutes = 5)
            repo.updateReminder(source)
            assertEquals(1, ReminderBackupManager.export(app, Uri.fromFile(archive), listOf(source)))
            val duplicate = ReminderBackupManager.import(app, Uri.fromFile(archive), repo) {}
            assertEquals(1, duplicate.skippedDuplicates)
            repo.deleteReminder(source)
            val restored = ReminderBackupManager.import(app, Uri.fromFile(archive), repo) {}
            assertEquals(1, restored.imported)
            val copy = rows().single()
            assertEquals(source.reminderText, copy.reminderText)
            assertEquals(source.nextTriggerAt, copy.nextTriggerAt)
            assertEquals(source.followUpCheckMinutes, copy.followUpCheckMinutes)
            assertTrue(copy.loopPlayback)
            assertArrayEquals(audio.readBytes(), File(copy.audioPath!!).readBytes())
            File(copy.audioPath!!).delete()
        } finally { audio.delete(); archive.delete() }
    }
}
