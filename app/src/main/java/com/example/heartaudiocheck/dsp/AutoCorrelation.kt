package com.example.heartaudiocheck.dsp

import kotlin.math.max

object AutoCorrelation {
    data class PeriodEstimate(val periodSamples: Int, val strength: Double)

    fun estimatePeriodSamples(envelope: DoubleArray, sampleRate: Int): PeriodEstimate {
        // шукаємо в діапазоні 40..200 bpm => період 1.5..0.3s
        val minLag = (0.30 * sampleRate).toInt()
        val maxLag = (1.50 * sampleRate).toInt().coerceAtMost(envelope.size / 2)

        if (maxLag <= minLag + 2) return PeriodEstimate(sampleRate, 0.0)

        // нормалізація: відняти середнє
        val n = envelope.size
        var mean = 0.0
        for (v in envelope) mean += v
        mean /= n

        val x = DoubleArray(n)
        for (i in 0 until n) x[i] = envelope[i] - mean

        // енергія
        var e0 = 0.0
        for (i in 0 until n) e0 += x[i] * x[i]
        if (e0 < 1e-9) return PeriodEstimate(sampleRate, 0.0)

        var bestLag = minLag
        var bestVal = Double.NEGATIVE_INFINITY

        // проста автокореляція (достатньо швидко для 30с при 16к з envelope)
        for (lag in minLag..maxLag) {
            var s = 0.0
            var i = 0
            val lim = n - lag
            while (i < lim) {
                s += x[i] * x[i + lag]
                i++
            }
            val valNorm = s / e0
            if (valNorm > bestVal) {
                bestVal = valNorm
                bestLag = lag
            }
        }

        val strength = max(0.0, bestVal)
        return PeriodEstimate(bestLag, strength)
    }
}
