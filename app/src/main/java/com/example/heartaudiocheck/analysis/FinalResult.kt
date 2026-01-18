package com.example.heartaudiocheck.analysis

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FinalResult(
    val qualityScore: Double,
    val bpm: Double,

    // Instead of raw English text:
    val labelCode: LabelCode,
    val confidence: Double,

    val cv: Double,
    val rmssd: Double,
    val pnn50: Double,
    val acStrength: Double,

    val diagnosisCode: DiagnosisCode,
    val diagnosisConfidence: Double,
    val murmurScore: Double
) : Parcelable

@Parcelize
enum class LabelCode : Parcelable {
    REGULAR,
    IRREGULAR,
    INSUFFICIENT
}

@Parcelize
enum class DiagnosisCode : Parcelable {
    REGULAR,
    AFIB_LIKE,
    BIGEMINY_LIKE,
    MURMUR_LIKE,
    NON_SPECIFIC,
    INSUFFICIENT
}
