package com.example.surroundsound.model

enum class BeatStyle(val id: String, val displayName: String) {
    LOFI("lofi", "Lo-Fi Chill Hop"),
    TRAP("trap", "Modern Trap"),
    HOUSE("house", "4-on-the-Floor Club"),
    AFROBEAT("afrobeat", "Afrobeat & Amapiano"),
    SYNTHWAVE("synthwave", "Cyber Synthwave");

    companion object {
        fun fromId(id: String): BeatStyle = values().find { it.id == id } ?: LOFI
    }
}

enum class TrackId(
    val id: String,
    val displayName: String,
    val colorHex: Long,
    /** Short stage name used by the studio rows, mixer chips and pads. */
    val shortName: String
) {
    KICK("kick", "Desk Kick", 0xFF10B981, "Kick"),       // Emerald
    SNARE("snare", "Crack Snare", 0xFFF59E0B, "Snare"),    // Amber
    HIHAT("hihat", "Metal Hat", 0xFF06B6D4, "Hi-hat"),     // Cyan
    PERC("perc", "Knock Perc", 0xFFEAB308, "Percussion"),      // Yellow
    MELODY("melody", "Tuned Resonator", 0xFF8B5CF6, "Melody"), // Violet
    BASS("bass", "Sub Bass", 0xFFEC4899, "Bass");        // Pink

    companion object {
        /** The six rows, top to bottom, in token track-colour order. */
        val ROWS = listOf(KICK, SNARE, HIHAT, PERC, MELODY, BASS)
    }
}

data class MusicalScale(
    val id: String,
    val name: String,
    val intervals: List<Int> // semitones from root
)

data class MelodicStep(
    val active: Boolean = false,
    val pitch: Int = 0 // semitone offset
)

data class PatternState(
    val kick: BooleanArray = BooleanArray(16),
    val snare: BooleanArray = BooleanArray(16),
    val hihat: BooleanArray = BooleanArray(16),
    val perc: BooleanArray = BooleanArray(16),
    val melody: List<MelodicStep> = List(16) { MelodicStep() },
    val bass: List<MelodicStep> = List(16) { MelodicStep(pitch = -12) }
) {
    fun copyDeep(): PatternState {
        return PatternState(
            kick = kick.clone(),
            snare = snare.clone(),
            hihat = hihat.clone(),
            perc = perc.clone(),
            melody = melody.map { it.copy() },
            bass = bass.map { it.copy() }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PatternState) return false
        return kick.contentEquals(other.kick) &&
                snare.contentEquals(other.snare) &&
                hihat.contentEquals(other.hihat) &&
                perc.contentEquals(other.perc) &&
                melody == other.melody &&
                bass == other.bass
    }

    override fun hashCode(): Int {
        var result = kick.contentHashCode()
        result = 31 * result + snare.contentHashCode()
        result = 31 * result + hihat.contentHashCode()
        result = 31 * result + perc.contentHashCode()
        result = 31 * result + melody.hashCode()
        result = 31 * result + bass.hashCode()
        return result
    }
}

data class FxConfig(
    val reverbMix: Float = 0.35f,       // 0 to 1
    val delayMix: Float = 0.25f,        // 0 to 1
    val delayTime: Float = 0.28f,       // seconds (0.05 to 0.8)
    val delayFeedback: Float = 0.4f,    // 0 to 0.8
    val filterCutoff: Float = 8000f,    // 300 to 20000 Hz
    val distortion: Float = 0.15f,      // 0 to 1
    val scaleId: String = "minor-pentatonic",
    val rootNote: Int = 0,              // semitone transpose (-12 to +12)
    val swing: Float = 0.25f            // 0 to 0.5
)

data class Preset(
    val id: BeatStyle,
    val name: String,
    val bpm: Int,
    val tagline: String,
    val description: String,
    val swing: Float,
    val defaultScale: String,
    val pattern: PatternState,
    val fxDefaults: FxConfig
)

data class SampleMetadata(
    val duration: Float,         // in seconds (trimmed)
    val rawDuration: Float,      // in seconds (original unedited)
    val sampleRate: Int = 44100,
    val channels: Int = 1,
    val peakValue: Float = 1.0f,
    val transientOffset: Float = 0f,
    val trimStart: Float = 0f,
    val trimEnd: Float = 0.45f,
    val sourceType: String = "preset-table",
    val sourceName: String = "Hardwood Desk Pound"
)
