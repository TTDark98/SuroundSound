package com.surroundsound.app

import com.surroundsound.app.core.AudioConstants
import com.surroundsound.app.core.audio.AudioTrimmer
import com.surroundsound.app.core.pcm.PcmAudioBuffer
import com.surroundsound.app.domain.model.Project
import com.surroundsound.app.domain.model.SourceAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Phase1MilestoneTest {

    @Test
    fun milestoneGateA_tenSecondMaxConstraintEnforced() {
        val sampleRate = AudioConstants.SAMPLE_RATE
        val fifteenSecondSamples = FloatArray(sampleRate * 15) { 0.25f }

        // Test AudioTrimmer strictly limits to 10.0s
        val trimmed = AudioTrimmer.trim(
            fullSamples = fifteenSecondSamples,
            sampleRate = sampleRate,
            startSeconds = 0f,
            windowSeconds = 10f
        )

        assertEquals(
            "Samples must be exactly 10s * 44100 = 441,000 samples",
            AudioConstants.MAX_SOURCE_SAMPLES_MONO,
            trimmed.size
        )
    }

    @Test
    fun milestoneGateA_sourceAudioModelIntegrity() {
        val samples = FloatArray(44100 * 5) { 0.1f } // 5 seconds
        val source = SourceAudio(
            name = "Test Mic Capture",
            samples = samples
        )

        assertEquals(5.0f, source.durationSeconds, 0.001f)
        assertEquals(44100, source.sampleRate)
        assertEquals(1, source.channelCount)
        assertNotNull(source.id)
    }

    @Test
    fun milestoneGateA_projectEntitySchemaIntegrity() {
        val project = Project(
            name = "Beat Session 01",
            primarySourceId = "source-123",
            secondarySourceIds = listOf("source-456")
        )

        assertEquals(2, project.allSourceIds.size)
        assertEquals("source-123", project.primarySourceId)
        assertEquals(120, project.bpm)
        assertEquals(16, project.stepCount)
    }

    @Test
    fun milestoneGateA_pcmFloatNormalizationBounds() {
        val rawShorts = shortArrayOf(Short.MIN_VALUE, -16384, 0, 16384, Short.MAX_VALUE)
        val rawBytes = ByteArray(rawShorts.size * 2)
        for (i in rawShorts.indices) {
            val s = rawShorts[i].toInt()
            rawBytes[i * 2] = (s and 0xFF).toByte()
            rawBytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }

        val floats = PcmAudioBuffer.bytesToNormalizedFloats(rawBytes)
        assertEquals(rawShorts.size, floats.size)

        for (f in floats) {
            assertTrue("Sample $f must be in range [-1.0f, 1.0f]", f in -1.0f..1.0f)
        }

        val roundTripBytes = PcmAudioBuffer.normalizedFloatsToBytes(floats)
        val roundTripFloats = PcmAudioBuffer.bytesToNormalizedFloats(roundTripBytes)

        for (i in floats.indices) {
            assertEquals(floats[i], roundTripFloats[i], 0.001f)
        }
    }
}
