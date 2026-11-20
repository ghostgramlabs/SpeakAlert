package com.ghostgramlabs.speakalert.util

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference

/** Keeps a late engine callback from completing a newer voice probe. */
internal class VoiceCheckCallbacks {
    private val pending = AtomicReference<Pair<String, CompletableDeferred<Boolean>>?>(null)

    fun begin(id: String): CompletableDeferred<Boolean> = CompletableDeferred<Boolean>().also {
        pending.set(id to it)
    }

    fun complete(id: String?, success: Boolean) {
        val current = pending.get() ?: return
        if (current.first == id) current.second.complete(success)
    }

    fun clear() { pending.set(null) }
}
