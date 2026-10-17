package com.example.surroundsound.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.example.surroundsound.model.BeatStyle
import com.example.surroundsound.model.FxConfig
import com.example.surroundsound.model.PatternState
import com.example.surroundsound.model.Preset
import com.example.surroundsound.model.Presets
import com.example.surroundsound.model.SampleMetadata
import com.example.surroundsound.model.Scales
import com.example.surroundsound.model.TrackId
import java.io.ByteArrayOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class AudioEngine {
    companion object {
        const val SAMPLE_RATE = 44100

        /** Hard cap on a single take so the in-memory buffer can't grow without bound. */
        const val MAX_RECORD_SECONDS = 12
        private const val MAX_RECORD_SAMPLES = SAMPLE_RATE * MAX_RECORD_SECONDS

        /** Below this peak there is no impact to slice, so the take is rejected. */
        private const val MIN_TAKE_PEAK = 0.02f
    }

    var rawBuffer: FloatArray? = null
        private set
    var sampleBuffer: FloatArray? = null
        private set
    var sampleMetadata: SampleMetadata? = null
        private set

    var trimStart = 0f
    var trimEnd = 0.45f

    var isPlaying = false
        private set
    var currentStep = -1
        private set

    var activePattern: PatternState = Presets.LOFI.pattern.copyDeep()
    var activeFx: FxConfig = Presets.LOFI.fxDefaults
    var activeBpm: Int = Presets.LOFI.bpm

    var onStepChange: ((Int) -> Unit)? = null
    var onSampleLoaded: ((SampleMetadata) -> Unit)? = null

    // Audio Output Stream
    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    @Volatile private var isEngineRunning = false

    // Active voices list
    private val activeVoices = CopyOnWriteArrayList<Voice>()

    // Effects
    private val masterFilter = BiquadFilter(FilterType.LOWPASS, 18000f, 0.7f, SAMPLE_RATE.toFloat())
    private val delayEffect = DelayEffect(SAMPLE_RATE)
    private val reverbEffect = ReverbEffect(SAMPLE_RATE)

    // Dry playback source
    private var rawPlaybackPos: Int = -1
    private var trimmedPlaybackPos: Int = -1

    /** Live audition state, polled by the sample station so its buttons can show Stop. */
    val isPlayingRaw: Boolean get() = rawPlaybackPos >= 0
    val isPlayingTrimmed: Boolean get() = trimmedPlaybackPos >= 0

    // Microphone Recording
    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private val recordedShorts = mutableListOf<Short>()
    @Volatile var isRecording = false
        private set
    @Volatile var recordingRms = 0f
        private set

    /** True once a take has hit [MAX_RECORD_SECONDS]; the UI stops recording when it sees this. */
    @Volatile var recordingLimitReached = false
        private set

    /** Why the last take was rejected, if it was. */
    @Volatile var lastRecordingIssue: String? = null
        private set

    @Volatile private var sequencerRestartRequested = false
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Stop eases the master out over ~120ms so the reverb/delay tail doesn't ring on. */
    @Volatile private var stopFadeRequested = false
    private var masterFade = 1f

    // Track Mute & Solo
    val mutedTracks = mutableSetOf<TrackId>()
    val soloTracks = mutableSetOf<TrackId>()

    fun toggleMute(trackId: TrackId) {
        if (mutedTracks.contains(trackId)) {
            mutedTracks.remove(trackId)
        } else {
            mutedTracks.add(trackId)
        }
    }

    fun toggleSolo(trackId: TrackId) {
        if (soloTracks.contains(trackId)) {
            soloTracks.remove(trackId)
        } else {
            soloTracks.add(trackId)
        }
    }

    fun isTrackAudible(trackId: TrackId): Boolean {
        if (soloTracks.isNotEmpty()) {
            return soloTracks.contains(trackId)
        }
        return !mutedTracks.contains(trackId)
    }

    init {
        // Initialize with default table pound
        generateSyntheticTableBang()
        startAudioTrack()
    }

    fun startAudioTrack() {
        if (isEngineRunning) return
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        ) * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        isEngineRunning = true

        audioThread = Thread {
            renderLoop()
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stopAudioEngine() {
        if (isRecording) stopMicrophoneRecording()
        isEngineRunning = false
        try {
            audioThread?.join(500)
        } catch (_: Exception) {}
        audioThread = null

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    // --- SYNTHETIC PRESETS GENERATION ---

    fun generateSyntheticTableBang() {
        val data = SyntheticAudioGenerator.generateTableBang()
        loadAudioBuffer(data, "preset-table", "Hardwood Desk Pound", 0.035f, 0.035f + 0.45f)
    }

    fun generateSyntheticMetalHit() {
        val data = SyntheticAudioGenerator.generateMetalHit()
        loadAudioBuffer(data, "preset-metal", "Steel Spoon Clink", 0.04f, 0.04f + 0.48f)
    }

    fun generateSyntheticKnuckleTap() {
        val data = SyntheticAudioGenerator.generateKnuckleTap()
        loadAudioBuffer(data, "preset-tap", "Finger Snap & Knuckle Tap", 0.03f, 0.03f + 0.38f)
    }

    fun generateSyntheticGlassPing() {
        val data = SyntheticAudioGenerator.generateGlassPing()
        loadAudioBuffer(data, "preset-glass", "Resonant Glass Bottle", 0.04f, 0.04f + 0.52f)
    }

    // --- BUFFER PROCESSING & TRIMMING ---

    fun loadAudioBuffer(
        buffer: FloatArray,
        sourceType: String,
        sourceName: String,
        initialTrimStart: Float = 0f,
        initialTrimEnd: Float? = null
    ) {
        this.rawBuffer = buffer
        val dur = buffer.size.toFloat() / SAMPLE_RATE
        this.trimStart = initialTrimStart.coerceIn(0f, dur - 0.02f)
        this.trimEnd = (initialTrimEnd ?: min(dur, 0.45f)).coerceIn(trimStart + 0.02f, dur)

        applyTrimInternal(trimStart, trimEnd)

        sampleBuffer?.let { trimmed ->
            var peak = 0f
            for (v in trimmed) {
                val a = abs(v)
                if (a > peak) peak = a
            }

            val meta = SampleMetadata(
                duration = trimmed.size.toFloat() / SAMPLE_RATE,
                rawDuration = dur,
                sampleRate = SAMPLE_RATE,
                channels = 1,
                peakValue = peak,
                transientOffset = trimStart,
                trimStart = trimStart,
                trimEnd = trimEnd,
                sourceType = sourceType,
                sourceName = sourceName
            )
            this.sampleMetadata = meta
            onSampleLoaded?.invoke(meta)
        }
    }

    private fun applyTrimInternal(startSec: Float, endSec: Float) {
        val raw = rawBuffer ?: return
        val rawDur = raw.size.toFloat() / SAMPLE_RATE

        val clampedStart = startSec.coerceIn(0f, rawDur - 0.02f)
        val clampedEnd = endSec.coerceIn(clampedStart + 0.02f, rawDur)

        val startIdx = (clampedStart * SAMPLE_RATE).toInt()
        val endIdx = (clampedEnd * SAMPLE_RATE).toInt()
        val sliceLength = max(1, endIdx - startIdx)

        val trimmed = FloatArray(sliceLength)
        val fadeSamples = min((SAMPLE_RATE * 0.0025f).toInt(), sliceLength / 4)
        var maxPeak = 0f

        for (i in 0 until sliceLength) {
            val srcIdx = startIdx + i
            var sample = if (srcIdx < raw.size) raw[srcIdx] else 0f

            if (i < fadeSamples) {
                sample *= (i.toFloat() / fadeSamples)
            } else if (i > sliceLength - fadeSamples) {
                sample *= ((sliceLength - i).toFloat() / fadeSamples)
            }

            trimmed[i] = sample
            val absVal = abs(sample)
            if (absVal > maxPeak) maxPeak = absVal
        }

        // Normalize to -0.8dB (0.91)
        if (maxPeak > 0.001f) {
            val gain = 0.91f / maxPeak
            for (i in 0 until sliceLength) {
                trimmed[i] *= gain
            }
        }

        this.sampleBuffer = trimmed
        this.trimStart = clampedStart
        this.trimEnd = clampedEnd
    }

    fun setTrim(startSec: Float, endSec: Float) {
        applyTrimInternal(startSec, endSec)
        sampleBuffer?.let { trimmed ->
            sampleMetadata = sampleMetadata?.copy(
                duration = trimmed.size.toFloat() / SAMPLE_RATE,
                trimStart = trimStart,
                trimEnd = trimEnd,
                transientOffset = trimStart
            )
            sampleMetadata?.let { onSampleLoaded?.invoke(it) }
        }
    }

    fun autoDetectTransient() {
        val raw = rawBuffer ?: return
        var peakIdx = 0
        var peakVal = 0f
        for (i in raw.indices) {
            val a = abs(raw[i])
            if (a > peakVal) {
                peakVal = a
                peakIdx = i
            }
        }

        var onsetIdx = peakIdx
        val threshold = max(0.04f, peakVal * 0.15f)
        val searchStart = max(0, peakIdx - (SAMPLE_RATE * 0.05f).toInt())
        for (i in searchStart until peakIdx) {
            if (abs(raw[i]) >= threshold) {
                onsetIdx = i
                break
            }
        }

        val preRoll = (SAMPLE_RATE * 0.005f).toInt()
        val startIdx = max(0, onsetIdx - preRoll)
        val maxSlice = (SAMPLE_RATE * 0.45f).toInt()
        val sliceLength = min(raw.size - startIdx, maxSlice)

        val startSec = startIdx.toFloat() / SAMPLE_RATE
        val endSec = (startIdx + sliceLength).toFloat() / SAMPLE_RATE

        setTrim(startSec, endSec)
    }

    fun playRawAudio() {
        stopFadeRequested = false
        rawPlaybackPos = 0
    }

    fun stopRawAudio() {
        rawPlaybackPos = -1
    }

    fun playTrimmedSample() {
        stopFadeRequested = false
        trimmedPlaybackPos = 0
    }

    fun stopTrimmedSample() {
        trimmedPlaybackPos = -1
    }

    // --- MICROPHONE RECORDING ---

    @SuppressLint("MissingPermission")
    fun startMicrophoneRecording(): Boolean {
        if (isRecording) return false
        try {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = max(minBuf, SAMPLE_RATE * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            synchronized(recordedShorts) {
                recordedShorts.clear()
            }
            recordingLimitReached = false
            lastRecordingIssue = null

            audioRecord?.startRecording()
            isRecording = true

            val readBuf = ShortArray(1024)

            recordThread = Thread {
                while (isRecording && audioRecord != null) {
                    val read = audioRecord?.read(readBuf, 0, readBuf.size) ?: 0
                    if (read > 0) {
                        var sumSq = 0f
                        val atLimit: Boolean
                        synchronized(recordedShorts) {
                            for (i in 0 until read) {
                                val s = readBuf[i]
                                recordedShorts.add(s)
                                val norm = s / 32768f
                                sumSq += norm * norm
                            }
                            atLimit = recordedShorts.size >= MAX_RECORD_SAMPLES
                        }
                        recordingRms = kotlin.math.sqrt(sumSq / read)
                        if (atLimit) {
                            recordingLimitReached = true
                            break
                        }
                    }
                }
            }.apply { start() }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            audioRecord?.release()
            audioRecord = null
            return false
        }
    }

    fun stopMicrophoneRecording(): Boolean {
        if (!isRecording) return false
        isRecording = false
        try {
            audioRecord?.stop()
            recordThread?.join(500)
        } catch (_: Exception) {}
        audioRecord?.release()
        audioRecord = null
        recordThread = null
        recordingRms = 0f

        val captured = synchronized(recordedShorts) {
            val arr = ShortArray(recordedShorts.size)
            for (i in recordedShorts.indices) {
                arr[i] = recordedShorts[i]
            }
            recordedShorts.clear()
            arr
        }

        if (captured.size <= 200) {
            lastRecordingIssue = "That take was too short to slice — hit REC and bang the desk once."
            return false
        }

        val floats = FloatArray(captured.size)
        var peak = 0f
        for (i in captured.indices) {
            val norm = captured[i] / 32768f
            floats[i] = norm
            val magnitude = kotlin.math.abs(norm)
            if (magnitude > peak) peak = magnitude
        }

        if (peak < MIN_TAKE_PEAK) {
            lastRecordingIssue = "Nothing loud enough was captured — move closer to the desk and try again."
            return false
        }

        // Normalise, so a quiet phone mic still drives the drum voices at a usable level.
        val gain = (0.95f / peak).coerceAtMost(8f)
        if (gain > 1f) {
            for (i in floats.indices) {
                floats[i] = (floats[i] * gain).coerceIn(-1f, 1f)
            }
        }

        loadAudioBuffer(floats, "microphone", "Live Microphone Recording")
        autoDetectTransient()
        return true
    }

    fun processRecordedAudio(pcmFloats: FloatArray, sourceName: String = "Live Microphone Bang") {
        this.rawBuffer = pcmFloats
        autoDetectTransient()
        sampleBuffer?.let {
            loadAudioBuffer(
                pcmFloats,
                "microphone",
                sourceName,
                trimStart,
                trimEnd
            )
        }
    }

    // --- SEQUENCER & VOICE TRIGGERS ---

    fun startSequencer(preset: Preset? = null, fx: FxConfig? = null, pattern: PatternState? = null) {
        fx?.let { activeFx = it }
        pattern?.let { activePattern = it.copyDeep() }
        stopFadeRequested = false
        isPlaying = true
        currentStep = 0
        sequencerRestartRequested = true
    }

    fun stopSequencer() {
        isPlaying = false
        stopFadeRequested = true
        currentStep = -1
        mainHandler.post { onStepChange?.invoke(-1) }
    }

    fun updatePattern(newPattern: PatternState) {
        activePattern = newPattern.copyDeep()
    }

    fun updateBpm(newBpm: Int) {
        activeBpm = newBpm
    }

    fun updateFx(newFx: FxConfig) {
        activeFx = newFx
    }

    fun triggerVoice(trackId: TrackId, semitone: Int = 0, velocity: Float = 1.0f) {
        when (trackId) {
            TrackId.KICK -> triggerKick(velocity)
            TrackId.SNARE -> triggerSnare(velocity)
            TrackId.HIHAT -> triggerHiHat(velocity)
            TrackId.PERC -> triggerPerc(velocity)
            TrackId.MELODY -> triggerMelody(semitone, velocity)
            TrackId.BASS -> triggerBass(semitone, velocity)
        }
    }

    fun triggerKick(velocity: Float = 1.0f) {
        val buf = sampleBuffer ?: return
        // 1. Pitched down sample with Lowpass filter
        val filter = BiquadFilter(FilterType.LOWPASS, 160f, 1.8f, SAMPLE_RATE.toFloat())
        val v = Voice(
            buffer = buf,
            playbackRate = 0.42f,
            filter = filter,
            decayRate = 9.0f,
            maxDuration = 0.35f,
            gain = 1.0f * velocity,
            reverbSend = 0f,
            delaySend = 0f,
            subOscFreqStart = 110f,
            subOscFreqEnd = 42f,
            subOscDuration = 0.12f,
            subOscGain = 0.65f * velocity
        )
        activeVoices.add(v)
    }

    fun triggerSnare(velocity: Float = 1.0f) {
        val buf = sampleBuffer ?: return
        val filter = BiquadFilter(FilterType.BANDPASS, 1850f, 1.2f, SAMPLE_RATE.toFloat())
        val v = Voice(
            buffer = buf,
            playbackRate = 1.12f,
            filter = filter,
            decayRate = 18.0f,
            maxDuration = 0.18f,
            gain = 0.9f * velocity,
            reverbSend = 0.4f * velocity,
            delaySend = 0f,
            distort = true
        )
        activeVoices.add(v)
    }

    fun triggerHiHat(velocity: Float = 0.8f) {
        val buf = sampleBuffer ?: return
        val filter = BiquadFilter(FilterType.HIGHPASS, 6500f, 1.6f, SAMPLE_RATE.toFloat())
        val hitDur = 0.055f + (velocity * 0.025f)
        val v = Voice(
            buffer = buf,
            playbackRate = 2.6f,
            filter = filter,
            decayRate = 35.0f,
            maxDuration = hitDur,
            gain = 0.55f * velocity,
            reverbSend = 0f,
            delaySend = 0f
        )
        activeVoices.add(v)
    }

    fun triggerPerc(velocity: Float = 0.8f) {
        val buf = sampleBuffer ?: return
        val filter = BiquadFilter(FilterType.BANDPASS, 1150f, 3.5f, SAMPLE_RATE.toFloat())
        val v = Voice(
            buffer = buf,
            playbackRate = 1.65f,
            filter = filter,
            decayRate = 22.0f,
            maxDuration = 0.14f,
            gain = 0.6f * velocity,
            reverbSend = 0.15f * velocity,
            delaySend = 0f
        )
        activeVoices.add(v)
    }

    fun triggerMelody(semitone: Int, velocity: Float = 0.85f) {
        val buf = sampleBuffer ?: return
        val pitchRatio = Scales.semitoneToPitchRatio(semitone)
        val baseFreq = (330.0 * pitchRatio).toFloat().coerceIn(100f, 12000f)
        val filter = BiquadFilter(FilterType.BANDPASS, baseFreq, 4.5f, SAMPLE_RATE.toFloat())
        val v = Voice(
            buffer = buf,
            playbackRate = pitchRatio,
            filter = filter,
            decayRate = 8.0f,
            maxDuration = 0.38f,
            gain = 0.75f * velocity,
            reverbSend = 0.55f * velocity,
            delaySend = 0.4f * velocity
        )
        activeVoices.add(v)
    }

    fun triggerBass(semitone: Int, velocity: Float = 0.9f) {
        val buf = sampleBuffer ?: return
        val pitchRatio = Scales.semitoneToPitchRatio(semitone)
        val filter = BiquadFilter(FilterType.LOWPASS, 220f, 2.0f, SAMPLE_RATE.toFloat())
        val v = Voice(
            buffer = buf,
            playbackRate = max(0.18f, pitchRatio * 0.4f),
            filter = filter,
            decayRate = 11.0f,
            maxDuration = 0.28f,
            gain = 0.8f * velocity,
            reverbSend = 0.05f * velocity,
            delaySend = 0f
        )
        activeVoices.add(v)
    }

    // --- REALTIME DSP AUDIO THREAD RENDER LOOP ---

    private fun renderLoop() {
        val chunkSize = 512
        val outStereo = FloatArray(chunkSize * 2)
        val fxOut = FloatArray(2)

        var samplesPerStep = (60.0 / activeBpm / 4.0 * SAMPLE_RATE).toInt()
        var stepSampleCounter = 0
        var internalStep = 0

        while (isEngineRunning) {
            if (sequencerRestartRequested) {
                stepSampleCounter = 0
                internalStep = 0
                sequencerRestartRequested = false
            }
            samplesPerStep = (60.0 / max(40, activeBpm) / 4.0 * SAMPLE_RATE).toInt()

            // Update master FX settings
            masterFilter.setFrequency(activeFx.filterCutoff)
            delayEffect.delayTimeSec = activeFx.delayTime
            delayEffect.feedback = activeFx.delayFeedback
            delayEffect.mix = activeFx.delayMix
            reverbEffect.mix = activeFx.reverbMix

            var outIdx = 0
            for (f in 0 until chunkSize) {
                // Sequencer step clock
                if (isPlaying) {
                    val swingOffset = if (internalStep % 2 == 1) {
                        (activeFx.swing * (samplesPerStep / 2)).toInt()
                    } else 0

                    if (stepSampleCounter >= samplesPerStep + swingOffset) {
                        stepSampleCounter = 0
                        currentStep = internalStep
                        val stepToTrigger = internalStep
                        internalStep = (internalStep + 1) % 16

                        // Notify UI
                        mainHandler.post { onStepChange?.invoke(stepToTrigger) }

                        // Trigger step notes
                        val pat = activePattern
                        if (pat.kick[stepToTrigger] && isTrackAudible(TrackId.KICK)) triggerKick(1.0f)
                        if (pat.snare[stepToTrigger] && isTrackAudible(TrackId.SNARE)) triggerSnare(0.95f)
                        if (pat.hihat[stepToTrigger] && isTrackAudible(TrackId.HIHAT)) {
                            val vel = if (stepToTrigger % 4 == 0) 0.9f else if (stepToTrigger % 2 == 0) 0.75f else 0.6f
                            triggerHiHat(vel)
                        }
                        if (pat.perc[stepToTrigger] && isTrackAudible(TrackId.PERC)) triggerPerc(0.85f)
                        val mel = pat.melody.getOrNull(stepToTrigger)
                        if (mel?.active == true && isTrackAudible(TrackId.MELODY)) triggerMelody(mel.pitch, 0.85f)
                        val bas = pat.bass.getOrNull(stepToTrigger)
                        if (bas?.active == true && isTrackAudible(TrackId.BASS)) triggerBass(bas.pitch, 0.9f)
                    }
                    stepSampleCounter++
                }

                var dryL = 0f
                var dryR = 0f
                var sendReverb = 0f
                var sendDelay = 0f

                // Raw dry audio preview
                if (rawPlaybackPos >= 0) {
                    val raw = rawBuffer
                    if (raw != null && rawPlaybackPos < raw.size) {
                        dryL += raw[rawPlaybackPos]
                        dryR += raw[rawPlaybackPos]
                        rawPlaybackPos++
                    } else {
                        rawPlaybackPos = -1
                    }
                }

                // Trimmed dry audio preview
                if (trimmedPlaybackPos >= 0) {
                    val tr = sampleBuffer
                    if (tr != null && trimmedPlaybackPos < tr.size) {
                        dryL += tr[trimmedPlaybackPos]
                        dryR += tr[trimmedPlaybackPos]
                        trimmedPlaybackPos++
                    } else {
                        trimmedPlaybackPos = -1
                    }
                }

                // Mix voices
                val iterator = activeVoices.iterator()
                while (iterator.hasNext()) {
                    val voice = iterator.next()
                    if (voice.isFinished) {
                        activeVoices.remove(voice)
                        continue
                    }

                    val sample = voice.renderNext()
                    dryL += sample * voice.gain
                    dryR += sample * voice.gain
                    sendReverb += sample * voice.reverbSend
                    sendDelay += sample * voice.delaySend
                }

                // Distortion shaper
                if (activeFx.distortion > 0.01f) {
                    dryL = WaveShaper.distort(dryL, activeFx.distortion)
                    dryR = WaveShaper.distort(dryR, activeFx.distortion)
                }

                // Master Lowpass filter
                dryL = masterFilter.process(dryL)
                dryR = masterFilter.process(dryR)

                // Master FX: Delay
                delayEffect.process(dryL + sendDelay, dryR + sendDelay, fxOut)
                var wetL = fxOut[0]
                var wetR = fxOut[1]

                // Master FX: Reverb
                reverbEffect.process(wetL + sendReverb, wetR + sendReverb, fxOut)
                wetL = fxOut[0]
                wetR = fxOut[1]

                // Master gain & soft-knee limiter
                val fadeTarget = if (stopFadeRequested) 0f else 1f
                if (masterFade != fadeTarget) {
                    val fadeStep = 1f / (SAMPLE_RATE * if (fadeTarget == 0f) 0.12f else 0.05f)
                    masterFade = if (masterFade < fadeTarget) {
                        (masterFade + fadeStep).coerceAtMost(fadeTarget)
                    } else {
                        (masterFade - fadeStep).coerceAtLeast(fadeTarget)
                    }
                    if (masterFade == 0f) activeVoices.clear()
                }

                val masterGain = 0.85f * masterFade
                var outL = (dryL * 0.7f + wetL * 0.5f) * masterGain
                var outR = (dryR * 0.7f + wetR * 0.5f) * masterGain

                // Limiter
                outL = outL.coerceIn(-1.0f, 1.0f)
                outR = outR.coerceIn(-1.0f, 1.0f)

                outStereo[outIdx++] = outL
                outStereo[outIdx++] = outR
            }

            audioTrack?.write(outStereo, 0, outStereo.size, AudioTrack.WRITE_BLOCKING)
        }
    }

    // --- 4-BAR MASTER WAV RENDERER ---

    fun renderWav(
        preset: Preset? = null,
        fx: FxConfig = activeFx,
        pattern: PatternState = activePattern,
        bpm: Int = activeBpm
    ): ByteArray {
        val totalBars = 4
        val totalSteps = totalBars * 16
        val samplesPerStep = (60.0 / bpm / 4.0 * SAMPLE_RATE).toInt()
        val totalSamples = samplesPerStep * totalSteps + (SAMPLE_RATE * 2) // Extra tail for reverb & delay

        val leftSamples = FloatArray(totalSamples)
        val rightSamples = FloatArray(totalSamples)

        val localMasterFilter = BiquadFilter(FilterType.LOWPASS, fx.filterCutoff, 0.7f, SAMPLE_RATE.toFloat())
        val localDelay = DelayEffect(SAMPLE_RATE).apply {
            delayTimeSec = fx.delayTime
            feedback = fx.delayFeedback
            mix = fx.delayMix
        }
        val localReverb = ReverbEffect(SAMPLE_RATE).apply {
            mix = fx.reverbMix
        }
        val voices = mutableListOf<Voice>()
        val fxOut = FloatArray(2)

        var stepCounter = 0
        var currentStepIdx = 0

        for (s in 0 until totalSamples) {
            if (s < samplesPerStep * totalSteps) {
                val swingOffset = if (currentStepIdx % 2 == 1) {
                    (fx.swing * (samplesPerStep / 2)).toInt()
                } else 0

                if (stepCounter >= samplesPerStep + swingOffset) {
                    stepCounter = 0
                    val patStep = currentStepIdx % 16
                    currentStepIdx++

                    sampleBuffer?.let { buf ->
                        if (pattern.kick[patStep]) {
                            val filter = BiquadFilter(FilterType.LOWPASS, 160f, 1.8f, SAMPLE_RATE.toFloat())
                            voices.add(Voice(buf, 0.42f, filter, 9f, 0.35f, 1.0f, 0f, 0f, 110f, 42f, 0.12f, 0.65f))
                        }
                        if (pattern.snare[patStep]) {
                            val filter = BiquadFilter(FilterType.BANDPASS, 1850f, 1.2f, SAMPLE_RATE.toFloat())
                            voices.add(Voice(buf, 1.12f, filter, 18f, 0.18f, 0.95f, 0.4f, 0f, distort = true))
                        }
                        if (pattern.hihat[patStep]) {
                            val filter = BiquadFilter(FilterType.HIGHPASS, 6500f, 1.6f, SAMPLE_RATE.toFloat())
                            val vel = if (patStep % 4 == 0) 0.9f else if (patStep % 2 == 0) 0.75f else 0.6f
                            voices.add(Voice(buf, 2.6f, filter, 35f, 0.055f + vel * 0.025f, 0.55f * vel, 0f, 0f))
                        }
                        if (pattern.perc[patStep]) {
                            val filter = BiquadFilter(FilterType.BANDPASS, 1150f, 3.5f, SAMPLE_RATE.toFloat())
                            voices.add(Voice(buf, 1.65f, filter, 22f, 0.14f, 0.6f, 0.15f, 0f))
                        }
                        val mel = pattern.melody.getOrNull(patStep)
                        if (mel?.active == true) {
                            val pRatio = Scales.semitoneToPitchRatio(mel.pitch)
                            val bFreq = (330.0 * pRatio).toFloat().coerceIn(100f, 12000f)
                            val filter = BiquadFilter(FilterType.BANDPASS, bFreq, 4.5f, SAMPLE_RATE.toFloat())
                            voices.add(Voice(buf, pRatio, filter, 8f, 0.38f, 0.75f, 0.55f, 0.4f))
                        }
                        val bas = pattern.bass.getOrNull(patStep)
                        if (bas?.active == true) {
                            val pRatio = Scales.semitoneToPitchRatio(bas.pitch)
                            val filter = BiquadFilter(FilterType.LOWPASS, 220f, 2.0f, SAMPLE_RATE.toFloat())
                            voices.add(Voice(buf, max(0.18f, pRatio * 0.4f), filter, 11f, 0.28f, 0.8f, 0.05f, 0f))
                        }
                    }
                }
                stepCounter++
            }

            var dryL = 0f
            var dryR = 0f
            var sendReverb = 0f
            var sendDelay = 0f

            val it = voices.iterator()
            while (it.hasNext()) {
                val v = it.next()
                if (v.isFinished) {
                    it.remove()
                    continue
                }
                val sample = v.renderNext()
                dryL += sample * v.gain
                dryR += sample * v.gain
                sendReverb += sample * v.reverbSend
                sendDelay += sample * v.delaySend
            }

            if (fx.distortion > 0.01f) {
                dryL = WaveShaper.distort(dryL, fx.distortion)
                dryR = WaveShaper.distort(dryR, fx.distortion)
            }

            dryL = localMasterFilter.process(dryL)
            dryR = localMasterFilter.process(dryR)

            localDelay.process(dryL + sendDelay, dryR + sendDelay, fxOut)
            var wetL = fxOut[0]
            var wetR = fxOut[1]

            localReverb.process(wetL + sendReverb, wetR + sendReverb, fxOut)
            wetL = fxOut[0]
            wetR = fxOut[1]

            val masterGain = 0.85f
            leftSamples[s] = ((dryL * 0.7f + wetL * 0.5f) * masterGain).coerceIn(-1f, 1f)
            rightSamples[s] = ((dryR * 0.7f + wetR * 0.5f) * masterGain).coerceIn(-1f, 1f)
        }

        return WavEncoder.encodeToWav(leftSamples, rightSamples, SAMPLE_RATE)
    }

    // --- INNER VOICE CLASS ---

    private class Voice(
        val buffer: FloatArray,
        val playbackRate: Float,
        val filter: BiquadFilter,
        val decayRate: Float,
        val maxDuration: Float,
        val gain: Float,
        val reverbSend: Float,
        val delaySend: Float,
        val subOscFreqStart: Float = 0f,
        val subOscFreqEnd: Float = 0f,
        val subOscDuration: Float = 0f,
        val subOscGain: Float = 0f,
        val distort: Boolean = false
    ) {
        var samplePos: Float = 0f
        var currentSampleCount: Int = 0
        val maxSamples: Int = (maxDuration * SAMPLE_RATE).toInt()
        var isFinished = false
            private set

        // Sub oscillator state
        private var subPhase: Double = 0.0

        fun renderNext(): Float {
            if (isFinished) return 0f

            val t = currentSampleCount.toFloat() / SAMPLE_RATE
            if (currentSampleCount >= maxSamples || samplePos >= buffer.size - 1) {
                isFinished = true
                return 0f
            }

            // Linear interpolation of sample buffer
            val idx0 = samplePos.toInt()
            val idx1 = min(idx0 + 1, buffer.size - 1)
            val frac = samplePos - idx0
            val s0 = buffer[idx0]
            val s1 = buffer[idx1]
            var rawSample = s0 + frac * (s1 - s0)

            samplePos += playbackRate
            currentSampleCount++

            // Amplitude envelope
            val env = exp(-t * decayRate)
            rawSample *= env

            // Filter
            var filtered = filter.process(rawSample)

            if (distort) {
                filtered = WaveShaper.distort(filtered, 0.25f)
            }

            // Sub bass reinforcement (Kick)
            if (subOscGain > 0.001f && t < subOscDuration) {
                val subProgress = (t / subOscDuration).coerceIn(0f, 1f)
                val curFreq = subOscFreqStart + (subOscFreqEnd - subOscFreqStart) * subProgress
                subPhase += (2.0 * Math.PI * curFreq / SAMPLE_RATE)
                val subEnv = (1.0f - subProgress) * subOscGain
                val subSample = sin(subPhase).toFloat() * subEnv
                filtered += subSample
            }

            return filtered
        }
    }
}
