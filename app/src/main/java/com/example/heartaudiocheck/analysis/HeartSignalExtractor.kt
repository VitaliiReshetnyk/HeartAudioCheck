package com.example.heartaudiocheck.analysis

import com.example.heartaudiocheck.dsp.Downsample
import com.example.heartaudiocheck.dsp.Envelope
import com.example.heartaudiocheck.dsp.FftBandpass
import kotlin.math.abs
import kotlin.math.max

object HeartSignalExtractor {

    data class Series(
        val values: FloatArray,
        val durationSec: Float,
        val sampleRateHz: Float
    )

    fun buildEnvelopeSeries(
        raw: ShortArray,
        nRead: Int,
        sampleRate: Int,
        outHz: Int = 100
    ): Series {
        val x = DoubleArray(nRead)
        for (i in 0 until nRead) x[i] = raw[i].toDouble()

        val lowBand = FftBandpass(sampleRate, 20.0, 200.0).apply(x)
        val env = Envelope.rectifiedLowpass(lowBand, sampleRate, 4.0)

        val dsFactor = max(1, sampleRate / outHz)
        val envDs = Downsample.byFactorMean(env, dsFactor)
        val srDs = sampleRate / dsFactor

        val durSec = nRead.toFloat() / sampleRate.toFloat()

        val norm = normalizeForDisplay(envDs)
        val out = FloatArray(norm.size)
        for (i in norm.indices) out[i] = norm[i].toFloat()

        return Series(
            values = out,
            durationSec = durSec,
            sampleRateHz = srDs.toFloat()
        )
    }

    private fun normalizeForDisplay(v: DoubleArray): DoubleArray {
        if (v.isEmpty()) return v

        val absVals = DoubleArray(v.size)
        for (i in v.indices) absVals[i] = abs(v[i])

        val sorted = absVals.clone()
        sorted.sort()

        fun quantile(q: Double): Double {
            val idx = ((sorted.size - 1) * q).toInt().coerceIn(0, sorted.size - 1)
            return sorted[idx]
        }

        val p5 = quantile(0.05)
        val p95 = quantile(0.95)
        val lo = p5
        val hi = max(p95, lo + 1e-9)

        val out = DoubleArray(v.size)
        for (i in v.indices) {
            var t = (abs(v[i]) - lo) / (hi - lo)
            if (t < 0.0) t = 0.0
            if (t > 1.0) t = 1.0
            out[i] = t
        }
        return out
    }
}
