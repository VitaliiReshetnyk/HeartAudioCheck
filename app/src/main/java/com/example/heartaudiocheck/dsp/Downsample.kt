package com.example.heartaudiocheck.dsp

object Downsample {
    fun byFactorMean(x: DoubleArray, factor: Int): DoubleArray {
        val m = x.size / factor
        if (m <= 0) return doubleArrayOf()
        val y = DoubleArray(m)
        var j = 0
        var i = 0
        while (j < m) {
            var s = 0.0
            var k = 0
            while (k < factor) {
                s += x[i + k]
                k++
            }
            y[j] = s / factor
            i += factor
            j++
        }
        return y
    }
}
