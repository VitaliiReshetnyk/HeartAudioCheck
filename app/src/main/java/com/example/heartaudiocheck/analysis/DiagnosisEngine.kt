package com.example.heartaudiocheck.analysis

object DiagnosisEngine {

    data class Diagnosis(
        val label: String,
        val confidence: Double
    )

    fun decide(
        rhythmLabel: String,
        rhythmConfidence: Double,
        murmurScore: Double,
        murmurCycles: Int,
        qualityScore: Double
    ): Diagnosis {

        val murmurOk = murmurCycles >= 8
        val murmurConf = (murmurScore * qualityScore * if (murmurOk) 1.0 else 0.7).coerceIn(0.0, 1.0)

        if (murmurConf >= 0.70) {
            return Diagnosis("Possible murmur-like sound pattern", murmurConf)
        }

        val rl = rhythmLabel
        val rc = (rhythmConfidence * qualityScore).coerceIn(0.0, 1.0)

        return when {
            rl.contains("AFib-like", ignoreCase = true) ->
                Diagnosis("Possible atrial fibrillation pattern", rc)

            rl.contains("Bigeminy-like", ignoreCase = true) ->
                Diagnosis("Possible premature beats / bigeminy pattern", rc)

            rl.contains("Tachycardia", ignoreCase = true) ->
                Diagnosis("Possible tachycardia-range pattern", rc)

            rl.contains("Bradycardia", ignoreCase = true) ->
                Diagnosis("Possible bradycardia-range pattern", rc)

            rl.contains("Irregular rhythm", ignoreCase = true) ->
                Diagnosis("Possible irregular rhythm pattern", rc)

            else ->
                Diagnosis("No specific pattern detected", (0.45 + 0.35 * qualityScore).coerceIn(0.0, 1.0))
        }
    }
}
