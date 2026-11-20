package com.ghostgramlabs.speakalert

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.dataStore
import com.ghostgramlabs.speakalert.util.SnoozePreferences
import com.ghostgramlabs.speakalert.util.WeekStartPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** "Reset settings to defaults" through the real Settings screen; reminders must survive it. */
class SettingsResetUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private val settings get() = app.container.settingsRepository
    private val repo get() = app.container.reminderRepository
    private lateinit var original: Preferences
    private var originalSnooze = true
    private var originalWeekStart = 0
    private var reminder: ReminderEntity? = null

    @Before fun setup(): Unit = runBlocking {
        original = app.dataStore.data.first()
        originalSnooze = SnoozePreferences.snoozeEnabled(app)
        originalWeekStart = WeekStartPreferences.firstDayOfWeek(app)
        settings.setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
        settings.setBatteryOptimizationPromptShown(true)
        settings.dismissBatteryGuidance()
    }

    @After fun restore() = runBlocking {
        reminder?.let { repo.deleteReminder(it) }
        app.dataStore.updateData { original }
        SnoozePreferences.setSnoozeEnabled(app, originalSnooze)
        WeekStartPreferences.setFirstDayOfWeek(app, originalWeekStart)
        Unit
    }

    @Test fun resetRestoresDefaultsAndKeepsReminders() {
        runBlocking {
            settings.setAutoPlayEnabled(false)
            settings.setDefaultSnoozeDuration(30)
            settings.setQuietTimeEnabled(true)
            settings.setLoopTimeoutMinutes(0)
            val seeded = ReminderEntity(title = "QA-reset", reminderText = "QA-reset message",
                createdAt = System.currentTimeMillis(), nextTriggerAt = System.currentTimeMillis() + 3_600_000)
            reminder = seeded.copy(id = repo.insertReminder(seeded))
        }
        SnoozePreferences.setSnoozeEnabled(app, false)
        WeekStartPreferences.setFirstDayOfWeek(app, java.util.Calendar.MONDAY)

        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription(app.getString(R.string.home_cd_settings)).performClick()
            compose.onNodeWithText(app.getString(R.string.set_section_backup)).performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.set_reset_defaults)).performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.set_reset_defaults_title)).assertIsDisplayed()
            compose.onNodeWithText(app.getString(R.string.set_reset_defaults_confirm)).performClick()
            compose.waitUntil(5_000) { SnoozePreferences.snoozeEnabled(app) }
        }

        runBlocking {
            assertTrue(settings.autoPlayEnabled.first())
            assertEquals(5, settings.defaultSnoozeDuration.first())
            assertFalse(settings.quietTimeEnabled.first())
            assertEquals(10, settings.loopTimeoutMinutes.first())
            assertEquals(0, WeekStartPreferences.firstDayOfWeek(app))
            assertEquals(BuildConfig.VERSION_NAME, settings.lastWhatsNewVersionShown.first())
            assertNotNull("Reset must not delete reminders", repo.getReminder(reminder!!.id))
        }
    }

    @Test fun cancelLeavesSettingsAlone() {
        runBlocking { settings.setDefaultSnoozeDuration(30) }
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription(app.getString(R.string.home_cd_settings)).performClick()
            compose.onNodeWithText(app.getString(R.string.set_section_backup)).performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.set_reset_defaults)).performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.action_cancel)).performClick()
            compose.onAllNodesWithText(app.getString(R.string.set_reset_defaults_title)).assertCountEquals(0)
        }
        assertEquals(30, runBlocking { settings.defaultSnoozeDuration.first() })
    }
}
