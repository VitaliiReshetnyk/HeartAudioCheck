package com.example.heartaudiocheck.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.heartaudiocheck.analysis.FinalResult

class MainViewModel(private val state: SavedStateHandle) : ViewModel() {

    var lastResult: FinalResult?
        get() = state.get<FinalResult>("lastResult")
        set(value) { state["lastResult"] = value }

    var statusText: String
        get() = state["statusText"] ?: ""
        set(value) { state["statusText"] = value }

    var progressMs: Int
        get() = state["progressMs"] ?: 0
        set(value) { state["progressMs"] = value }

    var progressLabel: String
        get() = state["progressLabel"] ?: ""
        set(value) { state["progressLabel"] = value }


    // Для графіка:
    var signalValues: FloatArray? = null
    var signalDurationSec: Float = 30f
    var signalSampleRateHz: Float = 100f
}
