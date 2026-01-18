package com.example.heartaudiocheck.dsp


import kotlin.math.abs

object Envelope {
    fun rectifiedLowpass(x: DoubleArray, sampleRate: Int, cutoffHz: Double = 8.0): DoubleArray {
        val y = DoubleArray(x.size)
        val rc = 1.0 / (2.0 * Math.PI * cutoffHz)
        val dt = 1.0 / sampleRate
        val alpha = dt / (rc + dt)

        var prev = 0.0
        for (i in x.indices) {
            val v = abs(x[i])
            prev = prev + alpha * (v - prev)
            y[i] = prev
        }
        return y
    }
}
