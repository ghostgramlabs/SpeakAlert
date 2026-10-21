package com.ghostgramlabs.speakalert

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.ui.alert.ReminderAlertActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ReminderDesignTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private fun snapshot(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            File(app.getExternalFilesDir(null), name).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }

    @Test fun cardActionsConfirmDeleteAndBatteryChoicePersists() {
        val repo = app.container.reminderRepository
        val settings = app.container.settingsRepository
        val id = runBlocking {
            settings.setBatteryOptimizationPromptShown(true)
            settings.setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
            repo.insertReminder(ReminderEntity(title = "Take a break", reminderText = "Stretch and drink some water.",
                nextTriggerAt = System.currentTimeMillis() + 60_000))
        }
        try {
            ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use { scenario ->
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithText(app.getString(R.string.battery_keep_current)).fetchSemanticsNodes().isNotEmpty() ||
                        runBlocking { settings.batteryGuidanceDismissed.first() }
                }
                if (!runBlocking { settings.batteryGuidanceDismissed.first() }) {
                    snapshot("design-battery.png")
                    compose.onNodeWithText(app.getString(R.string.battery_keep_current)).performClick()
                }
                compose.waitUntil(10_000) { runBlocking { settings.batteryGuidanceDismissed.first() } }
                scenario.recreate()
                compose.onNodeWithText(app.getString(R.string.battery_keep_current)).assertDoesNotExist()
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithText("Take a break").fetchSemanticsNodes().isNotEmpty()
                }
                // Scroll to the actual card footer before clicking its action.
                compose.onNode(hasText(app.getString(R.string.action_delete)) and
                    hasAnyAncestor(hasContentDescription("Take a break", substring = true))).performScrollTo().performClick()
                compose.onNodeWithText(app.getString(R.string.det_delete_title)).assertIsDisplayed()
                assertNotNull(runBlocking { repo.getReminder(id) })
                snapshot("design-delete-confirmation.png")
                compose.onNodeWithText(app.getString(R.string.action_cancel)).performClick()
                assertNotNull(runBlocking { repo.getReminder(id) })
                snapshot("design-card.png")
            }
        } finally {
            runBlocking { repo.getReminder(id)?.let { repo.deleteReminder(it) } }
        }
    }

    @Test fun alertPromotesMessageInsteadOfAppName() {
        val repo = app.container.reminderRepository
        val message = "Take your medication after breakfast"
        val id = runBlocking { repo.insertReminder(ReminderEntity(title = "SpeakAlert", reminderText = message,
            nextTriggerAt = System.currentTimeMillis())) }
        try {
            ActivityScenario.launch<ReminderAlertActivity>(Intent(app, ReminderAlertActivity::class.java)
                .putExtra("reminderId", id)).use {
                compose.waitUntil(10_000) { compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(message).assertIsDisplayed()
                assertTrue(compose.onNodeWithText(message).fetchSemanticsNode().boundsInRoot.height >
                    compose.onNodeWithText("SpeakAlert").fetchSemanticsNode().boundsInRoot.height)
                snapshot("design-alert.png")
            }
        } finally {
            runBlocking { repo.getReminder(id)?.let { repo.deleteReminder(it) } }
        }
    }
}
