package com.surroundsound.app.domain.mode

/**
 * The 5 confirmed creative transformation modes.
 */
enum class CreativeMode(
    val title: String,
    val subtitle: String,
    val description: String
) {
    INDUSTRIAL(
        title = "Industrial",
        subtitle = "Distorted, Bitcrushed & Metallic",
        description = "Aggressive overdrive, lo-fi bit reduction, resonant combs, and metallic short echoes."
    ),
    RHYTHMIC(
        title = "Rhythmic",
        subtitle = "Percussive, Gated & Snappy",
        description = "Transient-sliced micro-loops, punchy envelope filtering, and synced syncopation."
    ),
    MELODIC(
        title = "Melodic",
        subtitle = "Tuned, Pitched & Resonant",
        description = "Pitched harmonic intervals, resonant bandpass filters, and tuned granular pads."
    ),
    AMBIENT(
        title = "Ambient",
        subtitle = "Stretched, Reverb & Textural",
        description = "Time-expanded granular textures, lush Schroeder-Moorer reverberation, and reversed beds."
    ),
    GLITCH(
        title = "Glitch",
        subtitle = "Fragmented, Stutter & Chaotic",
        description = "Sub-beat micro-stutters, chaotic buffer jumps, erratic direction reversals, and crushed artifacts."
    )
}
