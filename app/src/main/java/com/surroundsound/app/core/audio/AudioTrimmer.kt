package com.surroundsound.app.core.audio

import com.surroundsound.app.core.AudioConstants
import com.surroundsound.app.domain.model.SourceAudio

/**
 * Trims audio samples strictly enforcing the 10.0-second maximum duration window.
 */
object AudioTrimmer {

    /**
     * Extracts a sub-window of up to 10.0 seconds from full samples.
     * @param fullSamples Normalized float audio samples
     * @param sampleRate Sample rate in Hz (typically 44100)
     * @param startSeconds Offset start in seconds
     * @param windowSeconds Window length (capped at 10.0 seconds)
     */
    fun trim(
        fullSamples: FloatArray,
        sampleRate: Int = AudioConstants.SAMPLE_RATE,
        startSeconds: Float,
        windowSeconds: Float = AudioConstants.MAX_SOURCE_DURATION_SECONDS.toFloat()
    ): FloatArray {
        val clampedWindow = windowSeconds.coerceIn(0.1f, AudioConstants.MAX_SOURCE_DURATION_SECONDS.toFloat())
        val startIndex = (startSeconds * sampleRate).toInt().coerceIn(0, fullSamples.size)
        val maxSamplesToTake = (clampedWindow * sampleRate).toInt().coerceAtMost(AudioConstants.MAX_SOURCE_SAMPLES_MONO)
        val availableSamples = (fullSamples.size - startIndex).coerceAtLeast(0)
        val lengthToCopy = maxSamplesToTake.coerceAtMost(availableSamples)

        val trimmed = FloatArray(lengthToCopy)
        System.arraycopy(fullSamples, startIndex, trimmed, 0, lengthToCopy)
        return trimmed
    }

    /**
     * Creates a new SourceAudio with trimmed samples.
     */
    fun trimSource(
        source: SourceAudio,
        fullSamples: FloatArray,
        startSeconds: Float,
        windowSeconds: Float = AudioConstants.MAX_SOURCE_DURATION_SECONDS.toFloat()
    ): SourceAudio {
        val trimmedSamples = trim(
            fullSamples = fullSamples,
            sampleRate = source.sampleRate,
            startSeconds = startSeconds,
            windowSeconds = windowSeconds
        )

        return source.copy(
            samples = trimmedSamples,
            durationSeconds = trimmedSamples.size.toFloat() / source.sampleRate.toFloat()
        )
    }
}
