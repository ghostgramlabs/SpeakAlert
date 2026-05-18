package com.ghostgramlabs.speakalert.util

fun String.toLocalizedIntOrNull(): Int? {
    val normalized = normalizeLocalizedDigitsOrNull() ?: return null
    return normalized.toIntOrNull()
}

fun String.normalizeLocalizedDigitsOrNull(): String? {
    if (isEmpty()) return ""

    val builder = StringBuilder(length)
    for (char in this) {
        val digit = Character.digit(char, 10)
        if (digit == -1) return null
        builder.append(('0'.code + digit).toChar())
    }
    return builder.toString()
}
