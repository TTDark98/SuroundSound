package com.example.surroundsound.ui.studio

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.surroundsound.model.SampleMetadata
import com.example.surroundsound.ui.theme.LocalStudioTokens
import com.example.surroundsound.ui.theme.mix
import kotlin.math.abs

/** One of the four built-in foley sources. */
data class StudioSource(val id: String, val label: String, val hint: String)

val BUILT_IN_SOURCES = listOf(
    StudioSource("woods", "Woods", "dry and close"),
    StudioSource("kitchen", "Kitchen", "hard, bright hits"),
    StudioSource("field", "Field", "long open tails"),
    StudioSource("paper", "Paper", "soft and airy")
)

enum class MicPhase { READY, RECORDING, PROCESSING, DENIED, UNAVAILABLE, ERROR }

enum class StatusTone { NEUTRAL, GOOD, BAD }

@Composable
fun SamplePane(
    sourceId: String,
    sampleMetadata: SampleMetadata?,
    rawBuffer: FloatArray?,
    trimStart: Float,
    trimEnd: Float,
    isRecording: Boolean,
    recordingSeconds: Float,
    micPhase: MicPhase,
    statusTitle: String,
    statusDetail: String,
    statusTone: StatusTone,
    isPlayingRaw: Boolean,
    isPlayingTrimmed: Boolean,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectSource: (String) -> Unit,
    onTrimChange: (Float, Float) -> Unit,
    onAutoDetect: () -> Unit,
    onPlayRaw: () -> Unit,
    onPlayTrimmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = BUILT_IN_SOURCES.firstOrNull { it.id == sourceId }
    val selectedSource = source ?: BUILT_IN_SOURCES.first()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FoleyRecorderPanel(
            isRecording = isRecording,
            recordingSeconds = recordingSeconds,
            micPhase = micPhase,
            sampleName = sampleMetadata?.sourceName,
            onStartRecord = onStartRecord,
            onStopRecord = onStopRecord
        )

        SourcesPanel(
            selectedId = selectedSource.id,
            onSelect = onSelectSource
        )

        WaveformPanel(
            rawBuffer = rawBuffer,
            sampleMetadata = sampleMetadata,
            trimStart = trimStart,
            trimEnd = trimEnd,
            micPhase = micPhase,
            isPlayingRaw = isPlayingRaw,
            isPlayingTrimmed = isPlayingTrimmed,
            onTrimChange = onTrimChange,
            onAutoDetect = onAutoDetect,
            onPlayRaw = onPlayRaw,
            onPlayTrimmed = onPlayTrimmed,
            onUseSources = { onSelectSource(BUILT_IN_SOURCES.first().id) },
            onOpenSettings = onOpenSettings
        )

        if (statusTitle.isNotEmpty()) {
            StatusPanel(title = statusTitle, detail = statusDetail, tone = statusTone)
        }
    }
}

/* ==========================================================================
   Foley recorder
   ========================================================================== */

@Composable
private fun FoleyRecorderPanel(
    isRecording: Boolean,
    recordingSeconds: Float,
    micPhase: MicPhase,
    sampleName: String?,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit
) {
    val t = LocalStudioTokens.current
    val clock = if (isRecording) {
        StudioFormats.recorderClock(recordingSeconds)
    } else {
        StudioFormats.recorderClock(0f)
    }
    val caption = when {
        isRecording -> "Listening — hit the desk once."
        micPhase == MicPhase.DENIED -> "Mic unavailable"
        micPhase == MicPhase.UNAVAILABLE -> "No microphone on this device"
        micPhase == MicPhase.ERROR -> "The last take could not be used"
        else -> "Tap to record, or pick a source"
    }
    val captionColor = when (micPhase) {
        MicPhase.DENIED, MicPhase.UNAVAILABLE, MicPhase.ERROR -> t.bad
        else -> t.sub
    }
    val status = when {
        isRecording -> "Recording"
        micPhase == MicPhase.PROCESSING -> "Processing"
        else -> "Ready"
    }
    val blocked = micPhase == MicPhase.DENIED || micPhase == MicPhase.UNAVAILABLE

    StudioPanel {
        PanelLabel("Foley record")
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MicButton(
                isRecording = isRecording,
                enabled = !blocked,
                onClick = { if (isRecording) onStopRecord() else onStartRecord() }
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (blocked) "--:--.-" else clock,
                    color = t.ink,
                    fontSize = studioSize(30.sp, 22.sp),
                    fontWeight = t.numWeight,
                    fontFamily = t.fontNum,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = caption,
                    color = captionColor,
                    fontSize = 12.5.sp,
                    fontFamily = t.fontRead,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = status.uppercase(),
                color = if (isRecording) t.rec else t.faint,
                fontSize = studioSize(9.sp, 8.sp),
                fontWeight = FontWeight.SemiBold,
                fontFamily = t.fontUi,
                letterSpacing = 0.14.em,
                maxLines = 1
            )
        }
        if (sampleName != null && !isRecording && !blocked) {
            Spacer(Modifier.height(10.dp))
            StudioNumber(
                "Loaded · $sampleName",
                color = t.faint,
                size = 11.sp,
                weight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MicButton(
    isRecording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val t = LocalStudioTokens.current
    val transition = rememberInfiniteTransition(label = "mic")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Restart),
        label = "micRing"
    )
    val size = 54.dp
    Box(Modifier.size(size + 14.dp), contentAlignment = Alignment.Center) {
        if (isRecording) {
            Canvas(Modifier.fillMaxSize()) {
                val growth = 0.5f + phase * 0.5f
                drawCircle(
                    color = t.rec.copy(alpha = (1f - growth) * 0.85f),
                    radius = size.toPx() / 2f + 7.dp.toPx() * growth,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isRecording) t.rec else t.surface)
                .border(t.bw, if (isRecording) t.rec else t.line2, RoundedCornerShape(999.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = if (isRecording) "Stop recording" else "Record foley",
                tint = if (isRecording) Color.White else t.ink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/* ==========================================================================
   Built-in sources
   ========================================================================== */

@Composable
private fun SourcesPanel(selectedId: String, onSelect: (String) -> Unit) {
    val t = LocalStudioTokens.current
    StudioPanel {
        PanelLabel("Built-in sound source")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BUILT_IN_SOURCES.forEach { source ->
                val selected = source.id == selectedId
                val shape = RoundedCornerShape(t.rInner)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clip(shape)
                        .background(if (selected) t.accentFill else t.panel2)
                        .border(t.bw, if (selected) t.accentD else t.line2, shape)
                        .clickable(role = Role.Button) { onSelect(source.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = source.label,
                        color = if (selected) t.onAccent else t.sub,
                        fontSize = studioSize(11.5.sp, 9.sp),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = t.fontUi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        NoteLine(BUILT_IN_SOURCES.firstOrNull { it.id == selectedId }?.hint ?: "")
    }
}

/* ==========================================================================
   Waveform + trim
   ========================================================================== */

@Composable
private fun WaveformPanel(
    rawBuffer: FloatArray?,
    sampleMetadata: SampleMetadata?,
    trimStart: Float,
    trimEnd: Float,
    micPhase: MicPhase,
    isPlayingRaw: Boolean,
    isPlayingTrimmed: Boolean,
    onTrimChange: (Float, Float) -> Unit,
    onAutoDetect: () -> Unit,
    onPlayRaw: () -> Unit,
    onPlayTrimmed: () -> Unit,
    onUseSources: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val t = LocalStudioTokens.current
    StudioPanel {
        PanelLabel("Waveform")

        if (micPhase == MicPhase.DENIED || micPhase == MicPhase.UNAVAILABLE) {
            Spacer(Modifier.height(12.dp))
            BlockedNotice(
                denied = micPhase == MicPhase.DENIED,
                onOpenSettings = onOpenSettings,
                onUseSources = onUseSources
            )
        } else {
            Spacer(Modifier.height(12.dp))
            WaveformTrim(
                rawBuffer = rawBuffer,
                trimStart = trimStart,
                trimEnd = trimEnd,
                onTrimChange = onTrimChange
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TrimCaption(label = "Trim start", value = StudioFormats.timecode(trimStart))
                Spacer(Modifier.weight(1f))
                TrimCaption(label = "End", value = StudioFormats.timecode(trimEnd))
                Spacer(Modifier.weight(1f))
                StudioNumber(
                    text = StudioFormats.seconds(sampleMetadata?.duration ?: 0f),
                    color = t.sub,
                    size = 12.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudioButton(
                text = "Auto-detect",
                onClick = onAutoDetect,
                modifier = Modifier.weight(1f)
            )
            StudioButton(
                text = if (isPlayingRaw) "Stop" else "Play original",
                onClick = onPlayRaw,
                role = if (isPlayingRaw) StudioButtonRole.PRIMARY else StudioButtonRole.PANEL,
                modifier = Modifier.weight(1f),
                leading = if (isPlayingRaw) null else {
                    { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
                }
            )
            StudioButton(
                text = if (isPlayingTrimmed) "Stop" else "Play trimmed",
                onClick = onPlayTrimmed,
                role = if (isPlayingTrimmed) StudioButtonRole.PRIMARY else StudioButtonRole.PANEL,
                modifier = Modifier.weight(1f),
                leading = if (isPlayingTrimmed) null else {
                    { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
                }
            )
        }
    }
}

@Composable
private fun TrimCaption(label: String, value: String) {
    val t = LocalStudioTokens.current
    Column {
        Text(
            text = label.uppercase(),
            color = t.faint,
            fontSize = studioSize(8.5.sp, 7.5.sp),
            fontWeight = FontWeight.SemiBold,
            fontFamily = t.fontUi,
            letterSpacing = 0.12.em,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        StudioNumber(value, color = t.ink, size = 12.sp)
    }
}

@Composable
private fun BlockedNotice(
    denied: Boolean,
    onOpenSettings: () -> Unit,
    onUseSources: () -> Unit
) {
    val t = LocalStudioTokens.current
    val shape = RoundedCornerShape(t.rInner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(t.bad.copy(alpha = 0.10f))
            .border(t.bw, t.bad.copy(alpha = 0.35f), shape)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.MicOff,
                contentDescription = null,
                tint = t.bad,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                StudioText(
                    text = if (denied) "Microphone needs permission" else "No microphone available",
                    color = t.bad,
                    size = studioSize(14.sp, 11.sp)
                )
                Spacer(Modifier.height(3.dp))
                NoteLine(
                    if (denied) "Allow mic access to record foley." else "The four built-in sources still work."
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        StudioButton(
            text = if (denied) "Open Settings" else "Use a built-in source",
            onClick = if (denied) onOpenSettings else onUseSources,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WaveformTrim(
    rawBuffer: FloatArray?,
    trimStart: Float,
    trimEnd: Float,
    onTrimChange: (Float, Float) -> Unit
) {
    val t = LocalStudioTokens.current
    val bars = WAVE_BARS
    val peaks = remember(rawBuffer) { waveformPeaks(rawBuffer, bars) }
    val duration = (rawBuffer?.size ?: 0).toFloat() / 44100f
    var widthPx by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val startState = rememberUpdatedState(trimStart)
    val endState = rememberUpdatedState(trimEnd)

    val regionStart = if (duration > 0f) (trimStart / duration).coerceIn(0f, 1f) else 0f
    val regionEnd = if (duration > 0f) (trimEnd / duration).coerceIn(0f, 1f) else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(t.rInner))
            .background(t.surface)
            .border(t.bw, t.line, RoundedCornerShape(t.rInner))
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centreY = size.height / 2f
            val barW = size.width / bars
            // kept region wash
            drawRect(
                color = t.accent.copy(alpha = 0.12f),
                topLeft = Offset(size.width * regionStart, 0f),
                size = Size(size.width * (regionEnd - regionStart), size.height)
            )
            for (i in 0 until bars) {
                val peak = peaks[i].coerceIn(0f, 1f)
                val x = i * barW
                val inRegion = i >= (regionStart * bars) && i < (regionEnd * bars)
                val colour = if (inRegion) t.accent else mix(t.surface, t.faint, 0.55f)
                val h = (peak * (size.height * 0.46f)).coerceAtLeast(1.5f)
                drawRoundRect(
                    color = colour,
                    topLeft = Offset(x + barW * 0.18f, centreY - h),
                    size = Size((barW * 0.64f).coerceAtLeast(1f), h * 2f),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            }
            // trim boundary hairlines
            listOf(regionStart, regionEnd).forEach { fraction ->
                val x = size.width * fraction
                drawLine(
                    color = t.line2,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        TrimHandle(
            fraction = regionStart,
            alignTop = false,
            onDelta = { delta ->
                if (duration <= 0f) return@TrimHandle
                val deltaSec = delta / widthPx * duration
                val newStart = (startState.value + deltaSec)
                    .coerceIn(0f, (endState.value - 0.02f).coerceAtLeast(0f))
                onTrimChange(newStart, endState.value)
            }
        )
        TrimHandle(
            fraction = regionEnd,
            alignTop = true,
            onDelta = { delta ->
                if (duration <= 0f) return@TrimHandle
                val deltaSec = delta / widthPx * duration
                val newEnd = (endState.value + deltaSec)
                    .coerceAtLeast(startState.value + 0.02f)
                    .coerceAtMost(duration)
                onTrimChange(startState.value, newEnd)
            }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.TrimHandle(
    fraction: Float,
    alignTop: Boolean,
    onDelta: (Float) -> Unit
) {
    val t = LocalStudioTokens.current
    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offsetFraction(fraction)
            .width(20.dp)
            .height(96.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, amount ->
                    change.consume()
                    onDelta(amount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(10.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(t.accent)
                .border(1.dp, t.accentD, RoundedCornerShape(3.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                repeat(3) { index ->
                    val y = size.height * (index + 1) / 4f
                    drawLine(
                        color = t.onAccent.copy(alpha = 0.45f),
                        start = Offset(size.width * 0.28f, y),
                        end = Offset(size.width * 0.72f, y),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}

/** Positions a child at `fraction` across a parent measured later, keeping it on screen. */
private fun Modifier.offsetFraction(fraction: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val parentWidth = constraints.maxWidth
        val centre = (parentWidth * fraction.coerceIn(0f, 1f)).toInt()
        val x = (centre - placeable.width / 2).coerceIn(0, (parentWidth - placeable.width).coerceAtLeast(0))
        layout(placeable.width, placeable.height) { placeable.placeRelative(x, 0) }
    }

private const val WAVE_BARS = 96

private fun waveformPeaks(buffer: FloatArray?, bars: Int): FloatArray {
    val out = FloatArray(bars)
    val data = buffer ?: return out
    if (data.isEmpty()) return out
    val bucket = (data.size.toFloat() / bars).coerceAtLeast(1f)
    var maxPeak = 0.001f
    for (i in 0 until bars) {
        val from = (i * bucket).toInt()
        val to = (((i + 1) * bucket).toInt()).coerceAtMost(data.size)
        var peak = 0f
        for (j in from until to) {
            val v = abs(data[j])
            if (v > peak) peak = v
        }
        out[i] = peak
        if (peak > maxPeak) maxPeak = peak
    }
    for (i in out.indices) out[i] /= maxPeak
    return out
}

/* ==========================================================================
   Status line
   ========================================================================== */

@Composable
fun StatusPanel(title: String, detail: String, tone: StatusTone) {
    val t = LocalStudioTokens.current
    val accent = when (tone) {
        StatusTone.GOOD -> t.good
        StatusTone.BAD -> t.bad
        StatusTone.NEUTRAL -> t.accent
    }
    val shape = RoundedCornerShape(t.rInner)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = if (tone == StatusTone.NEUTRAL) 0.09f else 0.12f))
            .border(
                t.bw,
                accent.copy(alpha = 0.30f),
                shape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(9.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            StudioText(
                text = title,
                color = if (tone == StatusTone.BAD) t.bad else t.ink,
                size = studioSize(14.sp, 11.sp)
            )
            if (detail.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                NoteLine(detail)
            }
        }
    }
}
