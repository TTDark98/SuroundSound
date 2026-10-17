package com.example.surroundsound.audio

import kotlin.math.cos
import kotlin.math.sin

enum class FilterType {
    LOWPASS,
    HIGHPASS,
    BANDPASS
}

class BiquadFilter(
    private var type: FilterType = FilterType.LOWPASS,
    private var frequency: Float = 1000f,
    private var q: Float = 1.0f,
    private val sampleRate: Float = 44100f
) {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    // State registers for Transposed Direct Form II
    private var z1 = 0f
    private var z2 = 0f

    init {
        updateCoefficients()
    }

    fun setParameters(type: FilterType, frequency: Float, q: Float) {
        this.type = type
        this.frequency = frequency.coerceIn(20f, sampleRate * 0.49f)
        this.q = q.coerceAtLeast(0.1f)
        updateCoefficients()
    }

    fun setFrequency(frequency: Float) {
        this.frequency = frequency.coerceIn(20f, sampleRate * 0.49f)
        updateCoefficients()
    }

    fun reset() {
        z1 = 0f
        z2 = 0f
    }

    private fun updateCoefficients() {
        val omega = (2.0 * Math.PI * frequency / sampleRate).toFloat()
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)
        val alpha = sinOmega / (2f * q)

        when (type) {
            FilterType.LOWPASS -> {
                val a0 = 1f + alpha
                b0 = ((1f - cosOmega) / 2f) / a0
                b1 = (1f - cosOmega) / a0
                b2 = ((1f - cosOmega) / 2f) / a0
                a1 = (-2f * cosOmega) / a0
                a2 = (1f - alpha) / a0
            }
            FilterType.HIGHPASS -> {
                val a0 = 1f + alpha
                b0 = ((1f + cosOmega) / 2f) / a0
                b1 = (-(1f + cosOmega)) / a0
                b2 = ((1f + cosOmega) / 2f) / a0
                a1 = (-2f * cosOmega) / a0
                a2 = (1f - alpha) / a0
            }
            FilterType.BANDPASS -> {
                val a0 = 1f + alpha
                b0 = alpha / a0
                b1 = 0f
                b2 = -alpha / a0
                a1 = (-2f * cosOmega) / a0
                a2 = (1f - alpha) / a0
            }
        }
    }

    fun process(sample: Float): Float {
        val out = b0 * sample + z1
        z1 = b1 * sample - a1 * out + z2
        z2 = b2 * sample - a2 * out
        return out
    }
}
