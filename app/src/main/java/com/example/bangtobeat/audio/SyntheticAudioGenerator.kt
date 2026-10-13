package com.example.bangtobeat.audio

import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object SyntheticAudioGenerator {
    const val SAMPLE_RATE = 44100

    fun generateTableBang(): FloatArray {
        val duration = 0.85f
        val length = (SAMPLE_RATE * duration).toInt()
        val data = FloatArray(length)
        val onsetDelay = 0.035f

        for (i in 0 until length) {
            val time = i.toFloat() / SAMPLE_RATE
            if (time < onsetDelay) {
                data[i] = (Random.nextFloat() * 2f - 1f) * 0.003f
                continue
            }
            val t = time - onsetDelay

            // 1. Initial transient click (wood impact knock)
            val clickEnv = exp(-t * 180f)
            val click = (Random.nextFloat() * 2f - 1f) * clickEnv * 0.9f

            // 2. Heavy desk body resonance (sub thud around 75Hz -> 45Hz)
            val freq = 75f - t * 40f
            val subEnv = exp(-t * 12f)
            val sub = sin(2.0 * Math.PI * freq * t).toFloat() * subEnv * 0.95f

            // 3. Wood cavity mid ring (180Hz)
            val midEnv = exp(-t * 22f)
            val mid = sin(2.0 * Math.PI * 180.0 * t).toFloat() * midEnv * 0.35f

            data[i] = click + sub + mid
        }
        return data
    }

    fun generateMetalHit(): FloatArray {
        val duration = 0.95f
        val length = (SAMPLE_RATE * duration).toInt()
        val data = FloatArray(length)
        val onsetDelay = 0.04f

        val freqs = floatArrayOf(620f, 1140f, 1980f, 2740f, 4200f)
        val decays = floatArrayOf(8f, 14f, 18f, 24f, 30f)
        val amps = floatArrayOf(0.6f, 0.4f, 0.3f, 0.2f, 0.15f)

        for (i in 0 until length) {
            val time = i.toFloat() / SAMPLE_RATE
            if (time < onsetDelay) {
                data[i] = (Random.nextFloat() * 2f - 1f) * 0.002f
                continue
            }
            val t = time - onsetDelay

            var `val` = 0f
            for (f in freqs.indices) {
                `val` += sin(2.0 * Math.PI * freqs[f] * t).toFloat() * exp(-t * decays[f]) * amps[f]
            }
            // Crisp strike click
            `val` += (Random.nextFloat() * 2f - 1f) * exp(-t * 300f) * 0.6f
            data[i] = `val`
        }
        return data
    }

    fun generateKnuckleTap(): FloatArray {
        val duration = 0.7f
        val length = (SAMPLE_RATE * duration).toInt()
        val data = FloatArray(length)
        val onsetDelay = 0.03f

        for (i in 0 until length) {
            val time = i.toFloat() / SAMPLE_RATE
            if (time < onsetDelay) {
                data[i] = (Random.nextFloat() * 2f - 1f) * 0.002f
                continue
            }
            val t = time - onsetDelay

            val click = (Random.nextFloat() * 2f - 1f) * exp(-t * 220f) * 0.8f
            val thud = sin(2.0 * Math.PI * 130.0 * t).toFloat() * exp(-t * 35f) * 0.7f
            data[i] = click + thud
        }
        return data
    }

    fun generateGlassPing(): FloatArray {
        val duration = 1.0f
        val length = (SAMPLE_RATE * duration).toInt()
        val data = FloatArray(length)
        val onsetDelay = 0.04f

        for (i in 0 until length) {
            val time = i.toFloat() / SAMPLE_RATE
            if (time < onsetDelay) {
                data[i] = (Random.nextFloat() * 2f - 1f) * 0.002f
                continue
            }
            val t = time - onsetDelay

            val f1 = 1520.0
            val f2 = 3040.0
            val ring = (sin(2.0 * Math.PI * f1 * t).toFloat() * 0.7f +
                    sin(2.0 * Math.PI * f2 * t).toFloat() * 0.3f) * exp(-t * 5.5f)
            val ping = (Random.nextFloat() * 2f - 1f) * exp(-t * 400f) * 0.4f
            data[i] = ring + ping
        }
        return data
    }
}
