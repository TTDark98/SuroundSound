package com.surroundsound.app.core.audio

import com.surroundsound.app.core.AudioConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTrimmerTest {

    @Test
    fun testTrim_strictlyEnforcesMax10Seconds() {
        val sampleRate = 44100
        // Create 20 seconds of audio (882,000 samples)
        val twentySecSamples = FloatArray(sampleRate * 20) { i -> (i % 100).toFloat() / 100f }

        // Request 15 seconds window (should be clamped to 10s = 441,000 samples)
        val trimmed = AudioTrimmer.trim(
            fullSamples = twentySecSamples,
            sampleRate = sampleRate,
            startSeconds = 5.0f,
            windowSeconds = 15.0f
        )

        assertEquals(AudioConstants.MAX_SOURCE_SAMPLES_MONO, trimmed.size)
        val expectedDuration = trimmed.size.toFloat() / sampleRate
        assertEquals(10.0f, expectedDuration, 0.001f)
    }

    @Test
    fun testTrim_offsetWindow() {
        val sampleRate = 44100
        val samples = FloatArray(sampleRate * 12) { i -> i.toFloat() }

        // Window of 2 seconds starting at 3.0s
        val trimmed = AudioTrimmer.trim(
            fullSamples = samples,
            sampleRate = sampleRate,
            startSeconds = 3.0f,
            windowSeconds = 2.0f
        )

        assertEquals(sampleRate * 2, trimmed.size)
        assertEquals((sampleRate * 3).toFloat(), trimmed[0], 0.001f)
    }
}
