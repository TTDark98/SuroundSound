package com.surroundsound.app.domain.dna

/**
 * Sound DNA metrics extracted strictly from user-provided audio via offline DSP.
 * All 6 metrics computed offline without any external services.
 */
data class SoundDna(
    val rmsEnergy: Float,            // 0.0f .. 1.0f: Overall acoustic energy
    val transientStrength: Float,     // 0.0f .. 1.0f: Attack sharpness and onset strength
    val spectralCentroid: Float,     // Hz: Center of gravity of frequency spectrum (Brightness)
    val dominantFrequency: Float,    // Hz: Peak spectral magnitude bin frequency
    val decayTimeMs: Float,          // Milliseconds: Time from peak onset to -20dB/-60dB
    val spectralFlux: Float,         // Frame-to-frame frequency change rate (Harmonic vs Noisy)
    val badges: List<String> = emptyList(), // Heuristic labels (e.g. "Punchy", "Bright", "Resonant")
    val isSilent: Boolean = false,
    val isClipped: Boolean = false
)
