package com.surroundsound.app.domain.dna

import com.surroundsound.app.domain.model.SourceAudio

/**
 * Recommends which audio source should serve as the "Primary Source"
 * in a multi-source project, based on transient clarity and acoustic power.
 */
object SourceRankingHeuristic {

    data class ScoredSource(
        val source: SourceAudio,
        val dna: SoundDna,
        val primaryScore: Float // 0.0f .. 100.0f
    )

    /**
     * Scores a sound based on its suitability to anchor rhythmic grooves and drive mode presets.
     */
    fun calculateScore(dna: SoundDna): Float {
        if (dna.isSilent) return 0f

        // High transient sharpness is the highest signal for rhythm anchoring (45%)
        val transientScore = dna.transientStrength * 45f

        // Good, clear RMS volume level without extreme clipping (25%)
        val energyScore = (dna.rmsEnergy.coerceIn(0.05f, 0.7f) / 0.7f) * 25f

        // Frequency presence (moderate centroid preferable over extreme sub/shrill) (15%)
        val freqFactor = when {
            dna.spectralCentroid in 200f..4000f -> 1.0f
            dna.spectralCentroid > 4000f -> 0.7f
            else -> 0.5f
        }
        val freqScore = freqFactor * 15f

        // Controlled decay (snappy/punchy hits make cleaner beats than infinite diffuse drone) (15%)
        val decayFactor = when {
            dna.decayTimeMs in 50f..400f -> 1.0f
            dna.decayTimeMs < 50f -> 0.6f
            else -> 0.7f
        }
        val decayScore = decayFactor * 15f

        return (transientScore + energyScore + freqScore + decayScore).coerceIn(0f, 100f)
    }

    /**
     * Ranks a list of source/DNA pairs, highest score first.
     */
    fun rankSources(sourcesWithDna: List<Pair<SourceAudio, SoundDna>>): List<ScoredSource> {
        return sourcesWithDna.map { (source, dna) ->
            ScoredSource(
                source = source,
                dna = dna,
                primaryScore = calculateScore(dna)
            )
        }.sortedByDescending { it.primaryScore }
    }
}
