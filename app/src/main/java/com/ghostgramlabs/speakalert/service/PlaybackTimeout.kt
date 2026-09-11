package com.ghostgramlabs.speakalert.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Each playback request owns its timeout, including the pending settings read. */
internal class PlaybackTimeout(
    private val scope: CoroutineScope,
    private val readTimeoutMinutes: suspend () -> Int,
    private val onTimeout: (Int) -> Unit
) {
    private var job: Job? = null

    fun restart(loopEnabled: Boolean) {
        cancel()
        if (!loopEnabled) return
        job = scope.launch {
            val minutes = readTimeoutMinutes()
            if (minutes <= 0) return@launch
            delay(minutes * 60_000L)
            onTimeout(minutes)
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
