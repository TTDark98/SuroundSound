package com.surroundsound.app.ui.screens.recorder

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.surroundsound.app.core.audio.PlaybackState
import com.surroundsound.app.domain.model.SourceAudio
import com.surroundsound.app.ui.components.ForegroundPlaybackLifecycleListener
import com.surroundsound.app.ui.components.InteractiveWaveform
import com.surroundsound.app.ui.theme.AcidGreen
import com.surroundsound.app.ui.theme.CrimsonAlert
import com.surroundsound.app.ui.theme.DarkBackground
import com.surroundsound.app.ui.theme.ElectricAmber
import com.surroundsound.app.ui.theme.NeonCyan
import com.surroundsound.app.ui.theme.SurfaceBorder
import com.surroundsound.app.ui.theme.SurfaceDark
import com.surroundsound.app.ui.theme.SurfaceElevated
import com.surroundsound.app.ui.theme.TextMuted
import com.surroundsound.app.ui.theme.TextPrimary
import com.surroundsound.app.ui.theme.TextSecondary
import com.surroundsound.app.ui.theme.VibrantPurple
import java.util.Locale

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    onNavigateToSoundDna: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    // Foreground lifecycle listener strictly halting playback when app leaves foreground or locks
    ForegroundPlaybackLifecycleListener {
        viewModel.haltPlayback()
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.startRecording()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importAudioFile(it, "Imported Audio")
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SURROUNDSOUND",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Source Sound Capture",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Every beat is strictly created from your raw sound (max 10.0s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Central Interactive Section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is RecorderUiState.Idle -> {
                        IdleCaptureView(
                            onRecordClick = {
                                if (hasPermission) {
                                    viewModel.startRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onImportClick = {
                                filePickerLauncher.launch("audio/*")
                            }
                        )
                    }

                    is RecorderUiState.Recording -> {
                        ActiveRecordingView(
                            elapsedSeconds = state.elapsedSeconds,
                            currentRms = state.currentRms,
                            isWarning = state.isWarning,
                            onStopClick = { viewModel.stopRecording() }
                        )
                    }

                    is RecorderUiState.Decoding -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Decoding ${state.fileName}...",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Extracting offline PCM audio at 44.1 kHz",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is RecorderUiState.TrimmingRequired -> {
                        RangeSelectorDialog(
                            totalDurationSeconds = state.totalDurationSeconds,
                            onTrimConfirmed = { startSec ->
                                viewModel.applyTrim(startSec)
                            },
                            onCancel = { viewModel.reset() }
                        )
                    }

                    is RecorderUiState.AudioReady -> {
                        AudioCapturedPlaybackView(
                            sourceAudio = state.sourceAudio,
                            playbackState = playbackState,
                            onTogglePlay = { viewModel.togglePlayback(state.sourceAudio) },
                            onSeek = { progress -> viewModel.seekPlayback(progress, state.sourceAudio) },
                            onDiscard = { viewModel.reset() },
                            onProceed = { onNavigateToSoundDna(state.sourceAudio.id) }
                        )
                    }

                    is RecorderUiState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = CrimsonAlert,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Capture Error",
                                color = CrimsonAlert,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.reset() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                            ) {
                                Text("Try Again", color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Bottom Footer
            Text(
                text = "100% Offline • Zero synthetic instruments • Zero cloud APIs",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
fun IdleCaptureView(
    onRecordClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Record Button
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(2.dp, NeonCyan, CircleShape)
                .clickable { onRecordClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record",
                    tint = NeonCyan,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "TAP TO RECORD (MAX 10s)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Or Import from SAF
        OutlinedButton(
            onClick = onImportClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VibrantPurple
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AudioFile,
                contentDescription = "Import",
                tint = VibrantPurple
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Import Audio File (WAV / MP3 / AAC)",
                style = MaterialTheme.typography.bodyMedium,
                color = VibrantPurple,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ActiveRecordingView(
    elapsedSeconds: Float,
    currentRms: Float,
    isWarning: Boolean,
    onStopClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val activeColor = if (isWarning) ElectricAmber else NeonCyan

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Warning Banner
        AnimatedVisibility(visible = isWarning) {
            Surface(
                color = ElectricAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricAmber),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = ElectricAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto-stopping at 10.0 seconds!",
                        color = ElectricAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Live Timer
        Text(
            text = String.format(Locale.US, "00:%04.1f / 10.0s", elapsedSeconds),
            fontFamily = FontFamily.Monospace,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = activeColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { (elapsedSeconds / 10.0f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = activeColor,
            trackColor = SurfaceElevated
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Live Audio Level Visualizer
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(36.dp)
        ) {
            val barCount = 16
            val barWidth = size.width / (barCount * 1.5f)
            val centerY = size.height / 2f
            for (i in 0 until barCount) {
                val factor = (i.toFloat() / barCount.toFloat())
                val height = (size.height * (currentRms * 1.8f) * (1.0f - factor * 0.4f)).coerceIn(4f, size.height)
                val x = i * (barWidth * 1.5f)
                drawLine(
                    color = activeColor,
                    start = Offset(x, centerY - height / 2f),
                    end = Offset(x, centerY + height / 2f),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Glowing Stop Button
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(activeColor.copy(alpha = pulseAlpha * 0.3f))
                .border(2.dp, activeColor, CircleShape)
                .clickable { onStopClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(activeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = DarkBackground,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "TAP TO FINISH EARLY",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun AudioCapturedPlaybackView(
    sourceAudio: SourceAudio,
    playbackState: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onDiscard: () -> Unit,
    onProceed: () -> Unit
) {
    val isPlaying = playbackState is PlaybackState.Playing
    val progress = when (playbackState) {
        is PlaybackState.Playing -> playbackState.progressPercent
        is PlaybackState.Paused -> playbackState.progressPercent
        else -> 0f
    }
    val currentPositionSeconds = when (playbackState) {
        is PlaybackState.Playing -> playbackState.currentPositionSeconds
        is PlaybackState.Paused -> playbackState.currentPositionSeconds
        else -> 0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = AcidGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sourceAudio.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = String.format(Locale.US, "%.1fs", sourceAudio.durationSeconds),
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Waveform with animated playhead
                InteractiveWaveform(
                    samples = sourceAudio.samples,
                    progressPercent = progress,
                    playedColor = NeonCyan,
                    unplayedColor = SurfaceElevated,
                    playheadColor = ElectricAmber,
                    onSeek = onSeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "00:%04.1f / 00:%04.1f",
                            currentPositionSeconds,
                            sourceAudio.durationSeconds
                        ),
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Play/Pause Button
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DarkBackground,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(0.95f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDiscard,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Re-record",
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Re-record")
            }

            Button(
                onClick = onProceed,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(
                    text = "Sound DNA",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RangeSelectorDialog(
    totalDurationSeconds: Float,
    onTrimConfirmed: (Float) -> Unit,
    onCancel: () -> Unit
) {
    var startSeconds by remember { mutableFloatStateOf(0f) }
    val maxStart = (totalDurationSeconds - 10.0f).coerceAtLeast(0f)

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Select 10-Second Window",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "This file is ${String.format(Locale.US, "%.1f", totalDurationSeconds)}s long. SurroundSound strictly requires a maximum 10.0-second source audio window.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Selected Window: ${String.format(Locale.US, "%.1fs - %.1fs", startSeconds, (startSeconds + 10.0f).coerceAtMost(totalDurationSeconds))}",
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = startSeconds,
                    onValueChange = { startSeconds = it },
                    valueRange = 0f..maxStart,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = SurfaceElevated
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onTrimConfirmed(startSeconds) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Confirm 10s Window", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}
