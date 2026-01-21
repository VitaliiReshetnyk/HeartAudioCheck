package com.example.heartaudiocheck.analysis

import kotlin.math.*
import org.jtransforms.fft.DoubleFFT_1D

object RsaHfProxy {

    data class Result(
        val score: Double,        // 0..1 (HF ratio proxy)
        val breathHz: Double,     // peak in HF band, Hz (0 if not available)
        val confidence: Double    // 0..1
    )

    fun compute(
        peakIdx: IntArray,
        sampleRate: Int,
        qualityScore: Double
    ): Result {
        if (peakIdx.size < 8) return Result(0.0, 0.0, 0.0)

        val rr = DoubleArray(peakIdx.size - 1)
        for (i in 1 until peakIdx.size) {
            rr[i - 1] = (peakIdx[i] - peakIdx[i - 1]).toDouble() / sampleRate.toDouble()
        }

        // Basic plausibility filter (seconds)
        val cleaned = rr.map { it.coerceIn(0.30, 2.00) }.toDoubleArray()

        // Replace strong outliers using median-based rule
        val med = median(cleaned)
        val rr2 = DoubleArray(cleaned.size)
        for (i in cleaned.indices) {
            val v = cleaned[i]
            rr2[i] = if (abs(v - med) / med > 0.25) med else v
        }

        // Build beat times (seconds) at each interval center
        val t = DoubleArray(rr2.size)
        var cum = 0.0
        for (i in rr2.indices) {
            val dt = rr2[i]
            t[i] = cum + dt * 0.5
            cum += dt
        }
        val totalT = cum
        if (totalT < 30.0) {
            // too short to estimate HF/RSA reliably
            val confShort = 0.05 * (totalT / 30.0) * qualityScore.coerceIn(0.0, 1.0)
            return Result(0.0, 0.0, confShort)
        }

        // Resample RR(t) to uniform grid (4 Hz)
        val fs = 4.0
        val n = ((totalT * fs).roundToInt()).coerceAtLeast(128)
        val y = DoubleArray(n)
        val dt = 1.0 / fs

        var j = 0
        for (k in 0 until n) {
            val tk = k * dt
            while (j + 1 < t.size && t[j + 1] < tk) j++

            y[k] = when {
                tk <= t.first() -> rr2.first()
                tk >= t.last() -> rr2.last()
                j + 1 >= t.size -> rr2.last()
                else -> {
                    val t0 = t[j]
                    val t1 = t[j + 1]
                    val a = if (t1 > t0) (tk - t0) / (t1 - t0) else 0.0
                    rr2[j] * (1.0 - a) + rr2[j + 1] * a
                }
            }
        }

        // Detrend (remove mean)
        val mean = y.average()
        for (k in y.indices) y[k] -= mean

        // Window (Hann)
        for (k in y.indices) {
            val w = 0.5 * (1.0 - cos(2.0 * Math.PI * k / (y.size - 1).toDouble()))
            y[k] *= w
        }

        // FFT (real input -> packed complex in DoubleArray(2n))
        val fftN = nextPow2(n).coerceAtLeast(256)
        val buf = DoubleArray(2 * fftN)
        for (i in 0 until n) buf[i] = y[i]

        val fft = DoubleFFT_1D(fftN.toLong())
        fft.realForwardFull(buf)

        // Power spectrum (one-sided)
        val df = fs / fftN.toDouble()
        val nyqBins = fftN / 2

        var totalPower = 0.0
        var hfPower = 0.0
        var hfPeakPower = 0.0
        var hfPeakHz = 0.0

        val hfLo = 0.15
        val hfHi = 0.40
        val totalLo = 0.04
        val totalHi = 0.40

        for (k in 1..nyqBins) {
            val re = buf[2 * k]
            val im = buf[2 * k + 1]
            val p = re * re + im * im
            val f = k * df

            if (f in totalLo..totalHi) totalPower += p
            if (f in hfLo..hfHi) {
                hfPower += p
                if (p > hfPeakPower) {
                    hfPeakPower = p
                    hfPeakHz = f
                }
            }
        }

        if (totalPower <= 1e-12) return Result(0.0, 0.0, 0.0)

        val ratio = (hfPower / totalPower).coerceIn(0.0, 1.0)

        // Confidence: depends on duration + quality
        val durFactor = ((totalT - 30.0) / 30.0).coerceIn(0.0, 1.0) // 0 at 30s, 1 at 60s
        val conf = (0.15 + 0.85 * durFactor) * qualityScore.coerceIn(0.0, 1.0)

        return Result(score = ratio, breathHz = hfPeakHz, confidence = conf)
    }

    private fun median(a: DoubleArray): Double {
        val b = a.copyOf()
        b.sort()
        val m = b.size / 2
        return if (b.size % 2 == 0) 0.5 * (b[m - 1] + b[m]) else b[m]
    }

    private fun nextPow2(x: Int): Int {
        var v = 1
        while (v < x) v = v shl 1
        return v
    }
}
