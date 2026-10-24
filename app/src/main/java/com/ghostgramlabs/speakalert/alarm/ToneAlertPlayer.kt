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
    private var activeReminderId: Long? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var appContext: Context? = null

    /** True while [reminderId]'s alarm tone is sounding. */
    @Synchronized
    fun isPlayingFor(reminderId: Long): Boolean = ringtone != null && activeReminderId == reminderId

    @Synchronized
    fun start(
        context: Context,
        loopTimeoutMinutes: Int,
        selectedToneUri: String? = null,
        dndBypass: Boolean = true,
        reminderId: Long? = null
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
            activeReminderId = reminderId
            this.appContext = appContext
            broadcastStatus(appContext, reminderId, isPlaying = true)
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

    @Synchronized
    fun stop(reminderId: Long? = null) {
        if (reminderId != null && reminderId != activeReminderId) return
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
        val stoppedId = activeReminderId
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        } finally {
            ringtone = null
            activeReminderId = null
        }
        appContext?.let { if (stoppedId != null) broadcastStatus(it, stoppedId, isPlaying = false) }
    }

    // The same status the playback service sends, so the alert screen shows Silence for a tone too.
    private fun broadcastStatus(context: Context, reminderId: Long?, isPlaying: Boolean) {
        if (reminderId == null) return
        context.sendBroadcast(
            android.content.Intent("ACTION_PLAYBACK_STATUS").apply {
                setPackage(context.packageName)
                putExtra("reminderId", reminderId)
                putExtra("isPlaying", isPlaying)
            }
        )
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
