package com.ghostgramlabs.speakalert.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The silence between two plays of a looping reminder. A short message like "Meeting" looped
 * back-to-back is relentless; a pause makes it a periodic nudge until the loop times out.
 */
internal class LoopGap(
    private val scope: CoroutineScope,
    private val readGapSeconds: suspend () -> Int
) {
    private var job: Job? = null

    /** Runs [replay] after the configured pause. A new request or stop must [cancel] it. */
    fun schedule(replay: () -> Unit) {
        cancel()
        job = scope.launch {
            val seconds = readGapSeconds()
            if (seconds > 0) delay(seconds * 1_000L)
            replay()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
