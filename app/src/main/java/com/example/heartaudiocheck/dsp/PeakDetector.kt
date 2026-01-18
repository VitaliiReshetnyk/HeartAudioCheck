package com.example.heartaudiocheck.dsp

import kotlin.math.abs
import kotlin.math.max

data class PeaksResult(val peakIndices: IntArray)

object PeakDetector {

    fun detect(envelope: DoubleArray, sampleRate: Int, periodSamples: Int): PeaksResult {
        val n = envelope.size
        if (n < 10) return PeaksResult(intArrayOf())

        val sorted = envelope.clone()
        sorted.sort()
        val med = sorted[n / 2]

        val dev = DoubleArray(n)
        for (i in 0 until n) dev[i] = abs(envelope[i] - med)
        dev.sort()
        val mad = dev[n / 2] + 1e-12

        val thr = med + 3.0 * mad

        // 1) спочатку збираємо всі локальні максимуми вище порога
        val candidates = ArrayList<Int>()
        for (i in 1 until n - 1) {
            if (envelope[i] > thr && envelope[i] > envelope[i - 1] && envelope[i] >= envelope[i + 1]) {
                candidates.add(i)
            }
        }
        if (candidates.isEmpty()) return PeaksResult(intArrayOf())

        // 2) зливаємо піки, які ближче ніж ~0.55 періоду (щоб S1+S2 не рахувати двічі)
        val mergeDist = (0.55 * periodSamples).toInt().coerceAtLeast((0.25 * sampleRate).toInt())

        val merged = ArrayList<Int>()
        var groupStart = candidates[0]
        var bestIdx = candidates[0]
        var bestVal = envelope[bestIdx]

        for (k in 1 until candidates.size) {
            val idx = candidates[k]
            if (idx - groupStart <= mergeDist) {
                if (envelope[idx] > bestVal) {
                    bestVal = envelope[idx]
                    bestIdx = idx
                }
            } else {
                merged.add(bestIdx)
                groupStart = idx
                bestIdx = idx
                bestVal = envelope[idx]
            }
        }
        merged.add(bestIdx)

        // 3) додатково: робимо мін. дистанцію між ударами ~0.70 періоду (для стабільності)
        val minDist = (0.70 * periodSamples).toInt()
        val finalPeaks = ArrayList<Int>()
        var last = -1_000_000
        for (p in merged) {
            if (p - last >= minDist) {
                finalPeaks.add(p)
                last = p
            } else {
                // якщо дуже близько — залишаємо більший
                val prev = finalPeaks.last()
                if (envelope[p] > envelope[prev]) {
                    finalPeaks[finalPeaks.size - 1] = p
                    last = p
                }
            }
        }

        return PeaksResult(finalPeaks.toIntArray())
    }
}
