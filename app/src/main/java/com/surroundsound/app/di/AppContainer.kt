package com.surroundsound.app.di

import android.content.Context
import com.surroundsound.app.core.audio.AudioDecoder
import com.surroundsound.app.core.audio.MicrophoneRecorder
import com.surroundsound.app.core.audio.PcmAudioPlayer
import com.surroundsound.app.data.repository.LocalProjectRepository
import com.surroundsound.app.data.repository.ProjectRepository
import com.surroundsound.app.data.storage.AudioStorageManager

/**
 * Service Locator / Dependency Container for SurroundSound.
 * Provides singleton dependencies for audio engines, repositories, and DSP processors.
 */
class AppContainer(private val context: Context) {
    val appContext: Context get() = context.applicationContext

    val audioStorageManager: AudioStorageManager by lazy {
        AudioStorageManager(appContext)
    }

    val projectRepository: ProjectRepository by lazy {
        LocalProjectRepository(appContext)
    }

    val microphoneRecorder: MicrophoneRecorder by lazy {
        MicrophoneRecorder(appContext)
    }

    val audioDecoder: AudioDecoder by lazy {
        AudioDecoder(appContext)
    }

    val pcmAudioPlayer: PcmAudioPlayer by lazy {
        PcmAudioPlayer()
    }
}
