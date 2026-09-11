package com.ghostgramlabs.speakalert

internal enum class StartupIntro { QUICK_START, RELEASE_NOTES }

internal fun startupIntroFor(
    lastVersionShown: String?,
    currentVersion: String,
    isHomeLaunch: Boolean
): StartupIntro? = when {
    !isHomeLaunch -> null
    lastVersionShown.isNullOrBlank() -> StartupIntro.QUICK_START
    lastVersionShown != currentVersion -> StartupIntro.RELEASE_NOTES
    else -> null
}
