package com.example.bangtobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.bangtobeat.theme.*

enum class StudioTab(val label: String) {
    STUDIO("Studio"),
    FX("FX Rack"),
    PADS("Audition Pads"),
    ALL("All Views")
}

@Composable
fun MobileBottomBar(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    isRecording: Boolean,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    bpm: Int,
    currentStyle: BeatStyle,
    activeTab: StudioTab,
    onSelectTab: (StudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Slate950.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tab Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudioTab.values().forEach { tab ->
                    val isSelected = tab == activeTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Indigo600 else Slate900)
                            .border(1.dp, if (isSelected) Indigo500 else Slate800, RoundedCornerShape(8.dp))
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            color = if (isSelected) Color.White else Slate400,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Quick Action & Transport Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate900)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Emerald400 else Slate600)
                    )
                    Text(
                        text = "${currentStyle.displayName} • $bpm BPM",
                        color = Slate200,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Record quick button
                    IconButton(
                        onClick = { if (isRecording) onStopRecord() else onStartRecord() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Rose500 else Slate800)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Quick Record",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Play / Pause quick button
                    Button(
                        onClick = onTogglePlay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) Amber500 else Indigo600,
                            contentColor = if (isPlaying) Slate950 else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Pause" else "Play Beat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
