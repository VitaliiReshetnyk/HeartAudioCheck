package com.example.heartaudiocheck.dsp


import org.jtransforms.fft.DoubleFFT_1D

class FftBandpass(
    private val sampleRate: Int,
    private val fLow: Double = 20.0,
    private val fHigh: Double = 200.0
) {
    fun apply(x: DoubleArray): DoubleArray {
        val n = x.size
        val fft = DoubleFFT_1D(n.toLong())
        val data = DoubleArray(2 * n)

        for (i in 0 until n) {
            data[2 * i] = x[i]
            data[2 * i + 1] = 0.0
        }

        fft.complexForward(data)

        val df = sampleRate.toDouble() / n
        for (k in 0 until n) {
            val freq = k * df
            val pass = (freq in fLow..fHigh) || (freq in (sampleRate - fHigh)..(sampleRate - fLow))
            if (!pass) {
                data[2 * k] = 0.0
                data[2 * k + 1] = 0.0
            }
        }

        fft.complexInverse(data, true)

        val y = DoubleArray(n)
        for (i in 0 until n) y[i] = data[2 * i]
        return y
    }
}
