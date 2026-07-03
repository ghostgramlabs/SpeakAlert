package com.ghostgramlabs.speakalert.util

import java.util.Locale

/**
 * Offline best-effort detection of the language a reminder is written in, so the right
 * text-to-speech voice can be selected even when it differs from the device language.
 *
 * Detection is script-based: non-Latin scripts (Devanagari, Tamil, Arabic, etc.) map cleanly to a
 * language, which covers the realistic case of "my phone is in English but I typed a Hindi
 * reminder." Latin-script text cannot be told apart reliably offline (English vs Spanish vs
 * French...), so for Latin we return null and let the caller fall back to the device language.
 */
object TtsLanguageSupport {

    /**
     * Returns the detected [Locale] for [text], or null when it is Latin-script / undetermined
     * (caller should then use the device default).
     */
    fun detectLocale(text: String): Locale? {
        if (text.isBlank()) return null

        val counts = HashMap<Locale, Int>()
        var latinLetters = 0
        var nonLatinLetters = 0

        for (ch in text) {
            if (!Character.isLetter(ch)) continue
            val locale = scriptLocale(ch)
            if (locale == null) {
                latinLetters++
            } else {
                nonLatinLetters++
                counts[locale] = (counts[locale] ?: 0) + 1
            }
        }

        if (nonLatinLetters == 0) return null
        val dominant = counts.maxByOrNull { it.value } ?: return null
        // Only override the device language when the non-Latin script actually dominates the text,
        // so an English sentence containing a single foreign name still uses the device voice.
        return if (dominant.value >= latinLetters) dominant.key else null
    }

    /** Maps a single character's Unicode script to a language Locale, or null for Latin/other. */
    private fun scriptLocale(ch: Char): Locale? {
        val block = Character.UnicodeBlock.of(ch) ?: return null
        return when (block) {
            Character.UnicodeBlock.DEVANAGARI -> Locale("hi") // Hindi
            Character.UnicodeBlock.TAMIL -> Locale("ta")
            Character.UnicodeBlock.MALAYALAM -> Locale("ml")
            Character.UnicodeBlock.TELUGU -> Locale("te")
            Character.UnicodeBlock.KANNADA -> Locale("kn")
            Character.UnicodeBlock.BENGALI -> Locale("bn")
            Character.UnicodeBlock.GUJARATI -> Locale("gu")
            Character.UnicodeBlock.GURMUKHI -> Locale("pa") // Punjabi
            Character.UnicodeBlock.ORIYA -> Locale("or")
            Character.UnicodeBlock.SINHALA -> Locale("si")
            Character.UnicodeBlock.ARABIC -> Locale("ar")
            Character.UnicodeBlock.HEBREW -> Locale("he")
            Character.UnicodeBlock.THAI -> Locale("th")
            Character.UnicodeBlock.GREEK, Character.UnicodeBlock.GREEK_EXTENDED -> Locale("el")
            Character.UnicodeBlock.CYRILLIC, Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY -> Locale("ru")
            Character.UnicodeBlock.HANGUL_SYLLABLES,
            Character.UnicodeBlock.HANGUL_JAMO,
            Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO -> Locale.KOREAN
            Character.UnicodeBlock.HIRAGANA,
            Character.UnicodeBlock.KATAKANA -> Locale.JAPANESE
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION -> Locale.SIMPLIFIED_CHINESE
            else -> null // Basic Latin and everything else
        }
    }
}
