package com.ghostgramlabs.speakalert

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ghostgramlabs.speakalert.data.repository.dataStore
import com.ghostgramlabs.speakalert.util.TtsVoiceCheck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.Locale

/** Settings, When a reminder rings: the spoken-language voice check and its download button. */
class SpokenVoiceCheckTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()
    private lateinit var originalSettings: androidx.datastore.preferences.core.Preferences
    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

    @Before fun setup(): Unit = runBlocking {
        originalSettings = app.dataStore.data.first()
        app.container.settingsRepository.apply {
            setLastWhatsNewVersionShown(BuildConfig.VERSION_NAME)
            setBatteryOptimizationPromptShown(true)
            dismissBatteryGuidance()
            setToneOnlyMode(false)
            setTtsLanguageMode(1)
        }
    }

    @After fun restore() = runBlocking { app.dataStore.updateData { originalSettings }; Unit }

    private fun shot(name: String) {
        Thread.sleep(800)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        File(app.getExternalFilesDir(null), "voicecheck_$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun settingsShowsWhatTheEngineCanReallySpeak() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.waitForIdle()
            // Read the locale after the activity has applied the app language.
            val locale = Locale.getDefault()
            val language = locale.getDisplayLanguage(locale)
            val expected = runBlocking { TtsVoiceCheck.check(app, locale) }
            compose.onNodeWithContentDescription(text(R.string.home_cd_settings)).performClick()
            compose.onNodeWithTag("voice_check").performScrollTo()
            val texts = mapOf(
                TtsVoiceCheck.Status.WORKING to text(R.string.set_voice_check_ok, language),
                TtsVoiceCheck.Status.ALTERNATIVE_VOICE to text(R.string.set_voice_check_alternative, language),
                TtsVoiceCheck.Status.NOT_WORKING to text(R.string.set_voice_check_missing, language),
                TtsVoiceCheck.Status.NO_ENGINE to text(R.string.set_voice_check_no_engine)
            )
            fun shown() = texts.filterValues { compose.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }.keys
            try {
                compose.waitUntil(60_000) { shown().isNotEmpty() }
            } finally {
                shot("${locale.language}_status")
                android.util.Log.i("VOICECHECK", "locale=$locale expected=${expected.status} shown=${shown()}")
            }
            assertEquals("Settings should show what a direct check finds", setOf(expected.status), shown())
            val status = texts.getValue(expected.status)
            compose.onNodeWithText(status).performScrollTo().assertIsDisplayed()
            val download = compose.onAllNodesWithText(text(R.string.set_voice_download))
            if (expected.status == TtsVoiceCheck.Status.WORKING) {
                download.assertCountEquals(0)
            } else {
                download.onFirst().performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(text(R.string.set_voice_check_again)).assertIsDisplayed()
            }
            shot("${locale.language}_${expected.status.name.lowercase()}")
            android.util.Log.i("VOICECHECK", "locale=$locale status=${expected.status} engine=${expected.enginePackage}")
        }
    }

    @Test fun englishChoiceChecksTheEnglishVoice() {
        runBlocking { app.container.settingsRepository.setTtsLanguageMode(2) }
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.waitForIdle()
            val english = Locale.ENGLISH.getDisplayLanguage(Locale.getDefault())
            compose.onNodeWithContentDescription(text(R.string.home_cd_settings)).performClick()
            compose.onNodeWithTag("voice_check").performScrollTo()
            compose.waitUntil(40_000) {
                compose.onAllNodesWithText(english, substring = true).fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithText(text(R.string.set_voice_check_checking, english)).fetchSemanticsNodes().isEmpty()
            }
            shot("english_choice")
        }
    }

    @Test fun downloadButtonOpensTheEnginesVoiceScreen() {
        val engine = runBlocking { TtsVoiceCheck.check(app, Locale.getDefault()) }.enginePackage
        assertTrue("A voice download screen should open", TtsVoiceCheck.openVoiceDownload(app, engine))
        Thread.sleep(2_500)
        shot("download_screen")
        val top = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("dumpsys activity activities")
        ).bufferedReader().use { it.readText() }.lineSequence().firstOrNull { it.contains("topResumedActivity") }
        android.util.Log.i("VOICECHECK", "download screen: $top")
        assertFalse("Download should leave the app", top.orEmpty().contains(app.packageName + "/"))
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
    }
}
