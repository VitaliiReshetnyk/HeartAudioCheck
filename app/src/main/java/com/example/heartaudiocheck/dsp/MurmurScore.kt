package com.example.heartaudiocheck.dsp

import kotlin.math.sqrt

object MurmurScore {

    data class MurmurResult(
        val score: Double,
        val cyclesUsed: Int
    )

    fun compute(
        lowBand: DoubleArray,      // 20–200 Hz (вже є)
        highBand: DoubleArray,     // 200–600 Hz (нове)
        peakIdx: IntArray,
        sampleRate: Int,
        periodSamples: Int
    ): MurmurResult {
        if (peakIdx.size < 6) return MurmurResult(0.0, 0)

        val ratios = ArrayList<Double>()

        val systoleLen = (0.35 * periodSamples).toInt().coerceAtLeast((0.12 * sampleRate).toInt())
        for (i in 0 until peakIdx.size - 1) {
            val a = peakIdx[i]
            val b = peakIdx[i + 1]
            if (b - a < (0.40 * sampleRate).toInt()) continue

            val s1 = a
            val s2 = (a + systoleLen).coerceAtMost(b)

            val eHighSys = rms(highBand, s1, s2)
            val eHighDia = rms(highBand, s2, b)
            val eLowSys  = rms(lowBand,  s1, s2)

            val r1 = eHighSys / (eLowSys + 1e-9)
            val r2 = eHighSys / (eHighDia + 1e-9)

            ratios.add(0.6 * r1 + 0.4 * r2)
        }

        if (ratios.isEmpty()) return MurmurResult(0.0, 0)

        ratios.sort()
        val med = ratios[ratios.size / 2]

        val score = sigmoid(1.8 * (med - 0.35))  // стартовий поріг
        return MurmurResult(score.coerceIn(0.0, 1.0), ratios.size)
    }

    private fun rms(x: DoubleArray, a: Int, b: Int): Double {
        if (b <= a + 2) return 0.0
        var s = 0.0
        var n = 0
        var i = a
        while (i < b && i < x.size) {
            val v = x[i]
            s += v * v
            n++
            i++
        }
        if (n == 0) return 0.0
        return sqrt(s / n.toDouble())
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + kotlin.math.exp(-z))
}
