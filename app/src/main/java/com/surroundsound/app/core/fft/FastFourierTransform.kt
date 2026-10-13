package com.surroundsound.app.core.fft

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance, zero-allocation Cooley-Tukey Radix-2 FFT in pure Kotlin.
 * Supports power-of-two frame sizes (e.g. 512, 1024, 2048).
 */
class FastFourierTransform(val fftSize: Int = 2048) {

    init {
        require(fftSize > 0 && (fftSize and (fftSize - 1)) == 0) {
            "FFT size must be a power of two, got $fftSize"
        }
    }

    // Precomputed Hann window to minimize spectral leakage
    val window = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
    }

    // Precomputed bit-reversal indices
    private val bitReversalIndices = IntArray(fftSize) { i ->
        var j = 0
        var tempI = i
        var k = 0
        val bits = Integer.numberOfTrailingZeros(fftSize)
        while (k < bits) {
            j = (j shl 1) or (tempI and 1)
            tempI = tempI shr 1
            k++
        }
        j
    }

    // Precomputed twiddle factor tables (cos and sin)
    private val cosTable = FloatArray(fftSize / 2)
    private val sinTable = FloatArray(fftSize / 2)

    init {
        for (i in 0 until fftSize / 2) {
            val angle = -2.0 * PI * i / fftSize
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    /**
     * Applies Hann window and computes forward FFT.
     * @param real Input samples array (at least offset + fftSize length). Transformed in-place to FFT real parts.
     * @param imag Imaginary parts array (at least fftSize length). Transformed in-place.
     * @param offset Starting index in input samples.
     */
    fun transform(real: FloatArray, imag: FloatArray, offset: Int = 0) {
        // Bit-reversal permutation with Hann windowing
        for (i in 0 until fftSize) {
            val j = bitReversalIndices[i]
            if (i <= j) {
                val realI = real[offset + i] * window[i]
                val realJ = real[offset + j] * window[j]
                real[offset + i] = realJ
                real[offset + j] = realI
            }
            imag[i] = 0f
        }

        // Cooley-Tukey butterfly computations
        var len = 2
        while (len <= fftSize) {
            val halfLen = len / 2
            val step = fftSize / len

            var k = 0
            while (k < fftSize) {
                for (j in 0 until halfLen) {
                    val tableIdx = j * step
                    val c = cosTable[tableIdx]
                    val s = sinTable[tableIdx]

                    val idx1 = offset + k + j
                    val idx2 = offset + k + j + halfLen

                    val uR = real[idx1]
                    val uI = imag[k + j]
                    val vR = real[idx2]
                    val vI = imag[k + j + halfLen]

                    val tR = vR * c - vI * s
                    val tI = vR * s + vI * c

                    real[idx1] = uR + tR
                    imag[k + j] = uI + tI
                    real[idx2] = uR - tR
                    imag[k + j + halfLen] = uI - tI
                }
                k += len
            }
            len = len shl 1
        }
    }

    /**
     * Computes the magnitude spectrum for the positive frequency bins (size = fftSize / 2 + 1).
     */
    fun computeMagnitudes(real: FloatArray, imag: FloatArray, offset: Int, magnitudes: FloatArray) {
        val halfSize = fftSize / 2
        for (i in 0..halfSize) {
            val r = real[offset + i]
            val im = imag[i]
            magnitudes[i] = sqrt(r * r + im * im)
        }
    }
}
