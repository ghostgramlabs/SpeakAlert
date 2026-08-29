package com.ghostgramlabs.speakalert.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteOrder
import kotlin.math.min

interface AudioEnhancer {
    /**
     * Clean up a finished recording in place. Returns true when [source] was replaced with an
     * improved version. Every failure path leaves the original file untouched - a reminder that
     * plays back raw is infinitely better than one that does not play back at all.
     */
    fun enhance(source: File): Boolean
}

/**
 * Codec plumbing around [VoiceProcessor]: decode the recording to PCM, run the voice cleanup
 * over it, encode it back to AAC in an MP4 container.
 *
 * Two passes over the file. The first learns the room's noise signature and how loud the take
 * is; the second does the work. Nothing is held in memory but a frame at a time, so a
 * five-minute reminder costs the same as a five-second one.
 */
class Mp4AudioEnhancer : AudioEnhancer {

    override fun enhance(source: File): Boolean {
        if (!source.exists() || source.length() <= 0L) return false
        val working = File(source.parentFile, source.name + ENHANCED_SUFFIX)

        return try {
            val format = readFormat(source) ?: return false
            if (format.channels != 1) {
                // Everything this app records is mono. Anything else is not ours to reshape.
                Log.i(TAG, "Skipping enhancement of ${format.channels}-channel audio")
                return false
            }

            val processor = VoiceProcessor(format.sampleRate)
            decode(source) { samples, count -> processor.analyze(samples, count) }
            val analysis = processor.finishAnalysis()

            val gain = processor.normalizationGain(analysis.peak)
            if (gain == null && !analysis.canDenoise) {
                Log.i(TAG, "Recording is already clean and at level; leaving it untouched")
                return false
            }

            processor.beginRender(analysis, gain ?: 1f)
            val written = render(source, working, format, processor)
            if (!written) {
                working.delete()
                return false
            }
            // Same directory, so this is an atomic swap. A player holding the old file open
            // keeps reading it safely; the next play picks up the improved one.
            // The user may have cancelled the take while codec work was in progress. Never
            // recreate a recording that the UI has deliberately removed.
            if (source.exists() && working.renameTo(source)) {
                true
            } else {
                working.delete()
                false
            }
        } catch (failure: Exception) {
            Log.w(TAG, "Could not enhance recording; keeping the original", failure)
            working.delete()
            false
        }
    }

    private fun render(
        source: File,
        destination: File,
        format: SourceFormat,
        processor: VoiceProcessor
    ): Boolean {
        val encoder = MediaCodec.createEncoderByType(MIME_AAC)
        val muxer = MediaMuxer(destination.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxing = false
        var framesEncoded = 0L
        var wroteAudio = false
        // Carries processed samples that did not fit in one encoder input buffer.
        var carry = FloatArray(0)
        var carrySize = 0

        try {
            val outputFormat = MediaFormat.createAudioFormat(
                MIME_AAC,
                format.sampleRate,
                format.channels
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE_BPS)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE)
            }
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val info = MediaCodec.BufferInfo()

            fun drain(endOfStream: Boolean) {
                var idlePolls = 0
                while (true) {
                    val index = encoder.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
                    when {
                        index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (!muxing) {
                                trackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxing = true
                            }
                        }
                        index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) return
                            if (++idlePolls >= MAX_END_OF_STREAM_POLLS) {
                                throw IllegalStateException("AAC encoder did not finish")
                            }
                        }
                        index >= 0 -> {
                            idlePolls = 0
                            val encoded = encoder.getOutputBuffer(index)
                            val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            if (encoded != null && info.size > 0 && !isConfig && muxing) {
                                encoded.position(info.offset)
                                encoded.limit(info.offset + info.size)
                                muxer.writeSampleData(trackIndex, encoded, info)
                                wroteAudio = true
                            }
                            encoder.releaseOutputBuffer(index, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                        }
                    }
                }
            }

            /** Hand processed samples to the encoder, buffering whatever does not fit. */
            fun feed(processed: FloatArray, count: Int) {
                if (carry.size < carrySize + count) {
                    carry = carry.copyOf(maxOf(carrySize + count, MAX_INPUT_SIZE))
                }
                System.arraycopy(processed, 0, carry, carrySize, count)
                carrySize += count

                var consumed = 0
                var idlePolls = 0
                while (consumed < carrySize) {
                    val inputIndex = encoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inputIndex < 0) {
                        drain(endOfStream = false)
                        if (++idlePolls >= MAX_CODEC_IDLE_POLLS) {
                            throw IllegalStateException("AAC encoder stopped accepting audio")
                        }
                        if (consumed > 0) break else continue
                    }
                    idlePolls = 0
                    val input = encoder.getInputBuffer(inputIndex)
                    if (input == null) {
                        drain(endOfStream = false)
                        continue
                    }
                    input.clear()
                    val shorts = input.order(ByteOrder.nativeOrder()).asShortBuffer()
                    val chunk = min(shorts.remaining(), carrySize - consumed)
                    for (i in 0 until chunk) {
                        shorts.put(carry[consumed + i].toInt().toShort())
                    }
                    consumed += chunk

                    val presentationTimeUs = framesEncoded * 1_000_000L / format.sampleRate
                    framesEncoded += chunk / format.channels
                    encoder.queueInputBuffer(inputIndex, 0, chunk * 2, presentationTimeUs, 0)
                    drain(endOfStream = false)
                }

                if (consumed > 0) {
                    System.arraycopy(carry, consumed, carry, 0, carrySize - consumed)
                    carrySize -= consumed
                }
            }

            decode(source) { samples, count ->
                processor.render(samples, count) { processed, produced -> feed(processed, produced) }
            }
            processor.finishRender { processed, produced -> feed(processed, produced) }

            // Signal end of stream and flush whatever the encoder still holds.
            var endInputPolls = 0
            while (true) {
                val inputIndex = encoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                if (inputIndex >= 0) {
                    encoder.queueInputBuffer(
                        inputIndex,
                        0,
                        0,
                        framesEncoded * 1_000_000L / format.sampleRate,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    break
                }
                drain(endOfStream = false)
                if (++endInputPolls >= MAX_CODEC_IDLE_POLLS) {
                    throw IllegalStateException("AAC encoder could not accept end of stream")
                }
            }
            drain(endOfStream = true)

            return wroteAudio
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxing) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun readFormat(source: File): SourceFormat? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = audioTrack(extractor) ?: return null
            val format = extractor.getTrackFormat(trackIndex)
            return SourceFormat(
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            )
        } catch (failure: Exception) {
            Log.w(TAG, "Could not read recording format", failure)
            return null
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun audioTrack(extractor: MediaExtractor): Int? {
        return (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        }
    }

    /** Decode [source] to 16-bit PCM, handing each block of samples to [onSamples]. */
    private fun decode(source: File, onSamples: (samples: ShortArray, count: Int) -> Unit) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = audioTrack(extractor) ?: return
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return

            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var sawInputEnd = false
            var sawOutputEnd = false
            var scratch = ShortArray(0)
            var idlePolls = 0

            while (!sawOutputEnd) {
                var madeProgress = false
                if (!sawInputEnd) {
                    val inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val buffer = codec.getInputBuffer(inputIndex)
                        val read = if (buffer == null) -1 else extractor.readSampleData(buffer, 0)
                        if (read < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEnd = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, read, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                        madeProgress = true
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
                if (outputIndex >= 0) {
                    madeProgress = true
                    val buffer = codec.getOutputBuffer(outputIndex)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val shorts = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                        val count = shorts.remaining()
                        if (scratch.size < count) scratch = ShortArray(count)
                        shorts.get(scratch, 0, count)
                        onSamples(scratch, count)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEnd = true
                    }
                }
                if (madeProgress) {
                    idlePolls = 0
                } else if (++idlePolls >= MAX_CODEC_IDLE_POLLS) {
                    throw IllegalStateException("Audio decoder stopped making progress")
                }
            }
        } finally {
            decoder?.let {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
            runCatching { extractor.release() }
        }
    }

    private data class SourceFormat(val sampleRate: Int, val channels: Int)

    private companion object {
        const val TAG = "Mp4AudioEnhancer"
        val MIME_AAC: String = MediaFormat.MIMETYPE_AUDIO_AAC
        const val ENHANCED_SUFFIX = ".enhanced"

        const val BIT_RATE_BPS = 128_000
        const val MAX_INPUT_SIZE = 16_384
        const val DEQUEUE_TIMEOUT_US = 10_000L
        const val MAX_END_OF_STREAM_POLLS = 100
        const val MAX_CODEC_IDLE_POLLS = 300
    }
}
