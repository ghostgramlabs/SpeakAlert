package com.ghostgramlabs.speakalert.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.ghostgramlabs.speakalert.MainActivity
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.alarm.ToneAlertPlayer
import com.ghostgramlabs.speakalert.alarm.ReminderActionReceiver
import com.ghostgramlabs.speakalert.VoiceReminderApp
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.FileLogger
import com.ghostgramlabs.speakalert.util.ReminderAudioSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class ReminderPlaybackService : Service(), TextToSpeech.OnInitListener, SensorEventListener {

    // ExoPlayer
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private lateinit var playerNotificationManager: PlayerNotificationManager
    
    // TTS
    private var tts: TextToSpeech? = null
    private var pendingTtsText: String? = null
    private var isTtsInitialized = false
    private var isTtsMode = false
    
    // For TTS replay
    private var currentTtsText: String? = null
    private var currentTtsTitle: String? = null
    private var currentTtsId: Long = -1L
    
    // Loop mode
    private var loopEnabled: Boolean = false
    private var loopTimeoutHandler: android.os.Handler? = null
    private var loopTimeoutRunnable: Runnable? = null

    // Audio Focus
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pendingSpeakAfterFocusGain: String? = null
    private var privatePlaybackEnabled: Boolean = false
    private var privateRouteNearEar: Boolean = false
    private var dndBypassEnabled: Boolean = true
    private var originalAudioMode: Int? = null
    private var originalSpeakerphoneOn: Boolean? = null
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var proximityListening: Boolean = false
    
    // Volume Control
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentVolume: Float = 1.0f
    private var progressJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    private fun broadcastPlaybackStatus(isPlaying: Boolean) {
        val positionMs = if (!isTtsMode && ::player.isInitialized) {
            runCatching { player.currentPosition.coerceAtLeast(0L) }.getOrDefault(0L)
        } else {
            0L
        }
        val durationMs = if (!isTtsMode && ::player.isInitialized) {
            runCatching { player.duration.takeIf { it > 0L } ?: 0L }.getOrDefault(0L)
        } else {
            0L
        }
        val statusIntent = Intent("ACTION_PLAYBACK_STATUS").apply {
            setPackage(packageName)
            putExtra("reminderId", currentReminderId)
            putExtra("isPlaying", isPlaying)
            putExtra("positionMs", positionMs)
            putExtra("durationMs", durationMs)
        }
        sendBroadcast(statusIntent)
    }

    private fun startProgressUpdates() {
        if (isTtsMode) return
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && ::player.isInitialized && player.isPlaying) {
                broadcastPlaybackStatus(isPlaying = true)
                delay(250)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        FileLogger.log("SERVICE: onCreate started")
        
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

            // Initialize ExoPlayer. Audio attributes are set when playback starts
            // because private playback uses phone-call style routing when enabled.
            player = ExoPlayer.Builder(this)
                .build()
                
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (currentReminderId != -1L) {
                        broadcastPlaybackStatus(isPlaying = isPlaying)
                    }
                    if (isPlaying) {
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        stopProgressUpdates()
                        FileLogger.log("SERVICE: ExoPlayer playback ended, loop=$loopEnabled")
                        if (loopEnabled) {
                            // Restart playback from beginning
                            player.seekTo(0)
                            player.play()
                        } else {

                            // User request: Playback from home is PREVIEW ONLY.
                            // Do NOT mark as done automatically. Just stop.
                            if (currentReminderId != -1L) {
                                FileLogger.log("SERVICE: Playback finished for $currentReminderId. Stopping service (no auto-complete).")
                                broadcastPlaybackStatus(isPlaying = false)
                            }
                            stopSelf()
                        }
                    }
                }
            })

            // MediaSession
            mediaSession = MediaSession.Builder(this, player).build()

            initPlayerNotificationManager()
            
            // Initialize TTS
            tts = TextToSpeech(this, this)
            
            // Observe Volume Setting
            try {
                val repository = (application as VoiceReminderApp).container.settingsRepository
                scope.launch {
                    repository.appVolume.collect { volume ->
                        currentVolume = volume
                        player.volume = volume
                        FileLogger.log("SERVICE: Volume updated to $volume")
                    }
                }
            } catch (e: Exception) {
                 FileLogger.logError("SERVICE", "Failed to init settings observer", e)
            }
            
            FileLogger.log("SERVICE: onCreate completed")
        } catch (e: Exception) {
            FileLogger.logError("SERVICE", "Error in onCreate", e)
            throw e
        }
    }

    @UnstableApi
    private fun initPlayerNotificationManager() {
        FileLogger.log("SERVICE: Initializing PlayerNotificationManager")
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
        .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return player.currentMediaItem?.mediaMetadata?.title ?: "Reminder"
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                val intent = Intent(this@ReminderPlaybackService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                return PendingIntent.getActivity(
                    this@ReminderPlaybackService,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                val position = player.currentPosition / 1000
                val duration = player.duration / 1000
                return if (duration > 0) {
                    String.format("%d:%02d / %d:%02d", position / 60, position % 60, duration / 60, duration % 60)
                } else {
                    "Playing..."
                }
            }

            override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? = null
        })
        .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                FileLogger.log("SERVICE: Notification posted (audio mode), ongoing=$ongoing")
                if (!isTtsMode && ongoing) {
                    startForeground(notificationId, notification)
                }
            }
            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                FileLogger.log("SERVICE: Notification cancelled")
                if (!isTtsMode) stopSelf()
            }
        })
        .setSmallIconResourceId(R.drawable.ic_notification)
        .setCustomActionReceiver(object : PlayerNotificationManager.CustomActionReceiver {
            override fun createCustomActions(
                context: Context,
                instanceId: Int
            ): MutableMap<String, NotificationCompat.Action> {
                // Create Stop action
                val stopIntent = Intent(context, ReminderPlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                val stopPendingIntent = PendingIntent.getService(
                    context,
                    instanceId,
                    stopIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val stopAction = NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_pause,
                    "Stop",
                    stopPendingIntent
                ).build()
                
                return mutableMapOf("stop" to stopAction)
            }
            
            override fun getCustomActions(player: Player): MutableList<String> {
                return mutableListOf("stop")
            }
            
            override fun onCustomAction(player: Player, action: String, intent: Intent) {
                if (action == "stop") {
                    FileLogger.log("SERVICE: Stop action from notification")
                    stopSelf()
                }
            }
        })
        .build()
        
        // Show play/pause and enable rewind/forward for seek
        playerNotificationManager.setUsePlayPauseActions(true)
        playerNotificationManager.setUseStopAction(true)
        
        playerNotificationManager.setPlayer(player)
        FileLogger.log("SERVICE: PlayerNotificationManager initialized")
    }

    private var currentReminderId: Long = -1L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FileLogger.log("SERVICE: onStartCommand started")
        
        try {
            // IMMEDIATELY satisfy FGS contract — must be the very first thing.
            // Android 16+ (SDK 36) enforces stricter timing on startForeground().
            // All code paths (ACTION_STOP, ACTION_REPLAY, normal start) must have
            // startForeground() called before any early return.
            FileLogger.log("SERVICE: Creating placeholder notification")
            val placeholderNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(APP_DISPLAY_NAME)
                .setContentText("Processing...")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            
            try {
                startForeground(NOTIFICATION_ID, placeholderNotification)
                FileLogger.log("SERVICE: startForeground succeeded")
            } catch (e: Exception) {
                // Catches ForegroundServiceStartNotAllowedException (Android 12+)
                FileLogger.logError("SERVICE", "startForeground failed", e)
                stopSelf()
                return START_NOT_STICKY
            }

            // Capture reminderId for completion logic
            val reminderId = intent?.getLongExtra(EXTRA_ID, -1L) ?: -1L
            if (reminderId != -1L) {
                currentReminderId = reminderId
            }

            if (intent?.action == "ACTION_STOP_SERVICE" || intent?.action == ACTION_STOP) {
                FileLogger.log("SERVICE: Received STOP action")
                stopProgressUpdates()
                broadcastPlaybackStatus(isPlaying = false)
                stopSelf()
                return START_NOT_STICKY
            }

            if (intent?.action == ACTION_SEEK) {
                val seekPositionMs = intent.getLongExtra(EXTRA_POSITION_MS, -1L)
                if (!isTtsMode && seekPositionMs >= 0L && ::player.isInitialized) {
                    player.seekTo(seekPositionMs)
                    broadcastPlaybackStatus(isPlaying = player.isPlaying)
                }
                return START_NOT_STICKY
            }

            if (reminderId != -1L) {
                broadcastPlaybackStatus(isPlaying = true)
            }
            
            if (intent?.action == ACTION_REPLAY) {
                FileLogger.log("SERVICE: Received REPLAY action")
                // Re-speak the current TTS text
                currentTtsText?.let { text ->
                    if (isTtsInitialized) {
                        performSpeak(text)
                    }
                }
                return START_NOT_STICKY
            }

            val audioPath = intent?.getStringExtra(EXTRA_AUDIO_PATH)
            val ttsText = intent?.getStringExtra(EXTRA_TTS_TEXT)
            val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Reminder"
            val id = intent?.getLongExtra(EXTRA_ID, -1L) ?: -1L
            loopEnabled = intent?.getBooleanExtra(EXTRA_LOOP, false) ?: false
            privatePlaybackEnabled = intent?.getBooleanExtra(EXTRA_PRIVATE_PLAYBACK, false) ?: false
            dndBypassEnabled = intent?.getBooleanExtra(EXTRA_DND_BYPASS, true) ?: true
            configureAudioRoute(privatePlaybackEnabled)
            
            // Start loop timeout countdown if looping is enabled
            if (loopEnabled) {
                startLoopTimeoutIfNeeded()
            }

            FileLogger.log("SERVICE: Params - audio=$audioPath, tts=${ttsText?.take(20)}, title=$title, id=$id, loop=$loopEnabled")

            if (audioPath != null) {
                isTtsMode = false
                FileLogger.log("SERVICE: Playing audio")
                playAudio(audioPath, title)
            } else if (ttsText != null) {
                isTtsMode = true
                FileLogger.log("SERVICE: Speaking TTS")
                speakTts(ttsText, title, id)
            } else {
                FileLogger.log("SERVICE: No audio or text provided, stopping")
                stopSelf()
            }
        } catch (e: Exception) {
            FileLogger.logError("SERVICE", "Error in onStartCommand", e)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun playAudio(path: String, title: String) {
        FileLogger.log("SERVICE: playAudio called with path=$path")
        try {
            tts?.stop()
            player.setAudioAttributes(buildPlayerAudioAttributes(), false)

            if (!ReminderAudioSource.isPlayable(this, path)) {
                FileLogger.log("SERVICE: Audio source unavailable: $path")
                stopSelf()
                return
            }

            val mediaItem = MediaItem.Builder()
                .setUri(ReminderAudioSource.toUri(path))
                .setMediaMetadata(
                     androidx.media3.common.MediaMetadata.Builder()
                         .setTitle(title)
                         .build()
                )
                .build()
            
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            
            FileLogger.log("SERVICE: ExoPlayer started")
        } catch (e: Exception) {
            FileLogger.logError("SERVICE", "Error playing audio", e)
            stopSelf()
        }
    }

    private fun speakTts(text: String, title: String, id: Long) {
        FileLogger.log("SERVICE: speakTts called with text='${text.take(30)}'")
        
        // Store for replay
        currentTtsText = text
        currentTtsTitle = title
        currentTtsId = id
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.setAudioAttributes(buildPlatformAudioAttributes())
            }
            // Show TTS Notification
            FileLogger.log("SERVICE: Creating TTS notification")
            val notification = createTtsNotification(title, text, id)
            FileLogger.log("SERVICE: Calling startForeground for TTS")
            startForeground(NOTIFICATION_ID, notification)
            FileLogger.log("SERVICE: TTS foreground started")

            if (isTtsInitialized) {
                FileLogger.log("SERVICE: TTS already initialized, speaking")
                performSpeak(text)
            } else {
                FileLogger.log("SERVICE: TTS not initialized, queuing text")
                pendingTtsText = text
            }
        } catch (e: Exception) {
            FileLogger.logError("SERVICE", "Error in speakTts", e)
            stopSelf()
        }
    }

    private fun performSpeak(text: String) {
        FileLogger.log("SERVICE: performSpeak called")
        if (tts == null) {
            FileLogger.log("SERVICE: TTS is null!")
            stopSelf()
            return
        }
        
        val focusRequestResult = requestAudioFocus()
        if (focusRequestResult == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
            FileLogger.log("SERVICE: Audio focus delayed, waiting for focus gain")
            pendingSpeakAfterFocusGain = text
            return
        }
        if (focusRequestResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Some Android 14 devices can reject focus transiently while still allowing TTS.
            // Do not kill playback flow; attempt speak as fallback.
            FileLogger.log("SERVICE: Audio focus not granted ($focusRequestResult), attempting TTS fallback")
        }

        speakNow(text)
    }

    private fun speakNow(text: String) {
        val params = android.os.Bundle()
        params.putInt(
            TextToSpeech.Engine.KEY_PARAM_STREAM,
            when {
                privatePlaybackEnabled && privateRouteNearEar -> AudioManager.STREAM_VOICE_CALL
                dndBypassEnabled -> AudioManager.STREAM_ALARM
                else -> AudioManager.STREAM_MUSIC
            }
        )
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
        
        val utteranceId = "REMINDER_TTS"
        
        FileLogger.log("SERVICE: Calling tts.speak()")
        val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId) ?: TextToSpeech.ERROR
        FileLogger.log("SERVICE: tts.speak() called, result=$speakResult")
        if (speakResult == TextToSpeech.ERROR) {
            FileLogger.log("SERVICE: tts.speak() failed with ERROR")
            stopSelf()
        }
    }

    private fun requestAudioFocus(): Int {
        FileLogger.log("SERVICE: Requesting audio focus")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = buildPlatformAudioAttributes()
                
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            pendingSpeakAfterFocusGain?.let { pendingText ->
                                FileLogger.log("SERVICE: Audio focus gained, resuming pending TTS")
                                pendingSpeakAfterFocusGain = null
                                speakNow(pendingText)
                            }
                        }
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            FileLogger.log("SERVICE: Audio focus lost")
                            tts?.stop()
                            stopSelf()
                        }
                    }
                }
                .build()
            
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            FileLogger.log("SERVICE: Audio focus request result code: $result")
            return result
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange -> if (focusChange == AudioManager.AUDIOFOCUS_LOSS) stopSelf() },
                when {
                    privatePlaybackEnabled && privateRouteNearEar -> AudioManager.STREAM_VOICE_CALL
                    dndBypassEnabled -> AudioManager.STREAM_ALARM
                    else -> AudioManager.STREAM_MUSIC
                },
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
            return result
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun configureAudioRoute(privatePlayback: Boolean) {
        if (!::audioManager.isInitialized) return
        if (privatePlayback) {
            if (originalAudioMode == null) {
                originalAudioMode = audioManager.mode
            }
            if (originalSpeakerphoneOn == null) {
                @Suppress("DEPRECATION")
                originalSpeakerphoneOn = audioManager.isSpeakerphoneOn
            }
            applyPrivatePlaybackRoute(useEarpiece = false)
            startProximityRouting()
            FileLogger.log("SERVICE: Adaptive private playback route enabled")
        } else {
            restoreAudioRoute()
        }
    }

    private fun restoreAudioRoute() {
        if (!::audioManager.isInitialized) return
        stopProximityRouting()
        originalAudioMode?.let { mode ->
            audioManager.mode = mode
            originalAudioMode = null
        }
        originalSpeakerphoneOn?.let { wasSpeakerphoneOn ->
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = wasSpeakerphoneOn
            originalSpeakerphoneOn = null
        }
        privateRouteNearEar = false
        updatePlaybackAudioAttributes()
    }

    private fun startProximityRouting() {
        if (proximityListening) return
        val sensor = proximitySensor
        if (sensor == null) {
            applyPrivatePlaybackRoute(useEarpiece = false)
            FileLogger.log("SERVICE: No proximity sensor; private playback stays on speaker")
            return
        }
        proximityListening = sensorManager?.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        ) == true
        FileLogger.log("SERVICE: Proximity routing listener registered=$proximityListening")
    }

    private fun stopProximityRouting() {
        if (!proximityListening) return
        sensorManager?.unregisterListener(this)
        proximityListening = false
    }

    private fun applyPrivatePlaybackRoute(useEarpiece: Boolean) {
        if (!::audioManager.isInitialized) return
        privateRouteNearEar = useEarpiece
        if (useEarpiece) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        } else {
            audioManager.mode = originalAudioMode ?: AudioManager.MODE_NORMAL
        }
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = !useEarpiece
        updatePlaybackAudioAttributes()
        FileLogger.log("SERVICE: Private playback route=${if (useEarpiece) "earpiece" else "speaker"}")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!privatePlaybackEnabled || event?.sensor?.type != Sensor.TYPE_PROXIMITY) return
        val maxRange = event.sensor.maximumRange
        val distance = event.values.firstOrNull() ?: maxRange
        val nearEar = distance < maxRange
        if (nearEar != privateRouteNearEar) {
            applyPrivatePlaybackRoute(nearEar)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun buildPlayerAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(
                when {
                    privatePlaybackEnabled && privateRouteNearEar -> C.USAGE_VOICE_COMMUNICATION
                    dndBypassEnabled -> C.USAGE_ALARM
                    else -> C.USAGE_MEDIA
                }
            )
            .build()
    }

    private fun buildPlatformAudioAttributes(): android.media.AudioAttributes {
        return android.media.AudioAttributes.Builder()
            .setUsage(
                when {
                    privatePlaybackEnabled && privateRouteNearEar -> {
                        android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION
                    }
                    dndBypassEnabled -> {
                        android.media.AudioAttributes.USAGE_ALARM
                    }
                    else -> {
                        android.media.AudioAttributes.USAGE_MEDIA
                    }
                }
            )
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }

    private fun updatePlaybackAudioAttributes() {
        if (::player.isInitialized) {
            runCatching {
                player.setAudioAttributes(buildPlayerAudioAttributes(), false)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runCatching {
                tts?.setAudioAttributes(buildPlatformAudioAttributes())
            }
        }
    }
    
    /**
     * Starts a countdown timer to auto-stop looping playback.
     * Timer duration is read from settings. If set to 0 (Never), no timer is started.
     */
    private fun startLoopTimeoutIfNeeded() {
        scope.launch {
            try {
                val app = applicationContext as com.ghostgramlabs.speakalert.VoiceReminderApp
                val timeoutMinutes = app.container.settingsRepository.loopTimeoutMinutes.first()
                
                FileLogger.log("SERVICE: Loop timeout setting = $timeoutMinutes minutes")
                
                if (timeoutMinutes == 0) {
                    // "Never" - no timeout
                    FileLogger.log("SERVICE: Loop timeout disabled (Never)")
                    return@launch
                }
                
                val timeoutMs = timeoutMinutes * 60 * 1000L
                
                // Cancel any existing timeout
                loopTimeoutRunnable?.let { loopTimeoutHandler?.removeCallbacks(it) }
                
                // Create handler on main thread
                loopTimeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
                loopTimeoutRunnable = Runnable {
                    FileLogger.log("SERVICE: Loop timeout reached ($timeoutMinutes minutes), auto-stopping")
                    loopEnabled = false
                    stopSelf()
                }
                
                loopTimeoutHandler?.postDelayed(loopTimeoutRunnable!!, timeoutMs)
                FileLogger.log("SERVICE: Loop timeout scheduled for $timeoutMinutes minutes")
                
            } catch (e: Exception) {
                FileLogger.logError("SERVICE", "Error starting loop timeout", e)
            }
        }
    }

    private fun createTtsNotification(title: String, text: String, id: Long): Notification {
        FileLogger.log("SERVICE: Creating TTS notification")
        
        // Stop action
        val stopIntent = Intent(this, ReminderPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        // Replay action
        val replayIntent = Intent(this, ReminderPlaybackService::class.java).apply {
            action = ACTION_REPLAY
        }
        val replayPendingIntent = PendingIntent.getService(this, 2, replayIntent, PendingIntent.FLAG_IMMUTABLE)

        // Snooze action
        val snoozeIntent = Intent(this, ReminderActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("reminderId", id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(this, 1, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Speaking: \"${text.take(50)}${if (text.length > 50) "..." else ""}\"")
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(android.R.drawable.ic_media_play, "Replay", replayPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onInit(status: Int) {
        FileLogger.log("SERVICE: TTS onInit called with status=$status")
        if (status == TextToSpeech.SUCCESS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.setAudioAttributes(buildPlatformAudioAttributes())
            }
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                FileLogger.log("SERVICE: TTS language not supported")
                stopSelf()
            } else {
                isTtsInitialized = true
                FileLogger.log("SERVICE: TTS initialized successfully")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        FileLogger.log("SERVICE: TTS utterance started")
                    }
                    override fun onDone(utteranceId: String?) {
                        FileLogger.log("SERVICE: TTS utterance done, loop=$loopEnabled")
                        if (loopEnabled && currentTtsText != null) {
                            // Replay TTS text
                            performSpeak(currentTtsText!!)
                        } else {
                            broadcastPlaybackStatus(isPlaying = false)
                            stopSelf()
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        FileLogger.log("SERVICE: TTS utterance error")
                        broadcastPlaybackStatus(isPlaying = false)
                        stopSelf()
                    }
                })
                
                pendingTtsText?.let {
                    FileLogger.log("SERVICE: Speaking pending TTS text")
                    performSpeak(it)
                    pendingTtsText = null
                }
            }
        } else {
            FileLogger.log("SERVICE: TTS initialization failed with status=$status")
            stopSelf()
        }
    }

    @UnstableApi
    override fun onDestroy() {
        FileLogger.log("SERVICE: onDestroy called")
        try {
            stopProgressUpdates()
            // Ensure UI always gets a terminal "not playing" signal.
            broadcastPlaybackStatus(isPlaying = false)

            // Cancel loop timeout if active
            loopTimeoutRunnable?.let { loopTimeoutHandler?.removeCallbacks(it) }
            loopTimeoutHandler = null
            loopTimeoutRunnable = null
            pendingSpeakAfterFocusGain = null
            
            mediaSession?.release()
            mediaSession = null
            playerNotificationManager.setPlayer(null)
            player.release()
            
            tts?.stop()
            tts?.shutdown()
            abandonAudioFocus()
            restoreAudioRoute()
        } catch (e: Exception) {
            FileLogger.logError("SERVICE", "Error in onDestroy", e)
        }
        
        super.onDestroy()
        scope.cancel()
        FileLogger.log("SERVICE: onDestroy completed")
    }

    companion object {
        const val CHANNEL_ID = "playback_channel"
        // Reserve a high fixed ID so it never collides with reminder alert IDs (which use reminderId.toInt()).
        const val NOTIFICATION_ID = 2_000_001
        
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AUDIO_PATH = "extra_audio_path"
        const val EXTRA_TTS_TEXT = "extra_tts_text"
        
        const val ACTION_STOP = "action_stop"
        const val ACTION_REPLAY = "action_replay"
        const val ACTION_SEEK = "action_seek"
        const val EXTRA_LOOP = "extra_loop"
        const val EXTRA_POSITION_MS = "extra_position_ms"
        const val EXTRA_PRIVATE_PLAYBACK = "extra_private_playback"
        const val EXTRA_DND_BYPASS = "extra_dnd_bypass"
        
        fun start(
            context: Context,
            id: Long,
            title: String?,
            audioPath: String?,
            ttsText: String?,
            loop: Boolean = false,
            isFromBootContext: Boolean = false,
            privatePlayback: Boolean = false,
            dndBypass: Boolean = true
        ) {
            FileLogger.log("SERVICE.start() called - id=$id, audio=$audioPath, tts=${ttsText?.take(20)}, loop=$loop, bootContext=$isFromBootContext")
            ToneAlertPlayer.stop()

            // ANDROID 15+ GUARD: BOOT_COMPLETED receivers CANNOT start
            // mediaPlayback foreground services. If this call originates from
            // a boot-rescheduled alarm, unconditionally skip on Android 15+.
            if (isFromBootContext && Build.VERSION.SDK_INT >= 35) {
                FileLogger.log("SERVICE.start() - SKIPPED: Android 15+ boot context restriction. Notification-only fallback.")
                return
            }

            val intent = Intent(context, ReminderPlaybackService::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_LOOP, loop)
                putExtra(EXTRA_PRIVATE_PLAYBACK, privatePlayback)
                putExtra(EXTRA_DND_BYPASS, dndBypass)
                if (audioPath != null) putExtra(EXTRA_AUDIO_PATH, audioPath)
                if (ttsText != null) putExtra(EXTRA_TTS_TEXT, ttsText)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                FileLogger.log("SERVICE.start() - service started successfully")
            } catch (e: Exception) {
                // Catch ForegroundServiceStartNotAllowedException (Android 12+) and others
                FileLogger.logError("SERVICE.start()", "Failed to start service", e)
                // We do NOT rethrow here, because that crashes the app.
                // The caller falls through to show a notification-only experience.
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, ReminderPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                FileLogger.logError("SERVICE.stop()", "Failed to request service stop", e)
            }
        }

        fun seek(context: Context, positionMs: Long) {
            val intent = Intent(context, ReminderPlaybackService::class.java).apply {
                action = ACTION_SEEK
                putExtra(EXTRA_POSITION_MS, positionMs)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                FileLogger.logError("SERVICE.seek()", "Failed to request seek", e)
            }
        }
    }
}

