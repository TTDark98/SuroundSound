package com.example.bangtobeat.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.NavKey
import com.example.bangtobeat.audio.AudioEngine
import com.example.bangtobeat.model.*
import com.example.bangtobeat.theme.*
import com.example.bangtobeat.ui.components.*
import kotlinx.coroutines.delay

enum class StudioView(val label: String, val icon: String) {
    TIMELINE("TIMELINE", "🎛"),
    SAMPLE("SAMPLE & FOLEY", "🌊"),
    FX("FX RACK", "🎚"),
    PADS("MPC PADS", "🎹"),
    ALL("FULL STUDIO", "⚡")
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Audio Engine singleton state
    val audioEngine = remember { AudioEngine() }

    // UI state
    var currentStyle by remember { mutableStateOf(BeatStyle.LOFI) }
    var bpm by remember { mutableIntStateOf(Presets.LOFI.bpm) }
    var sampleMetadata by remember { mutableStateOf(audioEngine.sampleMetadata) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(-1) }
    var isExportOpen by remember { mutableStateOf(false) }
    var activeView by remember { mutableStateOf(StudioView.TIMELINE) }

    // Preset dropdown menu state
    var isStyleMenuOpen by remember { mutableStateOf(false) }

    // Recording State
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("Ready to record desk impact") }

    // Track Mute & Solo state counters to trigger recomposition
    var muteSoloTrigger by remember { mutableIntStateOf(0) }

    // FX State
    var fx by remember { mutableStateOf(Presets.LOFI.fxDefaults) }

    // Pattern State
    var pattern by remember { mutableStateOf(Presets.LOFI.pattern.copyDeep()) }

    // Register engine step callback
    DisposableEffect(audioEngine) {
        audioEngine.onStepChange = { step ->
            currentStep = step
        }
        audioEngine.onSampleLoaded = { meta ->
            sampleMetadata = meta
        }
        onDispose {
            audioEngine.stopSequencer()
            audioEngine.stopAudioEngine()
        }
    }

    // Timer effect when recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    // Permission launcher for RECORD_AUDIO
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = audioEngine.startMicrophoneRecording()
            if (started) {
                isRecording = true
                statusMessage = "Listening! Pound your desk or tap an object NOW!"
            } else {
                statusMessage = "Could not initialize microphone. Test with built-in sounds below!"
            }
        } else {
            statusMessage = "Microphone permission required for live recording."
        }
    }

    fun startRecord() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val started = audioEngine.startMicrophoneRecording()
            if (started) {
                isRecording = true
                statusMessage = "Listening! Pound desk or tap metal NOW!"
            } else {
                statusMessage = "Could not initialize microphone."
            }
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopRecord() {
        if (isRecording) {
            isRecording = false
            statusMessage = "Processing impact transient..."
            val success = audioEngine.stopMicrophoneRecording()
            if (success) {
                sampleMetadata = audioEngine.sampleMetadata
                statusMessage = "Live audio impact processed! Transformed into drums & melody."
            } else {
                statusMessage = "Recording was too short or empty."
            }
        }
    }

    // Style switch
    fun handleSelectStyle(style: BeatStyle) {
        val preset = Presets.getPreset(style)
        currentStyle = style
        bpm = preset.bpm

        val newPattern = preset.pattern.copyDeep()
        pattern = newPattern

        val newFx = preset.fxDefaults.copy(
            scaleId = preset.defaultScale,
            swing = preset.swing
        )
        fx = newFx
        audioEngine.updateFx(newFx)
        audioEngine.updatePattern(newPattern)
        audioEngine.updateBpm(preset.bpm)
    }

    fun togglePlay() {
        if (isPlaying) {
            audioEngine.stopSequencer()
            isPlaying = false
            currentStep = -1
        } else {
            val preset = Presets.getPreset(currentStyle)
            audioEngine.startSequencer(preset, fx, pattern)
            isPlaying = true
        }
    }

    fun stopSequencer() {
        audioEngine.stopSequencer()
        isPlaying = false
        currentStep = -1
    }

    // Toggle step
    fun handleToggleStep(track: TrackId, step: Int) {
        val newPat = pattern.copyDeep()
        when (track) {
            TrackId.KICK -> newPat.kick[step] = !newPat.kick[step]
            TrackId.SNARE -> newPat.snare[step] = !newPat.snare[step]
            TrackId.HIHAT -> newPat.hihat[step] = !newPat.hihat[step]
            TrackId.PERC -> newPat.perc[step] = !newPat.perc[step]
            TrackId.MELODY -> {
                val cur = newPat.melody[step]
                val updatedMel = newPat.melody.toMutableList()
                updatedMel[step] = MelodicStep(!cur.active, cur.pitch)
                pattern = newPat.copy(melody = updatedMel)
                audioEngine.updatePattern(pattern)
                return
            }
            TrackId.BASS -> {
                val cur = newPat.bass[step]
                val updatedBass = newPat.bass.toMutableList()
                updatedBass[step] = MelodicStep(!cur.active, cur.pitch)
                pattern = newPat.copy(bass = updatedBass)
                audioEngine.updatePattern(pattern)
                return
            }
        }
        pattern = newPat
        audioEngine.updatePattern(newPat)
    }

    // Cycle melody pitch
    fun handleCycleMelodyPitch(step: Int) {
        val scale = Scales.getScale(fx.scaleId)
        val curPitch = pattern.melody.getOrNull(step)?.pitch ?: 0
        var idx = scale.intervals.indexOf(curPitch)
        if (idx == -1) idx = 0
        val nextPitch = scale.intervals[(idx + 1) % scale.intervals.size]

        val updatedMel = pattern.melody.toMutableList()
        updatedMel[step] = MelodicStep(active = true, pitch = nextPitch)
        val newPat = pattern.copy(melody = updatedMel)
        pattern = newPat
        audioEngine.updatePattern(newPat)
    }

    // Clear Pattern
    fun handleClearPattern() {
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
    }

    // Randomize Pattern
    fun handleRandomizePattern() {
        val scale = Scales.getScale(fx.scaleId)
        val r = kotlin.random.Random
        val newPat = PatternState(
            kick = BooleanArray(16) { i -> i == 0 || (i == 6 && r.nextFloat() > 0.4f) || (i == 10 && r.nextFloat() > 0.3f) || (r.nextFloat() > 0.85f) },
            snare = BooleanArray(16) { i -> i == 4 || i == 12 || (i == 15 && r.nextFloat() > 0.7f) },
            hihat = BooleanArray(16) { i -> i % 2 == 0 || r.nextFloat() > 0.4f },
            perc = BooleanArray(16) { r.nextFloat() > 0.75f },
            melody = List(16) {
                val act = r.nextFloat() > 0.6f
                val p = scale.intervals[r.nextInt(scale.intervals.size)]
                MelodicStep(act, p)
            },
            bass = List(16) { i ->
                val act = (i == 0 || i == 6 || i == 10) && r.nextFloat() > 0.3f
                MelodicStep(act, -12)
            }
        )
        pattern = newPat
        audioEngine.updatePattern(newPat)
    }

    // Shared UI Elements
    @Composable
    fun LogoGroup() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isLandscape) 28.dp else 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(listOf(NeonCyan, Indigo600))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioCarbon, modifier = Modifier.size(if (isLandscape) 18.dp else 15.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "SURROUND SOUND",
                    color = Color.White,
                    fontSize = if (isLandscape) 12.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("PRO", color = NeonGreen, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    @Composable
    fun StyleSelectorButton() {
        Box {
            Button(
                onClick = { isStyleMenuOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = StudioPanelLight, contentColor = Slate200),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = if (isLandscape) 9.dp else 6.dp, vertical = 4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
            ) {
                Text(
                    text = currentStyle.displayName,
                    fontSize = if (isLandscape) 11.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(13.dp))
            }

            DropdownMenu(
                expanded = isStyleMenuOpen,
                onDismissRequest = { isStyleMenuOpen = false },
                modifier = Modifier.background(StudioPanel)
            ) {
                BeatStyle.values().forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style.displayName, color = if (style == currentStyle) NeonCyan else Slate200, fontSize = 12.sp) },
                        onClick = {
                            handleSelectStyle(style)
                            isStyleMenuOpen = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun MasterTransportControls() {
        if (isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Play / Pause Button
                Button(
                    onClick = { togglePlay() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) NeonAmber else NeonGreen,
                        contentColor = StudioCarbon
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPlaying) "PAUSE" else "PLAY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // Stop Button
                Button(
                    onClick = { stopSequencer() },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPanelLight, contentColor = Slate300),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                }

                // Record Button
                Button(
                    onClick = { if (isRecording) stopRecord() else startRecord() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) NeonRed else StudioPanelLight,
                        contentColor = if (isRecording) Color.White else NeonRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecording) NeonRed else StudioPanelBorder)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = "Record",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isRecording) "${recordingSeconds}s" else "REC", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // BPM Adjuster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioPanelLight)
                        .border(1.dp, StudioPanelBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newBpm = (bpm - 1).coerceAtLeast(60)
                            bpm = newBpm
                            audioEngine.updateBpm(newBpm)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Down", tint = Slate300, modifier = Modifier.size(13.dp))
                    }

                    Text(
                        text = "$bpm BPM",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            val newBpm = (bpm + 1).coerceAtMost(180)
                            bpm = newBpm
                            audioEngine.updateBpm(newBpm)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Up", tint = Slate300, modifier = Modifier.size(13.dp))
                    }
                }
            }
        } else {
            // PORTRAIT: Weighted transport row that fits 100% on any screen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause
                Button(
                    onClick = { togglePlay() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) NeonAmber else NeonGreen,
                        contentColor = StudioCarbon
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(if (isPlaying) "PAUSE" else "PLAY", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }

                // Stop
                Button(
                    onClick = { stopSequencer() },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPanelLight, contentColor = Slate300),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(15.dp))
                }

                // Record
                Button(
                    onClick = { if (isRecording) stopRecord() else startRecord() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) NeonRed else StudioPanelLight,
                        contentColor = if (isRecording) Color.White else NeonRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecording) NeonRed else StudioPanelBorder),
                    modifier = Modifier.weight(1.0f)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(if (isRecording) "${recordingSeconds}s" else "REC", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }

                // BPM
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioPanelLight)
                        .border(1.dp, StudioPanelBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newBpm = (bpm - 1).coerceAtLeast(60)
                            bpm = newBpm
                            audioEngine.updateBpm(newBpm)
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Down", tint = Slate300, modifier = Modifier.size(12.dp))
                    }

                    Text(
                        text = "$bpm BPM",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            val newBpm = (bpm + 1).coerceAtMost(180)
                            bpm = newBpm
                            audioEngine.updateBpm(newBpm)
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Up", tint = Slate300, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun ExportButton() {
        Button(
            onClick = { isExportOpen = true },
            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet, contentColor = StudioCarbon),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = if (isLandscape) 10.dp else 8.dp, vertical = if (isLandscape) 6.dp else 4.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(2.dp))
            Text("EXPORT", fontSize = if (isLandscape) 10.sp else 9.sp, fontWeight = FontWeight.Black)
        }
    }

    @Composable
    fun StudioViewTabs() {
        if (isLandscape) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioView.values().forEach { view ->
                    val isSelected = view == activeView
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Indigo600 else StudioPanelLight)
                            .border(1.dp, if (isSelected) Indigo500 else StudioPanelBorder, RoundedCornerShape(6.dp))
                            .clickable { activeView = view }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${view.icon} ${view.label}",
                            color = if (isSelected) Color.White else Slate400,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // PORTRAIT: 4 clean segmented tabs spanning full width
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val portraitTabs = listOf(
                    StudioView.TIMELINE to ("🎛" to "TIMELINE"),
                    StudioView.SAMPLE to ("🌊" to "SAMPLE"),
                    StudioView.FX to ("🎚" to "FX RACK"),
                    StudioView.PADS to ("🎹" to "PADS")
                )
                portraitTabs.forEach { (view, meta) ->
                    val isSelected = view == activeView
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Indigo600 else StudioPanelLight)
                            .border(1.dp, if (isSelected) Indigo500 else StudioPanelBorder, RoundedCornerShape(6.dp))
                            .clickable { activeView = view }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${meta.first} ${meta.second}",
                            color = if (isSelected) Color.White else Slate400,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioCarbon,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = StudioPanel,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
            ) {
                if (isLandscape) {
                    // LANDSCAPE: Single compact master toolbar across the wide screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Logo & Style
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LogoGroup()
                            StyleSelectorButton()
                        }

                        // Center: Transport
                        MasterTransportControls()

                        // Right: Views & Export
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StudioViewTabs()
                            ExportButton()
                        }
                    }
                } else {
                    // PORTRAIT: Professional 3-tier organized studio header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Tier 1: Logo, Style Selector, Export
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LogoGroup()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StyleSelectorButton()
                                ExportButton()
                            }
                        }

                        // Tier 2: Master Transport Controls
                        MasterTransportControls()

                        // Tier 3: Studio View Selector Tabs
                        StudioViewTabs()
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .displayCutoutPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isLandscape) 12.dp else 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // View 1: Timeline (Sequencer)
            if (activeView == StudioView.TIMELINE || activeView == StudioView.ALL) {
                val _trigger = muteSoloTrigger

                SequencerGrid(
                    currentStep = currentStep,
                    isPlaying = isPlaying,
                    pattern = pattern,
                    onToggleStep = { t, s -> handleToggleStep(t, s) },
                    onCycleMelodyPitch = { s -> handleCycleMelodyPitch(s) },
                    onClearPattern = { handleClearPattern() },
                    onRandomizePattern = { handleRandomizePattern() },
                    onAuditionTrack = { audioEngine.triggerVoice(it) },
                    mutedTracks = audioEngine.mutedTracks,
                    soloTracks = audioEngine.soloTracks,
                    onToggleMute = {
                        audioEngine.toggleMute(it)
                        muteSoloTrigger++
                    },
                    onToggleSolo = {
                        audioEngine.toggleSolo(it)
                        muteSoloTrigger++
                    },
                    scaleName = Scales.getScale(fx.scaleId).name
                )
            }

            // View 2: Sample & Foley Station
            if (activeView == StudioView.SAMPLE || activeView == StudioView.ALL) {
                if (isLandscape) {
                    // Landscape: Dual-card side-by-side workstation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WaveformDisplay(
                            rawBuffer = audioEngine.rawBuffer,
                            sampleMetadata = sampleMetadata,
                            trimStart = audioEngine.trimStart,
                            trimEnd = audioEngine.trimEnd,
                            onTrimChange = { s, e ->
                                audioEngine.setTrim(s, e)
                                sampleMetadata = audioEngine.sampleMetadata
                            },
                            onAutoDetect = {
                                audioEngine.autoDetectTransient()
                                sampleMetadata = audioEngine.sampleMetadata
                            },
                            onPlayRaw = { audioEngine.playRawAudio() },
                            onPlayTrimmed = { audioEngine.playTrimmedSample() },
                            modifier = Modifier.weight(1.1f)
                        )

                        SoundSourceSelector(
                            sampleMetadata = sampleMetadata,
                            isRecording = isRecording,
                            recordingSeconds = recordingSeconds,
                            recordingRms = audioEngine.recordingRms,
                            statusMessage = statusMessage,
                            onStartRecord = { startRecord() },
                            onStopRecord = { stopRecord() },
                            onSelectDeskPound = {
                                audioEngine.generateSyntheticTableBang()
                                sampleMetadata = audioEngine.sampleMetadata
                                statusMessage = "Loaded Hardwood Desk Pound!"
                            },
                            onSelectMetalHit = {
                                audioEngine.generateSyntheticMetalHit()
                                sampleMetadata = audioEngine.sampleMetadata
                                statusMessage = "Loaded Steel Spoon Clink!"
                            },
                            onSelectKnuckleTap = {
                                audioEngine.generateSyntheticKnuckleTap()
                                sampleMetadata = audioEngine.sampleMetadata
                                statusMessage = "Loaded Finger Snap & Knuckle Tap!"
                            },
                            onSelectGlassPing = {
                                audioEngine.generateSyntheticGlassPing()
                                sampleMetadata = audioEngine.sampleMetadata
                                statusMessage = "Loaded Resonant Glass Bottle!"
                            },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                } else {
                    // Portrait: Stacked
                    WaveformDisplay(
                        rawBuffer = audioEngine.rawBuffer,
                        sampleMetadata = sampleMetadata,
                        trimStart = audioEngine.trimStart,
                        trimEnd = audioEngine.trimEnd,
                        onTrimChange = { s, e ->
                            audioEngine.setTrim(s, e)
                            sampleMetadata = audioEngine.sampleMetadata
                        },
                        onAutoDetect = {
                            audioEngine.autoDetectTransient()
                            sampleMetadata = audioEngine.sampleMetadata
                        },
                        onPlayRaw = { audioEngine.playRawAudio() },
                        onPlayTrimmed = { audioEngine.playTrimmedSample() }
                    )

                    SoundSourceSelector(
                        sampleMetadata = sampleMetadata,
                        isRecording = isRecording,
                        recordingSeconds = recordingSeconds,
                        recordingRms = audioEngine.recordingRms,
                        statusMessage = statusMessage,
                        onStartRecord = { startRecord() },
                        onStopRecord = { stopRecord() },
                        onSelectDeskPound = {
                            audioEngine.generateSyntheticTableBang()
                            sampleMetadata = audioEngine.sampleMetadata
                            statusMessage = "Loaded Hardwood Desk Pound!"
                        },
                        onSelectMetalHit = {
                            audioEngine.generateSyntheticMetalHit()
                            sampleMetadata = audioEngine.sampleMetadata
                            statusMessage = "Loaded Steel Spoon Clink!"
                        },
                        onSelectKnuckleTap = {
                            audioEngine.generateSyntheticKnuckleTap()
                            sampleMetadata = audioEngine.sampleMetadata
                            statusMessage = "Loaded Finger Snap & Knuckle Tap!"
                        },
                        onSelectGlassPing = {
                            audioEngine.generateSyntheticGlassPing()
                            sampleMetadata = audioEngine.sampleMetadata
                            statusMessage = "Loaded Resonant Glass Bottle!"
                        }
                    )
                }
            }

            // View 3: FX Rack
            if (activeView == StudioView.FX || activeView == StudioView.ALL) {
                FxRack(
                    fx = fx,
                    onChangeFx = {
                        fx = it
                        audioEngine.updateFx(it)
                    }
                )
            }

            // View 4: MPC Audition Pads
            if (activeView == StudioView.PADS || activeView == StudioView.ALL) {
                LiveAuditionPads(
                    onTriggerVoice = { audioEngine.triggerVoice(it) }
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        // Export Dialog
        ExportDialog(
            isOpen = isExportOpen,
            onClose = { isExportOpen = false },
            currentStyle = currentStyle,
            bpm = bpm,
            onRenderWav = {
                val preset = Presets.getPreset(currentStyle)
                audioEngine.renderWav(preset, fx, pattern, bpm)
            }
        )
    }
}
