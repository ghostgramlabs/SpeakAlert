package com.ghostgramlabs.speakalert.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Exercises the actual signal processing on synthetic audio. This is where the voice cleanup is
 * verified - the codec plumbing around it cannot run off-device.
 */
class VoiceProcessorTest {

    private val sampleRate = 44_100

    @Test
    fun `speech is preserved and steady background noise is reduced`() {
        // Speech has to come in bursts here, and that is the whole point rather than a
        // convenience: the estimator separates room from voice by looking at the quietest
        // moment in each window. A continuous tone genuinely is stationary noise, and would be
        // removed - correctly - by any method built on that assumption.
        val speech = speechLike(amplitude = 4_000f, samples = SAMPLE_COUNT)
        val noise = whiteNoise(amplitude = 500f, samples = SAMPLE_COUNT, seed = 7)
        val noisy = mix(speech, noise)

        val cleaned = process(noisy)

        // Measured as high-frequency energy rather than a sample-by-sample difference: the
        // overlap-add delays the output, so comparing sample N to sample N would be measuring
        // the phase shift, not the noise. The 400Hz tone barely registers in this band while
        // white noise dominates it, so a drop here is a drop in noise.
        val noiseBefore = highFrequencyEnergy(scaleToMatch(noisy, speech))
        val noiseAfter = highFrequencyEnergy(scaleToMatch(cleaned, speech))
        assertTrue(
            "Expected residual noise to drop (before=$noiseBefore after=$noiseAfter)",
            noiseAfter < noiseBefore
        )

        // And the voice itself must survive: the tone should still dominate the result.
        val speechEnergy = rms(scaleToMatch(cleaned, speech))
        assertTrue(
            "Expected the tone to survive denoising (rms=$speechEnergy)",
            speechEnergy > rms(speech) * 0.6f
        )
    }

    @Test
    fun `a quiet recording is brought up to a consistent level`() {
        // Quiet enough to need lifting, but within the gain ceiling so it reaches the target.
        val quiet = tone(frequency = 300f, amplitude = 5_000f, samples = SAMPLE_COUNT)

        val processor = VoiceProcessor(sampleRate)
        processor.analyze(quiet, quiet.size)
        val analysis = processor.finishAnalysis()
        val gain = processor.normalizationGain(analysis.peak)

        assertNotNull("A quiet take should be lifted", gain)
        val lifted = analysis.peak * gain!!
        assertEquals(VoiceProcessor.TARGET_PEAK, lifted, VoiceProcessor.TARGET_PEAK * 0.02f)
    }

    @Test
    fun `a near-silent recording is lifted only so far`() {
        // Barely above the noise floor. Lifting this to full scale would just make the room
        // noise loud, so the gain ceiling should stop well short of the target.
        val faint = tone(frequency = 300f, amplitude = 300f, samples = SAMPLE_COUNT)

        val processor = VoiceProcessor(sampleRate)
        processor.analyze(faint, faint.size)
        val analysis = processor.finishAnalysis()
        val gain = processor.normalizationGain(analysis.peak)

        assertNotNull(gain)
        assertTrue(
            "Gain ${gain!!} should be capped rather than reaching the target",
            analysis.peak * gain < VoiceProcessor.TARGET_PEAK
        )
    }

    @Test
    fun `an already loud recording is left at its level`() {
        val loud = tone(frequency = 300f, amplitude = 28_000f, samples = SAMPLE_COUNT)

        val processor = VoiceProcessor(sampleRate)
        processor.analyze(loud, loud.size)
        val analysis = processor.finishAnalysis()

        assertNull(
            "Nothing to gain from re-encoding a take already at level",
            processor.normalizationGain(analysis.peak)
        )
    }

    @Test
    fun `normalization never pushes samples past full scale`() {
        val quiet = tone(frequency = 250f, amplitude = 900f, samples = SAMPLE_COUNT)

        val cleaned = process(quiet)

        val peak = cleaned.maxOf { abs(it.toInt()) }
        assertTrue("Peak $peak should stay within 16-bit range", peak <= 32_767)
    }

    @Test
    fun `low frequency rumble is filtered out`() {
        // 40 Hz is well below speech - handling thumps and mains hum live here.
        val rumble = tone(frequency = 40f, amplitude = 8_000f, samples = SAMPLE_COUNT)

        val processor = VoiceProcessor(sampleRate)
        processor.analyze(rumble, rumble.size)
        val analysis = processor.finishAnalysis()

        // The high-pass runs during analysis, so the measured peak is the filtered one.
        assertTrue(
            "Expected 40Hz rumble to be attenuated (peak=${analysis.peak})",
            analysis.peak < 8_000f * 0.5f
        )
    }

    @Test
    fun `a take too short to characterise is not denoised`() {
        val blip = tone(frequency = 400f, amplitude = 5_000f, samples = 512)

        val processor = VoiceProcessor(sampleRate)
        processor.analyze(blip, blip.size)
        val analysis = processor.finishAnalysis()

        assertFalse(
            "Too few frames to tell a room from a voice",
            analysis.canDenoise
        )
    }

    @Test
    fun `every input sample is accounted for in the output`() {
        val input = tone(frequency = 400f, amplitude = 5_000f, samples = SAMPLE_COUNT)

        val output = process(input)

        // Overlap-add holds back one hop, so allow a frame of slack either way.
        val difference = abs(output.size - input.size)
        assertTrue(
            "Output length ${output.size} should track input length ${input.size}",
            difference <= VoiceProcessor.FRAME_SIZE
        )
    }

    @Test
    fun `silence stays silent rather than becoming noise`() {
        val silence = ShortArray(SAMPLE_COUNT)

        val cleaned = process(silence)

        val peak = if (cleaned.isEmpty()) 0 else cleaned.maxOf { abs(it.toInt()) }
        assertTrue("Silence should not be amplified into anything (peak=$peak)", peak <= 1)
    }

    // ---------------------------------------------------------------- helpers

    /** Run both passes the way [Mp4AudioEnhancer] does, returning the processed samples. */
    private fun process(input: ShortArray): ShortArray {
        val processor = VoiceProcessor(sampleRate)
        processor.analyze(input, input.size)
        val analysis = processor.finishAnalysis()
        val gain = processor.normalizationGain(analysis.peak) ?: 1f

        processor.beginRender(analysis, gain)
        val collected = ArrayList<Short>(input.size)
        val collect: (FloatArray, Int) -> Unit = { block, count ->
            for (i in 0 until count) collected.add(block[i].toInt().toShort())
        }
        processor.render(input, input.size, collect)
        processor.finishRender(collect)
        return collected.toShortArray()
    }

    /**
     * A stand-in for a voice: a vowel-ish tone with a harmonic, switched on and off at roughly
     * syllable rate so there are gaps where only the room is audible.
     */
    private fun speechLike(amplitude: Float, samples: Int): ShortArray {
        val onSamples = sampleRate / 4      // 250ms of voice
        val cycleSamples = onSamples * 2    // followed by 250ms of silence
        return ShortArray(samples) { index ->
            if (index % cycleSamples >= onSamples) return@ShortArray 0
            val time = index.toDouble() / sampleRate
            val fundamental = sin(2.0 * PI * 220.0 * time)
            val harmonic = 0.5 * sin(2.0 * PI * 440.0 * time)
            (amplitude * (fundamental + harmonic) / 1.5).toInt().toShort()
        }
    }

    private fun tone(frequency: Float, amplitude: Float, samples: Int): ShortArray {
        return ShortArray(samples) { index ->
            (amplitude * sin(2.0 * PI * frequency * index / sampleRate)).toInt().toShort()
        }
    }

    private fun whiteNoise(amplitude: Float, samples: Int, seed: Int): ShortArray {
        val random = Random(seed)
        return ShortArray(samples) {
            (amplitude * (random.nextFloat() * 2f - 1f)).toInt().toShort()
        }
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        return ShortArray(a.size) { index ->
            (a[index] + b[index]).coerceIn(-32_768, 32_767).toShort()
        }
    }

    /** Undo the processor's normalisation so signals can be compared like for like. */
    private fun scaleToMatch(actual: ShortArray, reference: ShortArray): ShortArray {
        val actualPeak = if (actual.isEmpty()) 0 else actual.maxOf { abs(it.toInt()) }
        val referencePeak = reference.maxOf { abs(it.toInt()) }
        if (actualPeak == 0) return actual
        val scale = referencePeak.toFloat() / actualPeak
        return ShortArray(actual.size) { (actual[it] * scale).toInt().toShort() }
    }

    private fun rms(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (sample in samples) sum += sample.toDouble() * sample
        return sqrt(sum / samples.size).toFloat()
    }

    /**
     * Energy in the upper part of the spectrum, via a first-difference filter. It attenuates a
     * few-hundred-hertz tone to almost nothing while passing broadband noise nearly untouched,
     * which makes it a phase-independent stand-in for "how much hiss is left".
     */
    private fun highFrequencyEnergy(samples: ShortArray): Float {
        if (samples.size < 2) return 0f
        var sum = 0.0
        for (i in 1 until samples.size) {
            val delta = (samples[i] - samples[i - 1]).toDouble()
            sum += delta * delta
        }
        return sqrt(sum / (samples.size - 1)).toFloat()
    }

    private companion object {
        /** Two seconds of audio: several noise windows, and a few syllables inside them. */
        const val SAMPLE_COUNT = 88_200
    }
}
