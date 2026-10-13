package com.surroundsound.app.core.pcm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmAudioBufferTest {

    @Test
    fun testBytesToNormalizedFloats_silence() {
        val bytes = ByteArray(4) { 0 }
        val floats = PcmAudioBuffer.bytesToNormalizedFloats(bytes)
        assertEquals(2, floats.size)
        assertEquals(0f, floats[0], 0.0001f)
        assertEquals(0f, floats[1], 0.0001f)
    }

    @Test
    fun testFloatsToBytes_andBack() {
        val originalFloats = floatArrayOf(0.0f, 0.5f, -0.5f, 1.0f, -1.0f)
        val bytes = PcmAudioBuffer.normalizedFloatsToBytes(originalFloats)
        val recoveredFloats = PcmAudioBuffer.bytesToNormalizedFloats(bytes)

        assertEquals(originalFloats.size, recoveredFloats.size)
        for (i in originalFloats.indices) {
            assertEquals(originalFloats[i], recoveredFloats[i], 0.001f)
        }
    }
}
