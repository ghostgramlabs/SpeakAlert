package com.ghostgramlabs.speakalert.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards every translation against the English source: a new or renamed string that is missing
 * in one language, or a translation that drops or invents a %1$s / %2$d argument (which crashes
 * or shows the wrong value at runtime), fails here instead of on a user's phone.
 */
class TranslationParityTest {

    private val res = File("src/main/res")
    private val files = listOf("strings.xml", "quick_start.xml", "unnamed_titles.xml")
    private val placeholder = Regex("%(\\d+)\\$[sd]")

    private fun locales() = res.listFiles { f -> f.isDirectory && f.name.startsWith("values-") && File(f, "strings.xml").isFile }!!
        .sortedBy { it.name }

    private fun strings(file: File): Map<String, String> =
        Regex("<string name=\"([^\"]+)\"([^>]*)>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
            .findAll(file.readText())
            .filterNot { it.groupValues[2].contains("translatable=\"false\"") }
            .associate { it.groupValues[1] to it.groupValues[3] }

    private fun plurals(file: File): Map<String, List<String>> =
        Regex("<plurals name=\"([^\"]+)\"[^>]*>(.*?)</plurals>", RegexOption.DOT_MATCHES_ALL)
            .findAll(file.readText())
            .associate { m ->
                m.groupValues[1] to Regex("<item quantity=\"[a-z]+\">(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(m.groupValues[2]).map { it.groupValues[1] }.toList()
            }

    private fun args(text: String) = placeholder.findAll(text).map { it.value }.toSet()

    @Test
    fun `there are translations to check`() {
        assertTrue(locales().size >= 11)
    }

    @Test
    fun `every language has exactly the English strings`() {
        for (dir in locales()) for (name in files) {
            val english = strings(File(res, "values/$name")).keys
            val translated = strings(File(dir, name)).keys
            assertEquals("${dir.name}/$name missing", emptySet<String>(), english - translated)
            assertEquals("${dir.name}/$name unknown", emptySet<String>(), translated - english)
        }
    }

    @Test
    fun `every language has exactly the English plurals`() {
        for (dir in locales()) {
            assertEquals(dir.name, plurals(File(res, "values/strings.xml")).keys, plurals(File(dir, "strings.xml")).keys)
        }
    }

    @Test
    fun `translations keep the same format arguments`() {
        for (dir in locales()) for (name in files) {
            val english = strings(File(res, "values/$name"))
            strings(File(dir, name)).forEach { (key, text) ->
                assertEquals("${dir.name}/$name $key", args(english.getValue(key)), args(text))
            }
        }
    }

    @Test
    fun `plural items only use arguments the English plural provides`() {
        val english = plurals(File(res, "values/strings.xml"))
        for (dir in locales()) plurals(File(dir, "strings.xml")).forEach { (key, items) ->
            val allowed = english.getValue(key).flatMap { args(it) }.toSet()
            items.forEach { assertTrue("${dir.name} $key uses ${args(it) - allowed}", allowed.containsAll(args(it))) }
        }
    }
}
