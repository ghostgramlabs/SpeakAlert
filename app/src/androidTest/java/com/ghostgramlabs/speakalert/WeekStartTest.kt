package com.ghostgramlabs.speakalert

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ghostgramlabs.speakalert.ui.components.SystemDatePickerDialog
import com.ghostgramlabs.speakalert.ui.components.shouldUseSystemDatePicker
import com.ghostgramlabs.speakalert.util.WeekStartPreferences
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.Calendar

/** "Week starts on": the Settings choice is saved and selects the date picker that honors it. */
class WeekStartTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()

    @Before fun setup(): Unit = runBlocking {
        app.container.settingsRepository.apply {
            setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
            setBatteryOptimizationPromptShown(true)
            dismissBatteryGuidance()
        }
        WeekStartPreferences.setFirstDayOfWeek(app, 0)
    }

    @After fun cleanup() = WeekStartPreferences.setFirstDayOfWeek(app, 0)

    @Test fun settingsChoiceIsSaved() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription(app.getString(R.string.home_cd_settings)).performClick()
            compose.onNodeWithText(app.getString(R.string.set_appearance)).performScrollTo().performClick()
            compose.onNodeWithText(app.getString(R.string.set_week_start)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(app.getString(R.string.day_mon)).performScrollTo().performClick()
            compose.waitUntil(5_000) { WeekStartPreferences.firstDayOfWeek(app) == Calendar.MONDAY }
            assertTrue(shouldUseSystemDatePicker(app))
        }
    }

    /** Saves a screenshot of the picker per setting for a visual check of the first column. */
    @Test fun datePickerScreenshots() {
        listOf(Calendar.MONDAY to "monday", Calendar.SUNDAY to "sunday").forEach { (day, name) ->
            WeekStartPreferences.setFirstDayOfWeek(app, day)
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { it.setContent { SystemDatePickerDialog(1_800_000_000_000L, {}, {}) } }
                Thread.sleep(2_000)
                val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                assertNotNull(shot)
                File(app.getExternalFilesDir(null), "week_start_$name.png").outputStream().use {
                    shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
