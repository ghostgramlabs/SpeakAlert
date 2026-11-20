package com.ghostgramlabs.speakalert.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale

/**
 * Finds out whether the phone can really speak a language, by synthesising a word to a file.
 *
 * Asking the engine is not enough: it answers "available" for a language whose voice data was
 * never downloaded, and its per-voice "not installed" flag is unreliable. Only a synthesis
 * attempt tells the truth, and writing to a file keeps the check silent.
 */
object TtsVoiceCheck {

    enum class Status {
        /** The engine's own voice for the language speaks. */
        WORKING,
        /** The default voice fails but another voice of the language speaks. */
        ALTERNATIVE_VOICE,
        /** Nothing on the phone can speak the language. */
        NOT_WORKING,
        /** No usable text-to-speech engine. */
        NO_ENGINE
    }

    data class Result(val status: Status, val enginePackage: String?)

    suspend fun check(context: Context, locale: Locale): Result = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val init = CompletableDeferred<Int>()
        val tts = try {
            TextToSpeech(app) { init.complete(it) }
        } catch (e: Exception) {
            return@withContext Result(Status.NO_ENGINE, null)
        }
        val sampleFiles = mutableListOf<File>()
        try {
            if (withTimeoutOrNull(INIT_TIMEOUT_MS) { init.await() } != TextToSpeech.SUCCESS) {
                return@withContext Result(Status.NO_ENGINE, null)
            }
            val engine = runCatching { tts.defaultEngine }.getOrNull()
            if (tts.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) {
                return@withContext Result(Status.NOT_WORKING, engine)
            }

            val callbacks = VoiceCheckCallbacks()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { callbacks.complete(utteranceId, true) }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { callbacks.complete(utteranceId, false) }
                override fun onError(utteranceId: String?, errorCode: Int) { callbacks.complete(utteranceId, false) }
            })
            // The language's own name is a word every voice of that language can read.
            val sample = locale.getDisplayLanguage(locale).ifBlank { "1" }
            var attempt = 0
            suspend fun canSpeak(): Boolean {
                val id = "voice-check-${attempt++}"
                val sampleFile = File.createTempFile("voice-check-", ".wav", app.cacheDir)
                sampleFiles.add(sampleFile)
                val outcome = callbacks.begin(id)
                try {
                    val queued = tts.synthesizeToFile(sample, Bundle(), sampleFile, id)
                    if (queued != TextToSpeech.SUCCESS) return false
                    return withTimeoutOrNull(SYNTHESIS_TIMEOUT_MS) { outcome.await() } == true &&
                        sampleFile.length() > 44 // A WAV header alone is not speech.
                } finally {
                    callbacks.clear()
                    runCatching { tts.stop() }
                }
            }

            if (canSpeak()) return@withContext Result(Status.WORKING, engine)

            val failed = mutableSetOf<String>()
            runCatching { tts.voice?.name }.getOrNull()?.let { failed.add(it) }
            while (true) {
                val next = TtsLanguageSupport.nextVoice(voiceOptions(tts), locale, failed) ?: break
                val voice = runCatching { tts.voices?.firstOrNull { it.name == next.name } }.getOrNull()
                if (voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS && canSpeak()) {
                    return@withContext Result(Status.ALTERNATIVE_VOICE, engine)
                }
                failed.add(next.name)
            }
            Result(Status.NOT_WORKING, engine)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            FileLogger.logError("VOICE_CHECK", "Voice check failed", e)
            Result(Status.NO_ENGINE, null)
        } finally {
            runCatching { tts.shutdown() }
            sampleFiles.forEach { it.delete() }
        }
    }

    /** The engine's voices as plain values for [TtsLanguageSupport]. */
    fun voiceOptions(engine: TextToSpeech): List<TtsLanguageSupport.VoiceOption> =
        runCatching {
            engine.voices.orEmpty().map {
                TtsLanguageSupport.VoiceOption(
                    name = it.name,
                    language = it.locale.language,
                    country = it.locale.country,
                    needsNetwork = it.isNetworkConnectionRequired,
                    notInstalled = it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true,
                    quality = it.quality
                )
            }
        }.getOrDefault(emptyList())

    /**
     * Opens the place where voices are downloaded: the engine's own voice-data screen, or the
     * phone's text-to-speech settings when the engine has none. Returns whether a screen opened.
     */
    fun openVoiceDownload(context: Context, enginePackage: String?): Boolean {
        val candidates = listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply { enginePackage?.let { setPackage(it) } },
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent("com.android.settings.TTS_SETTINGS")
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return true
        }
        return false
    }

    private const val INIT_TIMEOUT_MS = 15_000L
    private const val SYNTHESIS_TIMEOUT_MS = 10_000L
}
