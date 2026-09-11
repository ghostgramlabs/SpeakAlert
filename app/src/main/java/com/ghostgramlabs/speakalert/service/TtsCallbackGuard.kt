package com.ghostgramlabs.speakalert.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Speech engines may deliver callbacks from a replaced utterance on a binder thread. */
internal class TtsCallbackGuard(
    private val scope: CoroutineScope,
    private val onDone: () -> Unit,
    private val onError: () -> Unit
) {
    private var sequence = 0L
    private var activeId: String? = null

    fun begin(): String = "REMINDER_TTS_${++sequence}".also { activeId = it }
    fun invalidate() { activeId = null }

    fun complete(id: String?, failed: Boolean = false) {
        scope.launch {
            // Check after dispatching to the playback thread: a new request may have
            // replaced this utterance while the callback was waiting in the queue.
            if (id == null || id != activeId) return@launch
            activeId = null
            if (failed) onError() else onDone()
        }
    }
}
