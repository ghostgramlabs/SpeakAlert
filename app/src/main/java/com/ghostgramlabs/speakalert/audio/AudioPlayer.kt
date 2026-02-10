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

    override fun playFile(file: File) {
        // Stop previous if exists
        stop()
        
        MediaPlayer.create(context, file.toUri()).apply {
            player = this
            setVolume(currentVolume, currentVolume)
            start()
            setOnCompletionListener { 
                // Just invoke the callback - let the caller handle cleanup
                // Don't call stop() here to avoid double-release issues
                onCompletion?.invoke()
            }
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
