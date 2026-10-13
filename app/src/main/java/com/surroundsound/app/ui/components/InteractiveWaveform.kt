package com.surroundsound.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.surroundsound.app.core.audio.WaveformExtractor
import com.surroundsound.app.ui.theme.AcidGreen
import com.surroundsound.app.ui.theme.ElectricAmber
import com.surroundsound.app.ui.theme.NeonCyan
import com.surroundsound.app.ui.theme.SurfaceBorder
import com.surroundsound.app.ui.theme.SurfaceElevated

/**
 * Interactive Waveform with real-time playhead scrubber and tap/drag seeking.
 */
@Composable
fun InteractiveWaveform(
    samples: FloatArray,
    progressPercent: Float, // 0.0f .. 1.0f
    modifier: Modifier = Modifier,
    barCount: Int = 100,
    playedColor: Color = NeonCyan,
    unplayedColor: Color = SurfaceElevated,
    playheadColor: Color = ElectricAmber,
    onSeek: (Float) -> Unit = {}
) {
    val amplitudes = remember(samples, barCount) {
        WaveformExtractor.extractAmplitudes(samples, barCount)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val seekProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(seekProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val seekProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(seekProgress)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val count = amplitudes.size

            if (count == 0) return@Canvas

            val totalGapRatio = 0.35f
            val barTotalWidth = width / count.toFloat()
            val barGap = barTotalWidth * totalGapRatio
            val barWidth = barTotalWidth - barGap
            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

            val currentProgressX = width * progressPercent.coerceIn(0f, 1f)

            // Draw waveform bars
            for (i in 0 until count) {
                val barX = i * barTotalWidth + (barGap / 2f)
                val amp = amplitudes[i]
                val barHalfHeight = (centerY * amp * 0.9f).coerceAtLeast(2f)

                val isPlayed = (barX + barWidth / 2f) <= currentProgressX
                val color = if (isPlayed) playedColor else unplayedColor

                drawRoundRect(
                    color = color,
                    topLeft = Offset(barX, centerY - barHalfHeight),
                    size = Size(barWidth, barHalfHeight * 2f),
                    cornerRadius = cornerRadius
                )
            }

            // Draw Playhead scrubber line
            val playheadX = currentProgressX.coerceIn(0f, width)
            drawLine(
                color = playheadColor,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 3.dp.toPx()
            )

            // Draw Playhead glowing thumb circle at center
            drawCircle(
                color = playheadColor.copy(alpha = 0.35f),
                radius = 10.dp.toPx(),
                center = Offset(playheadX, centerY)
            )
            drawCircle(
                color = playheadColor,
                radius = 5.dp.toPx(),
                center = Offset(playheadX, centerY)
            )
        }
    }
}
