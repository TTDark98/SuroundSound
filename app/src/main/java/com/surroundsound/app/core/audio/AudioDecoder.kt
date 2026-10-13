package com.surroundsound.app.core.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.surroundsound.app.core.AudioConstants
import com.surroundsound.app.core.pcm.PcmAudioBuffer
import com.surroundsound.app.domain.model.SourceAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DecodedAudioResult(
    val sourceAudio: SourceAudio,
    val originalDurationSeconds: Float,
    val needsTrimming: Boolean // true if originalDurationSeconds > 10.0s
)

/**
 * Offline Audio Decoder utilizing Android's MediaExtractor + MediaCodec.
 * Decodes MP3, AAC, M4A, WAV, FLAC, and OGG into normalized 44.1 kHz 32-bit Float PCM.
 */
class AudioDecoder(private val context: Context) {

    suspend fun decodeFromUri(uri: Uri, sourceName: String): Result<DecodedAudioResult> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Unable to open audio file: ${e.localizedMessage}"))
        }

        if (pfd == null) {
            return@withContext Result.failure(Exception("File descriptor is null"))
        }

        try {
            extractor.setDataSource(pfd.fileDescriptor)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            var mime: String? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (trackMime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    mime = trackMime
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null || mime == null) {
                return@withContext Result.failure(Exception("No valid audio track found in selected file."))
            }

            extractor.selectTrack(audioTrackIndex)

            val originalSampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                AudioConstants.SAMPLE_RATE
            }

            val originalChannelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                1
            }

            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            val originalDurationSeconds = durationUs.toFloat() / 1_000_000f

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val pcmOutputStream = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            val timeoutUs = 5000L

            var outputSampleRate = originalSampleRate
            var outputChannelCount = originalChannelCount

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                sawInputEOS = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    sampleSize,
                                    presentationTimeUs,
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outputBufferIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }

                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        pcmOutputStream.write(chunk)
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = codec.outputFormat
                    if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        outputSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        outputChannelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }

            val rawPcmBytes = pcmOutputStream.toByteArray()
            if (rawPcmBytes.isEmpty()) {
                return@withContext Result.failure(Exception("Decoded audio stream contains zero audio bytes."))
            }

            // Convert decoded bytes to floats
            val rawFloats = PcmAudioBuffer.bytesToNormalizedFloats(rawPcmBytes)

            // Convert to mono if multi-channel
            val monoFloats = if (outputChannelCount > 1) {
                mixToMono(rawFloats, outputChannelCount)
            } else {
                rawFloats
            }

            // Resample to 44.1 kHz if necessary
            val resampledFloats = if (outputSampleRate != AudioConstants.SAMPLE_RATE) {
                resampleLinear(monoFloats, outputSampleRate, AudioConstants.SAMPLE_RATE)
            } else {
                monoFloats
            }

            val finalDuration = resampledFloats.size.toFloat() / AudioConstants.SAMPLE_RATE.toFloat()
            val needsTrimming = finalDuration > AudioConstants.MAX_SOURCE_DURATION_SECONDS.toFloat()

            // If within 10s, use directly; if >10s, initial slice defaults to first 10s pending range trimming
            val initialSamples = if (needsTrimming) {
                resampledFloats.copyOf(AudioConstants.MAX_SOURCE_SAMPLES_MONO)
            } else {
                resampledFloats
            }

            val sourceAudio = SourceAudio(
                name = sourceName,
                sampleRate = AudioConstants.SAMPLE_RATE,
                channelCount = 1,
                samples = initialSamples
            )

            Result.success(
                DecodedAudioResult(
                    sourceAudio = sourceAudio,
                    originalDurationSeconds = finalDuration,
                    needsTrimming = needsTrimming
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
            try {
                pfd.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Mixes interleaved multi-channel float samples to mono.
     */
    private fun mixToMono(interleaved: FloatArray, channels: Int): FloatArray {
        val frameCount = interleaved.size / channels
        val mono = FloatArray(frameCount)
        val channelFactor = 1.0f / channels.toFloat()

        for (i in 0 until frameCount) {
            var sum = 0.0f
            val baseIndex = i * channels
            for (ch in 0 until channels) {
                sum += interleaved[baseIndex + ch]
            }
            mono[i] = sum * channelFactor
        }
        return mono
    }

    /**
     * Linear resampling algorithm converting from inputSampleRate to targetSampleRate.
     */
    fun resampleLinear(input: FloatArray, inputRate: Int, targetRate: Int): FloatArray {
        if (inputRate == targetRate || input.isEmpty()) return input

        val ratio = inputRate.toDouble() / targetRate.toDouble()
        val targetLength = (input.size / ratio).toInt()
        val output = FloatArray(targetLength)

        for (i in 0 until targetLength) {
            val srcIndex = i * ratio
            val indexFloor = srcIndex.toInt()
            val fraction = (srcIndex - indexFloor).toFloat()

            if (indexFloor + 1 < input.size) {
                output[i] = input[indexFloor] * (1.0f - fraction) + input[indexFloor + 1] * fraction
            } else if (indexFloor < input.size) {
                output[i] = input[indexFloor]
            }
        }
        return output
    }
}
