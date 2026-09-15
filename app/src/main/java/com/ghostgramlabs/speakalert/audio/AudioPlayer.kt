package com.ghostgramlabs.speakalert.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun playUri(uri: Uri)
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

/** Preview player. Commands and MediaPlayer callbacks run on the main looper. */
class AndroidAudioPlayer internal constructor(
    private val context: Context,
    private val createPlayer: () -> MediaPlayer,
    private val showMessage: (String) -> Unit
) : AudioPlayer {
    constructor(context: Context) : this(context.applicationContext, { MediaPlayer() }, { message ->
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    })

    private var player: MediaPlayer? = null
    private var prepared = false
    private var playWhenReady = false
    private var pendingSeek: Int? = null
    private var currentVolume = 1.0f
    override var onCompletion: (() -> Unit)? = null

    override fun playFile(file: File) = playUri(file.toUri())

    override fun playUri(uri: Uri) {
        stop()
        playWhenReady = true
        try {
            val next = createPlayer()
            player = next
            next.setOnPreparedListener { ready ->
                // A stopped or replaced request must never start from a late callback.
                if (player === ready) {
                    try {
                        prepared = true
                        ready.setVolume(currentVolume, currentVolume)
                        pendingSeek?.let { ready.seekTo(it) }
                        pendingSeek = null
                        if (playWhenReady) ready.start()
                    } catch (error: Exception) {
                        fail(ready, error)
                    }
                }
            }
            next.setOnCompletionListener { completed ->
                if (player === completed) {
                    stop()
                    onCompletion?.invoke()
                }
            }
            next.setOnErrorListener { failed, what, extra ->
                fail(failed, IllegalStateException("MediaPlayer error $what/$extra"))
                true
            }
            next.setDataSource(context, uri)
            // create() calls prepare() synchronously and caused the production preview ANR.
            // Wait via the callback so the UI remains responsive during preparation.
            next.prepareAsync()
        } catch (error: Exception) {
            fail(player, error)
        }
    }

    private fun fail(failed: MediaPlayer?, error: Exception) {
        if (player !== failed) return
        stop()
        Log.e("AudioPlayer", "Unable to play preview", error)
        showMessage("Unable to play audio")
        // Reset the caller's preview state on asynchronous failure as well as completion.
        onCompletion?.invoke()
    }

    override fun stop() {
        val previous = player
        player = null
        prepared = false
        playWhenReady = false
        pendingSeek = null
        // release() is valid during preparation; stop() is not.
        runCatching { previous?.release() }
            .onFailure { Log.w("AudioPlayer", "Unable to release preview", it) }
    }

    override fun setVolume(volume: Float) {
        currentVolume = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 1f
        if (prepared) player?.setVolume(currentVolume, currentVolume)
    }

    override fun pause() {
        playWhenReady = false
        if (prepared) player?.pause()
    }

    override fun resume() {
        playWhenReady = true
        if (prepared) player?.start()
    }

    override fun seekTo(position: Int) {
        if (prepared) player?.seekTo(position.coerceAtLeast(0))
        else pendingSeek = position.coerceAtLeast(0)
    }

    override fun isPlaying(): Boolean =
        prepared && runCatching { player?.isPlaying ?: false }.getOrDefault(false)

    override fun getDuration(): Int =
        if (prepared) runCatching { player?.duration ?: 0 }.getOrDefault(0) else 0

    override fun getCurrentPosition(): Int =
        if (prepared) runCatching { player?.currentPosition ?: 0 }.getOrDefault(0) else 0
}
