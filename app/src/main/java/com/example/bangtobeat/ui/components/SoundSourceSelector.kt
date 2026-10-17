package com.example.bangtobeat.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangtobeat.model.SampleMetadata
import com.example.bangtobeat.ui.theme.*

@Composable
fun SoundSourceSelector(
    sampleMetadata: SampleMetadata?,
    isRecording: Boolean,
    recordingSeconds: Int,
    recordingRms: Float,
    statusMessage: String,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSelectDeskPound: () -> Unit,
    onSelectMetalHit: () -> Unit,
    onSelectKnuckleTap: () -> Unit,
    onSelectGlassPing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val curType = sampleMetadata?.sourceType ?: "preset-table"

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
            // Header
            if (isLandscape) {
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
                                .background(NeonAmber)
                        )
                        Text(
                            text = "AUDIO INPUT & ACOUSTIC FOLEY LIBRARY",
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "• $statusMessage",
                        color = if (isRecording) NeonRed else NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(NeonAmber)
                        )
                        Text(
                            text = "AUDIO INPUT & FOLEY LIBRARY",
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "• $statusMessage",
                        color = if (isRecording) NeonRed else NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            @Composable
            fun RecordingStation(boxModifier: Modifier = Modifier) {
                Box(
                    modifier = boxModifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isRecording) Brush.horizontalGradient(listOf(Color(0xFF3F0B18), Color(0xFF1E0A16)))
                            else Brush.horizontalGradient(listOf(StudioCarbon, StudioPanelLight))
                        )
                        .border(
                            1.dp,
                            if (isRecording) NeonRed else StudioPanelBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pulsing Record Button
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .then(if (isRecording) Modifier.scale(pulseScale) else Modifier)
                                .clip(CircleShape)
                                .background(if (isRecording) NeonRed else Indigo600)
                                .clickable {
                                    if (isRecording) onStopRecord() else onStartRecord()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Record",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRecording) "RECORDING (${recordingSeconds}s)..." else "Record Audio Impact",
                                color = if (isRecording) NeonRed else Slate100,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = if (isRecording) "Pound desk or tap object now!" else "Captures transient & tunes 6 drum voices",
                                color = Slate400,
                                fontSize = 10.sp,
                                maxLines = 2
                            )

                            if (isRecording) {
                                Spacer(Modifier.height(5.dp))
                                LinearProgressIndicator(
                                    progress = { recordingRms.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = NeonRed,
                                    trackColor = StudioPanelBorder
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isRecording) onStopRecord() else onStartRecord()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) NeonRed else Indigo600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(if (isRecording) "STOP" else "RECORD", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            if (isLandscape) {
                // LANDSCAPE: Recording station on left, 4 Foley Cards on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RecordingStation(boxModifier = Modifier.weight(1.2f))

                    Row(
                        modifier = Modifier.weight(1.8f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FoleyPill(
                            title = "Desk Pound",
                            subtitle = "Sub Knock",
                            accentColor = NeonGreen,
                            isSelected = curType == "preset-table",
                            onClick = onSelectDeskPound,
                            modifier = Modifier.weight(1f)
                        )

                        FoleyPill(
                            title = "Steel Spoon",
                            subtitle = "Chime Hit",
                            accentColor = NeonCyan,
                            isSelected = curType == "preset-metal",
                            onClick = onSelectMetalHit,
                            modifier = Modifier.weight(1f)
                        )

                        FoleyPill(
                            title = "Finger Snap",
                            subtitle = "Sharp Tap",
                            accentColor = NeonAmber,
                            isSelected = curType == "preset-tap",
                            onClick = onSelectKnuckleTap,
                            modifier = Modifier.weight(1f)
                        )

                        FoleyPill(
                            title = "Glass Bottle",
                            subtitle = "Resonant Ping",
                            accentColor = NeonViolet,
                            isSelected = curType == "preset-glass",
                            onClick = onSelectGlassPing,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // PORTRAIT: Full-width recording station + 2x2 Foley Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecordingStation(boxModifier = Modifier.fillMaxWidth())

                    // 2x2 Grid for Foley Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FoleyPill(
                            title = "Desk Pound",
                            subtitle = "Sub Knock",
                            accentColor = NeonGreen,
                            isSelected = curType == "preset-table",
                            onClick = onSelectDeskPound,
                            modifier = Modifier.weight(1f)
                        )

                        FoleyPill(
                            title = "Steel Spoon",
                            subtitle = "Chime Hit",
                            accentColor = NeonCyan,
                            isSelected = curType == "preset-metal",
                            onClick = onSelectMetalHit,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FoleyPill(
                            title = "Finger Snap",
                            subtitle = "Sharp Tap",
                            accentColor = NeonAmber,
                            isSelected = curType == "preset-tap",
                            onClick = onSelectKnuckleTap,
                            modifier = Modifier.weight(1f)
                        )

                        FoleyPill(
                            title = "Glass Bottle",
                            subtitle = "Resonant Ping",
                            accentColor = NeonViolet,
                            isSelected = curType == "preset-glass",
                            onClick = onSelectGlassPing,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoleyPill(
    title: String,
    subtitle: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else StudioCarbon)
            .border(
                1.5.dp,
                if (isSelected) accentColor else StudioPanelBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Slate200,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (isSelected) accentColor else Slate400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
