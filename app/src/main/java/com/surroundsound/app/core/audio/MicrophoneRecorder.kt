package com.surroundsound.app.core.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.surroundsound.app.core.AudioConstants
import com.surroundsound.app.core.pcm.PcmAudioBuffer
import com.surroundsound.app.domain.model.SourceAudio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(
        val elapsedSeconds: Float,
        val currentRms: Float,
        val isWarning: Boolean // true when elapsedSeconds >= 8.0s
    ) : RecordingState()
    data class Completed(val sourceAudio: SourceAudio) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

/**
 * Microphone capture pipeline using AudioRecord.
 * Records 16-bit PCM at 44.1 kHz mono, strictly enforcing the 10.0-second maximum duration.
 */
class MicrophoneRecorder(private val context: Context) {

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startRecording(sourceName: String = "Mic Recording") {
        if (!hasRecordPermission()) {
            _recordingState.value = RecordingState.Error("Microphone permission (RECORD_AUDIO) not granted.")
            return
        }

        if (_recordingState.value is RecordingState.Recording) {
            return
        }

        val sampleRate = AudioConstants.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            _recordingState.value = RecordingState.Error("Unsupported audio hardware recording parameters.")
            return
        }

        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                _recordingState.value = RecordingState.Error("AudioRecord initialization failed.")
                return
            }

            audioRecord?.startRecording()
        } catch (e: SecurityException) {
            _recordingState.value = RecordingState.Error("Microphone permission denied: ${e.message}")
            return
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error("Failed to start AudioRecord: ${e.message}")
            return
        }

        recordingJob = scope.launch(Dispatchers.IO) {
            val outputStream = ByteArrayOutputStream()
            val readBuffer = ByteArray(bufferSize)
            val startTime = System.currentTimeMillis()
            val maxDurationMs = (AudioConstants.MAX_SOURCE_DURATION_SECONDS * 1000).toLong() // 10,000 ms
            val warningThresholdMs = (AudioConstants.WARNING_THRESHOLD_SECONDS * 1000).toLong() // 8,000 ms
            val maxBytes = AudioConstants.MAX_SOURCE_SAMPLES_MONO * AudioConstants.BYTES_PER_SAMPLE // 882,000 bytes

            try {
                while (isActive && outputStream.size() < maxBytes) {
                    val bytesToRead = (maxBytes - outputStream.size()).coerceAtMost(readBuffer.size)
                    val readResult = audioRecord?.read(readBuffer, 0, bytesToRead) ?: -1

                    if (readResult > 0) {
                        outputStream.write(readBuffer, 0, readResult)

                        // Calculate instantaneous RMS for UI visualization
                        var sumSquares = 0.0
                        val sampleCount = readResult / 2
                        for (i in 0 until sampleCount) {
                            val low = readBuffer[i * 2].toInt() and 0xFF
                            val high = readBuffer[i * 2 + 1].toInt()
                            val sample = ((high shl 8) or low).toFloat() / 32768.0f
                            sumSquares += (sample * sample)
                        }
                        val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f

                        val elapsedMs = System.currentTimeMillis() - startTime
                        val elapsedSeconds = (elapsedMs.toFloat() / 1000f).coerceAtMost(AudioConstants.MAX_SOURCE_DURATION_SECONDS.toFloat())
                        val isWarning = elapsedMs >= warningThresholdMs

                        _recordingState.value = RecordingState.Recording(
                            elapsedSeconds = elapsedSeconds,
                            currentRms = rms.coerceIn(0f, 1f),
                            isWarning = isWarning
                        )

                        // Strict 10.0-second auto-stop enforcement
                        if (elapsedMs >= maxDurationMs || outputStream.size() >= maxBytes) {
                            break
                        }
                    } else if (readResult < 0) {
                        // Error reading from microphone
                        break
                    }
                }
            } catch (e: CancellationException) {
                // Recording stopped prematurely
            } finally {
                stopAndFinalize(outputStream, sourceName)
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
    }

    private fun stopAndFinalize(outputStream: ByteArrayOutputStream, sourceName: String) {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        val pcmBytes = outputStream.toByteArray()
        if (pcmBytes.isEmpty()) {
            _recordingState.value = RecordingState.Idle
            return
        }

        // Convert to normalized floats and clamp strictly to 10.0s (441,000 samples)
        val floats = PcmAudioBuffer.bytesToNormalizedFloats(pcmBytes)
        val cappedFloats = if (floats.size > AudioConstants.MAX_SOURCE_SAMPLES_MONO) {
            floats.copyOf(AudioConstants.MAX_SOURCE_SAMPLES_MONO)
        } else {
            floats
        }

        val sourceAudio = SourceAudio(
            name = sourceName,
            sampleRate = AudioConstants.SAMPLE_RATE,
            channelCount = 1,
            samples = cappedFloats
        )

        _recordingState.value = RecordingState.Completed(sourceAudio)
    }

    fun reset() {
        stopRecording()
        _recordingState.value = RecordingState.Idle
    }
}
