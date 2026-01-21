package com.example.heartaudiocheck.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.heartaudiocheck.R
import com.example.heartaudiocheck.analysis.FinalResult

class MainViewModel : ViewModel() {
    @StringRes var statusResId: Int = R.string.status_idle

    var progressMs: Int = 0
    var stageReached: Int = 0

    var lastResult: FinalResult? = null

    var signalValues: FloatArray? = null
    var signalDurationSec: Float = 0f
    var signalSampleRateHz: Int = 0
}
