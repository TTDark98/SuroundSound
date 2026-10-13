package com.surroundsound.app.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformExtractorTest {

    @Test
    fun testExtractAmplitudes_correctBarCount() {
        val samples = FloatArray(44100) { i -> (i % 50).toFloat() / 50f }
        val amplitudes = WaveformExtractor.extractAmplitudes(samples, barCount = 100)

        assertEquals(100, amplitudes.size)
        for (amp in amplitudes) {
            assertTrue("Amplitude $amp must be between 0.0f and 1.0f", amp in 0.0f..1.0f)
        }
    }

    @Test
    fun testExtractAmplitudes_emptySamplesHandlesGracefully() {
        val amplitudes = WaveformExtractor.extractAmplitudes(floatArrayOf(), barCount = 50)
        assertEquals(50, amplitudes.size)
    }

    @Test
    fun testExtractAmplitudes_peakNormalized() {
        val samples = floatArrayOf(0.1f, 0.2f, 0.9f, 0.2f, 0.1f)
        val amplitudes = WaveformExtractor.extractAmplitudes(samples, barCount = 5)

        val maxAmp = amplitudes.maxOrNull() ?: 0f
        assertTrue("Max amplitude should be normalized close to 0.95f", maxAmp >= 0.9f)
    }
}
