package com.ghostgramlabs.speakalert.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsLanguageSupportTest {

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
