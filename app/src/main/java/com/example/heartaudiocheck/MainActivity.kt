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

        val signalView = findViewById<HeartSignalView>(R.id.signalView)

        // restore UI
        if (vm.statusText.isNotEmpty()) statusText.text = vm.statusText
        progressBar.progress = vm.progressMs
        if (vm.progressLabel.isNotEmpty()) progressTimeText.text = vm.progressLabel
        vm.lastResult?.let {
            renderResult(it, labelText, confText, bpmText, qualityText, metricsText, diagnosisText, diagnosisConfText)
        }
        vm.signalValues?.let { arr ->
            signalView.setSeries(arr, vm.signalDurationSec, vm.signalSampleRateHz)
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

        val recorder = AudioRecorder(16000)
        val pipeline = HeartAudioPipeline(16000)

        var isRecording = false

        fun hasMicPermission(): Boolean {
            return ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }

        startBtn.setOnClickListener {
            if (isRecording) return@setOnClickListener

            if (!hasMicPermission()) {
                requestMic.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }

            val ok = recorder.start()
            if (!ok) {
                statusText.text = getString(R.string.status_recorder_failed)
                vm.statusText = statusText.text.toString()
                return@setOnClickListener
            }

            isRecording = true
            startBtn.isEnabled = false
            stopBtn.isEnabled = true

            statusText.text = getString(R.string.status_recording)
            vm.statusText = statusText.text.toString()

            progressBar.progress = 0
            progressTimeText.text = getString(R.string.progress_default)
            vm.progressMs = 0
            vm.progressLabel = progressTimeText.text.toString()

            lifecycleScope.launch(Dispatchers.Default) {
                val chunk = ShortArray(16000)
                val all = ArrayList<Short>(16000 * 30)
                val durationMs = 30_000L
                val t0 = System.currentTimeMillis()

                while (isRecording && System.currentTimeMillis() - t0 < durationMs) {
                    val n = recorder.readShorts(chunk)
                    for (i in 0 until n) all.add(chunk[i])

                    val elapsed = System.currentTimeMillis() - t0
                    val elapsedClamped = elapsed.toInt().coerceIn(0, durationMs.toInt())
                    val sec = elapsedClamped / 1000.0

                    launch(Dispatchers.Main) {
                        progressBar.progress = elapsedClamped
                        progressTimeText.text = getString(R.string.progress_format, sec)
                    }

                    vm.progressMs = elapsedClamped
                    vm.progressLabel = getString(R.string.progress_format, sec)

                    delay(20)
                }

                recorder.stop()
                isRecording = false

                val raw = ShortArray(all.size)
                for (i in all.indices) raw[i] = all[i]

                val res = pipeline.analyze(raw, raw.size)

                val series = HeartSignalExtractor.buildEnvelopeSeries(
                    raw = raw,
                    nRead = raw.size,
                    sampleRate = 16000,
                    outHz = 100
                )

                launch(Dispatchers.Main) {
                    startBtn.isEnabled = true
                    stopBtn.isEnabled = false

                    statusText.text = getString(R.string.status_done)
                    vm.statusText = statusText.text.toString()

                    progressBar.progress = 30_000
                    progressTimeText.text = getString(R.string.progress_done)
                    vm.progressMs = 30_000
                    vm.progressLabel = progressTimeText.text.toString()

                    vm.lastResult = res
                    renderResult(res, labelText, confText, bpmText, qualityText, metricsText, diagnosisText, diagnosisConfText)

                    vm.signalValues = series.values
                    vm.signalDurationSec = series.durationSec
                    vm.signalSampleRateHz = series.sampleRateHz
                    signalView.setSeries(series.values, series.durationSec, series.sampleRateHz)
                }
            }
        }

        stopBtn.setOnClickListener {
            if (!isRecording) return@setOnClickListener
            isRecording = false
            recorder.stop()

            startBtn.isEnabled = true
            stopBtn.isEnabled = false

            statusText.text = getString(R.string.status_stopped)
            vm.statusText = statusText.text.toString()

            progressTimeText.text = getString(R.string.progress_stopped_early)
            vm.progressLabel = progressTimeText.text.toString()
        }
    }

    private data class LangOption(val code: String, val label: String)

    private fun setupLanguageSpinner(spinner: Spinner) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val options = listOf(
            LangOption("en", getString(R.string.lang_en)),
            LangOption("uk", getString(R.string.lang_uk)),
            LangOption("de", getString(R.string.lang_de)),
            LangOption("auto", getString(R.string.lang_auto))
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options.map { it.label })
        spinner.adapter = adapter

        fun codeToPos(code: String): Int {
            val idx = options.indexOfFirst { it.code == code }
            return if (idx >= 0) idx else 0
        }

        // IMPORTANT: set selection WITHOUT listener (so it won't fire)
        spinner.onItemSelectedListener = null
        val current = prefs.getString("lang", "en") ?: "en"
        spinner.setSelection(codeToPos(current), false)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val newLang = options.getOrNull(position)?.code ?: "en"
                val savedNow = prefs.getString("lang", "en") ?: "en"
                if (newLang == savedNow) return

                prefs.edit().putString("lang", newLang).apply()
                applyAppLanguage(newLang)

                // DO NOT call recreate() here.
                // AppCompatDelegate will handle activity restart when needed.
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
            "en" -> LocaleListCompat.forLanguageTags("en")
            "uk" -> LocaleListCompat.forLanguageTags("uk")
            "de" -> LocaleListCompat.forLanguageTags("de")
            else -> LocaleListCompat.getEmptyLocaleList() // auto
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
