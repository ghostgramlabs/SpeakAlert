package com.ghostgramlabs.speakalert.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
    fun getMaxAmplitude(): Int
}

class AndroidAudioRecorder(
    private val context: Context
): AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    override fun start(outputFile: File) {
        check(recorder == null) { "A recording is already in progress" }
        outputFile.parentFile?.mkdirs()

        // VOICE_COMMUNICATION asks the device audio HAL for its speech-processing path. On
        // supported devices this applies the available noise suppression, echo cancellation,
        // and voice gain handling before AAC encoding. Some OEMs do not expose that source to
        // third-party recorders, so fall back to the normal microphone instead of failing.
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC
        )
        var lastFailure: Exception? = null

        for (source in sources) {
            outputFile.delete()
            val candidate = createRecorder()
            try {
                configure(candidate, source, outputFile, highQuality = true)
                candidate.prepare()
                candidate.start()
                recorder = candidate
                this.outputFile = outputFile
                return
            } catch (failure: Exception) {
                lastFailure = failure
                Log.w(TAG, "Audio source $source was unavailable; trying fallback", failure)
                candidate.releaseSafely()
            }
        }

        // A small number of devices reject an explicitly requested AAC sample rate/bitrate.
        // Retain MediaRecorder's device defaults as a final compatibility path.
        outputFile.delete()
        val candidate = createRecorder()
        try {
            configure(candidate, MediaRecorder.AudioSource.MIC, outputFile, highQuality = false)
            candidate.prepare()
            candidate.start()
            recorder = candidate
            this.outputFile = outputFile
        } catch (failure: Exception) {
            candidate.releaseSafely()
            outputFile.delete()
            lastFailure?.let(failure::addSuppressed)
            throw failure
        }
    }

    override fun stop() {
        val activeRecorder = recorder ?: return
        recorder = null
        try {
            activeRecorder.stop()
        } catch (failure: RuntimeException) {
            // MediaRecorder throws when stopped before it has received enough audio. Do not
            // leave a corrupt M4A behind for the player to open.
            outputFile?.delete()
            throw failure
        } finally {
            activeRecorder.releaseSafely()
            outputFile = null
        }
    }

    override fun getMaxAmplitude(): Int {
        return recorder?.maxAmplitude ?: 0
    }

    private fun configure(
        recorder: MediaRecorder,
        audioSource: Int,
        outputFile: File,
        highQuality: Boolean
    ) {
        recorder.apply {
            setAudioSource(audioSource)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            if (highQuality) {
                setAudioChannels(CHANNEL_COUNT)
                setAudioSamplingRate(SAMPLE_RATE_HZ)
                setAudioEncodingBitRate(BIT_RATE_BPS)
            }
            setOutputFile(outputFile.absolutePath)
        }
    }

    private fun MediaRecorder.releaseSafely() {
        try {
            reset()
        } catch (_: RuntimeException) {
            // The recorder may not have reached the initialized state.
        }
        release()
    }

    private companion object {
        const val TAG = "AndroidAudioRecorder"
        const val CHANNEL_COUNT = 1
        const val SAMPLE_RATE_HZ = 44_100
        const val BIT_RATE_BPS = 128_000
    }
}
