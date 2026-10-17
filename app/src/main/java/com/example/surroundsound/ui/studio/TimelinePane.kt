package com.example.surroundsound.ui.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surroundsound.model.PatternState
import com.example.surroundsound.model.TrackId
import com.example.surroundsound.ui.theme.LocalStudioTokens
import com.example.surroundsound.ui.theme.Motif
import com.example.surroundsound.ui.theme.mix

private const val STEPS = 16

private fun PatternState.isOn(track: TrackId, step: Int): Boolean = when (track) {
    TrackId.KICK -> kick[step]
    TrackId.SNARE -> snare[step]
    TrackId.HIHAT -> hihat[step]
    TrackId.PERC -> perc[step]
    TrackId.MELODY -> melody.getOrNull(step)?.active == true
    TrackId.BASS -> bass.getOrNull(step)?.active == true
}

private fun PatternState.pitchAt(track: TrackId, step: Int): Int? = when (track) {
    TrackId.MELODY -> melody.getOrNull(step)?.pitch
    TrackId.BASS -> bass.getOrNull(step)?.pitch
    else -> null
}

fun TrackId.isMelodic(): Boolean = this == TrackId.MELODY || this == TrackId.BASS

/**
 * The Timeline tab: the 16-step sequencer, the mixer rows and the pitch strip.
 *
 * The playhead is drawn by a single overlay canvas that reads the animated
 * position and the current step in its *draw* phase, so a running transport
 * never recomposes a grid cell.
 */
@Composable
fun TimelinePane(
    pattern: PatternState,
    currentStep: Int,
    isPlaying: Boolean,
    bpm: Int,
    scaleIntervals: List<Int>,
    mutedTracks: Set<TrackId>,
    soloTracks: Set<TrackId>,
    selectedTrack: TrackId,
    selectedStep: Int,
    onToggleStep: (TrackId, Int) -> Unit,
    onSelectStep: (TrackId, Int) -> Unit,
    onSetPitch: (TrackId, Int, Int) -> Unit,
    onClear: () -> Unit,
    onRandomize: () -> Unit,
    onAuditionTrack: (TrackId) -> Unit,
    onToggleMute: (TrackId) -> Unit,
    onToggleSolo: (TrackId) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    val colors = t.tracks

    // While the transport runs the strip tracks the playhead; stopped, it edits
    // whatever step you tapped. The grid only rings that step while stopped.
    val activeStep = if (isPlaying && currentStep >= 0) currentStep else selectedStep
    val ringStep = if (isPlaying) -1 else selectedStep

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SequencerPanel(
            pattern = pattern,
            currentStep = currentStep,
            isPlaying = isPlaying,
            bpm = bpm,
            colors = colors,
            mutedTracks = mutedTracks,
            soloTracks = soloTracks,
            ringTrack = selectedTrack,
            ringStep = ringStep,
            onToggleStep = onToggleStep,
            onSelectStep = onSelectStep
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StudioButton("Clear", onClick = onClear, modifier = Modifier.weight(1f))
            StudioButton("Randomize", onClick = onRandomize, modifier = Modifier.weight(1f))
        }

        MixerPanel(
            pattern = pattern,
            colors = colors,
            mutedTracks = mutedTracks,
            soloTracks = soloTracks,
            isPlaying = isPlaying,
            onAuditionTrack = onAuditionTrack,
            onToggleMute = onToggleMute,
            onToggleSolo = onToggleSolo
        )

        PitchPanel(
            track = selectedTrack,
            step = activeStep,
            intervals = scaleIntervals,
            currentPitch = pattern.pitchAt(selectedTrack, activeStep),
            onPick = { pitch -> onSetPitch(selectedTrack, activeStep, pitch) }
        )
    }
}

/* ==========================================================================
   Sequencer
   ========================================================================== */

@Composable
private fun SequencerPanel(
    pattern: PatternState,
    currentStep: Int,
    isPlaying: Boolean,
    bpm: Int,
    colors: List<Color>,
    mutedTracks: Set<TrackId>,
    soloTracks: Set<TrackId>,
    ringTrack: TrackId,
    ringStep: Int,
    onToggleStep: (TrackId, Int) -> Unit,
    onSelectStep: (TrackId, Int) -> Unit
) {
    val t = LocalStudioTokens.current
    val gap = 3.dp

    val position = remember { Animatable(0f) }
    val stepMs = (60_000f / bpm.coerceAtLeast(40) / 4f).toInt().coerceAtLeast(60)
    LaunchedEffect(currentStep, isPlaying, bpm) {
        if (!isPlaying || currentStep < 0) {
            position.snapTo(0f)
            return@LaunchedEffect
        }
        if (currentStep == 0) position.snapTo(0f)
        val target = ((currentStep + 1) / STEPS.toFloat()).coerceIn(0f, 1f)
        position.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = (stepMs * 0.96f).toInt().coerceAtLeast(40),
                easing = LinearEasing
            )
        )
    }
    val positionState = position.asState()
    val stepState = androidx.compose.runtime.rememberUpdatedState(currentStep)

    StudioPanel(padding = androidx.compose.foundation.layout.PaddingValues(12.dp, 12.dp, 10.dp, 10.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cellW = (maxWidth - gap * (STEPS - 1)) / STEPS
            val cellH = if (cellW * 1.18f < 22.dp) 22.dp else cellW * 1.18f

            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    repeat(STEPS) { step ->
                        val beat = step % 4 == 0
                        Text(
                            text = "${step + 1}",
                            modifier = Modifier.width(cellW),
                            color = if (beat) t.ink else t.faint,
                            fontSize = if (t.motif == Motif.PIXEL) 8.sp else 9.5.sp,
                            fontWeight = if (t.motif == Motif.PIXEL) FontWeight.Normal else FontWeight.SemiBold,
                            fontFamily = t.fontNum,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))

                Box(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        TrackId.ROWS.forEachIndexed { index, track ->
                            val isMuted = mutedTracks.contains(track)
                            val soloDim = soloTracks.isNotEmpty() && !soloTracks.contains(track)
                            SequencerRow(
                                track = track,
                                pattern = pattern,
                                color = colors[index],
                                cellW = cellW,
                                cellH = cellH,
                                gap = gap,
                                muted = isMuted,
                                dimmed = soloDim,
                                ringStep = if (track == ringTrack) ringStep else -1,
                                onToggleStep = { step -> onToggleStep(track, step) },
                                onSelectStep = { step -> onSelectStep(track, step) }
                            )
                        }
                    }

                    SeqPlayhead(
                        position = positionState,
                        step = stepState,
                        cellW = cellW,
                        cellH = cellH,
                        gap = gap,
                        visible = isPlaying && currentStep >= 0,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SequencerRow(
    track: TrackId,
    pattern: PatternState,
    color: Color,
    cellW: Dp,
    cellH: Dp,
    gap: Dp,
    muted: Boolean,
    dimmed: Boolean,
    ringStep: Int,
    onToggleStep: (Int) -> Unit,
    onSelectStep: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.42f else 1f),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        repeat(STEPS) { step ->
            SequencerCell(
                on = pattern.isOn(track, step),
                beat = step % 4 == 0,
                color = color,
                muted = muted,
                ringed = step == ringStep,
                cellW = cellW,
                cellH = cellH,
                onClick = {
                    if (track.isMelodic()) onSelectStep(step)
                    onToggleStep(step)
                }
            )
        }
    }
}

@Composable
private fun SequencerCell(
    on: Boolean,
    beat: Boolean,
    color: Color,
    muted: Boolean,
    ringed: Boolean,
    cellW: Dp,
    cellH: Dp,
    onClick: () -> Unit
) {
    val t = LocalStudioTokens.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(if (t.motif == Motif.PIXEL) 0.dp else 6.dp)
    val base = if (beat) {
        if (t.motif == Motif.PIXEL) mix(t.panel, t.beatCell, 0.34f) else mix(t.panel, t.ink, 0.12f)
    } else {
        mix(t.panel, t.ink, 0.07f)
    }
    val background = when {
        muted -> Color.Transparent
        on -> color
        else -> base
    }
    val borderColor = when {
        muted && on -> color
        muted -> t.ink.copy(alpha = 0.26f)
        t.motif == Motif.PIXEL -> t.deep.copy(alpha = 0.28f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .width(cellW)
            .height(cellH)
            .scale(if (pressed) 0.9f else 1f)
            .clip(shape)
            .background(background)
            .border(if (muted && on) 2.dp else t.bw, borderColor, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                onClick = onClick
            )
    ) {
        if (ringed) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(shape)
                    .border(2.dp, t.ink.copy(alpha = 0.4f), shape)
            )
        }
    }
}

@Composable
private fun SeqPlayhead(
    position: State<Float>,
    step: State<Int>,
    cellW: Dp,
    cellH: Dp,
    gap: Dp,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    if (!visible) return
    Canvas(modifier) {
        // Read in the draw phase: a running playhead must never recompose the grid.
        val pos = position.value
        val index = step.value
        val cw = cellW.toPx()
        val g = gap.toPx()
        val span = cw * STEPS + g * (STEPS - 1)

        // beat dividers
        for (group in 1 until 4) {
            val x = group * 4 * (cw + g) - g / 2f
            drawLine(
                color = t.line2.copy(alpha = 0.55f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }

        if (index >= 0) {
            drawPlayhead(
                tokens = t,
                columnLeft = index * (cw + g),
                columnWidth = cw,
                headX = (span + g) * pos,
                topPadding = cellH.toPx() * 0f
            )
        }
    }
}

/* ==========================================================================
   Mixer + pitch
   ========================================================================== */

@Composable
private fun MixerPanel(
    pattern: PatternState,
    colors: List<Color>,
    mutedTracks: Set<TrackId>,
    soloTracks: Set<TrackId>,
    isPlaying: Boolean,
    onAuditionTrack: (TrackId) -> Unit,
    onToggleMute: (TrackId) -> Unit,
    onToggleSolo: (TrackId) -> Unit
) {
    val t = LocalStudioTokens.current
    StudioPanel {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TrackId.ROWS.forEachIndexed { index, track ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAuditionTrack(track) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackDot(colors[index])
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = track.shortName,
                            modifier = Modifier.width(trackNameWidth(t.motif)),
                            color = t.ink,
                            fontSize = studioSize(12.5.sp, 9.sp),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = t.fontUi,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(9.dp))
                        LevelBars(
                            color = colors[index],
                            isLive = isPlaying && pattern.isOn(track, 0),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    StudioChip(
                        text = "Mute",
                        selected = mutedTracks.contains(track),
                        onClick = { onToggleMute(track) },
                        minWidth = 56.dp,
                        minHeight = 32.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    StudioChip(
                        text = "Solo",
                        selected = soloTracks.contains(track),
                        onClick = { onToggleSolo(track) },
                        selectedColor = t.good,
                        minWidth = 56.dp,
                        minHeight = 32.dp
                    )
                }
            }
        }
    }
}

private fun trackNameWidth(motif: Motif): Dp = when (motif) {
    Motif.PIXEL -> 78.dp
    else -> 72.dp
}

@Composable
private fun PitchPanel(
    track: TrackId,
    step: Int,
    intervals: List<Int>,
    currentPitch: Int?,
    onPick: (Int) -> Unit
) {
    val t = LocalStudioTokens.current
    val melody = track != TrackId.BASS
    StudioPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudioText(
                text = if (melody) "Melody pitch" else "Bass pitch",
                size = studioSize(13.sp, 10.sp)
            )
            Spacer(Modifier.width(8.dp))
            NoteLine("step ${step + 1} selected")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            intervals.forEach { interval ->
                val selected = currentPitch == interval
                val shape = RoundedCornerShape(t.rInner)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(shape)
                        .background(if (selected) t.accentFill else t.panel2)
                        .border(t.bw, if (selected) t.accentD else t.line2, shape)
                        .clickable { onPick(interval) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = StudioFormats.midiLabel(60, interval),
                        color = if (selected) t.onAccent else t.ink,
                        fontSize = studioSize(12.sp, 9.sp),
                        fontWeight = t.numWeight,
                        fontFamily = t.fontNum,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
