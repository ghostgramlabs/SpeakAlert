package com.ghostgramlabs.speakalert

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ghostgramlabs.speakalert.data.repository.dataStore
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.domain.RecurrenceUtils
import com.ghostgramlabs.speakalert.domain.models.*
import com.ghostgramlabs.speakalert.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.Locale

/** Run once per app locale (de, fr, id). Restores settings and removes its temporary reminder. */
class TranslationEmulatorAuditTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private lateinit var settings: androidx.datastore.preferences.core.Preferences
    private val language get() = app.resources.configuration.locales[0].language
    private fun text(id: Int) = app.getString(id)

    @Before fun setup(): Unit = runBlocking {
        settings = app.dataStore.updateData { it }
        app.container.settingsRepository.apply {
            setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
            setBatteryOptimizationPromptShown(true)
            dismissBatteryGuidance()
            setShowTypedReminderSection(true)
        }
    }

    @After fun restore() = runBlocking { app.dataStore.updateData { settings }; Unit }

    private fun shot(name: String) {
        compose.waitForIdle()
        // UIAutomation captures the display, which may trail the Compose semantics frame.
        Thread.sleep(600)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        assertNotNull(bitmap)
        File(app.getExternalFilesDir(null), "translation_${language}_$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun translatedSettingsAndHelp() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription(text(R.string.home_cd_settings)).performClick()
            shot("settings")
            compose.onNodeWithText(text(R.string.set_appearance)).performScrollTo().performClick()
            compose.onNodeWithText("Deutsch").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Français").assertExists()
            compose.onNodeWithText("Bahasa Indonesia").assertExists()
            shot("languages")
            compose.onNodeWithText(text(R.string.set_reminder_form)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.set_experimental_voice_enhancement)).performScrollTo().assertIsDisplayed()
            shot("recording_settings")
        }
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription(text(R.string.home_cd_help)).performClick()
            shot("help")
        }
    }

    @Test fun customIntervalsAndEndDate() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java).putExtra("openAddEdit", true)).use {
            compose.onNodeWithText(text(R.string.ae_date)).performScrollTo().assertIsDisplayed()
            shot("editor_date")
            compose.onNodeWithText(text(R.string.ae_repeat)).performScrollTo().performClick()
            compose.onAllNodesWithText(text(R.string.rec_custom)).onLast().performScrollTo().performClick()
            for (count in listOf(1, 2)) {
                compose.onNodeWithText(text(R.string.rs_number)).performTextReplacement(count.toString())
                if (language == "de") {
                    compose.onNodeWithText("Wiederholungsintervall: $count", substring = true).assertExists()
                }
                shot("interval_$count")
            }
            compose.onNodeWithText(text(R.string.rs_until_date)).performScrollTo().performClick()
            compose.onNodeWithText(text(R.string.rs_must_be_after).substringBefore("%1"), substring = true).performScrollTo().assertIsDisplayed()
            shot("end_date")
        }
    }

    @Test fun platformDateOrderAndTranslatedResources() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            assertTrue("Expected a new locale, got $language", language in listOf("de", "fr", "id", "in"))
            assertEquals(language, Locale.getDefault().language)
            for (parts in DateUtils.DateParts.values()) {
                val pattern = DateUtils.datePattern(parts)
                assertTrue("Day must precede month: $pattern", pattern.indexOf('d') < pattern.indexOf('M'))
            }
            assertNotEquals("Voice · improving…", text(R.string.card_type_voice_improving))
            assertNotEquals("Backup", text(R.string.set_section_backup))
            if (language == "de") {
                assertEquals("Endet nach 2 Wiederholungen", app.resources.getQuantityString(R.plurals.rec_ends_times, 2, 2))
            }
        }
    }

    @Test fun reminderCardDetailsAndDatePicker() {
        val repo = app.container.reminderRepository
        val due = System.currentTimeMillis() + 86_400_000L * 3
        val end = due + 86_400_000L * 30
        val model = RecurrenceModel.Custom(2, TimeUnit.DAYS,
            endRule = RecurrenceEndRule(EndRuleType.UNTIL_DATE, endDateMillis = end))
        val row = ReminderEntity(title = "QA-translation-$language", reminderText = "QA translation",
            nextTriggerAt = due, recurrenceType = RecurrenceType.CUSTOM, recurrenceJson = RecurrenceUtils.toJson(model))
        val id = runBlocking { repo.insertReminder(row) }
        try {
            ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
                compose.onNodeWithText(text(R.string.home_filter_upcoming)).performClick()
                compose.onNodeWithText(row.title!!).performScrollTo().assertIsDisplayed()
                if (language == "de") compose.onNodeWithText("Intervall: 2 Tage").assertIsDisplayed()
                val chip = "${DateUtils.formatDate(end, DateUtils.DateParts.MONTH_DAY_YEAR)} • ${DateUtils.formatTimeOnly(end)}"
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(chip, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue("End-date chip has a text layout", layouts.isNotEmpty())
                layouts.forEach { layout ->
                    assertFalse("End-date chip must not hide the time", layout.didOverflowHeight)
                    for (line in 0 until layout.lineCount) assertFalse("End-date chip must not ellipsize", layout.isLineEllipsized(line))
                }
                shot("card")
            }
            ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java).putExtra("reminderId", id)).use {
                compose.onNodeWithText(text(R.string.det_end_rule)).performScrollTo().assertIsDisplayed()
                val dateTime = app.getString(R.string.ae_datetime_at,
                    DateUtils.formatDate(end, DateUtils.DateParts.MONTH_DAY_YEAR), DateUtils.formatTimeOnly(end))
                compose.onNodeWithText(app.getString(R.string.det_ends_by, dateTime)).assertExists()
                shot("details")
                compose.onNodeWithText(text(R.string.ae_date)).performScrollTo().performClick()
                shot("inline_date_picker")
            }
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    com.ghostgramlabs.speakalert.ui.theme.VoiceReminderTheme {
                        com.ghostgramlabs.speakalert.ui.components.DateTimePickerDialog(due, {}, {})
                    }
                } }
                compose.onNodeWithText(text(R.string.dtp_title)).assertIsDisplayed()
                shot("date_time_picker")
            }
        } finally {
            runBlocking { repo.deleteReminder(row.copy(id = id)) }
        }
    }
}
