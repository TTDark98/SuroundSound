package com.surroundsound.app.core

/**
 * Global audio engine constants for SurroundSound.
 * Adheres strictly to the architectural principles:
 * - 44.1 kHz sample rate
 * - 16-bit PCM source format normalized internally to 32-bit Float [-1.0f, 1.0f]
 * - Maximum 10.0 seconds source audio duration limit
 */
object AudioConstants {
    const val SAMPLE_RATE = 44100
    const val CHANNELS_MONO = 1
    const val CHANNELS_STEREO = 2
    const val BITS_PER_SAMPLE = 16
    const val BYTES_PER_SAMPLE = 2

    const val MAX_SOURCE_DURATION_SECONDS = 10.0
    const val MAX_SOURCE_SAMPLES_MONO = (SAMPLE_RATE * MAX_SOURCE_DURATION_SECONDS).toInt() // 441,000 samples

    const val WARNING_THRESHOLD_SECONDS = 8.0
}
