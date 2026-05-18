package com.ghostgramlabs.speakalert.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalizedNumberUtilsTest {

    @Test
    fun `toLocalizedIntOrNull parses ascii digits`() {
        assertEquals(123, "123".toLocalizedIntOrNull())
    }

    @Test
    fun `toLocalizedIntOrNull parses arabic indic digits`() {
        assertEquals(123, "\u0661\u0662\u0663".toLocalizedIntOrNull())
    }

    @Test
    fun `toLocalizedIntOrNull parses eastern arabic indic digits`() {
        assertEquals(123, "\u06F1\u06F2\u06F3".toLocalizedIntOrNull())
    }

    @Test
    fun `normalizeLocalizedDigitsOrNull preserves empty text for editable fields`() {
        assertEquals("", "".normalizeLocalizedDigitsOrNull())
    }

    @Test
    fun `normalizeLocalizedDigitsOrNull rejects non digits`() {
        assertNull("12m".normalizeLocalizedDigitsOrNull())
    }
}
