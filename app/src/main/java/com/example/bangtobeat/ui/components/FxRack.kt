package com.example.bangtobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bangtobeat.model.FxConfig
import com.example.bangtobeat.model.Scales
import com.example.bangtobeat.theme.*

@Composable
fun FxRack(
    fx: FxConfig,
    onChangeFx: (FxConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioPanelBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(NeonViolet)
                    )
                    Text(
                        text = "STUDIO DSP EFFECTS RACK & HARMONIC SCALE TUNING",
                        color = Slate100,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Musical Scales Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Scales.ALL_SCALES.forEach { scale ->
                    val isSelected = scale.id == fx.scaleId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonViolet else StudioCarbon)
                            .border(
                                1.dp,
                                if (isSelected) NeonViolet else StudioPanelBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onChangeFx(fx.copy(scaleId = scale.id))
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = scale.name,
                            color = if (isSelected) StudioCarbon else Slate300,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                        )
                    }
                }
            }

            val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                // Landscape: 3 modules side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RackModule(
                        title = "ANALOG FILTER & DRIVE",
                        accentColor = NeonGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        FxControlSlider(
                            name = "Cutoff",
                            valueStr = "${fx.filterCutoff.toInt()}Hz",
                            value = fx.filterCutoff,
                            range = 300f..20000f,
                            color = NeonGreen,
                            onChange = { onChangeFx(fx.copy(filterCutoff = it)) }
                        )
                        FxControlSlider(
                            name = "Distortion",
                            valueStr = "${(fx.distortion * 100).toInt()}%",
                            value = fx.distortion,
                            range = 0f..1f,
                            color = NeonPink,
                            onChange = { onChangeFx(fx.copy(distortion = it)) }
                        )
                    }

                    RackModule(
                        title = "STEREO TAPE DELAY",
                        accentColor = NeonCyan,
                        modifier = Modifier.weight(1f)
                    ) {
                        FxControlSlider(
                            name = "Delay Time",
                            valueStr = "${(fx.delayTime * 1000).toInt()}ms",
                            value = fx.delayTime,
                            range = 0.05f..0.8f,
                            color = NeonCyan,
                            onChange = { onChangeFx(fx.copy(delayTime = it)) }
                        )
                        FxControlSlider(
                            name = "Delay Mix",
                            valueStr = "${(fx.delayMix * 100).toInt()}%",
                            value = fx.delayMix,
                            range = 0f..1f,
                            color = NeonCyan,
                            onChange = { onChangeFx(fx.copy(delayMix = it)) }
                        )
                    }

                    RackModule(
                        title = "STUDIO REVERB & GROOVE",
                        accentColor = NeonViolet,
                        modifier = Modifier.weight(1f)
                    ) {
                        FxControlSlider(
                            name = "Reverb Mix",
                            valueStr = "${(fx.reverbMix * 100).toInt()}%",
                            value = fx.reverbMix,
                            range = 0f..1f,
                            color = NeonViolet,
                            onChange = { onChangeFx(fx.copy(reverbMix = it)) }
                        )
                        FxControlSlider(
                            name = "Swing Groove",
                            valueStr = "${(fx.swing * 100).toInt()}%",
                            value = fx.swing,
                            range = 0f..0.5f,
                            color = NeonAmber,
                            onChange = { onChangeFx(fx.copy(swing = it)) }
                        )
                    }
                }
            } else {
                // Portrait: Stacked modules with full width
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RackModule(
                        title = "ANALOG FILTER & DRIVE",
                        accentColor = NeonGreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FxControlSlider(
                            name = "Cutoff",
                            valueStr = "${fx.filterCutoff.toInt()}Hz",
                            value = fx.filterCutoff,
                            range = 300f..20000f,
                            color = NeonGreen,
                            onChange = { onChangeFx(fx.copy(filterCutoff = it)) }
                        )
                        FxControlSlider(
                            name = "Distortion",
                            valueStr = "${(fx.distortion * 100).toInt()}%",
                            value = fx.distortion,
                            range = 0f..1f,
                            color = NeonPink,
                            onChange = { onChangeFx(fx.copy(distortion = it)) }
                        )
                    }

                    RackModule(
                        title = "STEREO TAPE DELAY",
                        accentColor = NeonCyan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FxControlSlider(
                            name = "Delay Time",
                            valueStr = "${(fx.delayTime * 1000).toInt()}ms",
                            value = fx.delayTime,
                            range = 0.05f..0.8f,
                            color = NeonCyan,
                            onChange = { onChangeFx(fx.copy(delayTime = it)) }
                        )
                        FxControlSlider(
                            name = "Delay Mix",
                            valueStr = "${(fx.delayMix * 100).toInt()}%",
                            value = fx.delayMix,
                            range = 0f..1f,
                            color = NeonCyan,
                            onChange = { onChangeFx(fx.copy(delayMix = it)) }
                        )
                    }

                    RackModule(
                        title = "STUDIO REVERB & GROOVE",
                        accentColor = NeonViolet,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FxControlSlider(
                            name = "Reverb Mix",
                            valueStr = "${(fx.reverbMix * 100).toInt()}%",
                            value = fx.reverbMix,
                            range = 0f..1f,
                            color = NeonViolet,
                            onChange = { onChangeFx(fx.copy(reverbMix = it)) }
                        )
                        FxControlSlider(
                            name = "Swing Groove",
                            valueStr = "${(fx.swing * 100).toInt()}%",
                            value = fx.swing,
                            range = 0f..0.5f,
                            color = NeonAmber,
                            onChange = { onChangeFx(fx.copy(swing = it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RackModule(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioCarbon)
            .border(1.dp, StudioPanelBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = title,
                    color = Slate300,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
            content()
        }
    }
}

@Composable
private fun FxControlSlider(
    name: String,
    valueStr: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(
                valueStr,
                color = color,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.height(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
    }
}
