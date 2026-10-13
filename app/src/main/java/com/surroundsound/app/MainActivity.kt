package com.surroundsound.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.surroundsound.app.ui.screens.dna.SoundDnaScreen
import com.surroundsound.app.ui.screens.dna.SoundDnaViewModel
import com.surroundsound.app.ui.screens.recorder.RecorderScreen
import com.surroundsound.app.ui.screens.recorder.RecorderUiState
import com.surroundsound.app.ui.screens.recorder.RecorderViewModel
import com.surroundsound.app.ui.theme.SurroundSoundTheme

class MainActivity : ComponentActivity() {

    private val recorderViewModel: RecorderViewModel by viewModels {
        val app = application as SurroundSoundApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecorderViewModel(
                    microphoneRecorder = app.container.microphoneRecorder,
                    audioDecoder = app.container.audioDecoder,
                    audioStorageManager = app.container.audioStorageManager,
                    pcmAudioPlayer = app.container.pcmAudioPlayer
                ) as T
            }
        }
    }

    private val soundDnaViewModel: SoundDnaViewModel by viewModels {
        val app = application as SurroundSoundApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SoundDnaViewModel(
                    audioStorageManager = app.container.audioStorageManager,
                    pcmAudioPlayer = app.container.pcmAudioPlayer
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SurroundSoundTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        recorderViewModel = recorderViewModel,
                        soundDnaViewModel = soundDnaViewModel
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (application as? SurroundSoundApplication)?.container?.pcmAudioPlayer?.stop()
    }
}

@Composable
fun AppNavigation(
    recorderViewModel: RecorderViewModel,
    soundDnaViewModel: SoundDnaViewModel
) {
    var currentScreen by rememberSaveable { mutableStateOf("recorder") }

    when (currentScreen) {
        "recorder" -> {
            RecorderScreen(
                viewModel = recorderViewModel,
                onNavigateToSoundDna = { sourceId ->
                    // Pass the captured audio directly to the DNA analysis screen
                    val uiState = recorderViewModel.uiState.value
                    if (uiState is RecorderUiState.AudioReady) {
                        soundDnaViewModel.loadAndAnalyze(uiState.sourceAudio)
                    }
                    currentScreen = "sound_dna"
                }
            )
        }

        "sound_dna" -> {
            SoundDnaScreen(
                viewModel = soundDnaViewModel,
                onNavigateBack = {
                    currentScreen = "recorder"
                },
                onNavigateToModes = { sourceId ->
                    // Will navigate to Mode Selection in Phase 4
                }
            )
        }
    }
}
