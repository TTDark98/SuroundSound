package com.example.surroundsound.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surroundsound.R

/**
 * One app shell, three moods — the SurroundSound design system.
 *
 * The values here are the token blocks from the theme design's working prototype
 * (css/tokens.css), where Mossroom is the dark "Deep Hollow" forest interior and
 * Pixel Hearth is the daylight "Meadow" rework. Everything the studio draws —
 * colours, corner radii, border widths, fonts — is read from [StudioTokens], so a
 * theme swap is a single state change with no per-screen branching.
 */
enum class StudioTheme(
    val id: String,
    val displayName: String,
    val mood: String,
    val tag: String,
    val blurb: String,
    val swatches: List<Long>
) {
    MOSSROOM(
        id = "mossroom",
        displayName = "Mossroom",
        mood = "Deep forest",
        tag = "forest studio",
        blurb = "Forest greens, moss and amber.",
        swatches = listOf(0xFF0E1710, 0xFF22352A, 0xFF8FB35F, 0xFFD99A3C, 0xFFE8EFDF)
    ),
    PIXEL_HEARTH(
        id = "pixel",
        displayName = "Pixel Hearth",
        mood = "Meadow",
        tag = "pixel meadow",
        blurb = "Daylight, whole pixels and loud colour.",
        swatches = listOf(0xFFFFF8E7, 0xFF8FD8EE, 0xFF7ED05F, 0xFFE8502A, 0xFF6F5BE0)
    ),
    SUNDAY_ANALOG(
        id = "sunday",
        displayName = "Sunday Analog",
        mood = "Warm parlor",
        tag = "warm parlor",
        blurb = "Cream, dark wood and brass.",
        swatches = listOf(0xFFF3E9D8, 0xFFEADFC9, 0xFFB08D4F, 0xFFA24B33, 0xFF2E2019)
    );

    companion object {
        val DEFAULT = MOSSROOM

        fun fromId(id: String?): StudioTheme = values().firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** The decorative language a theme speaks. Drives the small material touches. */
enum class Motif { BOTANICAL, PIXEL, ANALOG }

/** Pixel Hearth's illustration-only palette. */
@Immutable
data class PixelPalette(
    val sky: Color,
    val sun: Color,
    val grass: Color,
    val pink: Color,
    val lilac: Color,
    val leaf: Color
)

@Immutable
data class StudioTokens(
    val theme: StudioTheme,
    val motif: Motif,
    val dark: Boolean,

    // surfaces
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val panel: Color,
    val panel2: Color,
    val deep: Color,

    // text
    val ink: Color,
    val sub: Color,
    val faint: Color,

    // rules
    val line: Color,
    val line2: Color,

    // accent
    val accent: Color,
    val accentD: Color,
    val accentFill: Color,
    val accentHover: Color,
    val accentText: Color,
    val onAccent: Color,

    val good: Color,
    val bad: Color,
    val rec: Color,

    /** Six track colours, in [com.example.surroundsound.model.TrackId] order. */
    val tracks: List<Color>,
    val pixels: PixelPalette?,

    // geometry
    val rPanel: Dp,
    val rInner: Dp,
    val bw: Dp,

    // type
    val fontDisplay: FontFamily,
    val fontUi: FontFamily,
    val fontNum: FontFamily,
    val fontRead: FontFamily,
    val numWeight: FontWeight,
    val tempoSize: TextUnit,

    // material traits
    val panelBorder: Color,
    val labelColor: Color,
    val lamp: Color,
    /** Flat offset shadow instead of a soft one (Pixel Hearth). */
    val hardShadow: Boolean,
    /** Inset second frame drawn inside a panel (Pixel Hearth, Sunday Analog). */
    val innerFrame: Dp,
    /** Sunday Analog prints dotted leaders and rotary dials on the FX rack. */
    val knobFx: Boolean,
    /** Saturated motif colours used to tint sequencer downbeats. */
    val beatCell: Color,
    /** Corner radius for palette swatch chips. */
    val swatchRadius: Dp
)

private val Sans = FontFamily.SansSerif
private val Serif = FontFamily.Serif
private val Mono = FontFamily.Monospace

private val Silkscreen = FontFamily(
    Font(R.font.silkscreen_regular, FontWeight.Normal),
    Font(R.font.silkscreen_bold, FontWeight.Bold)
)

private fun c(hex: Long): Color = Color(hex)

private val MOSSROOM = StudioTokens(
    theme = StudioTheme.MOSSROOM,
    motif = Motif.BOTANICAL,
    dark = true,
    bg = c(0xFF0E1710),
    bg2 = c(0xFF14211A),
    surface = c(0xFF1B2A1D),
    panel = c(0xFF22352A),
    panel2 = c(0xFF293E31),
    deep = c(0xFF070D09),
    ink = c(0xFFE8EFDF),
    sub = c(0xFFA8BFA0),
    faint = c(0xFF8FA98A),
    line = c(0xFF2C4230),
    line2 = c(0xFF3A5540),
    accent = c(0xFFD99A3C),
    accentD = c(0xFFB07724),
    accentFill = c(0xFFD99A3C),
    accentHover = c(0xFFB07724),
    accentText = c(0xFFD99A3C),
    onAccent = c(0xFF17130A),
    good = c(0xFF7E9E58),
    bad = c(0xFFC2634A),
    rec = c(0xFFD2604A),
    tracks = listOf(
        c(0xFF8FB35F), c(0xFFC08B5A), c(0xFFA8C77A),
        c(0xFFD9A83C), c(0xFF6FB9A0), c(0xFF9A8FA8)
    ),
    pixels = null,
    rPanel = 16.dp,
    rInner = 9.dp,
    bw = 1.dp,
    fontDisplay = Serif,
    fontUi = Sans,
    fontNum = Mono,
    fontRead = Sans,
    numWeight = FontWeight.Bold,
    tempoSize = 40.sp,
    panelBorder = c(0xFF3A5540),
    labelColor = c(0xFFA8BFA0),
    lamp = c(0xFFD99A3C),
    hardShadow = false,
    innerFrame = 0.dp,
    knobFx = false,
    beatCell = c(0xFFE8EFDF),
    swatchRadius = 4.dp
)

private val PIXEL_HEARTH = StudioTokens(
    theme = StudioTheme.PIXEL_HEARTH,
    motif = Motif.PIXEL,
    dark = false,
    bg = c(0xFFFFF8E7),
    bg2 = c(0xFFFFFDF3),
    surface = c(0xFFFBF3DC),
    panel = c(0xFFFFFFFF),
    panel2 = c(0xFFF2F7FF),
    deep = c(0xFF23283A),
    ink = c(0xFF23283A),
    sub = c(0xFF5B6270),
    faint = c(0xFF6A7280),
    line = c(0xFF23283A),
    line2 = c(0xFF23283A),
    accent = c(0xFFE8502A),
    accentD = c(0xFF8F2A0E),
    accentFill = c(0xFFC23C18),
    accentHover = c(0xFFA8320F),
    accentText = c(0xFFC23C18),
    onAccent = c(0xFFFFF8E7),
    good = c(0xFF2A7A20),
    bad = c(0xFFDC2626),
    rec = c(0xFFE01E5A),
    tracks = listOf(
        c(0xFFE8502A), c(0xFFC08000), c(0xFF359029),
        c(0xFF2479AC), c(0xFFB44FA8), c(0xFF6F5BE0)
    ),
    pixels = PixelPalette(
        sky = c(0xFF8FD8EE),
        sun = c(0xFFFFC93C),
        grass = c(0xFF7ED05F),
        pink = c(0xFFFF7FA8),
        lilac = c(0xFFC4B5FD),
        leaf = c(0xFF4E9E3E)
    ),
    rPanel = 2.dp,
    rInner = 2.dp,
    bw = 3.dp,
    fontDisplay = Silkscreen,
    fontUi = Silkscreen,
    fontNum = Silkscreen,
    fontRead = Sans,
    numWeight = FontWeight.Normal,
    tempoSize = 34.sp,
    panelBorder = c(0xFF23283A),
    labelColor = c(0xFF6A7280),
    lamp = c(0xFFFFC93C),
    hardShadow = true,
    innerFrame = 6.dp,
    knobFx = false,
    beatCell = c(0xFF8FD8EE),
    swatchRadius = 0.dp
)

private val SUNDAY_ANALOG = StudioTokens(
    theme = StudioTheme.SUNDAY_ANALOG,
    motif = Motif.ANALOG,
    dark = false,
    bg = c(0xFFF3E9D8),
    bg2 = c(0xFFF8F0E2),
    surface = c(0xFFEADFC9),
    panel = c(0xFFFBF5EA),
    panel2 = c(0xFFF4EBDC),
    deep = c(0xFF2E2019),
    ink = c(0xFF33241B),
    sub = c(0xFF665441),
    faint = c(0xFF82705A),
    line = c(0xFFDFD2B9),
    line2 = c(0xFFCDBEA1),
    accent = c(0xFF967138),
    accentD = c(0xFF856730),
    accentFill = c(0xFFA37F41),
    accentHover = c(0xFFC4A268),
    accentText = c(0xFF7A5C29),
    onAccent = c(0xFF241A14),
    good = c(0xFF8A8858),
    bad = c(0xFFA9503A),
    rec = c(0xFFA9462F),
    tracks = listOf(
        c(0xFFA24B33), c(0xFF6E4A34), c(0xFF9E7C40),
        c(0xFF8A7A62), c(0xFFAD7746), c(0xFF3E2A20)
    ),
    pixels = null,
    rPanel = 9.dp,
    rInner = 5.dp,
    bw = 1.dp,
    fontDisplay = Serif,
    fontUi = Sans,
    fontNum = Mono,
    fontRead = Sans,
    numWeight = FontWeight.Bold,
    tempoSize = 40.sp,
    panelBorder = c(0xFFDFD2B9),
    labelColor = c(0xFF665441),
    lamp = c(0xFFB08D4F),
    hardShadow = false,
    innerFrame = 4.dp,
    knobFx = true,
    beatCell = c(0xFF33241B),
    swatchRadius = 2.dp
)

fun studioTokens(theme: StudioTheme): StudioTokens = when (theme) {
    StudioTheme.MOSSROOM -> MOSSROOM
    StudioTheme.PIXEL_HEARTH -> PIXEL_HEARTH
    StudioTheme.SUNDAY_ANALOG -> SUNDAY_ANALOG
}

val LocalStudioTokens = staticCompositionLocalOf { MOSSROOM }

/** Mixes [top] over [base] the way CSS `color-mix(in srgb, top x%, base)` would. */
fun mix(base: Color, top: Color, amount: Float): Color = lerp(base, top, amount.coerceIn(0f, 1f))

/** Alpha wash of [this] over whatever is painted underneath. */
fun Color.wash(amount: Float): Color = copy(alpha = amount)

@Composable
fun SurroundSoundTheme(
    theme: StudioTheme = StudioTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val tokens = remember(theme) { studioTokens(theme) }
    CompositionLocalProvider(LocalStudioTokens provides tokens) {
        MaterialTheme(colorScheme = tokens.materialScheme(), content = content)
    }
}

private fun StudioTokens.materialScheme() =
    if (dark) {
        darkColorScheme(
            primary = accentFill,
            onPrimary = onAccent,
            secondary = accent,
            onSecondary = onAccent,
            tertiary = good,
            onTertiary = onAccent,
            background = bg,
            onBackground = ink,
            surface = panel,
            onSurface = ink,
            surfaceVariant = panel2,
            onSurfaceVariant = sub,
            outline = line2,
            error = bad,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accentFill,
            onPrimary = onAccent,
            secondary = accent,
            onSecondary = onAccent,
            tertiary = good,
            onTertiary = onAccent,
            background = bg,
            onBackground = ink,
            surface = panel,
            onSurface = ink,
            surfaceVariant = panel2,
            onSurfaceVariant = sub,
            outline = line2,
            error = bad,
            onError = Color.White
        )
    }

/** Remembers the chosen mood across launches. */
object ThemePreference {
    private const val FILE = "surroundsound.studio"
    private const val KEY_THEME = "theme"

    fun load(context: Context): StudioTheme {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return StudioTheme.fromId(prefs.getString(KEY_THEME, null))
    }

    fun save(context: Context, theme: StudioTheme) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme.id)
            .apply()
    }
}
