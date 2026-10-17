package com.example.surroundsound.ui.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The studio's read-outs are pure functions, so they get tested here rather than on a device:
 * a wrong delay division or timecode is exactly the sort of thing that ships unnoticed.
 */
class StudioFormatsTest {

    @Test
    fun timecode_showsHundredths() {
        assertEquals("00:02.80", StudioFormats.timecode(2.8f))
        assertEquals("00:00.00", StudioFormats.timecode(0f))
        assertEquals("01:07.64", StudioFormats.timecode(67.64f))
    }

    @Test
    fun recorderClock_showsTenths() {
        assertEquals("00:00.0", StudioFormats.recorderClock(0f))
        assertEquals("00:03.4", StudioFormats.recorderClock(3.44f))
        assertEquals("00:12.0", StudioFormats.recorderClock(12f))
    }

    @Test
    fun seconds_carriesTheUnit() {
        assertEquals("7.6 S", StudioFormats.seconds(7.64f))
        assertEquals("0.0 S", StudioFormats.seconds(-1f))
    }

    @Test
    fun compactClock_readsLikeATakeTimer() {
        assertEquals("0.0", StudioFormats.compactClock(0f))
        assertEquals("3.4", StudioFormats.compactClock(3.44f))
    }

    @Test
    fun cutoff_switchesToKilohertz() {
        assertEquals("780 Hz", StudioFormats.cutoff(780f))
        assertEquals("6.5k Hz", StudioFormats.cutoff(6500f))
        assertEquals("200 Hz", StudioFormats.cutoff(10f))
        assertEquals("16.0k Hz", StudioFormats.cutoff(90000f))
    }

    @Test
    fun percentValue_roundsToWholeNumbers() {
        assertEquals("18 %", StudioFormats.percentValue(17.6f))
        assertEquals("0 %", StudioFormats.percentValue(0f))
    }

    @Test
    fun delayDivision_matchesTheSixteenthGrid() {
        // 84 bpm -> a sixteenth is 0.1786 s, so the lo-fi preset's 0.32 s lands on 2/16.
        assertEquals(2, StudioFormats.delayDivision(0.32f, 84))
        assertEquals("2/16", StudioFormats.delayLabel(2))
        assertEquals(0.5357f, StudioFormats.delaySeconds(3, 84), 0.001f)
    }

    @Test
    fun delayDivision_roundTripsForEveryStep() {
        for (bpm in intArrayOf(40, 84, 112, 140, 220)) {
            for (division in StudioFormats.DELAY_DIVISIONS) {
                val seconds = StudioFormats.delaySeconds(division, bpm)
                assertEquals(
                    "division $division at $bpm bpm",
                    division,
                    StudioFormats.delayDivision(seconds, bpm)
                )
            }
        }
    }

    @Test
    fun logSweep_roundTrips() {
        for (fraction in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val hz = StudioFormats.logValue(fraction, StudioFormats.MIN_CUTOFF, StudioFormats.MAX_CUTOFF)
            assertEquals(
                fraction,
                StudioFormats.logFraction(hz, StudioFormats.MIN_CUTOFF, StudioFormats.MAX_CUTOFF),
                0.001f
            )
        }
    }

    @Test
    fun scaleShortNames_fitTheChips() {
        assertEquals("Minor Pent.", StudioFormats.scaleShortName("minor-pentatonic"))
        assertEquals("Major Pent.", StudioFormats.scaleShortName("major-pentatonic"))
        assertEquals("Dorian", StudioFormats.scaleShortName("dorian"))
        assertTrue(StudioFormats.scaleShortName("blues").length <= 12)
    }

    @Test
    fun midiLabels_trackTheScaleIntervals() {
        assertEquals("60", StudioFormats.midiLabel(60, 0))
        assertEquals("67", StudioFormats.midiLabel(60, 7))
    }

    @Test
    fun exportSpec_countsFourBars() {
        assertEquals(
            "16-step beat • 0:11 loop • WAV 44.1 kHz",
            StudioFormats.exportSpec(84)
        )
    }

    @Test
    fun exportFileName_namesTheMood() {
        assertEquals(
            "surroundsound-lofi-84bpm.wav",
            StudioFormats.exportFileName(com.example.surroundsound.model.BeatStyle.LOFI, 84)
        )
    }
}
