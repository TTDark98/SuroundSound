package com.example.surroundsound.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.surroundsound.audio.AudioEngine
import com.example.surroundsound.model.BeatStyle
import com.example.surroundsound.model.MelodicStep
import com.example.surroundsound.model.PatternState
import com.example.surroundsound.model.Presets
import com.example.surroundsound.model.Scales
import com.example.surroundsound.model.TrackId
import com.example.surroundsound.ui.studio.BUILT_IN_SOURCES
import com.example.surroundsound.ui.studio.ExportSheet
import com.example.surroundsound.ui.studio.FxPane
import com.example.surroundsound.ui.studio.MicPhase
import com.example.surroundsound.ui.studio.PadsPane
import com.example.surroundsound.ui.studio.PreviewBanner
import com.example.surroundsound.ui.studio.SamplePane
import com.example.surroundsound.ui.studio.StatusTone
import com.example.surroundsound.ui.studio.StudioFormats
import com.example.surroundsound.ui.studio.StudioTopBar
import com.example.surroundsound.ui.studio.StudioBackdrop
import com.example.surroundsound.ui.studio.StudioTabs
import com.example.surroundsound.ui.studio.StudioToast
import com.example.surroundsound.ui.studio.TempoPanel
import com.example.surroundsound.ui.studio.ThemePickerScreen
import com.example.surroundsound.ui.studio.TimelinePane
import com.example.surroundsound.ui.studio.TransportPanel
import com.example.surroundsound.ui.theme.LocalStudioTokens
import com.example.surroundsound.ui.theme.StudioTheme
import com.example.surroundsound.ui.theme.SurroundSoundTheme
import com.example.surroundsound.ui.theme.ThemePreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAB_TIMELINE = 0
private const val TAB_SAMPLE = 1
private const val TAB_FX = 2
private const val TAB_PADS = 3
private val TAB_LABELS = listOf("Timeline", "Sample", "FX", "Pads")

/**
 * The studio.
 *
 * A mood (theme) wraps everything, then one shell holds the transport, the four
 * panes, the export sheet and the theme picker. The mood is remembered between
 * launches; previewing one only borrows it until Apply.
 */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var activeTheme by remember { mutableStateOf(ThemePreference.load(context)) }
    var previewTheme by remember { mutableStateOf<StudioTheme?>(null) }
    val effective = previewTheme ?: activeTheme

    SurroundSoundTheme(theme = effective) {
        StudioRoot(
            activeTheme = activeTheme,
            previewTheme = previewTheme,
            onApplyTheme = { picked ->
                activeTheme = picked
                ThemePreference.save(context, picked)
                previewTheme = null
            },
            onPreviewTheme = { previewTheme = it },
            onCancelPreview = { previewTheme = null },
            modifier = modifier
        )
    }
}

@Composable
private fun StudioRoot(
    activeTheme: StudioTheme,
    previewTheme: StudioTheme?,
    onApplyTheme: (StudioTheme) -> Unit,
    onPreviewTheme: (StudioTheme) -> Unit,
    onCancelPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()

    // System bars and the window background follow the mood: a light theme keeps
    // dark status icons instead of the launch theme's fixed light-on-dark.
    val activity = context as? ComponentActivity
    SideEffect {
        val barStyle = if (t.dark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        activity?.enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        activity?.window?.decorView?.setBackgroundColor(t.bg.toArgb())
    }

    val audioEngine = remember { AudioEngine() }

    var tab by rememberSaveable { mutableIntStateOf(TAB_TIMELINE) }
    var currentStyle by rememberSaveable { mutableStateOf(BeatStyle.LOFI) }
    var bpm by rememberSaveable { mutableIntStateOf(Presets.LOFI.bpm) }
    var fx by remember { mutableStateOf(Presets.LOFI.fxDefaults) }
    var pattern by remember { mutableStateOf(Presets.LOFI.pattern.copyDeep()) }
    var sampleMetadata by remember { mutableStateOf(audioEngine.sampleMetadata) }
    var trimStart by remember { mutableFloatStateOf(audioEngine.trimStart) }
    var trimEnd by remember { mutableFloatStateOf(audioEngine.trimEnd) }
    var sourceId by remember { mutableStateOf(BUILT_IN_SOURCES.first().id) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(-1) }
    var selectedTrack by remember { mutableStateOf(TrackId.MELODY) }
    var selectedStep by remember { mutableIntStateOf(0) }
    var mutedTracks by remember { mutableStateOf<Set<TrackId>>(emptySet()) }
    var soloTracks by remember { mutableStateOf<Set<TrackId>>(emptySet()) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableFloatStateOf(0f) }
    var micPhase by remember { mutableStateOf(MicPhase.READY) }
    var statusTitle by remember { mutableStateOf("Ready") }
    var statusDetail by remember { mutableStateOf("Tap record, or load a built-in source.") }
    var statusTone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var isAuditioningRaw by remember { mutableStateOf(false) }
    var isAuditioningTrimmed by remember { mutableStateOf(false) }
    var isExportOpen by remember { mutableStateOf(false) }
    var isThemePickerOpen by remember { mutableStateOf(false) }
    var isStyleMenuOpen by remember { mutableStateOf(false) }
    var pickerSelection by remember { mutableStateOf(activeTheme) }
    var toast by remember { mutableStateOf<String?>(null) }
    var pendingRecord by remember { mutableStateOf(false) }

    val scaleIntervals = Scales.getScale(fx.scaleId).intervals
    val stripIntervals = if (selectedTrack == TrackId.BASS) {
        scaleIntervals.map { it - 12 }
    } else {
        scaleIntervals
    }
    val presetsByStyle = remember { BeatStyle.values().associateWith { Presets.getPreset(it) } }

    DisposableEffect(audioEngine) {
        audioEngine.onStepChange = { step -> currentStep = step }
        audioEngine.onSampleLoaded = { meta ->
            sampleMetadata = meta
            trimStart = audioEngine.trimStart
            trimEnd = audioEngine.trimEnd
        }
        onDispose {
            audioEngine.stopSequencer()
            audioEngine.stopAudioEngine()
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2400)
            toast = null
        }
    }

    // Recorder clock: tenths, so a fast hit reads like an impact.
    LaunchedEffect(isRecording) {
        if (isRecording) {
            val startedAt = System.currentTimeMillis()
            while (isRecording) {
                recordingSeconds = (System.currentTimeMillis() - startedAt) / 1000f
                delay(66)
            }
        }
    }


    // Only the sample station needs to watch the audition transport.
    LaunchedEffect(tab) {
        if (tab == TAB_SAMPLE) {
            while (true) {
                isAuditioningRaw = audioEngine.isPlayingRaw
                isAuditioningTrimmed = audioEngine.isPlayingTrimmed
                delay(90)
            }
        }
    }

    fun showToast(text: String) {
        toast = text
    }

    fun handleSelectStyle(style: BeatStyle) {
        val preset = presetsByStyle[style] ?: Presets.LOFI
        currentStyle = style
        bpm = preset.bpm
        val newPattern = preset.pattern.copyDeep()
        pattern = newPattern
        val newFx = preset.fxDefaults.copy(scaleId = preset.defaultScale, swing = preset.swing)
        fx = newFx
        audioEngine.updateFx(newFx)
        audioEngine.updatePattern(newPattern)
        audioEngine.updateBpm(preset.bpm)
        selectedTrack = TrackId.MELODY
        selectedStep = 0
        showToast("${style.displayName} · ${preset.bpm} BPM")
    }

    fun togglePlay() {
        if (isPlaying) {
            audioEngine.stopSequencer()
            isPlaying = false
            currentStep = -1
        } else {
            audioEngine.startSequencer(presetsByStyle[currentStyle], fx, pattern)
            isPlaying = true
        }
    }

    fun stopTransport() {
        audioEngine.stopSequencer()
        isPlaying = false
        currentStep = -1
    }

    fun toggleStep(track: TrackId, step: Int) {
        val newPattern = pattern.copyDeep()
        when (track) {
            TrackId.KICK -> newPattern.kick[step] = !newPattern.kick[step]
            TrackId.SNARE -> newPattern.snare[step] = !newPattern.snare[step]
            TrackId.HIHAT -> newPattern.hihat[step] = !newPattern.hihat[step]
            TrackId.PERC -> newPattern.perc[step] = !newPattern.perc[step]
            TrackId.MELODY -> {
                val current = newPattern.melody[step]
                val updated = newPattern.melody.toMutableList()
                updated[step] = current.copy(active = !current.active)
                pattern = newPattern.copy(melody = updated)
                audioEngine.updatePattern(pattern)
                if (!isPlaying && updated[step].active) {
                    audioEngine.triggerVoice(TrackId.MELODY, updated[step].pitch)
                }
                return
            }
            TrackId.BASS -> {
                val current = newPattern.bass[step]
                val updated = newPattern.bass.toMutableList()
                updated[step] = current.copy(active = !current.active)
                pattern = newPattern.copy(bass = updated)
                audioEngine.updatePattern(pattern)
                if (!isPlaying && updated[step].active) {
                    audioEngine.triggerVoice(TrackId.BASS, updated[step].pitch)
                }
                return
            }
        }
        pattern = newPattern
        audioEngine.updatePattern(newPattern)
        if (!isPlaying) audioEngine.triggerVoice(track)
    }

    fun setPitch(track: TrackId, step: Int, pitch: Int) {
        val newPattern = pattern.copyDeep()
        if (track == TrackId.BASS) {
            val updated = newPattern.bass.toMutableList()
            updated[step] = MelodicStep(active = true, pitch = pitch)
            pattern = newPattern.copy(bass = updated)
        } else {
            val updated = newPattern.melody.toMutableList()
            updated[step] = MelodicStep(active = true, pitch = pitch)
            pattern = newPattern.copy(melody = updated)
        }
        audioEngine.updatePattern(pattern)
        if (!isPlaying) audioEngine.triggerVoice(track, pitch)
    }

    fun selectStep(track: TrackId, step: Int) {
        selectedTrack = track
        selectedStep = step
    }

    fun clearPattern() {
        val empty = PatternState(
            kick = BooleanArray(16),
            snare = BooleanArray(16),
            hihat = BooleanArray(16),
            perc = BooleanArray(16),
            melody = List(16) { MelodicStep(false, 0) },
            bass = List(16) { MelodicStep(false, -12) }
        )
        pattern = empty
        audioEngine.updatePattern(empty)
        showToast("Pattern cleared")
    }

    fun randomizePattern() {
        val random = kotlin.random.Random
        val newPattern = PatternState(
            kick = BooleanArray(16) { i ->
                i == 0 || (i == 6 && random.nextFloat() > 0.4f) ||
                    (i == 10 && random.nextFloat() > 0.3f) || random.nextFloat() > 0.85f
            },
            snare = BooleanArray(16) { i -> i == 4 || i == 12 || (i == 15 && random.nextFloat() > 0.7f) },
            hihat = BooleanArray(16) { i -> i % 2 == 0 || random.nextFloat() > 0.4f },
            perc = BooleanArray(16) { random.nextFloat() > 0.75f },
            melody = List(16) {
                MelodicStep(
                    active = random.nextFloat() > 0.6f,
                    pitch = scaleIntervals[random.nextInt(scaleIntervals.size)]
                )
            },
            bass = List(16) { i ->
                MelodicStep((i == 0 || i == 6 || i == 10) && random.nextFloat() > 0.3f, -12)
            }
        )
        pattern = newPattern
        audioEngine.updatePattern(newPattern)
        showToast("New pattern")
    }

    fun loadSource(id: String) {
        sourceId = id
        when (id) {
            "kitchen" -> audioEngine.generateSyntheticMetalHit()
            "field" -> audioEngine.generateSyntheticKnuckleTap()
            "paper" -> audioEngine.generateSyntheticGlassPing()
            else -> audioEngine.generateSyntheticTableBang()
        }
        sampleMetadata = audioEngine.sampleMetadata
        trimStart = audioEngine.trimStart
        trimEnd = audioEngine.trimEnd
        micPhase = MicPhase.READY
        statusTone = StatusTone.GOOD
        statusTitle = "Sample ready"
        statusDetail = "Loaded ${audioEngine.sampleMetadata?.sourceName ?: id}."
        showToast("Loaded ${audioEngine.sampleMetadata?.sourceName ?: id}")
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingRecord = true
        } else {
            micPhase = MicPhase.DENIED
            statusTone = StatusTone.BAD
            statusTitle = "Microphone permission denied"
            statusDetail = "Enable the mic to use Record."
        }
    }

    fun startRecord() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (audioEngine.startMicrophoneRecording()) {
            isRecording = true
            recordingSeconds = 0f
            micPhase = MicPhase.RECORDING
            statusTone = StatusTone.NEUTRAL
            statusTitle = "Listening"
            statusDetail = "Hit the desk once, then tap the mic to stop."
        } else {
            micPhase = MicPhase.UNAVAILABLE
            statusTone = StatusTone.BAD
            statusTitle = "No microphone available"
            statusDetail = "The four built-in sources still work."
        }
    }

    fun finishRecording(limitReached: Boolean = false) {
        if (!isRecording) return
        isRecording = false
        recordingSeconds = 0f
        micPhase = MicPhase.PROCESSING
        statusTone = StatusTone.NEUTRAL
        statusTitle = "Processing"
        statusDetail = "Finding the first impact."

        val accepted = audioEngine.stopMicrophoneRecording()
        val issue = audioEngine.lastRecordingIssue
        val meta = audioEngine.sampleMetadata

        // Hold the processing state long enough to read before the result lands.
        scope.launch {
            delay(420)
            if (accepted) {
                sampleMetadata = meta
                trimStart = audioEngine.trimStart
                trimEnd = audioEngine.trimEnd
                micPhase = MicPhase.READY
                statusTone = StatusTone.GOOD
                statusTitle = if (limitReached) "Take saved at the 12 s cap" else "Sample ready"
                val length = meta?.duration ?: 0f
                statusDetail = "Trimmed selection is %.2f s.".format(length)
                showToast("Take processed")
            } else {
                micPhase = MicPhase.ERROR
                statusTone = StatusTone.BAD
                statusTitle = "Take discarded"
                statusDetail = issue ?: "Recording was too short or empty."
            }
        }
    }

    /**
     * Record lives in the transport, but a take is read on the sample station.
     * Taps are ignored while a take is being sliced, so a second take cannot
     * clobber the one that is still landing.
     */
    fun toggleRecord() {
        when {
            isRecording -> finishRecording()
            micPhase == MicPhase.PROCESSING -> Unit
            else -> {
                tab = TAB_SAMPLE
                startRecord()
            }
        }
    }

    // The engine stops itself at the take cap; keep the UI honest about it.
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(150)
            if (audioEngine.recordingLimitReached) {
                finishRecording(limitReached = true)
                break
            }
        }
    }

    LaunchedEffect(pendingRecord) {
        if (pendingRecord) {
            pendingRecord = false
            startRecord()
        }
    }

    BackHandler(
        enabled = isThemePickerOpen || isExportOpen || previewTheme != null ||
            isRecording || isStyleMenuOpen || tab != TAB_TIMELINE
    ) {
        when {
            isThemePickerOpen -> isThemePickerOpen = false
            isExportOpen -> isExportOpen = false
            isStyleMenuOpen -> isStyleMenuOpen = false
            previewTheme != null -> onCancelPreview()
            isRecording -> finishRecording()
            else -> tab = TAB_TIMELINE
        }
    }

    Box(modifier.fillMaxSize()) {
        StudioBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(6.dp))

                // Identity, mood entry, and the style menu.
                Box(Modifier.fillMaxWidth()) {
                    if (previewTheme != null) {
                        PreviewBanner(
                            theme = previewTheme,
                            onCancel = onCancelPreview,
                            onApply = { onApplyTheme(previewTheme) }
                        )
                    } else {
                        StudioTopBar(
                            styleName = currentStyle.displayName,
                            onStyleClick = { isStyleMenuOpen = true },
                            onThemeClick = {
                                pickerSelection = activeTheme
                                isThemePickerOpen = true
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = isStyleMenuOpen,
                        onDismissRequest = { isStyleMenuOpen = false }
                    ) {
                        BeatStyle.values().forEach { style ->
                            val preset = presetsByStyle[style] ?: Presets.LOFI
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = style.displayName,
                                            color = if (style == currentStyle) t.accentText else t.ink,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${preset.bpm} BPM · ${preset.tagline}",
                                            color = t.faint,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                },
                                onClick = {
                                    handleSelectStyle(style)
                                    isStyleMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (!isLandscape) {
                    TempoPanel(
                        bpm = bpm,
                        onBpmChange = { value ->
                            bpm = value
                            audioEngine.updateBpm(value)
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    TransportPanel(
                        isPlaying = isPlaying,
                        isRecording = isRecording,
                        recordCaption = StudioFormats.compactClock(recordingSeconds),
                        onPlayToggle = { togglePlay() },
                        onStop = { stopTransport() },
                        onRecordToggle = { toggleRecord() },
                        onExport = { isExportOpen = true }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                StudioTabs(
                    labels = TAB_LABELS,
                    selectedIndex = tab,
                    onSelect = { tab = it }
                )

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                ) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TempoPanel(
                                bpm = bpm,
                                onBpmChange = { value ->
                                    bpm = value
                                    audioEngine.updateBpm(value)
                                },
                                modifier = Modifier.weight(0.9f)
                            )
                            TransportPanel(
                                isPlaying = isPlaying,
                                isRecording = isRecording,
                                recordCaption = StudioFormats.compactClock(recordingSeconds),
                                onPlayToggle = { togglePlay() },
                                onStop = { stopTransport() },
                                onRecordToggle = { toggleRecord() },
                                onExport = { isExportOpen = true },
                                modifier = Modifier.weight(2.2f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Crossfade(
                        targetState = tab,
                        animationSpec = tween(durationMillis = 180),
                        label = "studioPane"
                    ) { index ->
                        when (index) {
                            TAB_SAMPLE -> SamplePane(
                                sourceId = sourceId,
                                sampleMetadata = sampleMetadata,
                                rawBuffer = audioEngine.rawBuffer,
                                trimStart = trimStart,
                                trimEnd = trimEnd,
                                isRecording = isRecording,
                                recordingSeconds = recordingSeconds,
                                micPhase = micPhase,
                                statusTitle = statusTitle,
                                statusDetail = statusDetail,
                                statusTone = statusTone,
                                isPlayingRaw = isAuditioningRaw,
                                isPlayingTrimmed = isAuditioningTrimmed,
                                onStartRecord = { startRecord() },
                                onStopRecord = { finishRecording() },
                                onOpenSettings = { openAppSettings(context) },
                                onSelectSource = { loadSource(it) },
                                onTrimChange = { start, end ->
                                    trimStart = start
                                    trimEnd = end
                                    audioEngine.setTrim(start, end)
                                },
                                onAutoDetect = {
                                    audioEngine.autoDetectTransient()
                                    trimStart = audioEngine.trimStart
                                    trimEnd = audioEngine.trimEnd
                                    sampleMetadata = audioEngine.sampleMetadata
                                    showToast("Trim tightened to the first hit")
                                },
                                onPlayRaw = {
                                    if (audioEngine.isPlayingRaw) {
                                        audioEngine.stopRawAudio()
                                    } else {
                                        audioEngine.playRawAudio()
                                    }
                                },
                                onPlayTrimmed = {
                                    if (audioEngine.isPlayingTrimmed) {
                                        audioEngine.stopTrimmedSample()
                                    } else {
                                        audioEngine.playTrimmedSample()
                                    }
                                }
                            )

                            TAB_FX -> FxPane(
                                fx = fx,
                                bpm = bpm,
                                twoColumn = isLandscape,
                                onFxChange = { updated ->
                                    fx = updated
                                    audioEngine.updateFx(updated)
                                }
                            )

                            TAB_PADS -> PadsPane(
                                onTrigger = { audioEngine.triggerVoice(it) }
                            )

                            else -> TimelinePane(
                                pattern = pattern,
                                currentStep = currentStep,
                                isPlaying = isPlaying,
                                bpm = bpm,
                                scaleIntervals = stripIntervals,
                                mutedTracks = mutedTracks,
                                soloTracks = soloTracks,
                                selectedTrack = selectedTrack,
                                selectedStep = selectedStep,
                                onToggleStep = { track, step -> toggleStep(track, step) },
                                onSelectStep = { track, step -> selectStep(track, step) },
                                onSetPitch = { track, step, pitch -> setPitch(track, step, pitch) },
                                onClear = { clearPattern() },
                                onRandomize = { randomizePattern() },
                                onAuditionTrack = { audioEngine.triggerVoice(it) },
                                onToggleMute = { track ->
                                    audioEngine.toggleMute(track)
                                    mutedTracks = audioEngine.mutedTracks.toSet()
                                    showToast(
                                        "${track.shortName} " +
                                            if (mutedTracks.contains(track)) "muted" else "live"
                                    )
                                },
                                onToggleSolo = { track ->
                                    audioEngine.toggleSolo(track)
                                    soloTracks = audioEngine.soloTracks.toSet()
                                }
                            )
                        }                    }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }

        StudioToast(
            text = toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 30.dp)
        )

        ExportSheet(
            visible = isExportOpen,
            style = currentStyle,
            styleName = currentStyle.displayName,
            bpm = bpm,
            onRenderWav = {
                audioEngine.renderWav(presetsByStyle[currentStyle] ?: Presets.LOFI, fx, pattern, bpm)
            },
            onClose = { isExportOpen = false },
            onToast = { showToast(it) }
        )

        ThemePickerScreen(
            visible = isThemePickerOpen,
            activeTheme = activeTheme,
            selectedTheme = pickerSelection,
            onSelect = { pickerSelection = it },
            onPreview = {
                pickerSelection = it
                isThemePickerOpen = false
                onPreviewTheme(it)
            },
            onApply = {
                isThemePickerOpen = false
                onApplyTheme(pickerSelection)
                showToast("${pickerSelection.displayName} applied")
            },
            onClose = { isThemePickerOpen = false }
        )
    }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = android.content.Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.fromParts("package", context.packageName, null)
    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
