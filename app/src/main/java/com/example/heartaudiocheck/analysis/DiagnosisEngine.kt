package com.example.heartaudiocheck.analysis

object DiagnosisEngine {

    data class Decision(val code: DiagnosisCode, val confidence: Double)

    fun decide(
        rhythmLabel: String,
        rhythmConfidence: Double,
        murmurScore: Double,
        murmurCycles: Int,
        qualityScore: Double,
        // new (safe): deep metric influences only diagnosis
        rsaScore: Double,
        rsaConfidence: Double
    ): Decision {

        // If signal is bad -> do not speculate
        if (qualityScore < 0.60) {
            return Decision(DiagnosisCode.INSUFFICIENT, qualityScore.coerceIn(0.0, 1.0))
        }

        // Murmur-like if strong high-band evidence and enough cycles
        if (murmurCycles >= 6 && murmurScore >= 0.65) {
            val c = (0.55 + 0.45 * murmurScore).coerceIn(0.0, 1.0)
            return Decision(DiagnosisCode.MURMUR_LIKE, c)
        }

        val isIrregular = rhythmLabel.contains("irregular", ignoreCase = true)

        // Base diagnosis from rhythm
        var code = if (!isIrregular) DiagnosisCode.REGULAR else DiagnosisCode.NON_SPECIFIC
        var conf = rhythmConfidence.coerceIn(0.0, 1.0)

        // If irregular and confidence high -> AFIB-like candidate
        if (isIrregular && conf >= 0.60) {
            code = DiagnosisCode.AFIB_LIKE
            conf = (0.55 + 0.45 * conf).coerceIn(0.0, 1.0)
        }

        // Deep adjustment (only if RSA analysis is confident enough)
        if (rsaConfidence >= 0.35) {
            // High HF ratio suggests respiration-linked variability; reduce AFIB-like tendency
            if (code == DiagnosisCode.AFIB_LIKE && rsaScore >= 0.55) {
                code = DiagnosisCode.NON_SPECIFIC
                conf = (conf * 0.75).coerceIn(0.0, 1.0)
            }
            // Very low HF ratio: keep AFIB-like (or boost slightly) when irregular
            if (isIrregular && rsaScore <= 0.15) {
                if (code == DiagnosisCode.NON_SPECIFIC) {
                    code = DiagnosisCode.AFIB_LIKE
                    conf = (conf * 0.90 + 0.10).coerceIn(0.0, 1.0)
                } else if (code == DiagnosisCode.AFIB_LIKE) {
                    conf = (conf * 0.90 + 0.10).coerceIn(0.0, 1.0)
                }
            }
        }

        return Decision(code, conf.coerceIn(0.0, 1.0))
    }
}
