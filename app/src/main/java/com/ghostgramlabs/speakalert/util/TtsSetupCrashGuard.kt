package com.ghostgramlabs.speakalert.util

import android.os.Looper
import android.util.Log

/**
 * Keeps one known framework failure from killing the app.
 *
 * When TextToSpeech connects, the framework asks the engine for its default voice on an
 * AsyncTask thread. Some engines answer that call with an exception ("Task was cancelled",
 * seen on an Oppo running Android 10), which the framework does not catch, so the process dies
 * even though nothing of ours is on the stack. The playback service already avoids the usual
 * trigger and times out a speech request whose engine never connects; this only stops that
 * background failure from taking down the whole app. Anything else, and anything on the main
 * thread, goes to the previous handler unchanged.
 */
object TtsSetupCrashGuard {
    private const val TAG = "TtsSetupCrashGuard"

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val onMainThread = thread === Looper.getMainLooper()?.thread
            if (!onMainThread && isTtsSetupFailure(error)) {
                Log.w(TAG, "Ignored speech engine setup failure on ${thread.name}", error)
                FileLogger.logError("TTS", "Speech engine setup failed in the framework", error)
            } else {
                previous?.uncaughtException(thread, error)
            }
        }
    }

    /** True when the failure came from TextToSpeech's own connection setup task. */
    fun isTtsSetupFailure(error: Throwable): Boolean =
        generateSequence(error) { it.cause }.take(8).any { cause ->
            cause.stackTrace.any { frame ->
                frame.className.startsWith("android.speech.tts.TextToSpeech\$Connection") &&
                    frame.methodName == "doInBackground"
            }
        }
}
