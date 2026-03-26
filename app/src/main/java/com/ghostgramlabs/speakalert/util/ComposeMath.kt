package com.ghostgramlabs.speakalert.util

fun Float.sanitizeUnitFloat(defaultValue: Float = 0f): Float {
    if (!isFinite()) return defaultValue
    return coerceIn(0f, 1f)
}
