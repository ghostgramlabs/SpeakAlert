package com.ghostgramlabs.speakalert

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.data.repository.dataStore
import com.ghostgramlabs.speakalert.domain.models.RecurrenceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.Calendar

/**
 * User report (Nov 2026): picking a time does not stick (a new reminder keeps its default or
 * another reminder's time) and editing one reminder erases another. Drives the real UI and
 * clock dial to check both.
 */
class ReminderIsolationReproTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private val repo get() = app.container.reminderRepository
    private val prefix = "QA-iso-"
    private lateinit var originalSettings: androidx.datastore.preferences.core.Preferences
    private fun rows() = runBlocking {
        repo.getAllRemindersStream().first().filter { it.reminderText?.startsWith(prefix) == true }
    }

    @Before fun setup(): Unit = runBlocking {
        // The install for each run revokes it, and Home's prompt would cover the screen.
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(app.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        }
        originalSettings = app.dataStore.data.first()
        app.container.settingsRepository.apply {
            setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
            setBatteryOptimizationPromptShown(true)
            dismissBatteryGuidance()
            setShowTypedReminderSection(true)
        }
    }

    @After fun cleanup() = runBlocking {
        rows().forEach { app.container.alarmScheduler.cancel(it); repo.deleteReminder(it) }
        app.dataStore.updateData { originalSettings }
        Unit
    }

    private fun hm(millis: Long): Pair<Int, Int> =
        Calendar.getInstance().apply { timeInMillis = millis }.let { it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE) }

    private fun tomorrowAt(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Opens the Time row's picker and picks [hour12]:[minute] [pm] on the dial. */
    private fun pickTime(hour12: Int, minute: Int, pm: Boolean) {
        compose.onAllNodesWithText(app.getString(R.string.ae_time)).onFirst().performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("$hour12 o'clock").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(if (pm) "PM" else "AM", useUnmergedTree = true).performClick()
        // The currently selected number is drawn twice (plain and highlighted).
        compose.onAllNodesWithContentDescription("$hour12 o'clock").onFirst().performClick()
        compose.waitForIdle()
        if (compose.onAllNodesWithContentDescription("$minute minutes").fetchSemanticsNodes().isEmpty()) {
            compose.onAllNodesWithContentDescription("Select minutes", substring = true).onFirst().performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("$minute minutes").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithContentDescription("$minute minutes").onFirst().performClick()
        compose.waitForIdle()
        compose.onNodeWithText(app.getString(R.string.action_apply)).performClick()
        compose.waitForIdle()
    }

    private fun has(cd: String): Boolean =
        runCatching { compose.onAllNodesWithContentDescription(cd).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
    private fun hasText(text: String): Boolean =
        runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)

    private fun createDailyFromHome(text: String, hour12: Int, minute: Int, pm: Boolean) {
        compose.waitUntil(15_000) { has(app.getString(R.string.home_cd_add)) }
        compose.onNodeWithContentDescription(app.getString(R.string.home_cd_add)).performClick()
        compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo().performTextInput(text)
        pickTime(hour12, minute, pm)
        compose.onNodeWithText(app.getString(R.string.ae_repeat)).performScrollTo().performClick()
        compose.onAllNodesWithText(app.getString(R.string.rec_daily)).onLast().performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.ae_save_reminder)).performScrollTo().performClick()
        compose.waitUntil(10_000) { rows().any { it.reminderText == text } }
        // Back on Home once the editor closes.
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription(app.getString(R.string.home_cd_add)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun twoDailyRemindersKeepTheirOwnPickedTimes() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            createDailyFromHome(prefix + "A", 11, 30, pm = false)
            createDailyFromHome(prefix + "B", 12, 30, pm = true)
            val a = rows().single { it.reminderText == prefix + "A" }
            val b = rows().single { it.reminderText == prefix + "B" }
            assertEquals(RecurrenceType.DAILY, a.recurrenceType)
            assertEquals(RecurrenceType.DAILY, b.recurrenceType)
            assertEquals("A time", 11 to 30, hm(a.nextTriggerAt))
            assertEquals("B time", 12 to 30, hm(b.nextTriggerAt))
        }
    }

    private fun seedPair(): Pair<ReminderEntity, ReminderEntity> = runBlocking {
        val a = ReminderEntity(reminderText = prefix + "two", nextTriggerAt = tomorrowAt(14, 0))
        val b = ReminderEntity(reminderText = prefix + "two-fifteen", nextTriggerAt = tomorrowAt(14, 15))
        a.copy(id = repo.insertReminder(a)) to b.copy(id = repo.insertReminder(b))
    }

    @Test fun editingOneInEditorLeavesTheOtherAlone() {
        val (a, b) = seedPair()
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java).putExtra("reminderId", a.id)).use {
            compose.waitUntil(15_000) { has(app.getString(R.string.action_edit)) }
            compose.onNodeWithContentDescription(app.getString(R.string.action_edit)).performClick()
            compose.onNodeWithText(app.getString(R.string.ae_msg_label)).performScrollTo()
            pickTime(2, 30, pm = true)
            compose.onNodeWithText(app.getString(R.string.ae_save_reminder)).performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.getReminder(a.id)?.nextTriggerAt == tomorrowAt(14, 30) } }
            assertEquals("Other reminder untouched", b, runBlocking { repo.getReminder(b.id) })
            assertEquals(2, rows().size)
        }
    }

    /** A daily reminder set on its details screen to a time already gone by today starts tomorrow. */
    @Test fun dailyOnDetailsAcceptsATimeAlreadyPassedToday() {
        val now = Calendar.getInstance()
        val hourNow = now.get(Calendar.HOUR_OF_DAY)
        Assume.assumeTrue("needs an earlier and a later hour today", hourNow in 1..22)
        val hour24 = hourNow - 1
        // Due later today, as a just-made reminder is: the picked time keeps this date.
        val laterToday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hourNow + 1); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val r = runBlocking {
            val e = ReminderEntity(
                reminderText = prefix + "daily-past",
                nextTriggerAt = laterToday,
                recurrenceType = RecurrenceType.DAILY,
                recurrenceJson = com.ghostgramlabs.speakalert.domain.RecurrenceUtils.toJson(
                    com.ghostgramlabs.speakalert.domain.models.RecurrenceModel.Daily()
                )
            )
            e.copy(id = repo.insertReminder(e))
        }
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java).putExtra("reminderId", r.id)).use {
            compose.waitUntil(15_000) { hasText(app.getString(R.string.ae_time)) }
            val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
            pickTime(hour12, 0, pm = hour24 >= 12)
            compose.waitUntil(10_000) { runBlocking { repo.getReminder(r.id)?.nextTriggerAt == tomorrowAt(hour24, 0) } }
        }
    }

    @Test fun editingOneOnDetailsLeavesTheOtherAlone() {
        val (a, b) = seedPair()
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java).putExtra("reminderId", a.id)).use {
            compose.waitUntil(15_000) { hasText(app.getString(R.string.ae_time)) }
            pickTime(2, 30, pm = true)
            compose.waitUntil(10_000) { runBlocking { repo.getReminder(a.id)?.nextTriggerAt == tomorrowAt(14, 30) } }
            assertEquals("Other reminder untouched", b, runBlocking { repo.getReminder(b.id) })
            assertEquals(2, rows().size)
        }
    }
}
