package com.example.surroundsound.model

import kotlin.math.pow

object Scales {
    val ALL_SCALES = listOf(
        MusicalScale(
            id = "minor-pentatonic",
            name = "Minor Pentatonic (Chill / Trap)",
            intervals = listOf(0, 3, 5, 7, 10, 12, 15, 17)
        ),
        MusicalScale(
            id = "major-pentatonic",
            name = "Major Pentatonic (Uplifting)",
            intervals = listOf(0, 2, 4, 7, 9, 12, 14, 16)
        ),
        MusicalScale(
            id = "dorian",
            name = "Lo-Fi Dorian (Nostalgic Jazz)",
            intervals = listOf(0, 2, 3, 5, 7, 9, 10, 12)
        ),
        MusicalScale(
            id = "hirajoshi",
            name = "Japanese Insen / Hirajoshi (Zen / Metal)",
            intervals = listOf(0, 2, 3, 7, 8, 12, 14, 15)
        ),
        MusicalScale(
            id = "blues",
            name = "Blues Scale (Soulful Funk)",
            intervals = listOf(0, 3, 5, 6, 7, 10, 12, 15)
        ),
        MusicalScale(
            id = "arabic-hijaz",
            name = "Hijaz / Phrygian Dominant (Exotic)",
            intervals = listOf(0, 1, 4, 5, 7, 8, 10, 12)
        )
    )

    fun getScale(id: String): MusicalScale {
        return ALL_SCALES.find { it.id == id } ?: ALL_SCALES[0]
    }

    fun semitoneToPitchRatio(semitones: Int): Float {
        return 2.0.pow(semitones.toDouble() / 12.0).toFloat()
    }
}
