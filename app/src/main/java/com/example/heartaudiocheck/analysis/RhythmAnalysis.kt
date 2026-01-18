package com.example.heartaudiocheck.analysis

import kotlin.math.abs
import kotlin.math.sqrt

data class RhythmMetrics(
    val bpm: Double,
    val cv: Double,
    val rmssd: Double,
    val pnn50: Double,
    val label: String,
    val confidence: Double
)

object RhythmAnalysis {

    fun compute(peakIndices: IntArray, sampleRate: Int, qualityScore: Double, acStrength: Double): RhythmMetrics {
        if (peakIndices.size < 6) return RhythmMetrics(0.0, 0.0, 0.0, 0.0, "Insufficient peaks", 0.0)

        val intervals = DoubleArray(peakIndices.size - 1)
        for (i in intervals.indices) {
            intervals[i] = (peakIndices[i + 1] - peakIndices[i]).toDouble() / sampleRate
        }

        val med = median(intervals)
        val mean = intervals.average()
        val std = std(intervals, mean)
        val cv = if (mean > 1e-9) std / mean else 0.0
        val bpm = if (med > 1e-9) 60.0 / med else 0.0

        val diffs = DoubleArray(intervals.size - 1)
        for (i in diffs.indices) diffs[i] = intervals[i + 1] - intervals[i]

        val rmssd = sqrt(diffs.map { it * it }.average())
        val pnn50 = diffs.count { abs(it) > 0.050 }.toDouble() / diffs.size.toDouble()

        val altScore = alternatingScore(intervals)  // 0..1
        val irregularScore = sigmoid(10.0 * (cv - 0.12) + 8.0 * (rmssd - 0.09) + 5.0 * (pnn50 - 0.20))
        val periodicPenalty = (1.0 - acStrength).coerceIn(0.0, 1.0)

        var label = "Regular rhythm pattern"
        var conf = (0.55 + 0.45 * qualityScore).coerceIn(0.0, 1.0)

        // діапазонні мітки (не взаємовиключні, але ми вибираємо найсильнішу)
        val tachy = bpm >= 110.0
        val brady = bpm in 1.0..55.0

        val afLike = irregularScore * periodicPenalty  // нерегулярно + низька періодичність
        val bigeminyLike = altScore * (1.0 - periodicPenalty) // чергування + хоч якась періодичність
        val irregularCap = if (acStrength > 0.55) 0.75 else 1.0

        val best = maxOf(
            Pair("AFib-like irregular pattern", afLike),
            Pair("Bigeminy-like alternating pattern", bigeminyLike),
            Pair("Irregular rhythm pattern", irregularScore * 0.85),
            Pair("Tachycardia-range rhythm", if (tachy) (0.6 + 0.4 * qualityScore) else 0.0),
            Pair("Bradycardia-range rhythm", if (brady) (0.6 + 0.4 * qualityScore) else 0.0),
            Pair("Regular rhythm pattern", (1.0 - irregularScore) * (0.8 + 0.2 * acStrength))
        ) { a, b -> a.second.compareTo(b.second) }

        label = best.first
        conf = (best.second * qualityScore * irregularCap).coerceIn(0.0, 1.0)

        return RhythmMetrics(bpm, cv, rmssd, pnn50, label, conf)
    }

    private fun alternatingScore(intervals: DoubleArray): Double {
        if (intervals.size < 6) return 0.0
        // якщо інтервали чергуються короткий/довгий, то знаки (Δi - median) змінюються
        val med = median(intervals)
        var changes = 0
        var total = 0
        var prevSign = 0
        for (i in intervals.indices) {
            val s = when {
                intervals[i] > med -> 1
                intervals[i] < med -> -1
                else -> 0
            }
            if (s != 0) {
                if (prevSign != 0) {
                    total++
                    if (s != prevSign) changes++
                }
                prevSign = s
            }
        }
        if (total == 0) return 0.0
        return (changes.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
    }

    private fun median(x: DoubleArray): Double {
        val a = x.clone()
        a.sort()
        return a[a.size / 2]
    }

    private fun std(x: DoubleArray, mean: Double): Double {
        var s = 0.0
        for (v in x) {
            val d = v - mean
            s += d * d
        }
        return kotlin.math.sqrt(s / x.size)
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + kotlin.math.exp(-z))
}
