package com.surroundsound.app.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.surroundsound.app.core.AudioConstants
import com.surroundsound.app.domain.model.SourceAudio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class PlaybackState {
    object Idle : PlaybackState()
    data class Playing(
        val progressPercent: Float,
        val currentPositionSeconds: Float,
        val totalDurationSeconds: Float
    ) : PlaybackState()
    data class Paused(
        val progressPercent: Float,
        val currentPositionSeconds: Float,
        val totalDurationSeconds: Float
    ) : PlaybackState()
    object Stopped : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

/**
 * Low-latency PCM audio playback engine using AudioTrack.
 * Features:
 * - Direct FloatArray PCM streaming [-1.0f, 1.0f]
 * - AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
 * - State machine (Play, Pause, Resume, Stop, Seek)
 * - Automatic background/lock safety stop
 */
class PcmAudioPlayer {

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var currentSource: SourceAudio? = null
    private var currentSampleIndex = 0
    private var isLooping = false
    private var isPaused = false

    fun isPlaying(): Boolean = _playbackState.value is PlaybackState.Playing

    fun setLooping(enabled: Boolean) {
        isLooping = enabled
    }

    /**
     * Starts playback of the given source audio from startPositionSeconds.
     */
    fun play(source: SourceAudio, startPositionSeconds: Float = 0f) {
        stop()

        currentSource = source
        val totalSamples = source.samples.size
        val startSample = (startPositionSeconds * source.sampleRate).toInt().coerceIn(0, totalSamples)
        currentSampleIndex = startSample
        isPaused = false

        initAudioTrack(source.sampleRate)
        startPlaybackLoop()
    }

    fun pause() {
        if (_playbackState.value is PlaybackState.Playing) {
            isPaused = true
            try {
                audioTrack?.pause()
            } catch (_: Exception) {}

            val totalDuration = currentSource?.durationSeconds ?: 0f
            val currentPos = if (currentSource != null && currentSource!!.sampleRate > 0) {
                currentSampleIndex.toFloat() / currentSource!!.sampleRate.toFloat()
            } else 0f
            val progress = if (totalDuration > 0f) (currentPos / totalDuration).coerceIn(0f, 1f) else 0f

            _playbackState.value = PlaybackState.Paused(
                progressPercent = progress,
                currentPositionSeconds = currentPos,
                totalDurationSeconds = totalDuration
            )
        }
    }

    fun resume() {
        val source = currentSource ?: return
        if (isPaused) {
            isPaused = false
            try {
                audioTrack?.play()
            } catch (_: Exception) {}
            startPlaybackLoop()
        } else {
            play(source, 0f)
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        isPaused = false
        currentSampleIndex = 0

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        _playbackState.value = PlaybackState.Stopped
    }

    fun seekTo(positionSeconds: Float) {
        val source = currentSource ?: return
        val targetSample = (positionSeconds * source.sampleRate).toInt().coerceIn(0, source.samples.size)
        currentSampleIndex = targetSample

        val totalDuration = source.durationSeconds
        val currentPos = currentSampleIndex.toFloat() / source.sampleRate.toFloat()
        val progress = if (totalDuration > 0f) (currentPos / totalDuration).coerceIn(0f, 1f) else 0f

        if (isPaused) {
            _playbackState.value = PlaybackState.Paused(
                progressPercent = progress,
                currentPositionSeconds = currentPos,
                totalDurationSeconds = totalDuration
            )
        } else if (_playbackState.value is PlaybackState.Playing) {
            _playbackState.value = PlaybackState.Playing(
                progressPercent = progress,
                currentPositionSeconds = currentPos,
                totalDurationSeconds = totalDuration
            )
        }
    }

    private fun initAudioTrack(sampleRate: Int) {
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_FLOAT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .setEncoding(encoding)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        audioTrack?.play()
    }

    private fun startPlaybackLoop() {
        val source = currentSource ?: return
        val samples = source.samples
        val totalSamples = samples.size
        val sampleRate = source.sampleRate
        val chunkSize = 1024

        playbackJob = scope.launch(Dispatchers.Default) {
            val chunk = FloatArray(chunkSize)

            try {
                while (isActive && !isPaused && currentSampleIndex < totalSamples) {
                    val remaining = totalSamples - currentSampleIndex
                    val samplesToWrite = chunkSize.coerceAtMost(remaining)

                    System.arraycopy(samples, currentSampleIndex, chunk, 0, samplesToWrite)
                    if (samplesToWrite < chunkSize) {
                        chunk.fill(0f, samplesToWrite, chunkSize)
                    }

                    val written = audioTrack?.write(
                        chunk,
                        0,
                        samplesToWrite,
                        AudioTrack.WRITE_BLOCKING
                    ) ?: -1

                    if (written > 0) {
                        currentSampleIndex += written

                        val totalDuration = source.durationSeconds
                        val currentPos = currentSampleIndex.toFloat() / sampleRate.toFloat()
                        val progress = if (totalDuration > 0f) (currentPos / totalDuration).coerceIn(0f, 1f) else 0f

                        _playbackState.value = PlaybackState.Playing(
                            progressPercent = progress,
                            currentPositionSeconds = currentPos,
                            totalDurationSeconds = totalDuration
                        )
                    }

                    if (currentSampleIndex >= totalSamples) {
                        if (isLooping) {
                            currentSampleIndex = 0
                        } else {
                            break
                        }
                    }
                }

                if (!isLooping && currentSampleIndex >= totalSamples) {
                    withContext(Dispatchers.Main) {
                        stop()
                    }
                }
            } catch (e: CancellationException) {
                // Playback cancelled cleanly
            } catch (e: Exception) {
                _playbackState.value = PlaybackState.Error(e.localizedMessage ?: "Playback error")
            }
        }
    }
}
