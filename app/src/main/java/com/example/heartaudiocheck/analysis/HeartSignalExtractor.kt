package com.example.heartaudiocheck.analysis

import com.example.heartaudiocheck.dsp.Envelope
import com.example.heartaudiocheck.dsp.FftBandpass

object HeartSignalExtractor {

    data class Series(
        val values: FloatArray,
        val durationSec: Float,
        val sampleRateHz: Int
    )

    fun buildEnvelopeSeries(
        raw: ShortArray,
        nRead: Int,
        sampleRate: Int,
        outHz: Int
    ): Series {
        val x = DoubleArray(nRead)
        for (i in 0 until nRead) x[i] = raw[i].toDouble()

        val bp = FftBandpass(sampleRate, 20.0, 200.0).apply(x)
        val env = Envelope.rectifiedLowpass(bp, sampleRate, 4.0)

        val step = (sampleRate / outHz).coerceAtLeast(1)
        val outN = ((nRead - 1) / step + 1).coerceAtLeast(1)
        val out = FloatArray(outN)

        var idx = 0
        var i = 0
        while (i < nRead && idx < outN) {
            out[idx] = env[i].toFloat()
            idx++
            i += step
        }

        val dur = nRead.toFloat() / sampleRate.toFloat()
        return Series(
            values = out,
            durationSec = dur,
            sampleRateHz = outHz
        )
    }
}
