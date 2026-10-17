package com.example.bangtobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangtobeat.model.BeatStyle
import com.example.bangtobeat.model.Presets
import com.example.bangtobeat.ui.theme.*

@Composable
fun GenreSelector(
    currentStyle: BeatStyle,
    onSelectStyle: (BeatStyle) -> Unit,
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val activePreset = Presets.getPreset(currentStyle)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with BPM Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Groove & Style Selection",
                        color = Slate100,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Instant production patterns, swing & tempo mapping",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }

                // BPM adjuster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate950)
                        .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = { onBpmChange((bpm - 2).coerceAtLeast(60)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease BPM", tint = Slate300, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "$bpm BPM",
                        color = Indigo500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { onBpmChange((bpm + 2).coerceAtMost(180)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase BPM", tint = Slate300, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Horizontal scrolling preset pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Presets.ALL_PRESETS.forEach { preset ->
                    val isSelected = preset.id == currentStyle

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Indigo600 else Slate950)
                            .border(
                                1.dp,
                                if (isSelected) Indigo500 else Slate800,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectStyle(preset.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    color = if (isSelected) Color.White else Slate200,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${preset.bpm} bpm",
                                    color = if (isSelected) Slate300 else Slate400,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Selected Preset Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate950)
                    .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = activePreset.tagline,
                        color = Indigo500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = activePreset.description,
                        color = Slate400,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
