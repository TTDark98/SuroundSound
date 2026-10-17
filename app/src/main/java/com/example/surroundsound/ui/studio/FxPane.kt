package com.example.surroundsound.ui.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.surroundsound.model.FxConfig
import com.example.surroundsound.model.Scales
import com.example.surroundsound.ui.theme.LocalStudioTokens

/** One dial's worth of state, so the rack can be laid out in one or two columns. */
private data class FxParam(
    val name: String,
    val valueText: String,
    val fraction: Float,
    val onChange: (Float) -> Unit
)

/** Six hardware-style parameters, the musical scale, and nothing else. */
@Composable
fun FxPane(
    fx: FxConfig,
    bpm: Int,
    onFxChange: (FxConfig) -> Unit,
    modifier: Modifier = Modifier,
    twoColumn: Boolean = false
) {
    val params = fxParams(fx, bpm, onFxChange)

    if (twoColumn) {
        // The wide frame splits the rack in two: scale and half the dials, then the rest.
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScalePanel(fx = fx, onFxChange = onFxChange)
                ParamPanel(params = params.subList(0, 3))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParamPanel(params = params.subList(3, params.size))
            }
        }
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScalePanel(fx = fx, onFxChange = onFxChange)
        ParamPanel(params = params)
    }
}

@Composable
private fun ScalePanel(fx: FxConfig, onFxChange: (FxConfig) -> Unit) {
    val t = LocalStudioTokens.current
    StudioPanel {
        PanelLabel("Musical scale")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Scales.ALL_SCALES.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { scale ->
                        StudioChip(
                            text = StudioFormats.scaleShortName(scale.id),
                            selected = fx.scaleId == scale.id,
                            onClick = { onFxChange(fx.copy(scaleId = scale.id)) },
                            modifier = Modifier.weight(1f),
                            minHeight = 38.dp
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        NoteLine(Scales.getScale(fx.scaleId).name)
        if (t.knobFx) {
            Spacer(Modifier.height(4.dp))
            NoteLine("Drag a dial up or down.")
        }
    }
}

@Composable
private fun ParamPanel(params: List<FxParam>) {
    StudioPanel {
        params.forEach { param ->
            ParamRow(
                name = param.name,
                valueText = param.valueText,
                fraction = param.fraction,
                onFractionChange = param.onChange
            )
        }
    }
}

private fun fxParams(
    fx: FxConfig,
    bpm: Int,
    onFxChange: (FxConfig) -> Unit
): List<FxParam> {
    val divisions = StudioFormats.DELAY_DIVISIONS
    val division = StudioFormats.delayDivision(fx.delayTime, bpm)

    return listOf(
        FxParam(
            name = "Filter cutoff",
            valueText = StudioFormats.cutoff(fx.filterCutoff),
            fraction = StudioFormats.logFraction(
                fx.filterCutoff,
                StudioFormats.MIN_CUTOFF,
                StudioFormats.MAX_CUTOFF
            ),
            onChange = { fraction ->
                onFxChange(
                    fx.copy(
                        filterCutoff = StudioFormats.logValue(
                            fraction,
                            StudioFormats.MIN_CUTOFF,
                            StudioFormats.MAX_CUTOFF
                        )
                    )
                )
            }
        ),
        FxParam(
            name = "Distortion",
            valueText = StudioFormats.percentValue(fx.distortion * 100f),
            fraction = fx.distortion,
            onChange = { onFxChange(fx.copy(distortion = it.coerceIn(0f, 1f))) }
        ),
        FxParam(
            name = "Delay time",
            valueText = StudioFormats.delayLabel(division),
            fraction = (division - StudioFormats.MIN_DELAY_DIV).toFloat() /
                (divisions.size - 1).coerceAtLeast(1),
            onChange = { fraction ->
                val index = (fraction * (divisions.size - 1)).toInt()
                    .coerceIn(0, divisions.size - 1)
                onFxChange(fx.copy(delayTime = StudioFormats.delaySeconds(divisions[index], bpm)))
            }
        ),
        FxParam(
            name = "Delay mix",
            valueText = StudioFormats.percentValue(fx.delayMix * 100f),
            fraction = (fx.delayMix / 0.6f).coerceIn(0f, 1f),
            onChange = { onFxChange(fx.copy(delayMix = (it * 0.6f).coerceIn(0f, 0.6f))) }
        ),
        FxParam(
            name = "Reverb mix",
            valueText = StudioFormats.percentValue(fx.reverbMix * 100f),
            fraction = (fx.reverbMix / 0.8f).coerceIn(0f, 1f),
            onChange = { onFxChange(fx.copy(reverbMix = (it * 0.8f).coerceIn(0f, 0.8f))) }
        ),
        FxParam(
            name = "Swing",
            valueText = StudioFormats.percentValue((fx.swing * 100f).coerceIn(0f, 50f)),
            fraction = (fx.swing / 0.5f).coerceIn(0f, 1f),
            onChange = { onFxChange(fx.copy(swing = (it * 0.5f).coerceIn(0f, 0.5f))) }
        )
    )
}
