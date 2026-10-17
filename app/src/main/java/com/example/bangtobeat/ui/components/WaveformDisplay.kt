package com.example.bangtobeat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangtobeat.model.SampleMetadata
import com.example.bangtobeat.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

@Composable
fun WaveformDisplay(
    rawBuffer: FloatArray?,
    sampleMetadata: SampleMetadata?,
    trimStart: Float,
    trimEnd: Float,
    onTrimChange: (Float, Float) -> Unit,
    onAutoDetect: () -> Unit,
    onPlayRaw: () -> Unit,
    onPlayTrimmed: () -> Unit,
    isPlayingRaw: Boolean = false,
    modifier: Modifier = Modifier
) {
    val duration = sampleMetadata?.rawDuration ?: 0.85f

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
            val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                // Landscape: Single row header
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
                                .background(NeonCyan)
                        )
                        Text(
                            text = "AUDIO WAVEFORM & TRANSIENT SLICER",
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "• ${sampleMetadata?.sourceName ?: "Audio Impact"} (${String.format("%.2f", sampleMetadata?.duration ?: 0.45f)}s)",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onAutoDetect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPanelLight,
                                contentColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Auto", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Auto Transient", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPlayRaw,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPanelLight,
                                contentColor = Slate300
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
                        ) {
                            Icon(if (isPlayingRaw) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play Raw", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPlayTrimmed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play Trimmed", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else {
                // Portrait: 2-tier header preventing button compression
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
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NeonCyan)
                            )
                            Text(
                                text = "WAVEFORM TRANSIENT SLICER",
                                color = Slate100,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "${sampleMetadata?.sourceName ?: "Impact"} (${String.format("%.2f", sampleMetadata?.duration ?: 0.45f)}s)",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onAutoDetect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPanelLight,
                                contentColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Auto", modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Auto Slicer", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = onPlayRaw,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPanelLight,
                                contentColor = Slate300
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (isPlayingRaw) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Play Raw", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = onPlayTrimmed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Trimmed", fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                    }
                }
            }

            // High-resolution Waveform Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCarbon)
                    .border(1.dp, StudioPanelBorder, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // Grid centerline
                    drawLine(
                        color = Color(0xFF1E283D),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1f
                    )

                    // Draw waveform bars
                    if (rawBuffer != null && rawBuffer.isNotEmpty()) {
                        val numBars = 160
                        val step = max(1, rawBuffer.size / numBars)
                        val barWidth = (w / numBars) * 0.75f

                        for (i in 0 until numBars) {
                            val idx = (i * step).coerceIn(0, rawBuffer.size - 1)
                            var peak = 0f
                            for (j in 0 until step) {
                                val sampleIdx = idx + j
                                if (sampleIdx < rawBuffer.size) {
                                    val a = abs(rawBuffer[sampleIdx])
                                    if (a > peak) peak = a
                                }
                            }

                            val barHeight = (peak * (h * 0.88f)).coerceAtLeast(2f)
                            val x = i * (w / numBars) + (w / numBars - barWidth) / 2f
                            val timeAtX = (i.toFloat() / numBars) * duration

                            val isInTrim = timeAtX >= trimStart && timeAtX <= trimEnd
                            val barColor = if (isInTrim) NeonCyan else Slate700

                            drawRect(
                                color = barColor,
                                topLeft = Offset(x, midY - barHeight / 2f),
                                size = Size(barWidth, barHeight)
                            )
                        }
                    }

                    // Shaded Trim Boundaries
                    val trimStartX = (trimStart / duration) * w
                    val trimEndX = (trimEnd / duration) * w

                    // Dim outside regions
                    drawRect(
                        color = Color(0x88000000),
                        topLeft = Offset(0f, 0f),
                        size = Size(trimStartX, h)
                    )
                    drawRect(
                        color = Color(0x88000000),
                        topLeft = Offset(trimEndX, 0f),
                        size = Size(w - trimEndX, h)
                    )

                    // Trim Boundary Lines
                    drawLine(
                        color = NeonGreen,
                        start = Offset(trimStartX, 0f),
                        end = Offset(trimStartX, h),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = NeonPink,
                        start = Offset(trimEndX, 0f),
                        end = Offset(trimEndX, h),
                        strokeWidth = 3f
                    )
                }

                // Trim metrics overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "START: ${String.format("%.3f", trimStart)}s",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DURATION: ${String.format("%.0f", (trimEnd - trimStart) * 1000)}ms",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "END: ${String.format("%.3f", trimEnd)}s",
                        color = NeonPink,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dual Trim Range Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trim Start Slider
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("IN", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Slider(
                        value = trimStart,
                        onValueChange = {
                            if (it < trimEnd - 0.02f) onTrimChange(it, trimEnd)
                        },
                        valueRange = 0f..max(0.1f, duration - 0.02f),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen
                        )
                    )
                }

                // Trim End Slider
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("OUT", color = NeonPink, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Slider(
                        value = trimEnd,
                        onValueChange = {
                            if (it > trimStart + 0.02f) onTrimChange(trimStart, it)
                        },
                        valueRange = 0.02f..duration,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPink,
                            activeTrackColor = NeonPink
                        )
                    )
                }
            }
        }
    }
}
