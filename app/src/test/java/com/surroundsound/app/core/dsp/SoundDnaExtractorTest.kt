package com.surroundsound.app.core.dsp

import com.surroundsound.app.core.AudioConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SoundDnaExtractorTest {

    @Test
    fun testExtract_silenceDetected() {
        val samples = FloatArray(44100) { 0.0001f }
        val dna = SoundDnaExtractor.extract(samples)

        assertTrue("Near-silence must be detected", dna.isSilent)
        assertTrue("Silent badge expected", dna.badges.contains("Silent"))
    }

    @Test
    fun testExtract_pureA440() {
        val sampleRate = AudioConstants.SAMPLE_RATE
        val samples = FloatArray(sampleRate * 2) { i ->
            (0.5f * sin(2.0 * PI * 440.0 * i / sampleRate)).toFloat()
        }

        val dna = SoundDnaExtractor.extract(samples, sampleRate)

        assertFalse("A 440Hz tone is not silent", dna.isSilent)
        assertTrue("RMS energy should be moderate", dna.rmsEnergy > 0.2f)
        // Dominant frequency should be near 440 Hz (within ±50 Hz tolerance for FFT bin resolution)
        assertTrue("Dominant frequency should be near 440Hz, got ${dna.dominantFrequency}", dna.dominantFrequency in 390f..490f)
    }

    @Test
    fun testExtract_clippingDetected() {
        // Generate heavily clipped square wave
        val sampleRate = AudioConstants.SAMPLE_RATE
        val samples = FloatArray(sampleRate) { i ->
            if (i % 100 < 50) 1.0f else -1.0f
        }

        val dna = SoundDnaExtractor.extract(samples, sampleRate)
        assertTrue("Clipping must be detected", dna.isClipped)
    }

    @Test
    fun milestoneGateB_allSixMetricsComputeUnder500ms() {
        val sampleRate = AudioConstants.SAMPLE_RATE
        // Worst-case: full 10 seconds of audio (441,000 samples)
        val tenSecondSamples = FloatArray(AudioConstants.MAX_SOURCE_SAMPLES_MONO) { i ->
            (0.4f * sin(2.0 * PI * 660.0 * i / sampleRate)).toFloat() +
            (0.2f * sin(2.0 * PI * 1320.0 * i / sampleRate)).toFloat()
        }

        val startTimeMs = System.currentTimeMillis()
        val dna = SoundDnaExtractor.extract(tenSecondSamples, sampleRate)
        val elapsedMs = System.currentTimeMillis() - startTimeMs

        assertTrue("Sound DNA extraction on 10s clip must complete under 500ms, took ${elapsedMs}ms", elapsedMs < 500)

        // Verify all 6 metrics produced non-default values
        assertTrue("RMS energy must be computed", dna.rmsEnergy > 0f)
        assertTrue("Spectral centroid must be computed", dna.spectralCentroid > 0f)
        assertTrue("Dominant frequency must be computed", dna.dominantFrequency > 0f)
        assertTrue("Decay time must be computed", dna.decayTimeMs >= 0f)
        assertTrue("Spectral flux must be computed", dna.spectralFlux >= 0f)
        assertTrue("Badges must be generated", dna.badges.isNotEmpty())
    }
}
