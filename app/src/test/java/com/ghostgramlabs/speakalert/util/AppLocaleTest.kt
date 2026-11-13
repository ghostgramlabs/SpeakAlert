package com.ghostgramlabs.speakalert.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppLocaleTest {

    @Test
    fun `regional tag the app ships is kept whole`() {
        assertEquals("pt-BR", AppLocale.matchSupported("pt-BR"))
    }

    @Test
    fun `tag case from the platform does not matter`() {
        assertEquals("pt-BR", AppLocale.matchSupported("pt-br"))
    }

    @Test
    fun `any Portuguese selects the Brazilian chip`() {
        assertEquals("pt-BR", AppLocale.matchSupported("pt"))
        assertEquals("pt-BR", AppLocale.matchSupported("pt-PT"))
    }

    @Test
    fun `regional variants of plain languages select the bare language`() {
        assertEquals("es", AppLocale.matchSupported("es-US"))
        assertEquals("en", AppLocale.matchSupported("en-GB"))
        assertEquals("ar", AppLocale.matchSupported("ar-EG"))
        assertEquals("de", AppLocale.matchSupported("de-AT"))
        assertEquals("fr", AppLocale.matchSupported("fr-CA"))
        assertEquals("id", AppLocale.matchSupported("id-ID"))
        assertEquals("ml", AppLocale.matchSupported("ml-IN"))
    }

    @Test
    fun `legacy Indonesian code selects the Indonesian chip`() {
        assertEquals("id", AppLocale.matchSupported("in"))
        assertEquals("id", AppLocale.matchSupported("in-ID"))
    }

    @Test
    fun `every picker language is listed in locales_config`() {
        val config = File("src/main/res/xml/locales_config.xml").readText()
        val configured = Regex("android:name=\"([^\"]+)\"").findAll(config).map { it.groupValues[1] }.toSet()
        val picker = AppLocale.supported.map { it.first }.filter { it.isNotEmpty() }.toSet()
        assertEquals(configured, picker)
    }

    @Test
    fun `every picker language has translated resources`() {
        AppLocale.supported.map { it.first }.filter { it.isNotEmpty() && it != "en" }.forEach { tag ->
            val parts = tag.split('-')
            // Android resource folders still use the legacy code "in" for Indonesian.
            val dir = if (parts.size == 2) "values-${parts[0]}-r${parts[1]}" else if (tag == "id") "values-in" else "values-$tag"
            assertTrue("missing $dir", File("src/main/res/$dir/strings.xml").isFile)
        }
    }
}
