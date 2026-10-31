package com.ghostgramlabs.speakalert.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardActionColorsTest {
    @Test
    fun `stored keys map back to their color`() {
        CardButtonColor.values().forEach { assertEquals(it, CardButtonColor.fromKey(it.key)) }
    }

    @Test
    fun `missing or unknown key means the app default`() {
        assertNull(CardButtonColor.fromKey(null))
        assertNull(CardButtonColor.fromKey("chartreuse"))
    }

    @Test
    fun `each color has a lighter shade for dark theme`() {
        CardButtonColor.values().forEach {
            val light = it.light.red + it.light.green + it.light.blue
            val dark = it.dark.red + it.dark.green + it.dark.blue
            assert(dark > light) { "${it.key} dark shade should be lighter" }
        }
    }
}
