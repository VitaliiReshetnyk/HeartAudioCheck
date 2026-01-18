package com.example.heartaudiocheck.analysis

import kotlin.math.abs

object QualityGate {
    data class Quality(val score: Double, val clipping: Double, val bandRatio: Double)

    fun compute(raw: ShortArray, bandpassed: DoubleArray): Quality {
        val n = minOf(raw.size, bandpassed.size)
        if (n <= 0) return Quality(0.0, 1.0, 0.0)

        var clip = 0
        for (i in 0 until n) {
            if (abs(raw[i].toInt()) > 32000) clip++
        }
        val clipping = clip.toDouble() / n.toDouble()

        var eBand = 0.0
        var eRaw = 0.0
        for (i in 0 until n) {
            val r = raw[i].toDouble()
            eRaw += r * r
            val b = bandpassed[i]
            eBand += b * b
        }
        val bandRatio = if (eRaw > 1e-9) eBand / eRaw else 0.0

        val s1 = (1.0 - (clipping / 0.02)).coerceIn(0.0, 1.0)
        val s2 = ((bandRatio - 0.05) / 0.25).coerceIn(0.0, 1.0)
        val score = (0.55 * s1 + 0.45 * s2).coerceIn(0.0, 1.0)

        return Quality(score, clipping, bandRatio)
    }
}