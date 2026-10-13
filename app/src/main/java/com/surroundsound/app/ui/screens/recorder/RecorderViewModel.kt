package com.surroundsound.app.ui.screens.recorder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surroundsound.app.core.AudioConstants
import com.surroundsound.app.core.audio.AudioDecoder
import com.surroundsound.app.core.audio.AudioTrimmer
import com.surroundsound.app.core.audio.MicrophoneRecorder
import com.surroundsound.app.core.audio.PcmAudioPlayer
import com.surroundsound.app.core.audio.PlaybackState
import com.surroundsound.app.core.audio.RecordingState
import com.surroundsound.app.data.storage.AudioStorageManager
import com.surroundsound.app.domain.model.SourceAudio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecorderUiState {
    object Idle : RecorderUiState()
    data class Recording(
        val elapsedSeconds: Float,
        val currentRms: Float,
        val isWarning: Boolean
    ) : RecorderUiState()
    data class Decoding(val fileName: String) : RecorderUiState()
    data class TrimmingRequired(
        val originalSource: SourceAudio,
        val fullSamples: FloatArray,
        val totalDurationSeconds: Float,
        val selectedStartSeconds: Float = 0f
    ) : RecorderUiState()
    data class AudioReady(
        val sourceAudio: SourceAudio,
        val savedFilePath: String? = null
    ) : RecorderUiState()
    data class Error(val message: String) : RecorderUiState()
}

class RecorderViewModel(
    private val microphoneRecorder: MicrophoneRecorder,
    private val audioDecoder: AudioDecoder,
    private val audioStorageManager: AudioStorageManager,
    private val pcmAudioPlayer: PcmAudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecorderUiState>(RecorderUiState.Idle)
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = pcmAudioPlayer.playbackState

    init {
        viewModelScope.launch {
            microphoneRecorder.recordingState.collect { recState ->
                when (recState) {
                    is RecordingState.Idle -> {
                        if (_uiState.value is RecorderUiState.Recording) {
                            _uiState.value = RecorderUiState.Idle
                        }
                    }
                    is RecordingState.Recording -> {
                        _uiState.value = RecorderUiState.Recording(
                            elapsedSeconds = recState.elapsedSeconds,
                            currentRms = recState.currentRms,
                            isWarning = recState.isWarning
                        )
                    }
                    is RecordingState.Completed -> {
                        onAudioCaptured(recState.sourceAudio)
                    }
                    is RecordingState.Error -> {
                        _uiState.value = RecorderUiState.Error(recState.message)
                    }
                }
            }
        }
    }

    fun startRecording(sourceName: String = "Mic Recording") {
        pcmAudioPlayer.stop()
        microphoneRecorder.startRecording(sourceName)
    }

    fun stopRecording() {
        microphoneRecorder.stopRecording()
    }

    fun importAudioFile(uri: Uri, displayName: String) {
        pcmAudioPlayer.stop()
        viewModelScope.launch {
            _uiState.value = RecorderUiState.Decoding(displayName)
            val result = audioDecoder.decodeFromUri(uri, displayName)
            result.onSuccess { decoded ->
                if (decoded.needsTrimming) {
                    _uiState.value = RecorderUiState.TrimmingRequired(
                        originalSource = decoded.sourceAudio,
                        fullSamples = decoded.sourceAudio.samples,
                        totalDurationSeconds = decoded.originalDurationSeconds,
                        selectedStartSeconds = 0f
                    )
                } else {
                    onAudioCaptured(decoded.sourceAudio)
                }
            }.onFailure { error ->
                _uiState.value = RecorderUiState.Error(error.localizedMessage ?: "Failed to decode audio file.")
            }
        }
    }

    fun applyTrim(startSeconds: Float) {
        val current = _uiState.value
        if (current is RecorderUiState.TrimmingRequired) {
            val trimmedSource = AudioTrimmer.trimSource(
                source = current.originalSource,
                fullSamples = current.fullSamples,
                startSeconds = startSeconds,
                windowSeconds = AudioConstants.MAX_SOURCE_DURATION_SECONDS.toFloat()
            )
            onAudioCaptured(trimmedSource)
        }
    }

    private fun onAudioCaptured(sourceAudio: SourceAudio) {
        viewModelScope.launch {
            val file = audioStorageManager.saveSourceAudio(sourceAudio)
            _uiState.value = RecorderUiState.AudioReady(
                sourceAudio = sourceAudio,
                savedFilePath = file.absolutePath
            )
        }
    }

    fun togglePlayback(sourceAudio: SourceAudio) {
        when (playbackState.value) {
            is PlaybackState.Playing -> {
                pcmAudioPlayer.pause()
            }
            is PlaybackState.Paused -> {
                pcmAudioPlayer.resume()
            }
            else -> {
                pcmAudioPlayer.play(sourceAudio)
            }
        }
    }

    fun seekPlayback(progressPercent: Float, sourceAudio: SourceAudio) {
        val targetSeconds = progressPercent * sourceAudio.durationSeconds
        pcmAudioPlayer.seekTo(targetSeconds)
    }

    fun haltPlayback() {
        pcmAudioPlayer.stop()
    }

    fun reset() {
        pcmAudioPlayer.stop()
        microphoneRecorder.reset()
        _uiState.value = RecorderUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        pcmAudioPlayer.stop()
    }
}
