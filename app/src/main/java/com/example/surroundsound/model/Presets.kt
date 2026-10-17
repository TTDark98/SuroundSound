package com.example.surroundsound.model

object Presets {
    val LOFI = Preset(
        id = BeatStyle.LOFI,
        name = "Lo-Fi Chill Hop",
        bpm = 84,
        tagline = "Dusty vinyl warmth with laid-back swing",
        description = "Syncopated desk thud kick, warm snappy rimshot, gentle 16th shakers, and soothing pentatonic chords.",
        swing = 0.25f,
        defaultScale = "minor-pentatonic",
        pattern = PatternState(
            kick = booleanArrayOf(true, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false),
            snare = booleanArrayOf(false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false),
            hihat = booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
            perc = booleanArrayOf(false, false, true, false, false, false, false, true, false, true, false, false, false, false, true, false),
            melody = listOf(
                MelodicStep(true, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 7),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 3),
                MelodicStep(false, 0),
                MelodicStep(true, 10),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 5),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 7),
                MelodicStep(true, 12),
                MelodicStep(false, 0)
            ),
            bass = listOf(
                MelodicStep(true, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(true, -14),
                MelodicStep(false, -14),
                MelodicStep(false, -14),
                MelodicStep(false, -14),
                MelodicStep(true, -9),
                MelodicStep(false, -9),
                MelodicStep(false, -9),
                MelodicStep(false, -9),
                MelodicStep(false, -9),
                MelodicStep(false, -9)
            )
        ),
        fxDefaults = FxConfig(
            reverbMix = 0.38f,
            delayMix = 0.28f,
            delayTime = 0.32f,
            delayFeedback = 0.42f,
            filterCutoff = 6500f,
            distortion = 0.15f,
            swing = 0.25f,
            scaleId = "minor-pentatonic"
        )
    )

    val TRAP = Preset(
        id = BeatStyle.TRAP,
        name = "Modern Trap",
        bpm = 140,
        tagline = "Sub-heavy punch with high-velocity rolls",
        description = "Deep 808-style desk thump, crisp metal crack snare, rapid hi-hat bursts, and hypnotic bells.",
        swing = 0.05f,
        defaultScale = "minor-pentatonic",
        pattern = PatternState(
            kick = booleanArrayOf(true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false),
            snare = booleanArrayOf(false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false),
            hihat = booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
            perc = booleanArrayOf(false, false, false, true, false, false, true, false, false, false, false, true, false, true, false, true),
            melody = listOf(
                MelodicStep(true, 12),
                MelodicStep(false, 12),
                MelodicStep(true, 15),
                MelodicStep(false, 15),
                MelodicStep(false, 0),
                MelodicStep(true, 10),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 7),
                MelodicStep(false, 0),
                MelodicStep(true, 12),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 10),
                MelodicStep(false, 0),
                MelodicStep(true, 14)
            ),
            bass = listOf(
                MelodicStep(true, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(true, -15),
                MelodicStep(false, -15),
                MelodicStep(false, -15),
                MelodicStep(true, -10),
                MelodicStep(false, -10),
                MelodicStep(false, -10),
                MelodicStep(false, -10),
                MelodicStep(false, -10),
                MelodicStep(false, -10)
            )
        ),
        fxDefaults = FxConfig(
            reverbMix = 0.25f,
            delayMix = 0.35f,
            delayTime = 0.214f,
            delayFeedback = 0.5f,
            filterCutoff = 14000f,
            distortion = 0.35f,
            swing = 0.05f,
            scaleId = "minor-pentatonic"
        )
    )

    val HOUSE = Preset(
        id = BeatStyle.HOUSE,
        name = "4-on-the-Floor Club",
        bpm = 124,
        tagline = "Driving kick pulse, offbeat hats, and soulful stabs",
        description = "Steady four-on-the-floor desk kicks with bright metallic offbeat hi-hats and rhythmic synth stabs.",
        swing = 0.08f,
        defaultScale = "dorian",
        pattern = PatternState(
            kick = booleanArrayOf(true, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false),
            snare = booleanArrayOf(false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false),
            hihat = booleanArrayOf(false, false, true, false, false, false, true, false, false, false, true, false, false, false, true, false),
            perc = booleanArrayOf(true, false, true, true, false, true, true, false, true, false, true, true, false, true, true, true),
            melody = listOf(
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 7),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 10),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 12),
                MelodicStep(true, 15),
                MelodicStep(false, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 10),
                MelodicStep(false, 0)
            ),
            bass = listOf(
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(true, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(true, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(true, -10),
                MelodicStep(false, -10),
                MelodicStep(false, -10),
                MelodicStep(false, -10),
                MelodicStep(true, -15),
                MelodicStep(false, -15)
            )
        ),
        fxDefaults = FxConfig(
            reverbMix = 0.3f,
            delayMix = 0.22f,
            delayTime = 0.24f,
            delayFeedback = 0.35f,
            filterCutoff = 12000f,
            distortion = 0.1f,
            swing = 0.08f,
            scaleId = "dorian"
        )
    )

    val AFROBEAT = Preset(
        id = BeatStyle.AFROBEAT,
        name = "Afrobeat & Amapiano",
        bpm = 112,
        tagline = "Organic polyrhythmic bounce with warm melodic kalimbas",
        description = "Syncopated clave desk thuds, metallic shaker rolls, and buoyant pentatonic plucks.",
        swing = 0.18f,
        defaultScale = "major-pentatonic",
        pattern = PatternState(
            kick = booleanArrayOf(true, false, false, true, false, false, true, false, false, true, false, false, true, false, false, false),
            snare = booleanArrayOf(false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, true),
            hihat = booleanArrayOf(true, true, false, true, true, false, true, true, false, true, true, false, true, true, false, true),
            perc = booleanArrayOf(false, true, true, false, false, true, false, true, true, false, false, true, false, true, true, false),
            melody = listOf(
                MelodicStep(true, 0),
                MelodicStep(false, 0),
                MelodicStep(true, 4),
                MelodicStep(true, 7),
                MelodicStep(false, 0),
                MelodicStep(true, 9),
                MelodicStep(false, 0),
                MelodicStep(true, 12),
                MelodicStep(false, 0),
                MelodicStep(true, 9),
                MelodicStep(false, 0),
                MelodicStep(true, 7),
                MelodicStep(false, 0),
                MelodicStep(true, 4),
                MelodicStep(false, 0),
                MelodicStep(true, 2)
            ),
            bass = listOf(
                MelodicStep(true, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(true, -12),
                MelodicStep(false, -12),
                MelodicStep(false, -12),
                MelodicStep(true, -10),
                MelodicStep(false, -10),
                MelodicStep(false, -10),
                MelodicStep(true, -10),
                MelodicStep(false, -10),
                MelodicStep(false, -10),
                MelodicStep(true, -14),
                MelodicStep(false, -14),
                MelodicStep(false, -14),
                MelodicStep(false, -14)
            )
        ),
        fxDefaults = FxConfig(
            reverbMix = 0.42f,
            delayMix = 0.32f,
            delayTime = 0.268f,
            delayFeedback = 0.45f,
            filterCutoff = 10000f,
            distortion = 0.08f,
            swing = 0.18f,
            scaleId = "major-pentatonic"
        )
    )

    val SYNTHWAVE = Preset(
        id = BeatStyle.SYNTHWAVE,
        name = "Cyber Synthwave",
        bpm = 118,
        tagline = "Gated 80s reverberation and futuristic synth arpeggios",
        description = "Punchy gated desk kick, huge metallic stadium snare, driving 16th hats, and resonant sci-fi arps.",
        swing = 0.0f,
        defaultScale = "minor-pentatonic",
        pattern = PatternState(
            kick = booleanArrayOf(true, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false),
            snare = booleanArrayOf(false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false),
            hihat = booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
            perc = booleanArrayOf(false, false, true, false, false, false, true, false, false, false, true, false, false, false, true, true),
            melody = listOf(
                MelodicStep(true, 0),
                MelodicStep(true, 7),
                MelodicStep(true, 12),
                MelodicStep(true, 15),
                MelodicStep(true, 10),
                MelodicStep(true, 7),
                MelodicStep(true, 5),
                MelodicStep(true, 7),
                MelodicStep(true, 0),
                MelodicStep(true, 7),
                MelodicStep(true, 12),
                MelodicStep(true, 15),
                MelodicStep(true, 17),
                MelodicStep(true, 15),
                MelodicStep(true, 12),
                MelodicStep(true, 10)
            ),
            bass = listOf(
                MelodicStep(true, -12),
                MelodicStep(true, -12),
                MelodicStep(true, -12),
                MelodicStep(true, -12),
                MelodicStep(true, -12),
                MelodicStep(true, -12),
                MelodicStep(true, -12),
                MelodicStep(true, -12),
                MelodicStep(true, -14),
                MelodicStep(true, -14),
                MelodicStep(true, -14),
                MelodicStep(true, -14),
                MelodicStep(true, -10),
                MelodicStep(true, -10),
                MelodicStep(true, -10),
                MelodicStep(true, -10)
            )
        ),
        fxDefaults = FxConfig(
            reverbMix = 0.5f,
            delayMix = 0.38f,
            delayTime = 0.254f,
            delayFeedback = 0.55f,
            filterCutoff = 15000f,
            distortion = 0.2f,
            swing = 0.0f,
            scaleId = "minor-pentatonic"
        )
    )

    val ALL_PRESETS = listOf(LOFI, TRAP, HOUSE, AFROBEAT, SYNTHWAVE)

    fun getPreset(style: BeatStyle): Preset {
        return ALL_PRESETS.find { it.id == style } ?: LOFI
    }
}
