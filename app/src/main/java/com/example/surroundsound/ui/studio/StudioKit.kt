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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.surroundsound.model.TrackId
import com.example.surroundsound.ui.theme.LocalStudioTokens
import com.example.surroundsound.ui.theme.Motif
import com.example.surroundsound.ui.theme.StudioTheme
import com.example.surroundsound.ui.theme.StudioTokens
import com.example.surroundsound.ui.theme.mix
import com.example.surroundsound.ui.theme.studioTokens
import com.example.surroundsound.ui.theme.wash

/* ==========================================================================
   Text primitives
   ========================================================================== */

/** Silkscreen is far wider than the sans stack, so Pixel Hearth steps its type down. */
@Composable
fun studioSize(normal: TextUnit, pixel: TextUnit): TextUnit =
    if (LocalStudioTokens.current.motif == Motif.PIXEL) pixel else normal

@Composable
fun PanelLabel(text: String, modifier: Modifier = Modifier) {
    val t = LocalStudioTokens.current
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = t.labelColor,
        fontSize = if (t.motif == Motif.PIXEL) 10.sp else 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = if (t.motif == Motif.PIXEL) 0.10.em else 0.14.em,
        maxLines = 1
    )
}

@Composable
fun NoteLine(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: TextUnit = 12.5.sp
) {
    val t = LocalStudioTokens.current
    Text(
        text = text,
        modifier = modifier,
        color = color ?: t.faint,
        fontSize = size,
        fontFamily = t.fontRead,
        lineHeight = (size.value * 1.45f).sp
    )
}

@Composable
fun StudioText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: TextUnit = 13.sp,
    weight: FontWeight = FontWeight.SemiBold,
    family: androidx.compose.ui.text.font.FontFamily? = null,
    letterSpacing: TextUnit = 0.sp,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val t = LocalStudioTokens.current
    Text(
        text = text,
        modifier = modifier,
        color = color ?: t.ink,
        fontSize = size,
        fontWeight = weight,
        fontFamily = family ?: t.fontUi,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        overflow = overflow
    )
}

/** A numeral read-out: monospace (or Silkscreen at regular weight on Pixel Hearth). */
@Composable
fun StudioNumber(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: TextUnit = 13.sp,
    weight: FontWeight? = null,
    letterSpacing: TextUnit = 0.sp
) {
    val t = LocalStudioTokens.current
    Text(
        text = text,
        modifier = modifier,
        color = color ?: t.ink,
        fontSize = size,
        fontWeight = weight ?: t.numWeight,
        fontFamily = t.fontNum,
        letterSpacing = letterSpacing,
        maxLines = 1
    )
}

/* ==========================================================================
   Surfaces
   ========================================================================== */

/** The page itself: flat token colour plus a soft lamp glow for the warm moods. */
@Composable
fun StudioBackdrop(modifier: Modifier = Modifier) {
    val t = LocalStudioTokens.current
    Box(modifier.fillMaxSize().background(t.bg)) {
        if (t.motif != Motif.PIXEL) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(t.lamp.wash(if (t.dark) 0.20f else 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.74f, size.height * 0.04f),
                        radius = size.width * 0.95f
                    )
                )
                if (t.dark) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, t.deep.wash(0.35f)),
                            startY = size.height * 0.55f,
                            endY = size.height
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StudioPanel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val t = LocalStudioTokens.current
    val shape = RoundedCornerShape(t.rPanel)
    Box(modifier.fillMaxWidth()) {
        if (t.hardShadow) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(4.dp, 4.dp)
                    .clip(shape)
                    .background(t.ink.wash(0.18f))
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (t.hardShadow) Modifier
                    else Modifier.shadow(
                        elevation = if (t.dark) 14.dp else 8.dp,
                        shape = shape,
                        clip = false
                    )
                )
                .clip(shape)
                .background(t.panel)
                .then(
                    if (t.motif == Motif.BOTANICAL) {
                        Modifier.background(
                            Brush.verticalGradient(
                                0f to Color.White.wash(0.04f),
                                0.45f to Color.Transparent
                            )
                        )
                    } else Modifier
                )
                .border(t.bw, t.panelBorder, shape)
                .padding(padding),
            content = content
        )
        if (t.innerFrame > 0.dp) {
            Canvas(Modifier.matchParentSize()) {
                val inset = t.innerFrame.toPx()
                val stroke = if (t.motif == Motif.PIXEL) 2f else 1f
                drawRoundRect(
                    color = if (t.motif == Motif.PIXEL) t.panel2 else t.line2.wash(0.55f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(t.rInner.toPx()),
                    style = Stroke(width = stroke)
                )
                if (t.motif == Motif.ANALOG) {
                    val dot = Offset(9.dp.toPx(), 9.dp.toPx())
                    val spots = listOf(
                        dot,
                        Offset(size.width - dot.x, dot.y),
                        Offset(dot.x, size.height - dot.y),
                        Offset(size.width - dot.x, size.height - dot.y)
                    )
                    spots.forEach {
                        drawCircle(t.line2.wash(0.7f), radius = 1.6.dp.toPx(), center = it)
                    }
                }
            }
        }
    }
}

/* ==========================================================================
   Buttons, chips, sliders
   ========================================================================== */

enum class StudioButtonRole { PANEL, PRIMARY, GHOST }

@Composable
fun StudioButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: StudioButtonRole = StudioButtonRole.PANEL,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val t = LocalStudioTokens.current
    val shape = RoundedCornerShape(t.rInner)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val background = when (role) {
        StudioButtonRole.PANEL -> t.panel
        StudioButtonRole.PRIMARY -> if (pressed) t.accentHover else t.accentFill
        StudioButtonRole.GHOST -> Color.Transparent
    }
    val contentColor = when (role) {
        StudioButtonRole.PRIMARY -> t.onAccent
        else -> t.ink
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(background)
            .border(
                t.bw,
                when (role) {
                    StudioButtonRole.PRIMARY -> t.accentD
                    StudioButtonRole.PANEL -> t.line2
                    StudioButtonRole.GHOST -> Color.Transparent
                },
                shape
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = source,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .alphaIfDisabled(enabled),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = studioSize(15.sp, 12.sp),
            fontWeight = FontWeight.SemiBold,
            fontFamily = t.fontUi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Modifier.alphaIfDisabled(enabled: Boolean): Modifier =
    if (enabled) this else this.alpha(0.5f)

@Composable
fun StudioIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color? = null,
    role: StudioButtonRole = StudioButtonRole.PANEL,
    content: @Composable () -> Unit
) {
    val t = LocalStudioTokens.current
    val shape = RoundedCornerShape(t.rInner)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                when (role) {
                    StudioButtonRole.PRIMARY -> if (pressed) t.accentHover else t.accentFill
                    StudioButtonRole.PANEL -> t.panel
                    StudioButtonRole.GHOST -> Color.Transparent
                }
            )
            .border(t.bw, if (role == StudioButtonRole.PANEL) t.panelBorder else t.accentD, shape)
            .clickable(
                role = Role.Button,
                interactionSource = source,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides (tint ?: t.ink)
        ) { content() }
    }
}

@Composable
fun StudioChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color? = null,
    minHeight: Dp = 38.dp,
    minWidth: Dp = 0.dp
) {
    val t = LocalStudioTokens.current
    val shape = RoundedCornerShape(t.rInner)
    val fill = selectedColor ?: t.accentFill
    val bg = if (selected) fill else t.panel2
    val fg = if (selected) t.onAccent else t.sub
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .widthIn(min = minWidth)
            .clip(shape)
            .background(bg)
            .border(t.bw, if (selected) t.accentD else t.line2, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = studioSize(11.sp, 10.sp),
            fontWeight = FontWeight.SemiBold,
            fontFamily = t.fontUi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A themed horizontal control: 6dp track, 22dp dial (Pixel: square, Sunday: fader bar). */
@Composable
fun StudioSlider(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    val current = rememberUpdatedState(fraction)
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragBase by remember { mutableFloatStateOf(0f) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onFractionChange((offset.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragBase = current.value
                        dragAccum = 0f
                    }
                ) { change, amount ->
                    change.consume()
                    dragAccum += amount
                    onFractionChange((dragBase + dragAccum / widthPx).coerceIn(0f, 1f))
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val f = fraction.coerceIn(0f, 1f)
            val cy = size.height / 2f
            when (t.motif) {
                Motif.PIXEL -> {
                    val trackH = 14f
                    drawRect(
                        color = t.surface,
                        topLeft = Offset(0f, cy - trackH / 2f),
                        size = Size(size.width, trackH)
                    )
                    drawRect(
                        color = t.line,
                        topLeft = Offset(0f, cy - trackH / 2f),
                        size = Size(size.width, trackH),
                        style = Stroke(width = 3f)
                    )
                    drawRect(
                        color = t.accent,
                        topLeft = Offset(0f, cy - trackH / 2f),
                        size = Size(size.width * f, trackH)
                    )
                    val thumbW = 16f
                    val thumbH = 24f
                    val thumbX = (size.width * f - thumbW / 2f).coerceIn(0f, size.width - thumbW)
                    drawRect(
                        color = t.accent,
                        topLeft = Offset(thumbX, cy - thumbH / 2f),
                        size = Size(thumbW, thumbH)
                    )
                    drawRect(
                        color = t.accentD,
                        topLeft = Offset(thumbX, cy - thumbH / 2f),
                        size = Size(thumbW, thumbH),
                        style = Stroke(width = 3f)
                    )
                }
                Motif.ANALOG -> {
                    drawRect(
                        color = t.line2,
                        topLeft = Offset(0f, cy - 1.2f),
                        size = Size(size.width, 2.4f)
                    )
                    val thumbW = 13f
                    val thumbH = 26f
                    val thumbX = (size.width * f - thumbW / 2f).coerceIn(0f, size.width - thumbW)
                    drawRect(
                        color = t.accent,
                        topLeft = Offset(thumbX, cy - thumbH / 2f),
                        size = Size(thumbW, thumbH)
                    )
                    drawRect(
                        color = t.accentD,
                        topLeft = Offset(thumbX, cy - thumbH / 2f),
                        size = Size(thumbW, thumbH),
                        style = Stroke(width = 1.5f)
                    )
                }
                Motif.BOTANICAL -> {
                    val trackH = 6f
                    val r = trackH / 2f
                    drawRoundRect(
                        color = t.surface,
                        topLeft = Offset(0f, cy - trackH / 2f),
                        size = Size(size.width, trackH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
                    )
                    drawRoundRect(
                        color = t.accentD,
                        topLeft = Offset(0f, cy - trackH / 2f),
                        size = Size(size.width, trackH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
                        style = Stroke(width = t.bw.toPx())
                    )
                    if (f > 0f) {
                        drawRoundRect(
                            color = t.accent,
                            topLeft = Offset(0f, cy - trackH / 2f),
                            size = Size(size.width * f, trackH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
                        )
                    }
                    val thumbRadius = 11f
                    val thumbX = (size.width * f).coerceIn(thumbRadius, size.width - thumbRadius)
                    drawCircle(t.accent, radius = thumbRadius, center = Offset(thumbX, cy))
                    drawCircle(t.accentD, radius = thumbRadius, center = Offset(thumbX, cy), style = Stroke(2f))
                }
            }
        }
    }
}

/** Sunday Analog's rotary dial. Drag up/down like the hardware. */
@Composable
fun StudioKnob(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    val current = rememberUpdatedState(fraction)
    var dragBase by remember { mutableFloatStateOf(0f) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(54.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragBase = current.value
                        dragAccum = 0f
                    }
                ) { change, amount ->
                    change.consume()
                    dragAccum -= amount
                    onFractionChange((dragBase + dragAccum / 180f).coerceIn(0f, 1f))
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(t.panel, t.surface),
                    center = Offset(size.width * 0.34f, size.height * 0.28f),
                    radius = radius * 1.3f
                ),
                radius = radius,
                center = center
            )
            drawCircle(t.line2, radius = radius, center = center, style = Stroke(t.bw.toPx()))
            // engraved tick ring
            val ticks = 28
            for (i in 0 until ticks) {
                val angle = -130f + 260f * (i / (ticks - 1f))
                rotate(angle, center) {
                    drawLine(
                        color = t.line2.wash(0.75f),
                        start = Offset(center.x, center.y - radius - 5.dp.toPx()),
                        end = Offset(center.x, center.y - radius - 2.dp.toPx()),
                        strokeWidth = 1.5f
                    )
                }
            }
            rotate(-130f + 260f * fraction.coerceIn(0f, 1f), center) {
                drawLine(
                    color = t.ink,
                    start = Offset(center.x, center.y - radius * 0.28f),
                    end = Offset(center.x, center.y - radius * 0.72f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/** Name (and optional dotted leader) · control · value — one FX parameter row. */
@Composable
fun ParamRow(
    name: String,
    valueText: String,
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    if (t.knobFx) {
        Row(
            modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioText(name, size = 12.5.sp)
                Spacer(Modifier.width(12.dp))
                DottedLeader(Modifier.weight(1f))
            }
            Spacer(Modifier.width(18.dp))
            StudioKnob(fraction, onFractionChange)
            Spacer(Modifier.width(18.dp))
            StudioNumber(
                valueText,
                modifier = Modifier.width(66.dp),
                color = t.sub,
                size = 12.5.sp
            )
        }
    } else {
        Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StudioText(name, size = studioSize(12.5.sp, 11.sp))
                StudioNumber(valueText, color = t.accentText, size = studioSize(12.5.sp, 11.sp))
            }
            Spacer(Modifier.height(4.dp))
            StudioSlider(fraction = fraction, onFractionChange = onFractionChange)
        }
    }
}

@Composable
private fun DottedLeader(modifier: Modifier = Modifier) {
    val t = LocalStudioTokens.current
    Canvas(modifier.height(1.dp)) {
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = t.line2,
                start = Offset(x, 0f),
                end = Offset((x + 2.dp.toPx()).coerceAtMost(size.width), 0f),
                strokeWidth = 1f
            )
            x += 6.dp.toPx()
        }
    }
}

/* ==========================================================================
   Small marks
   ========================================================================== */

/** Per-theme track bullet: a leaf, a hard pixel, or a stamped square. */
@Composable
fun TrackDot(color: Color, modifier: Modifier = Modifier) {
    val t = LocalStudioTokens.current
    val dotSize = if (t.motif == Motif.PIXEL) 12.dp else 11.dp
    Canvas(modifier.size(dotSize)) {
        val radius = size.minDimension / 2f
        when (t.motif) {
            Motif.PIXEL -> {
                drawRect(color)
                drawRect(t.ink, style = Stroke(2f))
            }
            Motif.ANALOG -> drawRoundRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            Motif.BOTANICAL -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.5f, 0f)
                    quadraticTo(size.width, size.height * 0.5f, size.width * 0.5f, size.height)
                    quadraticTo(0f, size.height * 0.5f, size.width * 0.5f, 0f)
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}

/** The six little levelled bars that sit beside a channel name. */
@Composable
fun LevelBars(
    color: Color,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    val phase = if (isLive) rememberLoopPhase() else 0f
    val shape = listOf(0.34f, 0.62f, 0.44f, 0.82f, 0.52f, 0.68f)
    Canvas(modifier.height(15.dp)) {
        val barW = 3.dp.toPx()
        val gap = 2.dp.toPx()
        shape.forEachIndexed { index, base ->
            val wobble = if (isLive) {
                val p = (phase + index * 0.17f) % 1f
                0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin(p * Math.PI).toFloat())
            } else base
            val h = size.height * (base * 0.55f + wobble * 0.45f).coerceIn(0.15f, 1f)
            drawRoundRect(
                color = color.wash(0.6f),
                topLeft = Offset(index * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
        }
    }
}

@Composable
private fun rememberLoopPhase(): Float {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "levels")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(760),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "levelPhase"
    )
    return phase
}

/** The abstract per-voice mark a pad wears, drawn in its track colour. */
@Composable
fun PadMark(track: TrackId, color: Color, modifier: Modifier = Modifier) {
    val t = LocalStudioTokens.current
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val radius = h * 0.5f
        val stroke = if (t.motif == Motif.PIXEL) 8f else 6f
        when (track) {
            TrackId.KICK -> {
                drawCircle(color, radius = radius * 0.92f, center = Offset(w / 2f, h / 2f))
                drawCircle(t.panel, radius = radius * 0.34f, center = Offset(w / 2f, h / 2f))
            }
            TrackId.SNARE -> {
                val barW = w * 0.16f
                drawRoundRect(
                    color,
                    topLeft = Offset(w / 2f - barW - 3f, 0f),
                    size = Size(barW, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f)
                )
                drawRoundRect(
                    color,
                    topLeft = Offset(w / 2f + 3f, 0f),
                    size = Size(barW, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f)
                )
            }
            TrackId.HIHAT -> {
                repeat(5) { i ->
                    val x = w * 0.28f + i * (w * 0.11f)
                    drawLine(
                        color = color,
                        start = Offset(x, h * 0.12f),
                        end = Offset(x, h * 0.88f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
            TrackId.PERC -> {
                val r = h * 0.16f
                drawCircle(color, r, Offset(w * 0.34f, h * 0.66f))
                drawCircle(color.wash(0.75f), r, Offset(w * 0.54f, h * 0.46f))
                drawCircle(color, r, Offset(w * 0.72f, h * 0.28f))
            }
            TrackId.MELODY -> {
                val barW = w * 0.11f
                listOf(0.34f, 0.5f, 0.68f, 0.86f).forEachIndexed { i, level ->
                    val barH = h * level
                    drawRoundRect(
                        color = color.wash(0.7f + i * 0.1f),
                        topLeft = Offset(w * 0.3f + i * (barW + 6f), h - barH),
                        size = Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                    )
                }
            }
            TrackId.BASS -> {
                drawRoundRect(
                    color,
                    topLeft = Offset(w * 0.16f, h * 0.32f),
                    size = Size(w * 0.68f, h * 0.28f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
                drawRoundRect(
                    color.wash(0.45f),
                    topLeft = Offset(w * 0.26f, h * 0.64f),
                    size = Size(w * 0.48f, h * 0.16f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                )
            }
        }
    }
}

/** A miniature of the studio, in its own theme — the theme picker's card art. */
@Composable
fun ThemeMiniPreview(theme: StudioTheme, modifier: Modifier = Modifier) {
    val t = remember(theme) { studioTokens(theme) }
    val shape = RoundedCornerShape(t.rInner)
    Column(
        modifier = modifier
            .clip(shape)
            .background(t.surface)
            .border(1.dp, t.line, shape)
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("84", color = t.ink, fontSize = 13.sp, fontWeight = t.numWeight, fontFamily = t.fontNum)
                Spacer(Modifier.width(2.dp))
                Text("BPM", color = t.sub, fontSize = 5.sp, fontFamily = t.fontUi, letterSpacing = 0.08.em)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    Modifier.size(9.dp).clip(RoundedCornerShape(t.rInner)).background(t.panel2)
                        .border(1.dp, t.line2, RoundedCornerShape(t.rInner))
                )
                Box(
                    Modifier.size(9.dp).clip(RoundedCornerShape(t.rInner)).background(t.accentFill)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(t.rInner))
                .background(t.panel)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val rows = listOf(
                listOf(0, 4, 8, 12),
                listOf(2, 6, 10, 14),
                listOf(0, 2, 4, 6, 8, 10, 12, 14)
            )
            rows.forEachIndexed { rowIndex, on ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(16) { step ->
                        val lit = on.contains(step)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(if (t.motif == Motif.PIXEL) 0.dp else 2.dp))
                                .background(
                                    if (lit) t.tracks[rowIndex] else mix(t.panel, t.ink, 0.10f)
                                )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier.weight(1f).height(11.dp).clip(RoundedCornerShape(t.rInner)).background(t.panel2)
                    .border(1.dp, t.line2, RoundedCornerShape(t.rInner))
            )
            Box(
                Modifier.weight(1f).height(11.dp).clip(RoundedCornerShape(t.rInner)).background(t.tracks[1])
            )
        }
    }
}

/* ==========================================================================
   Header: identity, preset, tabs, tempo and transport
   ========================================================================== */

@Composable
fun StudioTopBar(
    styleName: String,
    onStyleClick: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val t = LocalStudioTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "SurroundSound",
                color = t.ink,
                fontSize = studioSize(18.sp, 13.sp),
                fontWeight = FontWeight.Bold,
                fontFamily = t.fontDisplay,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "cozy beat studio",
                color = t.sub,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = t.fontUi,
                letterSpacing = 0.18.em,
                maxLines = 1
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Spacer(Modifier.width(10.dp))
            PresetPill(styleName = styleName, onClick = onStyleClick)
            Spacer(Modifier.width(8.dp))
            StudioIconButton(onClick = onThemeClick) {
                Icon(Icons.Filled.Tune, contentDescription = "Theme", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun PresetPill(styleName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalStudioTokens.current
    val shape = if (t.motif == Motif.PIXEL) RoundedCornerShape(t.rInner) else RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(t.panel)
            .border(t.bw, t.panelBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "style",
            color = t.sub.copy(alpha = 0.65f),
            fontSize = studioSize(11.5.sp, 9.sp),
            fontFamily = t.fontUi,
            letterSpacing = 0.06.em,
            maxLines = 1
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = styleName,
            color = t.ink,
            fontSize = studioSize(11.5.sp, 9.sp),
            fontWeight = FontWeight.SemiBold,
            fontFamily = t.fontUi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = t.sub,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun StudioTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(t.rPanel))
            .background(t.surface)
            .border(t.bw, t.line, RoundedCornerShape(t.rPanel))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(t.rInner))
                    .background(if (selected) t.accentFill else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) t.onAccent else t.sub,
                    fontSize = studioSize(13.sp, 10.sp),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = t.fontUi,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TempoPanel(bpm: Int, onBpmChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalStudioTokens.current
    StudioPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                PanelLabel("Tempo")
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    StudioNumber("$bpm", size = t.tempoSize, letterSpacing = (-0.02).em)
                    Spacer(Modifier.width(7.dp))
                    StudioNumber(
                        "BPM",
                        color = t.sub,
                        size = studioSize(10.5.sp, 9.sp),
                        weight = FontWeight.SemiBold,
                        letterSpacing = 0.16.em
                    )
                    Spacer(Modifier.height(0.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TempoStepper(negative = true, onClick = { onBpmChange((bpm - 1).coerceAtLeast(40)) })
                TempoStepper(negative = false, onClick = { onBpmChange((bpm + 1).coerceAtMost(220)) })
            }
        }
    }
}

@Composable
private fun TempoStepper(negative: Boolean, onClick: () -> Unit) {
    val t = LocalStudioTokens.current
    val shape = if (t.motif == Motif.PIXEL) RoundedCornerShape(t.rInner) else RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(shape)
            .background(if (t.motif == Motif.BOTANICAL) t.surface else t.panel2)
            .border(t.bw, t.line2, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (negative) Icons.Filled.Remove else Icons.Filled.Add,
            contentDescription = if (negative) "Slower" else "Faster",
            tint = t.ink,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun TransportPanel(
    isPlaying: Boolean,
    isRecording: Boolean,
    recordCaption: String,
    onPlayToggle: () -> Unit,
    onStop: () -> Unit,
    onRecordToggle: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
    recordEnabled: Boolean = true
) {
    val t = LocalStudioTokens.current
    StudioPanel(modifier = modifier, padding = PaddingValues(horizontal = 14.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RoundTransportButton(
                    size = 62.dp,
                    background = t.accent,
                    borderColor = t.accentD,
                    contentColor = t.onAccent,
                    onClick = onPlayToggle
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                TransportCaption(if (isPlaying) "Pause" else "Play")
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RoundTransportButton(
                    size = 46.dp,
                    background = t.surface,
                    borderColor = t.line2,
                    contentColor = t.ink,
                    onClick = onStop
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(10.dp))
                TransportCaption("Stop")
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RoundTransportButton(
                    size = 46.dp,
                    background = if (isRecording) t.rec else t.surface,
                    borderColor = if (isRecording) t.rec else t.line2,
                    contentColor = if (isRecording) Color.White else t.rec,
                    onClick = onRecordToggle,
                    enabled = recordEnabled,
                    pulse = isRecording
                ) {
                    Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        contentDescription = "Record",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                TransportCaption(if (isRecording) recordCaption else "Rec")
            }
            Spacer(Modifier.weight(1f))
            StudioButton(
                text = "Export",
                onClick = onExport,
                role = StudioButtonRole.PANEL,
                leading = {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
        }
    }
}

@Composable
private fun TransportCaption(text: String) {
    val t = LocalStudioTokens.current
    Text(
        text = text.uppercase(),
        color = t.sub,
        fontSize = studioSize(9.sp, 8.sp),
        fontWeight = FontWeight.SemiBold,
        fontFamily = t.fontUi,
        letterSpacing = 0.14.em,
        maxLines = 1
    )
}

@Composable
private fun RoundTransportButton(
    size: Dp,
    background: Color,
    borderColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pulse: Boolean = false,
    reserveRing: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val t = LocalStudioTokens.current
    val shape = if (t.motif == Motif.PIXEL) RoundedCornerShape(t.rInner) else RoundedCornerShape(999.dp)
    val source = remember { MutableInteractionSource() }
    val ringPhase = if (pulse) rememberLoopPhase() else 0f
    val outer = if (pulse || reserveRing) size + 16.dp else size
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(outer)) {
        if (pulse) {
            Canvas(Modifier.fillMaxSize()) {
                val growth = 0.5f + ringPhase * 0.5f
                drawCircle(
                    color = t.rec.copy(alpha = (1f - growth) * 0.9f),
                    radius = (size.toPx() / 2f) + 8.dp.toPx() * growth,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(background)
                    .border(t.bw, borderColor, shape)
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        interactionSource = source,
                        indication = null,
                        onClick = onClick
                    )
                    .alphaIfDisabled(enabled),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}

/* ==========================================================================
   Toast
   ========================================================================== */

@Composable
fun StudioToast(text: String?, modifier: Modifier = Modifier) {
    val t = LocalStudioTokens.current
    androidx.compose.animation.AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 4 },
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 4 }
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(t.ink)
                .padding(horizontal = 18.dp, vertical = 11.dp)
        ) {
            Text(
                text = text ?: "",
                color = t.bg,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = t.fontUi,
                maxLines = 2
            )
        }
    }
}

/** Draws a soft ring around the current step's column, plus the sweeping head. */
internal fun DrawScope.drawPlayhead(
    tokens: StudioTokens,
    columnLeft: Float,
    columnWidth: Float,
    headX: Float,
    topPadding: Float
) {
    drawRect(
        color = tokens.accent.wash(0.10f),
        topLeft = Offset(columnLeft, topPadding),
        size = Size(columnWidth, size.height - topPadding)
    )
    val lineWidth = 2.dp.toPx()
    drawRect(
        color = tokens.accent,
        topLeft = Offset(headX - lineWidth / 2f, topPadding),
        size = Size(lineWidth, size.height - topPadding)
    )
    val tri = 5.dp.toPx()
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(headX, topPadding + tri)
        lineTo(headX - tri, topPadding - tri * 0.6f)
        lineTo(headX + tri, topPadding - tri * 0.6f)
        close()
    }
    drawPath(path, tokens.accent)
}

