package com.example.surroundsound.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DelayEffect(private val sampleRate: Int = 44100, maxDelaySec: Float = 1.0f) {
    private val bufferSize = (sampleRate * maxDelaySec).toInt() + 1
    private val leftBuffer = FloatArray(bufferSize)
    private val rightBuffer = FloatArray(bufferSize)
    private var writeIndex = 0

    var delayTimeSec: Float = 0.28f
    var feedback: Float = 0.4f
    var mix: Float = 0.25f

    // Damping one-pole lowpass filter
    private var dampL = 0f
    private var dampR = 0f

    fun reset() {
        leftBuffer.fill(0f)
        rightBuffer.fill(0f)
        writeIndex = 0
        dampL = 0f
        dampR = 0f
    }

    fun process(inL: Float, inR: Float, out: FloatArray) {
        val delaySamples = (delayTimeSec * sampleRate).toInt().coerceIn(1, bufferSize - 1)
        var readIndex = writeIndex - delaySamples
        if (readIndex < 0) readIndex += bufferSize

        val delayedL = leftBuffer[readIndex]
        val delayedR = rightBuffer[readIndex]

        // Lowpass damping filter
        dampL = dampL * 0.4f + delayedL * 0.6f
        dampR = dampR * 0.4f + delayedR * 0.6f

        leftBuffer[writeIndex] = (inL + dampL * feedback.coerceIn(0f, 0.85f)).coerceIn(-2f, 2f)
        rightBuffer[writeIndex] = (inR + dampR * feedback.coerceIn(0f, 0.85f)).coerceIn(-2f, 2f)

        writeIndex = (writeIndex + 1) % bufferSize

        out[0] = inL * (1f - mix) + delayedL * mix
        out[1] = inR * (1f - mix) + delayedR * mix
    }
}

class CombFilter(bufferLength: Int) {
    private val buffer = FloatArray(bufferLength)
    private var bufferIndex = 0
    var feedback: Float = 0.8f
    var damp: Float = 0.2f
    private var filterStore: Float = 0f

    fun process(input: Float): Float {
        val output = buffer[bufferIndex]
        filterStore = output * (1f - damp) + filterStore * damp
        buffer[bufferIndex] = input + filterStore * feedback
        bufferIndex = (bufferIndex + 1) % buffer.size
        return output
    }

    fun reset() {
        buffer.fill(0f)
        bufferIndex = 0
        filterStore = 0f
    }
}

class AllPassFilter(bufferLength: Int) {
    private val buffer = FloatArray(bufferLength)
    private var bufferIndex = 0
    var feedback: Float = 0.5f

    fun process(input: Float): Float {
        val bufOut = buffer[bufferIndex]
        val output = -input + bufOut
        buffer[bufferIndex] = input + bufOut * feedback
        bufferIndex = (bufferIndex + 1) % buffer.size
        return output
    }

    fun reset() {
        buffer.fill(0f)
        bufferIndex = 0
    }
}

class ReverbEffect(sampleRate: Int = 44100) {
    var mix: Float = 0.35f

    // 8 parallel comb filters tuned to prime numbers for diffusion
    private val combTuning = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    private val combFiltersL = combTuning.map { CombFilter((it * sampleRate / 44100)) }
    private val combFiltersR = combTuning.map { CombFilter(((it + 23) * sampleRate / 44100)) }

    // 4 series allpass filters
    private val allPassTuning = intArrayOf(556, 441, 341, 225)
    private val allPassL = allPassTuning.map { AllPassFilter(it * sampleRate / 44100) }
    private val allPassR = allPassTuning.map { AllPassFilter((it + 23) * sampleRate / 44100) }

    fun reset() {
        combFiltersL.forEach { it.reset() }
        combFiltersR.forEach { it.reset() }
        allPassL.forEach { it.reset() }
        allPassR.forEach { it.reset() }
    }

    fun process(inL: Float, inR: Float, out: FloatArray) {
        val input = (inL + inR) * 0.5f * 0.015f

        var outL = 0f
        var outR = 0f
        for (i in combFiltersL.indices) {
            outL += combFiltersL[i].process(input)
            outR += combFiltersR[i].process(input)
        }

        for (ap in allPassL) {
            outL = ap.process(outL)
        }
        for (ap in allPassR) {
            outR = ap.process(outR)
        }

        out[0] = inL * (1f - mix) + outL * mix * 2.5f
        out[1] = inR * (1f - mix) + outR * mix * 2.5f
    }
}

object WaveShaper {
    fun distort(input: Float, amount: Float): Float {
        if (amount <= 0.001f) return input
        val k = max(0.1f, amount * 50f)
        val x = input.coerceIn(-1f, 1f)
        val deg = (Math.PI / 180.0).toFloat()
        val num = (3f + k) * x * 20f * deg
        val denom = Math.PI.toFloat() + k * abs(x)
        return (num / denom).coerceIn(-1f, 1f)
    }
}
