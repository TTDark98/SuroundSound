package com.example.bangtobeat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangtobeat.model.TrackId
import com.example.bangtobeat.ui.theme.*

@Composable
fun LiveAuditionPads(
    onTriggerVoice: (TrackId) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
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
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(NeonPink)
                    )
                    Text(
                        text = "LIVE MPC AUDITION DRUM PADS",
                        color = Slate100,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Zero-latency real-time impact finger drumming",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                // Landscape: 2 Rows of 3 Large Drum Pads
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MpcPad(
                            title = "KICK",
                            subtitle = "Sub Pound",
                            accentColor = NeonGreen,
                            onClick = { onTriggerVoice(TrackId.KICK) },
                            modifier = Modifier.weight(1f)
                        )
                        MpcPad(
                            title = "SNARE",
                            subtitle = "Crack Bite",
                            accentColor = NeonAmber,
                            onClick = { onTriggerVoice(TrackId.SNARE) },
                            modifier = Modifier.weight(1f)
                        )
                        MpcPad(
                            title = "HI-HAT",
                            subtitle = "Crisp Metal",
                            accentColor = NeonCyan,
                            onClick = { onTriggerVoice(TrackId.HIHAT) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MpcPad(
                            title = "PERC",
                            subtitle = "Wood Rim",
                            accentColor = Color(0xFFFFD600),
                            onClick = { onTriggerVoice(TrackId.PERC) },
                            modifier = Modifier.weight(1f)
                        )
                        MpcPad(
                            title = "MELODY",
                            subtitle = "Tuned Resonator",
                            accentColor = NeonViolet,
                            onClick = { onTriggerVoice(TrackId.MELODY) },
                            modifier = Modifier.weight(1f)
                        )
                        MpcPad(
                            title = "BASS",
                            subtitle = "Sub 808",
                            accentColor = NeonPink,
                            onClick = { onTriggerVoice(TrackId.BASS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Portrait: 3 Rows of 2 Large Drum Pads (Spacious finger drumming)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MpcPad(
                            title = "KICK",
                            subtitle = "Sub Pound",
                            accentColor = NeonGreen,
                            onClick = { onTriggerVoice(TrackId.KICK) },
                            modifier = Modifier.weight(1f)
                        )
                        MpcPad(
                            title = "SNARE",
                            subtitle = "Crack Bite",
                            accentColor = NeonAmber,
                            onClick = { onTriggerVoice(TrackId.SNARE) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MpcPad(
                            title = "HI-HAT",
                            subtitle = "Crisp Metal",
                            accentColor = NeonCyan,
                            onClick = { onTriggerVoice(TrackId.HIHAT) },
                            modifier = Modifier.weight(1f)
                        )
                        MpcPad(
                            title = "PERC",
                            subtitle = "Wood Rim",
                            accentColor = Color(0xFFFFD600),
                            onClick = { onTriggerVoice(TrackId.PERC) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MpcPad(
                            title = "MELODY",
                            subtitle = "Tuned Resonator",
                            accentColor = NeonViolet,
                            onClick = { onTriggerVoice(TrackId.MELODY) },
                            modifier = Modifier.weight(1f)
                        )
                        MpcPad(
                            title = "BASS",
                            subtitle = "Sub 808",
                            accentColor = NeonPink,
                            onClick = { onTriggerVoice(TrackId.BASS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MpcPad(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1.0f, label = "padScale")

    Box(
        modifier = modifier
            .height(75.dp)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isPressed) Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.5f)))
                else Brush.verticalGradient(listOf(StudioCarbon, StudioPanelLight))
            )
            .border(
                2.dp,
                if (isPressed) Color.White else accentColor.copy(alpha = 0.6f),
                RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                onClick()
            }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = if (isPressed) StudioCarbon else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = subtitle,
                color = if (isPressed) StudioCarbon else accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
