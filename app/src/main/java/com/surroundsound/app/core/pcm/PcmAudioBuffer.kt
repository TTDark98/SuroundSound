package com.surroundsound.app.core.pcm

/**
 * High-performance audio buffer utility in Kotlin.
 * Handles conversions between 16-bit PCM byte/short arrays and 32-bit Float normalization [-1.0f, 1.0f].
 */
object PcmAudioBuffer {

    /**
     * Converts a 16-bit PCM little-endian byte array to normalized float array [-1.0f, 1.0f].
     */
    fun bytesToNormalizedFloats(bytes: ByteArray, size: Int = bytes.size): FloatArray {
        val sampleCount = size / 2
        val floats = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            val byteIndex = i * 2
            val low = bytes[byteIndex].toInt() and 0xFF
            val high = bytes[byteIndex + 1].toInt()
            val sampleShort = (high shl 8) or low
            floats[i] = (sampleShort / 32768.0f).coerceIn(-1.0f, 1.0f)
        }
        return floats
    }

    /**
     * Converts normalized float samples [-1.0f, 1.0f] into 16-bit PCM little-endian byte array.
     */
    fun normalizedFloatsToBytes(floats: FloatArray, offset: Int = 0, count: Int = floats.size): ByteArray {
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            val sample = floats[offset + i].coerceIn(-1.0f, 1.0f)
            val shortVal = (sample * 32767.0f).toInt().coerceIn(-32768, 32767)
            val byteIndex = i * 2
            bytes[byteIndex] = (shortVal and 0xFF).toByte()
            bytes[byteIndex + 1] = ((shortVal shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Converts normalized float samples [-1.0f, 1.0f] to 16-bit signed shorts.
     */
    fun normalizedFloatsToShorts(floats: FloatArray, destination: ShortArray, offset: Int = 0, count: Int = floats.size) {
        for (i in 0 until count) {
            val sample = floats[offset + i].coerceIn(-1.0f, 1.0f)
            destination[i] = (sample * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }
    }
}
