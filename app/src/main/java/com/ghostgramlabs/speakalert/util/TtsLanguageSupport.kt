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
        // Japanese mixes kana with shared CJK ideographs. Kana identifies the language even
        // when the reminder has more kanji than kana; do not select a Chinese voice for it.
        if (counts.containsKey(Locale.JAPANESE)) {
            val kanji = counts.remove(Locale.SIMPLIFIED_CHINESE) ?: 0
            counts[Locale.JAPANESE] = counts.getValue(Locale.JAPANESE) + kanji
        }
        val dominant = counts.maxByOrNull { it.value } ?: return null
        // Only override the device language when the non-Latin script actually dominates the text,
        // so an English sentence containing a single foreign name still uses the device voice.
        return if (dominant.value >= latinLetters) dominant.key else null
    }

    /**
     * The voices to try for a reminder, best first, for the "Spoken language" preference
     * ([mode]: 0 = Auto-detect, 1 = Device language, 2 = English). [failed] holds language tags
     * whose voice already failed to speak; an engine can report a language as available and
     * still fail to synthesise it (voice data not downloaded), so those are skipped on a retry.
     */
    fun candidateLocales(
        mode: Int,
        detected: Locale?,
        deviceLocale: Locale,
        failed: Set<String> = emptySet()
    ): List<Locale> {
        val candidates = LinkedHashSet<Locale>()
        when (mode) {
            1 -> { candidates.add(deviceLocale); candidates.add(Locale.ENGLISH) }
            2 -> { candidates.add(Locale.ENGLISH); candidates.add(deviceLocale) }
            else -> {
                detected?.let { candidates.add(it) }
                candidates.add(deviceLocale)
                candidates.add(Locale.ENGLISH)
            }
        }
        return candidates.filter { it.toLanguageTag() !in failed }
    }

    /** What the engine says about one of its voices; a plain copy so the choice can be unit tested. */
    data class VoiceOption(
        val name: String,
        val language: String,
        val country: String,
        val needsNetwork: Boolean,
        val notInstalled: Boolean,
        val quality: Int
    )

    /**
     * The other voices worth trying for [locale] after its default voice failed, best first.
     *
     * An engine's default voice for a language is not always one it can use: Google's engine
     * answers "available" for Japanese and selects a voice whose data was never downloaded,
     * while another Japanese voice on the same phone works. Offline voices come first because a
     * reminder must not depend on a connection, then network ones, which still beat reading
     * the text in the wrong language. The engine's "not installed" flag only orders the list;
     * it is wrong often enough (every voice carries it before the engine has loaded its data)
     * that it must not rule a voice out.
     */
    fun alternativeVoices(
        voices: List<VoiceOption>,
        locale: Locale,
        failedVoices: Set<String>
    ): List<VoiceOption> = voices
        .filter { it.language.equals(locale.language, ignoreCase = true) && it.name !in failedVoices }
        .sortedWith(
            compareBy<VoiceOption> { locale.country.isNotEmpty() && !it.country.equals(locale.country, ignoreCase = true) }
                .thenBy { it.needsNetwork }
                .thenBy { it.notInstalled }
                .thenByDescending { it.quality }
                .thenBy { it.name }
        )

    /**
     * The next voice to try for [locale], or null once the language has had a fair chance:
     * up to [MAX_OFFLINE_VOICES] offline voices (the failed default counts), then one network
     * voice. Bounded so a phone with many broken voices still reaches the next language quickly.
     */
    fun nextVoice(voices: List<VoiceOption>, locale: Locale, failedVoices: Set<String>): VoiceOption? {
        val sameLanguage = voices.filter { it.language.equals(locale.language, ignoreCase = true) }
        val failedOffline = sameLanguage.count { !it.needsNetwork && it.name in failedVoices }
        val failedNetwork = sameLanguage.count { it.needsNetwork && it.name in failedVoices }
        val remaining = alternativeVoices(voices, locale, failedVoices)
        if (failedOffline < MAX_OFFLINE_VOICES) remaining.firstOrNull { !it.needsNetwork }?.let { return it }
        return if (failedNetwork < 1) remaining.firstOrNull { it.needsNetwork } else null
    }

    private const val MAX_OFFLINE_VOICES = 3

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
