package com.example.heartaudiocheck.analysis

import com.example.heartaudiocheck.dsp.AutoCorrelation
import com.example.heartaudiocheck.dsp.Downsample
import com.example.heartaudiocheck.dsp.Envelope
import com.example.heartaudiocheck.dsp.FftBandpass
import com.example.heartaudiocheck.dsp.MurmurScore
import com.example.heartaudiocheck.dsp.PeakDetector

class HeartAudioPipeline(private val sampleRate: Int) {

    private val bandpassLow = FftBandpass(sampleRate, 20.0, 200.0)
    private val bandpassHigh = FftBandpass(sampleRate, 200.0, 600.0)

    fun analyze(raw: ShortArray, nRead: Int): FinalResult {
        val x = DoubleArray(nRead)
        for (i in 0 until nRead) x[i] = raw[i].toDouble()

        val low = bandpassLow.apply(x)
        val high = bandpassHigh.apply(x)

        val q = QualityGate.compute(raw.copyOfRange(0, nRead), low)

        val env = Envelope.rectifiedLowpass(low, sampleRate, 4.0)

        val dsFactor = 40
        val envDs = Downsample.byFactorMean(env, dsFactor)
        val srDs = sampleRate / dsFactor

        val period = AutoCorrelation.estimatePeriodSamples(envDs, srDs)
        val periodSamplesFull = period.periodSamples * dsFactor

        val peaks = PeakDetector.detect(env, sampleRate, periodSamplesFull)

        val rhythm = RhythmAnalysis.compute(
            peaks.peakIndices,
            sampleRate,
            q.score,
            period.strength
        )

        val murmur = MurmurScore.compute(
            lowBand = low,
            highBand = high,
            peakIdx = peaks.peakIndices,
            sampleRate = sampleRate,
            periodSamples = periodSamplesFull
        )

        // --- RSA / HF proxy (deep metric) ---
        val durationSec = nRead.toDouble() / sampleRate.toDouble()
        val rsa = if (durationSec >= 45.0 && q.score >= 0.60) {
            RsaHfProxy.compute(peaks.peakIndices, sampleRate, q.score)
        } else {
            RsaHfProxy.Result(0.0, 0.0, 0.0)
        }

        // Base labelCode logic (doesn't depend on RSA)
        val labelCode: LabelCode = if (q.score < 0.60 || rhythm.bpm == 0.0) {
            LabelCode.INSUFFICIENT
        } else {
            val lab = rhythm.label.lowercase()
            when {
                lab.contains("regular") -> LabelCode.REGULAR
                lab.contains("irregular") -> LabelCode.IRREGULAR
                else -> LabelCode.IRREGULAR
            }
        }

        val baseConfidence: Double = if (labelCode == LabelCode.INSUFFICIENT) {
            q.score.coerceIn(0.0, 1.0)
        } else {
            rhythm.confidence.coerceIn(0.0, 1.0)
        }

        // Diagnosis decision (RSA affects ONLY diagnosis)
        val diag = DiagnosisEngine.decide(
            rhythmLabel = rhythm.label,              // string inside RhythmAnalysis
            rhythmConfidence = rhythm.confidence,
            murmurScore = murmur.score,
            murmurCycles = murmur.cyclesUsed,
            qualityScore = q.score,
            rsaScore = rsa.score,
            rsaConfidence = rsa.confidence
        )

        return FinalResult(
            qualityScore = q.score,
            bpm = rhythm.bpm,
            labelCode = labelCode,
            confidence = baseConfidence,
            cv = rhythm.cv,
            rmssd = rhythm.rmssd,
            pnn50 = rhythm.pnn50,
            acStrength = period.strength,
            diagnosisCode = diag.code,
            diagnosisConfidence = diag.confidence,
            murmurScore = murmur.score
        )
    }
}
