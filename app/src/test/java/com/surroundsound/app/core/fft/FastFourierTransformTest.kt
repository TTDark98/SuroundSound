package com.surroundsound.app.core.fft

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FastFourierTransformTest {

    @Test
    fun testFFT_puretone_peakAtCorrectBin() {
        val fftSize = 2048
        val sampleRate = 44100
        val frequency = 440.0 // A4 note

        val fft = FastFourierTransform(fftSize)
        val real = FloatArray(fftSize) { i ->
            sin(2.0 * PI * frequency * i / sampleRate).toFloat()
        }
        val imag = FloatArray(fftSize)

        fft.transform(real, imag, 0)
        val magnitudes = FloatArray(fftSize / 2 + 1)
        fft.computeMagnitudes(real, imag, 0, magnitudes)

        // Find peak bin
        var peakBin = 0
        var peakMag = 0f
        for (i in 1 until magnitudes.size) {
            if (magnitudes[i] > peakMag) {
                peakMag = magnitudes[i]
                peakBin = i
            }
        }

        val detectedFreq = peakBin.toFloat() * sampleRate / fftSize
        // Allow within 1 bin width tolerance (~21.5 Hz for 2048 at 44100)
        val tolerance = sampleRate.toFloat() / fftSize
        assertEquals(frequency.toFloat(), detectedFreq, tolerance)
    }

    @Test
    fun testFFT_silenceProducesNearZeroMagnitude() {
        val fftSize = 1024
        val fft = FastFourierTransform(fftSize)
        val real = FloatArray(fftSize) { 0f }
        val imag = FloatArray(fftSize)

        fft.transform(real, imag, 0)
        val magnitudes = FloatArray(fftSize / 2 + 1)
        fft.computeMagnitudes(real, imag, 0, magnitudes)

        for (mag in magnitudes) {
            assertTrue("Magnitude for silence must be near zero, got $mag", mag < 0.01f)
        }
    }

    @Test
    fun testFFT_windowIsHannShaped() {
        val fft = FastFourierTransform(512)
        // Hann window: 0 at edges, 1 at center
        assertEquals(0f, fft.window[0], 0.001f)
        assertEquals(0f, fft.window[511], 0.001f)
        assertEquals(1f, fft.window[256], 0.01f)
    }
}
