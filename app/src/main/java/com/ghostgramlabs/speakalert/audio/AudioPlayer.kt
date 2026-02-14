package com.ghostgramlabs.speakalert.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun stop()
    fun pause()
    fun resume()
    fun seekTo(position: Int)
    fun isPlaying(): Boolean
    fun getDuration(): Int
    fun getCurrentPosition(): Int
    fun setVolume(volume: Float)
    var onCompletion: (() -> Unit)?
}

class AndroidAudioPlayer(
    private val context: Context
): AudioPlayer {

    private var player: MediaPlayer? = null
    override var onCompletion: (() -> Unit)? = null
    private var currentVolume: Float = 1.0f

    private fun logToFile(message: String) {
        try {
            // Use app-specific external storage which doesn't require dangerous permissions on Android 10+
            // Path: /storage/emulated/0/Android/data/com.ghostgramlabs.speakalert/files/Download/SpeakAlert_DebugLog.txt
            val logDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val logFile = File(logDir, "SpeakAlert_DebugLog.txt")
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            logFile.appendText("$timestamp: $message\n")
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayer", "Failed to write log to file: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun playFile(file: File) {
        // Stop previous if exists
        stop()
        
        val msg1 = "Attempting to play file: ${file.absolutePath}, exists=${file.exists()}"
        android.util.Log.d("AudioPlayer", msg1)
        logToFile(msg1)

        if (!file.exists()) {
             val msgError = "File does not exist: ${file.absolutePath}"
             android.util.Log.e("AudioPlayer", msgError)
             logToFile("ERROR: $msgError")
             showToast("Error: File not found")
             return
        }

        try {
            MediaPlayer.create(context, file.toUri()).apply {
                if (this == null) {
                    val msgNull = "MediaPlayer.create returned null for ${file.absolutePath}"
                    android.util.Log.e("AudioPlayer", msgNull)
                    logToFile("ERROR: $msgNull")
                    showToast("Error: Media player failed")
                    return
                }
                player = this
                setVolume(currentVolume, currentVolume)
                start()
                setOnCompletionListener { 
                    android.util.Log.d("AudioPlayer", "Playback completed")
                    logToFile("Playback completed")
                    onCompletion?.invoke()
                }
                android.util.Log.d("AudioPlayer", "Playback started successfully")
                logToFile("Playback started successfully")
                showToast("Playing audio...")
            }
        } catch (e: Exception) {
            val msgEx = "Error playing file: ${e.message}"
            android.util.Log.e("AudioPlayer", msgEx, e)
            logToFile("EXCEPTION: $msgEx")
            showToast("Error: ${e.message}")
        }
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        player?.setVolume(currentVolume, currentVolume)
    }

    override fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    override fun pause() {
        player?.pause()
    }

    override fun resume() {
        player?.start()
    }

    override fun seekTo(position: Int) {
        player?.seekTo(position)
    }

    override fun isPlaying(): Boolean {
        return try {
            player?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun getDuration(): Int {
        return try {
            player?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override fun getCurrentPosition(): Int {
        return try {
            player?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
