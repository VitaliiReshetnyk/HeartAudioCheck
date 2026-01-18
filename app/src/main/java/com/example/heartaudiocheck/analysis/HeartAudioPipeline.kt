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

        val diag = DiagnosisEngine.decide(
            rhythmLabel = rhythm.label,
            rhythmConfidence = rhythm.confidence,
            murmurScore = murmur.score,
            murmurCycles = murmur.cyclesUsed,
            qualityScore = q.score
        )

        val qMin = 0.60

        val labelCode: LabelCode
        val baseConf: Double

        if (q.score < qMin || rhythm.bpm == 0.0) {
            labelCode = LabelCode.INSUFFICIENT
            baseConf = q.score
        } else {
            labelCode = labelCodeFromRhythmLabel(rhythm.label)
            baseConf = rhythm.confidence
        }

        val diagnosisCode: DiagnosisCode =
            if (labelCode == LabelCode.INSUFFICIENT) {
                DiagnosisCode.INSUFFICIENT
            } else {
                diagnosisCodeFromDiagLabel(diag.label)
            }

        return FinalResult(
            qualityScore = q.score,
            bpm = rhythm.bpm,
            labelCode = labelCode,
            confidence = baseConf,
            cv = rhythm.cv,
            rmssd = rhythm.rmssd,
            pnn50 = rhythm.pnn50,
            acStrength = period.strength,
            diagnosisCode = diagnosisCode,
            diagnosisConfidence = diag.confidence,
            murmurScore = murmur.score
        )
    }

    private fun labelCodeFromRhythmLabel(label: String): LabelCode {
        val s = label.trim().lowercase()
        return when {
            s.contains("insufficient") -> LabelCode.INSUFFICIENT
            s.contains("regular") -> LabelCode.REGULAR
            s.contains("irregular") -> LabelCode.IRREGULAR
            else -> LabelCode.IRREGULAR
        }
    }

    private fun diagnosisCodeFromDiagLabel(label: String): DiagnosisCode {
        val s = label.trim().lowercase()
        return when {
            s.contains("insufficient") -> DiagnosisCode.INSUFFICIENT
            s.contains("afib") || s.contains("a-fib") || s.contains("atrial") -> DiagnosisCode.AFIB_LIKE
            s.contains("bigem") -> DiagnosisCode.BIGEMINY_LIKE
            s.contains("murmur") -> DiagnosisCode.MURMUR_LIKE
            s.contains("no specific") || s.contains("nonspecific") || s.contains("non-specific") -> DiagnosisCode.NON_SPECIFIC
            s.contains("regular") -> DiagnosisCode.REGULAR
            else -> DiagnosisCode.NON_SPECIFIC
        }
    }
}
