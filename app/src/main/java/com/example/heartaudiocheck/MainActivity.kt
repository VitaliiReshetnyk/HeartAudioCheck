package com.example.heartaudiocheck

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.heartaudiocheck.analysis.*
import com.example.heartaudiocheck.audio.AudioRecorder
import com.example.heartaudiocheck.ui.HeartSignalView
import com.example.heartaudiocheck.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val requestMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLanguage()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val vm = ViewModelProvider(this)[MainViewModel::class.java]

        val statusText = findViewById<TextView>(R.id.statusText)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val progressTimeText = findViewById<TextView>(R.id.progressTimeText)

        val startBtn = findViewById<Button>(R.id.startBtn)
        val stopBtn = findViewById<Button>(R.id.stopBtn)
        val langSpinner = findViewById<Spinner>(R.id.langSpinner)

        val labelText = findViewById<TextView>(R.id.labelText)
        val confText = findViewById<TextView>(R.id.confText)
        val bpmText = findViewById<TextView>(R.id.bpmText)
        val qualityText = findViewById<TextView>(R.id.qualityText)
        val metricsText = findViewById<TextView>(R.id.metricsText)
        val diagnosisText = findViewById<TextView>(R.id.diagnosisText)
        val diagnosisConfText = findViewById<TextView>(R.id.diagnosisConfText)

        val signalTitle = findViewById<TextView>(R.id.signalTitle)
        val signalView = findViewById<HeartSignalView>(R.id.signalView)

        val stage10 = findViewById<TextView>(R.id.stage10)
        val stage20 = findViewById<TextView>(R.id.stage20)
        val stage30 = findViewById<TextView>(R.id.stage30)
        val stage60 = findViewById<TextView>(R.id.stage60)

        val durationMs = 60_000L
        val totalSec = (durationMs / 1000L).toInt()
        progressBar.max = durationMs.toInt()

        statusText.setText(vm.statusResId)

        progressBar.progress = vm.progressMs
        progressTimeText.text = getString(
            R.string.progress_format,
            vm.progressMs / 1000.0,
            totalSec
        )

        vm.lastResult?.let {
            renderResult(it, labelText, confText, bpmText, qualityText, metricsText, diagnosisText, diagnosisConfText)
        }

        if (vm.signalValues != null) {
            signalView.setSeries(vm.signalValues!!, vm.signalDurationSec, vm.signalSampleRateHz)
            signalTitle.text = getString(R.string.signal_graph_title, vm.signalDurationSec) // ✅ Float
        } else {
            signalTitle.text = getString(R.string.signal_graph_title, 0f) // ✅ Float
        }

        setupLanguageSpinner(langSpinner)

        fun showInfo(titleRes: Int, bodyRes: Int) {
            AlertDialog.Builder(this)
                .setTitle(getString(titleRes))
                .setMessage(getString(bodyRes))
                .setPositiveButton(getString(R.string.ok), null)
                .show()
        }

        findViewById<Button>(R.id.infoLabelBtn).setOnClickListener { showInfo(R.string.exp_label_title, R.string.exp_label_body) }
        findViewById<Button>(R.id.infoConfBtn).setOnClickListener { showInfo(R.string.exp_conf_title, R.string.exp_conf_body) }
        findViewById<Button>(R.id.infoBpmBtn).setOnClickListener { showInfo(R.string.exp_bpm_title, R.string.exp_bpm_body) }
        findViewById<Button>(R.id.infoQualityBtn).setOnClickListener { showInfo(R.string.exp_quality_title, R.string.exp_quality_body) }
        findViewById<Button>(R.id.infoMetricsBtn).setOnClickListener { showInfo(R.string.exp_metrics_title, R.string.exp_metrics_body) }
        findViewById<Button>(R.id.infoDiagnosisBtn).setOnClickListener { showInfo(R.string.exp_diagnosis_title, R.string.exp_diagnosis_body) }
        findViewById<Button>(R.id.infoDiagnosisConfBtn).setOnClickListener { showInfo(R.string.exp_diagnosis_conf_title, R.string.exp_diagnosis_conf_body) }

        stage10.setOnClickListener { showInfo(R.string.stage1_title, R.string.stage1_body) }
        stage20.setOnClickListener { showInfo(R.string.stage2_title, R.string.stage2_body) }
        stage30.setOnClickListener { showInfo(R.string.stage3_title, R.string.stage3_body) }
        stage60.setOnClickListener { showInfo(R.string.stage4_title, R.string.stage4_body) }

        val recorder = AudioRecorder(16000)
        val pipeline = HeartAudioPipeline(16000)

        var isRecording = false

        fun hasMicPermission(): Boolean {
            return ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun finalizeAndShow(raw: ShortArray, nRead: Int) {
            val res = pipeline.analyze(raw, nRead)

            val series = HeartSignalExtractor.buildEnvelopeSeries(
                raw = raw,
                nRead = nRead,
                sampleRate = 16000,
                outHz = 100
            )

            vm.lastResult = res
            vm.signalValues = series.values
            vm.signalDurationSec = series.durationSec
            vm.signalSampleRateHz = series.sampleRateHz

            renderResult(res, labelText, confText, bpmText, qualityText, metricsText, diagnosisText, diagnosisConfText)

            signalView.setSeries(series.values, series.durationSec, series.sampleRateHz)
            signalTitle.text = getString(R.string.signal_graph_title, series.durationSec) // ✅ Float
        }

        startBtn.setOnClickListener {
            if (isRecording) return@setOnClickListener

            if (!hasMicPermission()) {
                requestMic.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }

            val ok = recorder.start()
            if (!ok) {
                vm.statusResId = R.string.status_recorder_failed
                statusText.setText(vm.statusResId)
                return@setOnClickListener
            }

            isRecording = true
            startBtn.isEnabled = false
            stopBtn.isEnabled = true

            vm.statusResId = R.string.status_recording
            statusText.setText(vm.statusResId)

            vm.progressMs = 0
            progressBar.progress = 0
            progressTimeText.text = getString(R.string.progress_format, 0.0, totalSec)

            lifecycleScope.launch(Dispatchers.Default) {
                val chunk = ShortArray(16000)
                val all = ArrayList<Short>(16000 * 60)
                val t0 = System.currentTimeMillis()

                var stage10Done = vm.stageReached >= 1
                var stage20Done = vm.stageReached >= 2
                var stage30Done = vm.stageReached >= 3
                var stage60Done = vm.stageReached >= 4

                fun snapshot(list: ArrayList<Short>): ShortArray {
                    val arr = ShortArray(list.size)
                    for (i in list.indices) arr[i] = list[i]
                    return arr
                }

                fun runStage(stage: Int, rawSnap: ShortArray) {
                    lifecycleScope.launch(Dispatchers.Default) {
                        val res = pipeline.analyze(rawSnap, rawSnap.size)
                        launch(Dispatchers.Main) {
                            vm.lastResult = res
                            vm.stageReached = maxOf(vm.stageReached, stage)
                            renderResult(res, labelText, confText, bpmText, qualityText, metricsText, diagnosisText, diagnosisConfText)

                            vm.statusResId = when (stage) {
                                1 -> R.string.stage_ready_10
                                2 -> R.string.stage_ready_20
                                3 -> R.string.stage_ready_30
                                else -> R.string.stage_ready_60
                            }
                            statusText.setText(vm.statusResId)
                        }
                    }
                }

                while (isRecording && System.currentTimeMillis() - t0 < durationMs) {
                    val n = recorder.readShorts(chunk)
                    for (i in 0 until n) all.add(chunk[i])

                    val elapsed = (System.currentTimeMillis() - t0).coerceAtLeast(0L)
                    val elapsedClamped = elapsed.toInt().coerceIn(0, durationMs.toInt())
                    val sec = elapsedClamped / 1000.0

                    launch(Dispatchers.Main) {
                        vm.progressMs = elapsedClamped
                        progressBar.progress = elapsedClamped
                        progressTimeText.text = getString(R.string.progress_format, sec, totalSec)
                    }

                    if (!stage10Done && elapsedClamped >= 10_000) {
                        stage10Done = true
                        runStage(1, snapshot(all))
                    }
                    if (!stage20Done && elapsedClamped >= 20_000) {
                        stage20Done = true
                        runStage(2, snapshot(all))
                    }
                    if (!stage30Done && elapsedClamped >= 30_000) {
                        stage30Done = true
                        runStage(3, snapshot(all))
                    }
                    if (!stage60Done && elapsedClamped >= 60_000) {
                        stage60Done = true
                        runStage(4, snapshot(all))
                    }

                    delay(20)
                }

                recorder.stop()
                isRecording = false

                val raw = ShortArray(all.size)
                for (i in all.indices) raw[i] = all[i]

                launch(Dispatchers.Main) {
                    startBtn.isEnabled = true
                    stopBtn.isEnabled = false

                    vm.statusResId = R.string.status_done
                    statusText.setText(vm.statusResId)

                    if (vm.progressMs >= durationMs.toInt()) {
                        progressBar.progress = durationMs.toInt()
                        progressTimeText.text = getString(R.string.progress_format, totalSec.toDouble(), totalSec)
                        vm.progressMs = durationMs.toInt()
                    }

                    finalizeAndShow(raw, raw.size)
                }
            }
        }

        stopBtn.setOnClickListener {
            if (!isRecording) return@setOnClickListener

            isRecording = false
            recorder.stop()

            startBtn.isEnabled = true
            stopBtn.isEnabled = false

            vm.statusResId = R.string.status_stopped
            statusText.setText(vm.statusResId)
        }
    }

    private data class LangOption(val code: String, val label: String)

    private fun setupLanguageSpinner(spinner: Spinner) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val options = listOf(
            LangOption("en", getString(R.string.lang_en)),
            LangOption("uk", getString(R.string.lang_uk)),
            LangOption("de", getString(R.string.lang_de)),
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options.map { it.label })
        spinner.adapter = adapter

        fun codeToPos(code: String): Int {
            val idx = options.indexOfFirst { it.code == code }
            return if (idx >= 0) idx else 0
        }

        spinner.onItemSelectedListener = null
        val current = prefs.getString("lang", "en").let { if (it == "auto") "en" else it } ?: "en"
        spinner.setSelection(codeToPos(current), false)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val newLang = options.getOrNull(position)?.code ?: "en"
                val savedNow = prefs.getString("lang", "en") ?: "en"
                if (newLang == savedNow) return

                prefs.edit().putString("lang", newLang).apply()
                applyAppLanguage(newLang)
                recreate()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun applySavedLanguage() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val saved = prefs.getString("lang", "en") ?: "en"
        applyAppLanguage(saved)
    }

    private fun applyAppLanguage(code: String) {
        val locales = when (code) {
            "uk" -> LocaleListCompat.forLanguageTags("uk")
            "de" -> LocaleListCompat.forLanguageTags("de")
            else -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private fun labelTextFromCode(code: LabelCode): String = when (code) {
        LabelCode.REGULAR -> getString(R.string.label_regular)
        LabelCode.IRREGULAR -> getString(R.string.label_irregular)
        LabelCode.INSUFFICIENT -> getString(R.string.label_insufficient)
    }

    private fun diagnosisTextFromCode(code: DiagnosisCode): String = when (code) {
        DiagnosisCode.REGULAR -> getString(R.string.diag_regular)
        DiagnosisCode.AFIB_LIKE -> getString(R.string.diag_afib_like)
        DiagnosisCode.BIGEMINY_LIKE -> getString(R.string.diag_bigeminy_like)
        DiagnosisCode.MURMUR_LIKE -> getString(R.string.diag_murmur_like)
        DiagnosisCode.NON_SPECIFIC -> getString(R.string.diag_non_specific)
        DiagnosisCode.INSUFFICIENT -> getString(R.string.diag_insufficient)
    }

    private fun renderResult(
        res: FinalResult,
        labelText: TextView,
        confText: TextView,
        bpmText: TextView,
        qualityText: TextView,
        metricsText: TextView,
        diagnosisText: TextView,
        diagnosisConfText: TextView
    ) {
        val labelStr = labelTextFromCode(res.labelCode)
        val diagStr = diagnosisTextFromCode(res.diagnosisCode)

        labelText.text = getString(R.string.label_value, labelStr)
        confText.text = getString(R.string.conf_value, (res.confidence * 100).toInt())
        bpmText.text = getString(R.string.bpm_value, res.bpm)
        qualityText.text = getString(R.string.quality_value, (res.qualityScore * 100).toInt())
        metricsText.text = getString(R.string.metrics_value, res.cv, res.rmssd, res.pnn50, res.acStrength)
        diagnosisText.text = getString(R.string.diagnosis_value, diagStr)
        diagnosisConfText.text = getString(R.string.diagnosis_conf_value, (res.diagnosisConfidence * 100).toInt(), res.murmurScore)
    }
}
