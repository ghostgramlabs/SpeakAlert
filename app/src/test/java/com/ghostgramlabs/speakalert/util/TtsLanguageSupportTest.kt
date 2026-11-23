package com.ghostgramlabs.speakalert.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsLanguageSupportTest {

    @Test
    fun `kanji-heavy Japanese reminders use a Japanese voice`() {
        assertEquals(java.util.Locale.JAPANESE, TtsLanguageSupport.detectLocale("会議資料を確認"))
        assertEquals(java.util.Locale.JAPANESE, TtsLanguageSupport.detectLocale("資料チェック"))
    }

    @Test
    fun `shared ideographs without kana retain Chinese detection`() {
        assertEquals(java.util.Locale.SIMPLIFIED_CHINESE, TtsLanguageSupport.detectLocale("检查会议资料"))
        assertNull(TtsLanguageSupport.detectLocale("Review the meeting notes for 会議を"))
    }

    private fun tags(mode: Int, detected: java.util.Locale?, device: String, failed: Set<String> = emptySet()) =
        TtsLanguageSupport.candidateLocales(mode, detected, java.util.Locale.forLanguageTag(device), failed)
            .map { it.toLanguageTag() }

    @Test
    fun `auto-detect tries the text language, then the device language, then english`() {
        assertEquals(listOf("hi", "ja", "en"), tags(0, java.util.Locale("hi"), "ja"))
        assertEquals(listOf("ja", "en"), tags(0, null, "ja"))
    }

    @Test
    fun `device and english modes keep the other as a fallback`() {
        assertEquals(listOf("ja", "en"), tags(1, null, "ja"))
        assertEquals(listOf("en", "ja"), tags(2, null, "ja"))
    }

    @Test
    fun `an english device is offered once`() {
        assertEquals(listOf("en"), tags(0, null, "en"))
    }

    private fun voice(name: String, language: String = "ja", country: String = "JP", network: Boolean = false,
        notInstalled: Boolean = true, quality: Int = 400) =
        TtsLanguageSupport.VoiceOption(name, language, country, network, notInstalled, quality)

    // What Google's engine reports for Japanese on a phone without the default voice's data.
    private val japanese = listOf(
        voice("ja-JP-language"), voice("ja-jp-x-htm-local"), voice("ja-jp-x-htm-network", network = true),
        voice("ja-jp-x-jab-local"), voice("ja-jp-x-jab-network", network = true),
        voice("ja-jp-x-jac-local"), voice("en-us-x-iog-local", language = "en", country = "US")
    )
    private fun next(failed: Set<String>) =
        TtsLanguageSupport.nextVoice(japanese, java.util.Locale.JAPANESE, failed)?.name

    @Test
    fun `a failed default voice is followed by another offline voice of the same language`() {
        assertEquals("ja-jp-x-htm-local", next(setOf("ja-JP-language")))
    }

    @Test
    fun `offline voices are limited, then one network voice, then the language is given up`() {
        val offline = setOf("ja-JP-language", "ja-jp-x-htm-local")
        assertEquals("ja-jp-x-jab-local", next(offline))
        assertEquals("ja-jp-x-htm-network", next(offline + "ja-jp-x-jab-local"))
        assertNull(next(offline + "ja-jp-x-jab-local" + "ja-jp-x-htm-network"))
    }

    @Test
    fun `alternatives never cross into another language`() {
        assertNull(TtsLanguageSupport.nextVoice(japanese, java.util.Locale("tr"), emptySet()))
    }

    @Test
    fun `an installed, better voice of the right country is preferred`() {
        val voices = listOf(
            voice("pt-pt-a", language = "pt", country = "PT", notInstalled = false),
            voice("pt-br-low", language = "pt", country = "BR", quality = 200),
            voice("pt-br-missing", language = "pt", country = "BR", quality = 500),
            voice("pt-br-good", language = "pt", country = "BR", notInstalled = false)
        )
        val order = TtsLanguageSupport.alternativeVoices(voices, java.util.Locale("pt", "BR"), emptySet()).map { it.name }
        assertEquals(listOf("pt-br-good", "pt-br-missing", "pt-br-low", "pt-pt-a"), order)
    }

    @Test
    fun `a voice that failed to speak is skipped on the retry`() {
        assertEquals(listOf("en"), tags(0, null, "ja", failed = setOf("ja")))
        assertEquals(emptyList<String>(), tags(0, null, "ja", failed = setOf("ja", "en")))
    }

    @Test
    fun `latin text returns null so device default is used`() {
        assertNull(TtsLanguageSupport.detectLocale("Call John tomorrow morning"))
    }

    @Test
    fun `hindi text detects hindi`() {
        assertEquals("hi", TtsLanguageSupport.detectLocale("कल सुबह जॉन को फोन करना")?.language)
    }

    @Test
    fun `tamil text detects tamil`() {
        assertEquals("ta", TtsLanguageSupport.detectLocale("நாளை காலை மருந்து சாப்பிட")?.language)
    }

    @Test
    fun `arabic text detects arabic`() {
        assertEquals("ar", TtsLanguageSupport.detectLocale("تذكير بشرب الماء")?.language)
    }

    @Test
    fun `mostly-latin with one foreign name stays device default`() {
        // A single Devanagari name inside an English sentence should not flip the voice.
        assertNull(TtsLanguageSupport.detectLocale("Call राजू at 5 pm"))
    }

    @Test
    fun `blank text returns null`() {
        assertNull(TtsLanguageSupport.detectLocale("   "))
    }
}
