package com.surroundsound.app.domain.model

import java.util.UUID

/**
 * Represents a user-provided raw audio source strictly limited to 10.0 seconds.
 */
data class SourceAudio(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sampleRate: Int = 44100,
    val channelCount: Int = 1,
    val samples: FloatArray, // Normalized [-1.0f, 1.0f]
    val durationSeconds: Float = samples.size.toFloat() / (sampleRate * channelCount),
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SourceAudio
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
