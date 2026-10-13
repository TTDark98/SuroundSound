package com.surroundsound.app.domain.transform

/**
 * The 10 confirmed offline DSP transformations strictly derived from user source audio.
 */
enum class AudioTransformationType(val displayName: String) {
    SAMPLE_SLICING("Sample Slicing"),
    PITCH_SHIFTING("Pitch Shifting"),
    PLAYBACK_SPEED_STRETCHING("Speed & Time Stretch"),
    FILTERING("Biquad Filtering"),
    DISTORTION_BIT_REDUCTION("Distortion & Bitcrush"),
    DELAY_ECHO("Feedback Delay & Echo"),
    REVERB("Reverberation"),
    REVERSE_PLAYBACK("Reverse Playback"),
    RETRIGGER_STUTTER("Retrigger & Stutter"),
    GRANULAR("Granular Synthesis")
}
