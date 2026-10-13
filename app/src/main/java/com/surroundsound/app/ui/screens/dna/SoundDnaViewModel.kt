package com.surroundsound.app.ui.screens.dna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surroundsound.app.core.audio.PcmAudioPlayer
import com.surroundsound.app.core.audio.PlaybackState
import com.surroundsound.app.core.dsp.SoundDnaExtractor
import com.surroundsound.app.data.storage.AudioStorageManager
import com.surroundsound.app.domain.dna.SoundDna
import com.surroundsound.app.domain.dna.SourceRankingHeuristic
import com.surroundsound.app.domain.model.SourceAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SoundDnaUiState {
    object Loading : SoundDnaUiState()
    data class Success(
        val sourceAudio: SourceAudio,
        val soundDna: SoundDna,
        val calculationTimeMs: Long,
        val primaryScore: Float
    ) : SoundDnaUiState()
    data class Error(val message: String) : SoundDnaUiState()
}

class SoundDnaViewModel(
    private val audioStorageManager: AudioStorageManager,
    private val pcmAudioPlayer: PcmAudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<SoundDnaUiState>(SoundDnaUiState.Loading)
    val uiState: StateFlow<SoundDnaUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = pcmAudioPlayer.playbackState

    fun loadAndAnalyze(sourceAudio: SourceAudio) {
        viewModelScope.launch {
            _uiState.value = SoundDnaUiState.Loading
            val startTime = System.currentTimeMillis()

            val dna = withContext(Dispatchers.Default) {
                SoundDnaExtractor.extract(sourceAudio.samples, sourceAudio.sampleRate)
            }
            val calculationTimeMs = System.currentTimeMillis() - startTime
            val score = SourceRankingHeuristic.calculateScore(dna)

            _uiState.value = SoundDnaUiState.Success(
                sourceAudio = sourceAudio,
                soundDna = dna,
                calculationTimeMs = calculationTimeMs,
                primaryScore = score
            )
        }
    }

    fun loadFromStorage(sourceId: String) {
        viewModelScope.launch {
            _uiState.value = SoundDnaUiState.Loading
            val source = audioStorageManager.loadSourceAudio(sourceId, "User Sound")
            if (source == null) {
                _uiState.value = SoundDnaUiState.Error("Unable to locate audio file for analysis.")
                return@launch
            }
            loadAndAnalyze(source)
        }
    }

    fun togglePlayback(sourceAudio: SourceAudio) {
        when (playbackState.value) {
            is PlaybackState.Playing -> pcmAudioPlayer.pause()
            is PlaybackState.Paused -> pcmAudioPlayer.resume()
            else -> pcmAudioPlayer.play(sourceAudio)
        }
    }

    fun seekPlayback(progress: Float, sourceAudio: SourceAudio) {
        pcmAudioPlayer.seekTo(progress * sourceAudio.durationSeconds)
    }

    fun haltPlayback() {
        pcmAudioPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        pcmAudioPlayer.stop()
    }
}
