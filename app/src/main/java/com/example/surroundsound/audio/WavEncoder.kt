package com.example.surroundsound.audio

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream

object WavEncoder {
    fun encodeToWav(
        leftSamples: FloatArray,
        rightSamples: FloatArray,
        sampleRate: Int = 44100
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        writeWavToStream(leftSamples, rightSamples, sampleRate, bos)
        return bos.toByteArray()
    }

    fun writeWavToStream(
        leftSamples: FloatArray,
        rightSamples: FloatArray,
        sampleRate: Int = 44100,
        out: OutputStream
    ) {
        val numSamples = leftSamples.size
        val channels = 2
        val bitsPerSample = 16
        val subChunk2Size = numSamples * channels * (bitsPerSample / 8)
        val chunkSize = 36 + subChunk2Size

        val dos = DataOutputStream(out)

        // RIFF header
        dos.writeBytes("RIFF")
        dos.writeInt(Integer.reverseBytes(chunkSize))
        dos.writeBytes("WAVE")

        // fmt chunk
        dos.writeBytes("fmt ")
        dos.writeInt(Integer.reverseBytes(16)) // Subchunk1Size
        dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat = 1 (PCM)
        dos.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt()) // NumChannels
        dos.writeInt(Integer.reverseBytes(sampleRate)) // SampleRate
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        dos.writeInt(Integer.reverseBytes(byteRate)) // ByteRate
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()
        dos.writeShort(java.lang.Short.reverseBytes(blockAlign).toInt()) // BlockAlign
        dos.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt()) // BitsPerSample

        // data chunk
        dos.writeBytes("data")
        dos.writeInt(Integer.reverseBytes(subChunk2Size))

        // Audio sample data (Interleaved Little Endian 16-bit PCM)
        for (i in 0 until numSamples) {
            val sL = (leftSamples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            val sR = (rightSamples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()

            dos.writeShort(java.lang.Short.reverseBytes(sL).toInt())
            dos.writeShort(java.lang.Short.reverseBytes(sR).toInt())
        }

        dos.flush()
    }
}
