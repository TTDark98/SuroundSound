package com.example.surroundsound.ui.studio

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.surroundsound.model.BeatStyle
import com.example.surroundsound.ui.theme.LocalStudioTokens
import com.example.surroundsound.ui.theme.Motif
import com.example.surroundsound.ui.theme.StudioTheme
import com.example.surroundsound.ui.theme.mix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/* ==========================================================================
   Export
   ========================================================================== */

enum class ExportPhase { READY, WORKING, DONE, FAILED }

/** Writes the rendered master to app storage, or hands it to the share sheet. */
object WavExport {
    fun writeToRenders(context: Context, fileName: String, bytes: ByteArray): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(base, "renders")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    fun share(context: Context, fileName: String, bytes: ByteArray) {
        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { it.write(bytes) }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share beat WAV"))
    }
}

@Composable
fun ExportSheet(
    visible: Boolean,
    style: BeatStyle,
    styleName: String,
    bpm: Int,
    onRenderWav: () -> ByteArray,
    onClose: () -> Unit,
    onToast: (String) -> Unit
) {
    val t = LocalStudioTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember(visible) { mutableStateOf(ExportPhase.READY) }
    var progress by remember(visible) { mutableFloatStateOf(0f) }
    var message by remember(visible) { mutableStateOf("") }
    var fileName by remember(visible) { mutableStateOf("") }

    StudioSheetScrim(visible = visible, onDismiss = onClose)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(240)) { it } + fadeIn(tween(160)),
        exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(120))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            SheetPanel {
                SheetGrabber()
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export beat",
                        color = t.ink,
                        fontSize = studioSize(21.sp, 15.sp),
                        fontWeight = FontWeight.Bold,
                        fontFamily = t.fontDisplay
                    )
                    Spacer(Modifier.weight(1f))
                    StudioIconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = t.ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(t.rInner))
                        .background(t.panel2)
                        .border(t.bw, t.line2, RoundedCornerShape(t.rInner))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        PanelLabel("Beat style")
                        Spacer(Modifier.height(4.dp))
                        StudioText(styleName, size = studioSize(13.sp, 10.sp))
                    }
                    Column(Modifier.weight(1f)) {
                        PanelLabel("Tempo")
                        Spacer(Modifier.height(4.dp))
                        StudioNumber("$bpm BPM", size = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                NoteLine(StudioFormats.exportSpec(bpm))

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioButton(
                        text = if (phase == ExportPhase.WORKING) "Rendering…" else "Export WAV",
                        onClick = {
                            scope.launch {
                                phase = ExportPhase.WORKING
                                progress = 0f
                                val ticker = launch {
                                    while (progress < 0.94f) {
                                        delay(70)
                                        progress = (progress + 0.02f).coerceAtMost(0.94f)
                                    }
                                }
                                try {
                                    val bytes = withContext(Dispatchers.Default) { onRenderWav() }
                                    ticker.cancel()
                                    progress = 1f
                                    val name = StudioFormats.exportFileName(style, bpm)
                                    val file = withContext(Dispatchers.IO) {
                                        WavExport.writeToRenders(context, name, bytes)
                                    }
                                    fileName = name
                                    message = "Saved to your renders · ${file.parentFile?.name}/${file.name}"
                                    phase = ExportPhase.DONE
                                    onToast("Rendered $name")
                                } catch (error: Exception) {
                                    ticker.cancel()
                                    message = error.message ?: "The render did not finish."
                                    phase = ExportPhase.FAILED
                                }
                            }
                        },
                        role = StudioButtonRole.PRIMARY,
                        enabled = phase != ExportPhase.WORKING,
                        modifier = Modifier.weight(1.6f)
                    )
                    StudioButton(
                        text = "Share",
                        onClick = {
                            scope.launch {
                                phase = ExportPhase.WORKING
                                progress = 0f
                                val ticker = launch {
                                    while (progress < 0.94f) {
                                        delay(70)
                                        progress = (progress + 0.02f).coerceAtMost(0.94f)
                                    }
                                }
                                try {
                                    val bytes = withContext(Dispatchers.Default) { onRenderWav() }
                                    ticker.cancel()
                                    progress = 1f
                                    val name = StudioFormats.exportFileName(style, bpm)
                                    withContext(Dispatchers.IO) { WavExport.share(context, name, bytes) }
                                    fileName = name
                                    message = "Handed $name to the share sheet"
                                    phase = ExportPhase.DONE
                                } catch (error: Exception) {
                                    ticker.cancel()
                                    message = error.message ?: "The share did not start."
                                    phase = ExportPhase.FAILED
                                }
                            }
                        },
                        enabled = phase != ExportPhase.WORKING,
                        leading = {
                            Icon(
                                Icons.Filled.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                when (phase) {
                    ExportPhase.READY -> {
                        Spacer(Modifier.height(14.dp))
                        NoteLine(
                            "Four bars through the same voices and effects you hear now."
                        )
                    }
                    ExportPhase.WORKING -> {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StudioText("Rendering", size = studioSize(13.sp, 10.sp))
                            StudioNumber(
                                StudioFormats.percentValue(progress * 100f),
                                color = t.accentText,
                                size = 12.5.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ProgressBar(progress)
                        Spacer(Modifier.height(10.dp))
                        NoteLine("$bpm bpm · 4 bars through the same voices you hear.")
                    }
                    ExportPhase.DONE -> {
                        Spacer(Modifier.height(16.dp))
                        ResultBlock(
                            good = true,
                            title = "Saved to your renders",
                            detail = if (fileName.isNotEmpty()) fileName else message
                        )
                    }
                    ExportPhase.FAILED -> {
                        Spacer(Modifier.height(16.dp))
                        ResultBlock(good = false, title = "Export failed", detail = message)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    val t = LocalStudioTokens.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(t.surface)
            .border(t.bw, t.line2, RoundedCornerShape(999.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxSize()
                .clip(RoundedCornerShape(999.dp))
                .background(t.accent)
        )
    }
}

@Composable
private fun ResultBlock(good: Boolean, title: String, detail: String) {
    val t = LocalStudioTokens.current
    val accent = if (good) t.good else t.bad
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(t.rInner))
            .background(accent.copy(alpha = 0.12f))
            .border(t.bw, accent.copy(alpha = 0.32f), RoundedCornerShape(t.rInner))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (good) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            StudioText(
                text = title,
                color = if (good) t.ink else t.bad,
                size = studioSize(14.sp, 11.sp)
            )
            if (detail.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                NoteLine(detail)
            }
        }
    }
}

/* ==========================================================================
   Theme picker
   ========================================================================== */

@Composable
fun ThemePickerScreen(
    visible: Boolean,
    activeTheme: StudioTheme,
    selectedTheme: StudioTheme,
    onSelect: (StudioTheme) -> Unit,
    onPreview: (StudioTheme) -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit
) {
    val t = LocalStudioTokens.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { -it / 6 },
        exit = fadeOut(tween(140))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(t.bg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.fillMaxWidth().widthIn(max = 640.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudioIconButton(onClick = onClose) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = "Close theme picker",
                                tint = t.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Theme",
                                color = t.ink,
                                fontSize = studioSize(26.sp, 18.sp),
                                fontWeight = FontWeight.Bold,
                                fontFamily = t.fontDisplay
                            )
                            Text(
                                text = "pick a mood for your studio",
                                color = t.sub,
                                fontSize = studioSize(9.5.sp, 8.sp),
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = t.fontUi,
                                letterSpacing = 0.18.em
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    StudioTheme.values().forEach { theme ->
                        ThemeCard(
                            theme = theme,
                            isActive = theme == activeTheme,
                            isSelected = theme == selectedTheme,
                            onSelect = { onSelect(theme) },
                            onPreview = { onPreview(theme) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PanelLabel("Active theme")
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(t.swatchRadius))
                                .background(activeTheme.swatches.firstOrNull()?.let { Color(it) } ?: t.accent)
                                .border(1.dp, t.line2, RoundedCornerShape(t.swatchRadius))
                        )
                        Spacer(Modifier.width(8.dp))
                        StudioNumber(activeTheme.displayName, size = 12.5.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    StudioButton(
                        text = "Apply theme",
                        onClick = onApply,
                        role = StudioButtonRole.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    NoteLine("Preview the studio, then Apply to keep it.")
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: StudioTheme,
    isActive: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    val t = LocalStudioTokens.current
    val swatches = remember(theme) { theme.swatches.map { Color(it) } }
    StudioPanel {
        ThemeMiniPreview(theme, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = theme.displayName,
                    color = t.ink,
                    fontSize = studioSize(17.sp, 12.sp),
                    fontWeight = FontWeight.Bold,
                    fontFamily = t.fontDisplay
                )
                Spacer(Modifier.height(3.dp))
                NoteLine(theme.blurb)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            swatches.forEach { swatch ->
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(t.swatchRadius))
                        .background(swatch)
                        .border(1.dp, t.line2, RoundedCornerShape(t.swatchRadius))
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = theme.tag.uppercase(),
                color = t.faint,
                fontSize = studioSize(9.sp, 8.sp),
                fontWeight = FontWeight.SemiBold,
                fontFamily = t.fontUi,
                letterSpacing = 0.14.em
            )
            Spacer(Modifier.weight(1f))
            StudioChip(
                text = "Preview",
                selected = false,
                onClick = onPreview,
                minHeight = 36.dp
            )
            Spacer(Modifier.width(6.dp))
            StudioChip(
                text = if (isActive) "Active" else "Select",
                selected = isActive || isSelected,
                selectedColor = if (isActive) t.good else t.accentFill,
                onClick = onSelect,
                minHeight = 36.dp
            )
        }
    }
}

/** The strip that replaces the top bar while a mood is being previewed. */
@Composable
fun PreviewBanner(
    theme: StudioTheme,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalStudioTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Previewing",
                color = t.accent,
                fontSize = studioSize(9.5.sp, 8.sp),
                fontWeight = FontWeight.SemiBold,
                fontFamily = t.fontUi,
                letterSpacing = 0.18.em
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = theme.displayName,
                color = t.ink,
                fontSize = studioSize(20.sp, 14.sp),
                fontWeight = FontWeight.Bold,
                fontFamily = t.fontDisplay,
                maxLines = 1
            )
        }
        StudioChip(text = "Cancel", selected = false, onClick = onCancel, minHeight = 40.dp)
        Spacer(Modifier.width(8.dp))
        StudioChip(
            text = "Apply",
            selected = true,
            onClick = onApply,
            minHeight = 40.dp
        )
    }
}

/* ==========================================================================
   Sheet chrome
   ========================================================================== */

@Composable
private fun StudioSheetScrim(visible: Boolean, onDismiss: () -> Unit) {
    val t = LocalStudioTokens.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(140))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(t.deep.copy(alpha = if (t.dark) 0.62f else 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
}

@Composable
private fun SheetPanel(content: @Composable ColumnScope.() -> Unit) {
    val t = LocalStudioTokens.current
    val shape = RoundedCornerShape(topStart = t.rPanel, topEnd = t.rPanel)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(horizontal = if (t.motif == Motif.PIXEL) 0.dp else 8.dp, vertical = 0.dp)
            .clip(shape)
            .background(t.panel)
            .border(t.bw, t.line2, shape)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content
    )
}

@Composable
private fun SheetGrabber() {
    val t = LocalStudioTokens.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(mix(t.panel, t.ink, 0.25f))
        )
    }
}
