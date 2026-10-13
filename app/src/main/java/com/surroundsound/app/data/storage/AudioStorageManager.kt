package com.surroundsound.app.data.storage

import android.content.Context
import com.surroundsound.app.core.AudioConstants
import com.surroundsound.app.core.pcm.PcmAudioBuffer
import com.surroundsound.app.domain.model.SourceAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages local, offline file persistence in the app's internal sandbox.
 * Strictly adheres to 0 external network dependencies and 0 storage permissions.
 */
class AudioStorageManager(private val context: Context) {

    private val sourcesDir: File
        get() = File(context.filesDir, "sources").apply { if (!exists()) mkdirs() }

    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    /**
     * Persists source audio samples as raw 16-bit PCM file in internal sandbox.
     */
    suspend fun saveSourceAudio(source: SourceAudio): File = withContext(Dispatchers.IO) {
        val file = File(sourcesDir, "${source.id}.pcm")
        val bytes = PcmAudioBuffer.normalizedFloatsToBytes(source.samples)
        FileOutputStream(file).use { it.write(bytes) }
        file
    }

    /**
     * Loads raw 16-bit PCM file from internal sandbox into normalized FloatArray.
     */
    suspend fun loadSourceAudio(id: String, name: String, sampleRate: Int = AudioConstants.SAMPLE_RATE): SourceAudio? = withContext(Dispatchers.IO) {
        val file = File(sourcesDir, "$id.pcm")
        if (!file.exists()) return@withContext null

        try {
            val bytes = FileInputStream(file).use { it.readBytes() }
            val floats = PcmAudioBuffer.bytesToNormalizedFloats(bytes)
            SourceAudio(
                id = id,
                name = name,
                sampleRate = sampleRate,
                channelCount = 1,
                samples = floats
            )
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Deletes source audio cache file.
     */
    suspend fun deleteSourceAudio(id: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(sourcesDir, "$id.pcm")
        if (file.exists()) file.delete() else true
    }

    /**
     * Returns project storage directory.
     */
    fun getProjectDirectory(projectId: String): File {
        return File(projectsDir, projectId).apply { if (!exists()) mkdirs() }
    }
}
