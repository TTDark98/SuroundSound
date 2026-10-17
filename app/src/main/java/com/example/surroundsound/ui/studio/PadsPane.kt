package com.example.surroundsound.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surroundsound.model.TrackId
import com.example.surroundsound.ui.theme.LocalStudioTokens
import com.example.surroundsound.ui.theme.mix

/** Six heavy pads that fire the desk voices on touch-down. */
@Composable
fun PadsPane(
    onTrigger: (TrackId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TrackId.ROWS.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { track ->
                    StudioPad(
                        track = track,
                        onTrigger = { onTrigger(track) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioPad(
    track: TrackId,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    val color = t.tracks[track.ordinal]
    var pressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(t.rPanel)

    Box(
        modifier = modifier
            .aspectRatio(1.22f)
            .scale(if (pressed) 0.985f else 1f)
            .clip(shape)
            .background(if (pressed) mix(t.panel, color, 0.17f) else t.panel)
            .border(
                if (pressed) 2.dp else t.bw,
                if (pressed) color else t.line2,
                shape
            )
            .pointerInput(track) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onTrigger()
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
            .padding(13.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                PadMark(
                    track = track,
                    color = color,
                    modifier = Modifier.fillMaxWidth(0.62f).height(46.dp)
                )
            }
            Text(
                text = track.shortName,
                color = t.ink,
                fontSize = studioSize(13.sp, 10.sp),
                fontWeight = FontWeight.SemiBold,
                fontFamily = t.fontUi,
                maxLines = 1
            )
        }
    }
}
