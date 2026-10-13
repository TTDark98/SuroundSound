package com.surroundsound.app.ui.screens.dna

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surroundsound.app.core.audio.PlaybackState
import com.surroundsound.app.domain.dna.SoundDna
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoundDnaScreen(
    viewModel: SoundDnaViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToModes: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    ForegroundPlaybackLifecycleListener {
        viewModel.haltPlayback()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        when (val state = uiState) {
            is SoundDnaUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Extracting Sound DNA...", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("Running offline FFT & transient analysis", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            is SoundDnaUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.message, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onNavigateBack) { Text("Go Back", color = NeonCyan) }
                    }
                }
            }

            is SoundDnaUiState.Success -> {
                SoundDnaSuccessView(
                    sourceAudio = state.sourceAudio,
                    dna = state.soundDna,
                    calculationTimeMs = state.calculationTimeMs,
                    primaryScore = state.primaryScore,
                    playbackState = playbackState,
                    onTogglePlay = { viewModel.togglePlayback(state.sourceAudio) },
                    onSeek = { progress -> viewModel.seekPlayback(progress, state.sourceAudio) },
                    onNavigateBack = onNavigateBack,
                    onProceedToModes = { onNavigateToModes(state.sourceAudio.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundDnaSuccessView(
    sourceAudio: SourceAudio,
    dna: SoundDna,
    calculationTimeMs: Long,
    primaryScore: Float,
    playbackState: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onNavigateBack: () -> Unit,
    onProceedToModes: () -> Unit
) {
    val scrollState = rememberScrollState()
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "SOUND DNA PROFILE",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                letterSpacing = 2.sp
            )
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${calculationTimeMs}ms",
                    color = AcidGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title and Source Summary
        Text(
            text = sourceAudio.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "${String.format(Locale.US, "%.2f", sourceAudio.durationSeconds)}s • 44.1 kHz Mono • Pure Source Audio",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quality Warning Banner if clipped or silent
        if (dna.isClipped || dna.isSilent) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (dna.isSilent) CrimsonAlert.copy(alpha = 0.15f) else ElectricAmber.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (dna.isSilent) CrimsonAlert else ElectricAmber
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (dna.isSilent) CrimsonAlert else ElectricAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (dna.isSilent) "Near-Silence Detected" else "Digital Clipping Warning",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (dna.isSilent)
                                "Audio energy is extremely low. You may proceed, but beats may lack definition."
                            else
                                "Peak amplitudes exceed ceiling. Soft clipping will preserve texture.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Heuristic Badges Row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dna.badges.forEachIndexed { index, badge ->
                val badgeColor = when (index % 3) {
                    0 -> NeonCyan
                    1 -> VibrantPurple
                    else -> ElectricAmber
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badge,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Audio Preview Waveform & Scrubber
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InteractiveWaveform(
                    samples = sourceAudio.samples,
                    progressPercent = progress,
                    playedColor = NeonCyan,
                    unplayedColor = SurfaceElevated,
                    playheadColor = ElectricAmber,
                    onSeek = onSeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "00:%04.1f / 00:%04.1f", currentPositionSeconds, sourceAudio.durationSeconds),
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DarkBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Title: Acoustic Features
        Text(
            text = "EXTRACTED ACOUSTIC METRICS",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 6 Sound DNA Metric Cards (2-column grid layout)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "RMS Energy",
                    value = String.format(Locale.US, "%.0f%%", dna.rmsEnergy * 100f),
                    subtitle = "Acoustic Power",
                    icon = Icons.Default.Bolt,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    progress = dna.rmsEnergy
                )
                MetricCard(
                    title = "Transient Strength",
                    value = String.format(Locale.US, "%.0f%%", dna.transientStrength * 100f),
                    subtitle = if (dna.transientStrength > 0.6f) "Sharp Attack" else "Smooth Onset",
                    icon = Icons.Default.Speed,
                    accentColor = ElectricAmber,
                    modifier = Modifier.weight(1f),
                    progress = dna.transientStrength
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Brightness",
                    value = "${dna.spectralCentroid.toInt()} Hz",
                    subtitle = "Spectral Centroid",
                    icon = Icons.Default.Tune,
                    accentColor = VibrantPurple,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Dominant Pitch",
                    value = "${dna.dominantFrequency.toInt()} Hz",
                    subtitle = "Fundamental Peak",
                    icon = Icons.Default.MusicNote,
                    accentColor = AcidGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Decay Time",
                    value = "${dna.decayTimeMs.toInt()} ms",
                    subtitle = if (dna.decayTimeMs < 200f) "Staccato" else "Resonant Tail",
                    icon = Icons.Default.Timer,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Spectral Flux",
                    value = String.format(Locale.US, "%.2f", dna.spectralFlux),
                    subtitle = if (dna.spectralFlux > 2.0f) "Evolving / Noisy" else "Stable Tone",
                    icon = Icons.Default.Waves,
                    accentColor = VibrantPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Source Recommendation Score
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AcidGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Primary Groove Anchor", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Drives beat generation & timing", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                Text(
                    text = "${primaryScore.toInt()} / 100",
                    color = AcidGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // CTA Button to Next Phase (Creative Modes)
        Button(
            onClick = onProceedToModes,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Select Creative Mode",
                color = DarkBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DarkBackground)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)

            if (progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = SurfaceElevated
                )
            }
        }
    }
}
