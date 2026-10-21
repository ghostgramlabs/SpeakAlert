package com.ghostgramlabs.speakalert

import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ghostgramlabs.speakalert.audio.AndroidAudioRecorder
import com.ghostgramlabs.speakalert.data.model.ReminderEntity
import com.ghostgramlabs.speakalert.service.ReminderPlaybackService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class SpokenPlaybackResolutionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<VoiceReminderApp>()

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).bufferedReader().use { it.readText() }

    // Inspect current players, not historical audio events or the service's optimistic start broadcast.
    private fun activeAlarmPlayers(): Set<String> = shell("dumpsys audio").lineSequence()
        .filter { it.contains("AudioPlaybackConfiguration piid:") &&
            it.contains("state:started") && it.contains("usage=USAGE_ALARM") }
        .map { it.substringAfter("piid:").substringBefore(" ") }.toSet()

    @Suppress("DEPRECATION")
    private fun serviceRunning(): Boolean =
        (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == ReminderPlaybackService::class.java.name }

    private fun verifyDone(mode: String, source: (Context) -> String?) {
        val repository = app.container.reminderRepository
        val id = runBlocking {
            repository.insertReminder(ReminderEntity(
                title = "$mode playback regression", nextTriggerAt = System.currentTimeMillis() - 1000,
                reminderText = "This is a Speak Alert playback test. Mark this reminder done to stop it. ".repeat(10),
                loopPlayback = true
            ))
        }
        var audio: String? = null
        try {
            ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use { scenario ->
                scenario.onActivity { audio = source(it) }
            }
            val reminder = runBlocking { repository.getReminder(id)!! }
            runBlocking { repository.updateReminder(reminder.copy(audioPath = audio)) }
            ActivityScenario.launch<MainActivity>(
                Intent(app, MainActivity::class.java).putExtra("reminderId", id)
            ).use {
                compose.waitUntil(15_000) {
                    compose.onAllNodesWithText(app.getString(R.string.det_mark_done)).fetchSemanticsNodes().isNotEmpty()
                }
                val baseline = activeAlarmPlayers()
                assertTrue(ReminderPlaybackService.start(app, id, reminder.title, audio,
                    if (audio == null) reminder.reminderText else null, loop = true))
                var players = emptySet<String>()
                compose.waitUntil(30_000) {
                    players = activeAlarmPlayers() - baseline
                    players.isNotEmpty()
                }
                assertTrue("Playback service must be running before Done", serviceRunning())
                compose.onNodeWithText(app.getString(R.string.det_mark_done)).performScrollTo().performClick()
                compose.waitUntil(15_000) {
                    runBlocking { repository.getReminder(id)?.isCompleted == true } &&
                        !serviceRunning() && activeAlarmPlayers().intersect(players).isEmpty()
                }
                // Also rule out a queued speech callback restarting playback after shutdown.
                Thread.sleep(1000)
                assertFalse(serviceRunning())
                assertTrue(activeAlarmPlayers().intersect(players).isEmpty())
                assertEquals(audio, runBlocking { repository.getReminder(id)!!.audioPath })
            }
        } finally {
            app.stopService(Intent(app, ReminderPlaybackService::class.java))
            runBlocking {
                repository.getReminder(id)?.let {
                    app.container.alarmScheduler.cancel(it)
                    repository.deleteReminder(it)
                }
            }
            audio?.let {
                if (it.startsWith("content:")) app.contentResolver.delete(Uri.parse(it), null, null)
                else File(it).delete()
            }
        }
    }

    @Test fun recordedAudioDoneStopsPlaybackAndPersists() = verifyDone("Recorded audio") { context ->
        val file = File(app.filesDir, "regression-recording.m4a")
        val recorder = AndroidAudioRecorder(context)
        recorder.start(file, false)
        Thread.sleep(1500)
        recorder.stop()
        assertTrue(file.length() > 0)
        file.absolutePath
    }

    @Test fun selectedAudioDoneStopsPlaybackAndPersists() = verifyDone("Selected audio") {
        val rate = 16000
        val samples = rate * 5
        val bytes = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
        bytes.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2)
        bytes.putShort(2).putShort(16).put("data".toByteArray()).putInt(samples * 2)
        repeat(samples) { bytes.putShort((kotlin.math.sin(it * 2 * Math.PI * 440 / rate) * 2000).toInt().toShort()) }
        val uri = app.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "SpeakAlert-regression.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        })!!
        app.contentResolver.openOutputStream(uri)!!.use { it.write(bytes.array()) }
        app.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        uri.toString()
    }

    @Test fun ttsDoneStopsSpeechAndPersists() = verifyDone("Text to speech") { null }
}
