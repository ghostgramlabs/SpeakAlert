package com.ghostgramlabs.speakalert.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.ghostgramlabs.speakalert.util.FileLogger

object ToneAlertPlayer {
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    fun start(
        context: Context,
        loopTimeoutMinutes: Int,
        selectedToneUri: String? = null,
        dndBypass: Boolean = true
    ) {
        stop()

        val uri = resolveToneUri(context, selectedToneUri) ?: run {
            FileLogger.log("TONE: No alarm/notification/ringtone URI available")
            return
        }

        val appContext = context.applicationContext
        val nextTone = RingtoneManager.getRingtone(appContext, uri)
        if (nextTone == null) {
            FileLogger.log("TONE: Failed to obtain ringtone for uri=$uri")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                nextTone.isLooping = true
            }
            nextTone.audioAttributes = AudioAttributes.Builder()
                .setUsage(if (dndBypass) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            nextTone.play()
            ringtone = nextTone
            FileLogger.log("TONE: Started alarm tone, timeoutMinutes=$loopTimeoutMinutes")
        } catch (e: Exception) {
            FileLogger.logError("TONE", "Failed to start alarm tone", e)
            return
        }

        if (loopTimeoutMinutes > 0) {
            val timeoutMs = loopTimeoutMinutes * 60_000L
            timeoutRunnable = Runnable {
                FileLogger.log("TONE: Loop timeout reached, stopping alarm tone")
                stop()
            }
            handler.postDelayed(timeoutRunnable!!, timeoutMs)
        }
    }

    fun stop() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        } finally {
            ringtone = null
        }
    }

    private fun resolveToneUri(context: Context, selectedToneUri: String?): Uri? {
        val appContext = context.applicationContext
        if (!selectedToneUri.isNullOrBlank()) {
            val customUri = Uri.parse(selectedToneUri)
            if (RingtoneManager.getRingtone(appContext, customUri) != null) {
                return customUri
            }
            FileLogger.log("TONE: Selected tone unavailable, falling back to default uri=$selectedToneUri")
        }

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }
}
