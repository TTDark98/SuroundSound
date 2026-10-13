package com.surroundsound.app.core.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * High-performance RMS / Peak amplitude extractor for waveform visualization.
 */
object WaveformExtractor {

    /**
     * Extracts normalized waveform amplitude values [0.0f, 1.0f] across [barCount] bins.
     */
    fun extractAmplitudes(samples: FloatArray, barCount: Int = 120): FloatArray {
        if (samples.isEmpty() || barCount <= 0) return FloatArray(barCount) { 0.05f }

        val amplitudes = FloatArray(barCount)
        val samplesPerBar = samples.size / barCount

        if (samplesPerBar <= 0) {
            for (i in 0 until barCount) {
                val sampleIdx = (i * samples.size / barCount).coerceIn(0, samples.size - 1)
                amplitudes[i] = abs(samples[sampleIdx]).coerceIn(0.05f, 1.0f)
            }
            return amplitudes
        }

        var maxRms = 0.001f
        for (i in 0 until barCount) {
            val startIdx = i * samplesPerBar
            val endIdx = (startIdx + samplesPerBar).coerceAtMost(samples.size)
            var sumSquares = 0.0
            val count = endIdx - startIdx

            for (j in startIdx until endIdx) {
                val s = samples[j]
                sumSquares += (s * s)
            }

            val rms = if (count > 0) sqrt(sumSquares / count).toFloat() else 0f
            amplitudes[i] = rms
            if (rms > maxRms) {
                maxRms = rms
            }
        }

        // Normalize so peak bar reaches ~0.95f, with minimum visibility floor of 0.08f
        val normFactor = 0.95f / maxRms
        for (i in 0 until barCount) {
            amplitudes[i] = (amplitudes[i] * normFactor).coerceIn(0.08f, 1.0f)
        }

        return amplitudes
    }
}
