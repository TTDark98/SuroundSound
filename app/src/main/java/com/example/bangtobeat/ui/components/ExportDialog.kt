package com.example.bangtobeat.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.bangtobeat.model.BeatStyle
import com.example.bangtobeat.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ExportDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    currentStyle: BeatStyle,
    bpm: Int,
    onRenderWav: () -> ByteArray
) {
    if (!isOpen) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Icon(Icons.Default.Download, contentDescription = null, tint = Indigo500, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Export Your Beat",
                            color = Slate100,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate800)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = "Download or share the rendered studio master WAV audio.",
                    color = Slate400,
                    fontSize = 12.sp
                )

                // WAV Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate950)
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Indigo500, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Audio (.WAV)",
                                color = Indigo500,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "4-bar studio master rendered via offline 44.1kHz stereo DSP engine.",
                            color = Slate400,
                            fontSize = 11.sp
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    try {
                                        val wavBytes = withContext(Dispatchers.Default) {
                                            onRenderWav()
                                        }

                                        withContext(Dispatchers.IO) {
                                            val fileName = "bang-to-beat-${currentStyle.id}-${bpm}bpm.wav"
                                            val cacheFile = File(context.cacheDir, fileName)
                                            FileOutputStream(cacheFile).use { it.write(wavBytes) }

                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                cacheFile
                                            )

                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "audio/wav"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }

                                            context.startActivity(Intent.createChooser(shareIntent, "Share Beat WAV"))
                                        }
                                        exportSuccess = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (exportSuccess) Emerald500 else Indigo600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Rendering 4 Bars...", fontSize = 12.sp)
                            } else if (exportSuccess) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Rendered & Shared!", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Render & Share WAV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Explanation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate950.copy(alpha = 0.5f))
                        .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "How this works: Real-time transient detection isolates your microphone or foley impact. Lowpass pitch-shifting yields the Kick, bandpass distortion crafts the Snare, highpass scaling creates Hi-Hats, and resonant musical scale interval quantizing generates Melodies.",
                        color = Slate400,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
