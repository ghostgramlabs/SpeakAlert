package com.ghostgramlabs.speakalert.util

const val APP_DISPLAY_NAME = "SpeakAlert"

fun String?.isDefaultAppDisplayName(): Boolean {
    val value = this?.trim() ?: return false
    return value.equals(APP_DISPLAY_NAME, ignoreCase = true) ||
        value.equals("Speak Alert", ignoreCase = true)
}
