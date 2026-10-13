package com.surroundsound.app.core.dsp

import com.surroundsound.app.core.AudioConstants
import com.surroundsound.app.core.fft.FastFourierTransform
import com.surroundsound.app.domain.dna.SoundDna
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Sound DNA offline feature extractor.
 * Computes all 6 core acoustic features in pure Kotlin without cloud APIs.
 */
object SoundDnaExtractor {

    private const val FFT_SIZE = 2048
    private const val HOP_SIZE = 1024

    fun extract(
        samples: FloatArray,
        sampleRate: Int = AudioConstants.SAMPLE_RATE
    ): SoundDna {
        if (samples.isEmpty()) {
            return SoundDna(
                rmsEnergy = 0f,
                transientStrength = 0f,
                spectralCentroid = 0f,
                dominantFrequency = 0f,
                decayTimeMs = 0f,
                spectralFlux = 0f,
                badges = listOf("Silent"),
                isSilent = true,
                isClipped = false
            )
        }

        // 1. RMS Energy & DC Offset & Clipping Check
        var sumSquares = 0.0
        var sumSamples = 0.0
        var peakAbs = 0f
        var clippedCount = 0

        for (s in samples) {
            val a = abs(s)
            sumSquares += (s * s)
            sumSamples += s
            if (a > peakAbs) peakAbs = a
            if (a >= 0.999f) clippedCount++
        }

        val totalRms = sqrt(sumSquares / samples.size).toFloat().coerceIn(0f, 1f)
        val dcOffset = abs(sumSamples / samples.size).toFloat()
        val isSilent = totalRms < 0.005f || peakAbs < 0.015f
        val isClipped = clippedCount > (samples.size * 0.005f) // > 0.5% samples clipped

        if (isSilent) {
            return SoundDna(
                rmsEnergy = totalRms,
                transientStrength = 0f,
                spectralCentroid = 0f,
                dominantFrequency = 0f,
                decayTimeMs = 0f,
                spectralFlux = 0f,
                badges = listOf("Silent"),
                isSilent = true,
                isClipped = isClipped
            )
        }

        // 2. Transient Strength (Peak-to-Average Ratio / Crest Factor)
        val crestFactor = if (totalRms > 0.0001f) (peakAbs / totalRms) else 1f
        // Typical crest factor ranges from ~1.5 (dense noise) to 8+ (snappy transients)
        val transientStrength = ((crestFactor - 1.5f) / 6.5f).coerceIn(0f, 1f)

        // 3 & 4 & 6. FFT-based Metrics: Spectral Centroid, Dominant Frequency, Spectral Flux
        val fft = FastFourierTransform(FFT_SIZE)
        val halfFft = FFT_SIZE / 2
        val freqBinWidth = sampleRate.toFloat() / FFT_SIZE.toFloat()

        val frameCount = ((samples.size - FFT_SIZE) / HOP_SIZE).coerceAtLeast(1)
        val realBuffer = FloatArray(FFT_SIZE)
        val imagBuffer = FloatArray(FFT_SIZE)
        val currentMags = FloatArray(halfFft + 1)
        val previousMags = FloatArray(halfFft + 1)

        var totalCentroidSum = 0.0
        var totalCentroidWeights = 0.0
        var totalSpectralFlux = 0.0

        val maxMagnitudePerBin = FloatArray(halfFft + 1)

        for (frame in 0 until frameCount) {
            val offset = frame * HOP_SIZE
            System.arraycopy(samples, offset, realBuffer, 0, FFT_SIZE)

            fft.transform(realBuffer, imagBuffer, 0)
            fft.computeMagnitudes(realBuffer, imagBuffer, 0, currentMags)

            // Centroid for this frame
            var frameWeightedFreq = 0.0
            var frameMagSum = 0.0
            for (bin in 1..halfFft) {
                val mag = currentMags[bin]
                val freq = bin * freqBinWidth
                frameWeightedFreq += (freq * mag)
                frameMagSum += mag

                if (mag > maxMagnitudePerBin[bin]) {
                    maxMagnitudePerBin[bin] = mag
                }
            }

            if (frameMagSum > 0.0001) {
                val frameCentroid = frameWeightedFreq / frameMagSum
                totalCentroidSum += frameCentroid * frameMagSum
                totalCentroidWeights += frameMagSum
            }

            // Spectral Flux (Euclidean distance to previous frame)
            if (frame > 0) {
                var fluxSum = 0.0
                for (bin in 1..halfFft) {
                    val diff = currentMags[bin] - previousMags[bin]
                    if (diff > 0f) { // Positive spectral difference (onsets)
                        fluxSum += (diff * diff)
                    }
                }
                totalSpectralFlux += sqrt(fluxSum)
            }

            System.arraycopy(currentMags, 0, previousMags, 0, currentMags.size)
        }

        val spectralCentroid = if (totalCentroidWeights > 0.0) {
            (totalCentroidSum / totalCentroidWeights).toFloat()
        } else {
            1000f
        }

        val avgSpectralFlux = if (frameCount > 1) {
            (totalSpectralFlux / (frameCount - 1)).toFloat()
        } else {
            0f
        }

        // Dominant Frequency: Bin with highest overall peak magnitude
        var peakBin = 1
        var peakMag = 0f
        for (bin in 1..halfFft) {
            if (maxMagnitudePerBin[bin] > peakMag) {
                peakMag = maxMagnitudePerBin[bin]
                peakBin = bin
            }
        }
        val dominantFrequency = peakBin * freqBinWidth

        // 5. Decay Time: From highest frame peak down to -20dB (10% amplitude)
        val decayTimeMs = computeDecayTime(samples, sampleRate, peakAbs)

        // Generate Heuristic Badges
        val badges = generateBadges(
            transientStrength = transientStrength,
            spectralCentroid = spectralCentroid,
            dominantFrequency = dominantFrequency,
            decayTimeMs = decayTimeMs,
            spectralFlux = avgSpectralFlux,
            totalRms = totalRms,
            isClipped = isClipped
        )

        return SoundDna(
            rmsEnergy = totalRms,
            transientStrength = transientStrength,
            spectralCentroid = spectralCentroid,
            dominantFrequency = dominantFrequency,
            decayTimeMs = decayTimeMs,
            spectralFlux = avgSpectralFlux,
            badges = badges,
            isSilent = false,
            isClipped = isClipped
        )
    }

    private fun computeDecayTime(samples: FloatArray, sampleRate: Int, peakVal: Float): Float {
        if (peakVal <= 0.001f) return 50f
        val threshold = peakVal * 0.1f // -20dB threshold (10% amplitude)

        // Find index of maximum peak
        var peakIndex = 0
        var maxAmp = 0f
        for (i in samples.indices) {
            val a = abs(samples[i])
            if (a > maxAmp) {
                maxAmp = a
                peakIndex = i
            }
        }

        // Measure time until amplitude stays below threshold for at least 50ms window
        val holdWindowSamples = (sampleRate * 0.05f).toInt()
        var decayIndex = samples.size

        var i = peakIndex
        while (i < samples.size) {
            if (abs(samples[i]) <= threshold) {
                var staysBelow = true
                val checkLimit = (i + holdWindowSamples).coerceAtMost(samples.size)
                for (j in i until checkLimit) {
                    if (abs(samples[j]) > threshold) {
                        staysBelow = false
                        i = j
                        break
                    }
                }
                if (staysBelow) {
                    decayIndex = i
                    break
                }
            }
            i++
        }

        val decaySamples = (decayIndex - peakIndex).coerceAtLeast(0)
        return (decaySamples.toFloat() / sampleRate.toFloat()) * 1000f // in milliseconds
    }

    private fun generateBadges(
        transientStrength: Float,
        spectralCentroid: Float,
        dominantFrequency: Float,
        decayTimeMs: Float,
        spectralFlux: Float,
        totalRms: Float,
        isClipped: Boolean
    ): List<String> {
        val badges = mutableListOf<String>()

        if (isClipped) {
            badges.add("Overdriven")
        }

        // Transient / Attack characteristics
        if (transientStrength >= 0.55f && decayTimeMs < 350f) {
            badges.add("Punchy")
        } else if (transientStrength >= 0.65f) {
            badges.add("Percussive")
        }

        // Frequency brightness
        if (spectralCentroid >= 3500f) {
            badges.add("Bright")
        } else if (spectralCentroid <= 1200f) {
            badges.add("Warm / Deep")
        }

        // Dominant frequency / Pitch
        if (dominantFrequency in 40f..250f) {
            badges.add("Sub / Bass")
        } else if (dominantFrequency in 2000f..7000f) {
            badges.add("Metallic")
        }

        // Resonance / Sustain
        if (decayTimeMs >= 500f && totalRms >= 0.15f) {
            badges.add("Resonant")
        } else if (decayTimeMs < 150f) {
            badges.add("Snappy")
        }

        // Noise vs Texture
        if (spectralFlux > 2.5f) {
            badges.add("Textured / Noisy")
        } else if (spectralFlux < 0.8f && totalRms > 0.1f) {
            badges.add("Harmonic")
        }

        if (badges.isEmpty()) {
            badges.add("Balanced")
        }

        return badges.take(4) // Keep concise, high-signal badges
    }
}
