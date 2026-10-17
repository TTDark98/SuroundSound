package com.example.surroundsound.ui.studio

import com.example.surroundsound.model.BeatStyle
import com.example.surroundsound.model.Scales
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * The numeric and label plumbing the studio screens share: timecodes, the FX
 * scale mappings and the short names the design uses for scales and pitches.
 *
 * Pure Kotlin on purpose — no Compose — so the mappings can be unit tested.
 */
object StudioFormats {

    /** `00:02.80` — trim markers, milliseconds included. */
    fun timecode(seconds: Float): String {
        val s = seconds.coerceAtLeast(0f)
        val total = (s * 100f).roundToInt()
        val mm = total / 6000
        val ss = (total % 6000) / 100
        val cs = total % 100
        return "%02d:%02d.%02d".format(mm, ss, cs)
    }

    /** `00:00.0` — the recorder's running clock, tenths only. */
    fun recorderClock(seconds: Float): String {
        val s = seconds.coerceAtLeast(0f)
        val total = (s * 10f).roundToInt()
        val mm = total / 600
        val ss = (total % 600) / 10
        val tenths = total % 10
        return "%02d:%02d.%d".format(mm, ss, tenths)
    }

    /** `7.6 S` — the bed length caption under the waveform. */
    fun seconds(seconds: Float): String = "%.1f S".format(seconds.coerceAtLeast(0f))

    /** `3.4` — the compact seconds read-out under the transport. */
    fun compactClock(seconds: Float): String {
        val tenths = (seconds.coerceAtLeast(0f) * 10f).toInt()
        return "%d.%d".format(tenths / 10, tenths % 10)
    }

    /** `780 Hz` / `6.5k Hz` — the filter cutoff read-out. */
    fun cutoff(hz: Float): String {
        val v = hz.coerceIn(MIN_CUTOFF, MAX_CUTOFF).roundToInt()
        return if (v >= 1000) "%.1fk Hz".format(v / 1000f) else "$v Hz"
    }

    /** `18 %` from a 0..100 value. */
    fun percentValue(value: Float): String = "${value.roundToInt()} %"

    /** `3/16` — delay time expressed in sixteenths, the way the rack labels it. */
    fun delayLabel(division: Int): String = "${division.coerceIn(MIN_DELAY_DIV, MAX_DELAY_DIV)}/16"

    /** The sixteenth-note grid the delay time snaps to. */
    val DELAY_DIVISIONS = (MIN_DELAY_DIV..MAX_DELAY_DIV).toList()

    fun stepSeconds(bpm: Int): Float = 60f / bpm.coerceAtLeast(40) / 4f

    /** Seconds for a delay of `division` sixteenths at [bpm]. */
    fun delaySeconds(division: Int, bpm: Int): Float = stepSeconds(bpm) * division

    /** The nearest sixteenth division for a delay in seconds. */
    fun delayDivision(delaySeconds: Float, bpm: Int): Int {
        val step = stepSeconds(bpm)
        if (step <= 0f) return MIN_DELAY_DIV
        return (delaySeconds / step).roundToInt().coerceIn(MIN_DELAY_DIV, MAX_DELAY_DIV)
    }

    /** 0..1 across a log sweep, for a frequency slider. */
    fun logFraction(value: Float, min: Float, max: Float): Float {
        val v = value.coerceIn(min, max)
        return (ln(v / min) / ln(max / min)).coerceIn(0f, 1f)
    }

    /** The inverse of [logFraction]. */
    fun logValue(fraction: Float, min: Float, max: Float): Float =
        min * (max / min).pow(fraction.coerceIn(0f, 1f))

    private fun Float.pow(exponent: Float): Float =
        kotlin.math.exp(ln(this.coerceAtLeast(1e-6f)) * exponent)

    /** `Minor Pent.` — the chip labels on the musical-scale panel. */
    fun scaleShortName(id: String): String = when (id) {
        "minor-pentatonic" -> "Minor Pent."
        "major-pentatonic" -> "Major Pent."
        "dorian" -> "Dorian"
        "hirajoshi" -> "Hirajoshi"
        "blues" -> "Blues"
        "arabic-hijaz" -> "Hijaz"
        else -> Scales.getScale(id).name.substringBefore(" (")
    }

    /** MIDI note number for a semitone offset above [root], as the pitch strip prints it. */
    fun midiLabel(root: Int, semitoneOffset: Int): String = (root + semitoneOffset).toString()

    /** `16-step beat • 0:11 loop • WAV 44.1 kHz` — the export summary line. */
    fun exportSpec(bpm: Int, bars: Int = 4, sampleRate: Int = 44100): String {
        val beats = bars * 4
        val seconds = (beats * 60f / bpm.coerceAtLeast(40)).roundToInt()
        return "16-step beat • 0:%02d loop • WAV %.1f kHz".format(seconds, sampleRate / 1000f)
    }

    fun exportFileName(style: BeatStyle, bpm: Int): String = "surroundsound-${style.id}-${bpm}bpm.wav"

    fun mixPercentLabel(fraction: Float): String = "${(fraction.coerceIn(0f, 1f) * 100f).roundToInt()} %"

    /** True when two floats are within [tolerance]; used to skip no-op FX writes. */
    fun nearly(a: Float, b: Float, tolerance: Float = 0.0005f): Boolean = abs(a - b) < tolerance

    const val MIN_CUTOFF = 200f
    const val MAX_CUTOFF = 16000f
    const val MIN_DELAY_DIV = 1
    const val MAX_DELAY_DIV = 8
}
