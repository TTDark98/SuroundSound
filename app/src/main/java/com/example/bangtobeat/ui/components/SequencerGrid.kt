package com.example.bangtobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangtobeat.model.PatternState
import com.example.bangtobeat.model.TrackId
import com.example.bangtobeat.theme.*

@Composable
fun SequencerGrid(
    currentStep: Int,
    isPlaying: Boolean,
    pattern: PatternState,
    onToggleStep: (TrackId, Int) -> Unit,
    onCycleMelodyPitch: (Int) -> Unit,
    onClearPattern: () -> Unit,
    onRandomizePattern: () -> Unit,
    onAuditionTrack: (TrackId) -> Unit,
    mutedTracks: Set<TrackId>,
    soloTracks: Set<TrackId>,
    onToggleMute: (TrackId) -> Unit,
    onToggleSolo: (TrackId) -> Unit,
    scaleName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                // Landscape: Single row toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) NeonGreen else Slate600)
                        )
                        Text(
                            text = "16-STEP MASTER TIMELINE",
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioPanelLight)
                                .border(1.dp, StudioPanelBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Scale: $scaleName",
                                color = NeonViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRandomizePattern,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPanelLight,
                                contentColor = Slate200
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Randomize", modifier = Modifier.size(14.dp), tint = NeonCyan)
                            Spacer(Modifier.width(6.dp))
                            Text("Randomize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onClearPattern,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPanelLight,
                                contentColor = Slate200
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(14.dp), tint = NeonRed)
                            Spacer(Modifier.width(6.dp))
                            Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Portrait: 2-tier toolbar to prevent any button text squashing
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) NeonGreen else Slate600)
                            )
                            Text(
                                text = "16-STEP MASTER TIMELINE",
                                color = Slate100,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioPanelLight)
                                .border(1.dp, StudioPanelBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Scale: $scaleName",
                                color = NeonViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRandomizePattern,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPanelLight,
                                contentColor = Slate200
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Randomize", modifier = Modifier.size(14.dp), tint = NeonCyan)
                            Spacer(Modifier.width(6.dp))
                            Text("Randomize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onClearPattern,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPanelLight,
                                contentColor = Slate200
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(14.dp), tint = NeonRed)
                            Spacer(Modifier.width(6.dp))
                            Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Sequencer Channel Strips & Grid (Horizontally scrollable if screen is narrower)
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCarbon)
                    .border(1.dp, StudioPanelBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.horizontalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Step Numbers Timeline Bar (Grouped into 4 Beats)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Track header spacer
                        Box(modifier = Modifier.width(130.dp))

                        for (step in 0 until 16) {
                            val isPlayhead = step == currentStep
                            val isDownbeat = step % 4 == 0
                            val beatGroup = step / 4

                            Box(
                                modifier = Modifier
                                    .size(36.dp, 22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isPlayhead) NeonGreen
                                        else if (isDownbeat) StudioPanelBorder
                                        else if (beatGroup % 2 == 0) StudioStepBeatBg
                                        else StudioStepBg
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step + 1}",
                                    color = if (isPlayhead) StudioCarbon else if (isDownbeat) Color.White else Slate400,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isPlayhead || isDownbeat) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // 1. Kick Channel
                    ChannelStripRow(
                        trackId = TrackId.KICK,
                        trackName = "KICK",
                        accentColor = NeonGreen,
                        steps = pattern.kick,
                        currentStep = currentStep,
                        isMuted = mutedTracks.contains(TrackId.KICK),
                        isSolo = soloTracks.contains(TrackId.KICK),
                        onToggleMute = { onToggleMute(TrackId.KICK) },
                        onToggleSolo = { onToggleSolo(TrackId.KICK) },
                        onAudition = { onAuditionTrack(TrackId.KICK) },
                        onToggleStep = { onToggleStep(TrackId.KICK, it) }
                    )

                    // 2. Snare Channel
                    ChannelStripRow(
                        trackId = TrackId.SNARE,
                        trackName = "SNARE",
                        accentColor = NeonAmber,
                        steps = pattern.snare,
                        currentStep = currentStep,
                        isMuted = mutedTracks.contains(TrackId.SNARE),
                        isSolo = soloTracks.contains(TrackId.SNARE),
                        onToggleMute = { onToggleMute(TrackId.SNARE) },
                        onToggleSolo = { onToggleSolo(TrackId.SNARE) },
                        onAudition = { onAuditionTrack(TrackId.SNARE) },
                        onToggleStep = { onToggleStep(TrackId.SNARE, it) }
                    )

                    // 3. Hi-Hat Channel
                    ChannelStripRow(
                        trackId = TrackId.HIHAT,
                        trackName = "HI-HAT",
                        accentColor = NeonCyan,
                        steps = pattern.hihat,
                        currentStep = currentStep,
                        isMuted = mutedTracks.contains(TrackId.HIHAT),
                        isSolo = soloTracks.contains(TrackId.HIHAT),
                        onToggleMute = { onToggleMute(TrackId.HIHAT) },
                        onToggleSolo = { onToggleSolo(TrackId.HIHAT) },
                        onAudition = { onAuditionTrack(TrackId.HIHAT) },
                        onToggleStep = { onToggleStep(TrackId.HIHAT, it) }
                    )

                    // 4. Perc Channel
                    ChannelStripRow(
                        trackId = TrackId.PERC,
                        trackName = "PERC",
                        accentColor = Color(0xFFFFD600),
                        steps = pattern.perc,
                        currentStep = currentStep,
                        isMuted = mutedTracks.contains(TrackId.PERC),
                        isSolo = soloTracks.contains(TrackId.PERC),
                        onToggleMute = { onToggleMute(TrackId.PERC) },
                        onToggleSolo = { onToggleSolo(TrackId.PERC) },
                        onAudition = { onAuditionTrack(TrackId.PERC) },
                        onToggleStep = { onToggleStep(TrackId.PERC, it) }
                    )

                    // 5. Melody Channel (With Pitch Cycler in each step)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TrackHeader(
                            name = "MELODY",
                            color = NeonViolet,
                            isMuted = mutedTracks.contains(TrackId.MELODY),
                            isSolo = soloTracks.contains(TrackId.MELODY),
                            onToggleMute = { onToggleMute(TrackId.MELODY) },
                            onToggleSolo = { onToggleSolo(TrackId.MELODY) },
                            onAudition = { onAuditionTrack(TrackId.MELODY) }
                        )

                        for (step in 0 until 16) {
                            val melStep = pattern.melody.getOrNull(step)
                            val isActive = melStep?.active == true
                            val isPlayhead = step == currentStep
                            val isDownbeat = step % 4 == 0

                            val melBg = if (isActive) NeonViolet
                            else if (isPlayhead) NeonViolet.copy(alpha = 0.25f)
                            else if (isDownbeat) StudioStepBeatBg
                            else StudioStepBg

                            Box(
                                modifier = Modifier
                                    .size(36.dp, 36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(melBg)
                                    .border(
                                        1.5.dp,
                                        if (isPlayhead) Color.White
                                        else if (isActive) NeonViolet
                                        else StudioPanelBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        if (isActive) {
                                            onCycleMelodyPitch(step)
                                        } else {
                                            onToggleStep(TrackId.MELODY, step)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    val pitch = melStep.pitch
                                    val label = if (pitch > 0) "+$pitch" else if (pitch == 0) "Root" else "$pitch"
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // 6. Bass Channel
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TrackHeader(
                            name = "BASS",
                            color = NeonPink,
                            isMuted = mutedTracks.contains(TrackId.BASS),
                            isSolo = soloTracks.contains(TrackId.BASS),
                            onToggleMute = { onToggleMute(TrackId.BASS) },
                            onToggleSolo = { onToggleSolo(TrackId.BASS) },
                            onAudition = { onAuditionTrack(TrackId.BASS) }
                        )

                        for (step in 0 until 16) {
                            val bassStep = pattern.bass.getOrNull(step)
                            val isActive = bassStep?.active == true
                            val isPlayhead = step == currentStep
                            val isDownbeat = step % 4 == 0

                            val bassBg = if (isActive) NeonPink
                            else if (isPlayhead) NeonPink.copy(alpha = 0.25f)
                            else if (isDownbeat) StudioStepBeatBg
                            else StudioStepBg

                            Box(
                                modifier = Modifier
                                    .size(36.dp, 36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bassBg)
                                    .border(
                                        1.5.dp,
                                        if (isPlayhead) Color.White
                                        else if (isActive) NeonPink
                                        else StudioPanelBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        onToggleStep(TrackId.BASS, step)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelStripRow(
    trackId: TrackId,
    trackName: String,
    accentColor: Color,
    steps: BooleanArray,
    currentStep: Int,
    isMuted: Boolean,
    isSolo: Boolean,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onAudition: () -> Unit,
    onToggleStep: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TrackHeader(
            name = trackName,
            color = accentColor,
            isMuted = isMuted,
            isSolo = isSolo,
            onToggleMute = onToggleMute,
            onToggleSolo = onToggleSolo,
            onAudition = onAudition
        )

        for (step in 0 until 16) {
            val isActive = steps.getOrNull(step) == true
            val isPlayhead = step == currentStep
            val isDownbeat = step % 4 == 0

            val stepBg = if (isActive) accentColor
            else if (isPlayhead) accentColor.copy(alpha = 0.25f)
            else if (isDownbeat) StudioStepBeatBg
            else StudioStepBg

            Box(
                modifier = Modifier
                    .size(36.dp, 36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(stepBg)
                    .border(
                        1.5.dp,
                        if (isPlayhead) Color.White
                        else if (isActive) accentColor
                        else StudioPanelBorder,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggleStep(step) },
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackHeader(
    name: String,
    color: Color,
    isMuted: Boolean,
    isSolo: Boolean,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onAudition: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(StudioPanel)
            .border(1.dp, StudioPanelBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LED Tag & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onAudition() }
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Mute / Solo buttons
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                // Mute
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isMuted) NeonRed else StudioPanelBorder)
                        .clickable { onToggleMute() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        color = if (isMuted) Color.White else Slate400,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Solo
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSolo) NeonAmber else StudioPanelBorder)
                        .clickable { onToggleSolo() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        color = if (isSolo) StudioCarbon else Slate400,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
