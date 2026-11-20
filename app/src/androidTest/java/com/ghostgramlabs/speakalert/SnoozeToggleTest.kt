package com.ghostgramlabs.speakalert

import android.app.NotificationManager
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.ghostgramlabs.speakalert.alarm.NotificationHelper
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.util.SnoozePreferences
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** "Allow snooze": off removes Snooze from the reminder notification and hides its length. */
class SnoozeToggleTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private val repo get() = app.container.reminderRepository
    private val manager get() = app.getSystemService(NotificationManager::class.java)
    private var reminder: ReminderEntity? = null

    @Before fun setup(): Unit = runBlocking {
        app.container.settingsRepository.apply {
            setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
            setBatteryOptimizationPromptShown(true)
            dismissBatteryGuidance()
        }
    }

    @After fun cleanup() = runBlocking {
        SnoozePreferences.setSnoozeEnabled(app, true)
        reminder?.let { manager.cancel(it.id.toInt()); repo.deleteReminder(it) }
        Unit
    }

    private fun postedActionTitles(enabled: Boolean): List<String> {
        SnoozePreferences.setSnoozeEnabled(app, enabled)
        val r = reminder ?: runBlocking {
            val seeded = ReminderEntity(title = "QA-snooze", reminderText = "QA-snooze message",
                createdAt = System.currentTimeMillis(), nextTriggerAt = System.currentTimeMillis() + 3_600_000)
            seeded.copy(id = repo.insertReminder(seeded))
        }.also { reminder = it }
        assertTrue(NotificationHelper(app).showNotification(r.id, r.title, r.reminderText, reminderText = r.reminderText, silentAlert = true))
        compose.waitUntil(5_000) { manager.activeNotifications.any { it.id == r.id.toInt() } }
        val posted = manager.activeNotifications.first { it.id == r.id.toInt() }.notification
        return posted.actions.orEmpty().map { it.title.toString() }
    }

    @Test fun notificationOffersSnoozeOnlyWhenAllowed() {
        val snooze = app.getString(R.string.notif_action_snooze)
        val done = app.getString(R.string.notif_action_done)
        assertTrue(postedActionTitles(enabled = true).contains(snooze))
        val withoutSnooze = postedActionTitles(enabled = false)
        assertFalse(withoutSnooze.contains(snooze))
        assertTrue(withoutSnooze.contains(done))
    }

    @Test fun settingsSwitchTurnsSnoozeOffAndHidesItsLength() {
        SnoozePreferences.setSnoozeEnabled(app, true)
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription(app.getString(R.string.home_cd_settings)).performClick()
            compose.onNodeWithText(app.getString(R.string.set_section_timing)).performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.set_default_snooze)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(app.getString(R.string.set_snooze_enabled)).performScrollTo().performClick()
            compose.waitUntil(5_000) { !SnoozePreferences.snoozeEnabled(app) }
            compose.onAllNodesWithText(app.getString(R.string.set_default_snooze)).assertCountEquals(0)
        }
    }
}
