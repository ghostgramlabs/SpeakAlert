package com.ghostgramlabs.speakalert.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Saved recordings whose voice cleanup is still queued or running in the background, by path.
 * The reminder list uses it to say the audio is being improved; nothing waits on it.
 */
object AudioCleanupTracker {
    private val _inProgress = MutableStateFlow<Set<String>>(emptySet())
    val inProgress: StateFlow<Set<String>> = _inProgress.asStateFlow()

    fun started(path: String) = _inProgress.update { it + path }

    fun finished(path: String) = _inProgress.update { it - path }
}
