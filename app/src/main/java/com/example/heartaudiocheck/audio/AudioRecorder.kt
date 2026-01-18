package com.example.heartaudiocheck.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.max

class AudioRecorder(
    private val sampleRate: Int = 16000
) {
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var recorder: AudioRecord? = null
    private var bufferSize: Int = 0

    fun start(): Boolean {
        bufferSize = max(
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
            sampleRate / 2
        )

        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            recorder?.release()
            recorder = null
            return false
        }

        recorder?.startRecording()
        return true
    }

    fun readShorts(out: ShortArray): Int {
        val r = recorder ?: return 0
        return r.read(out, 0, out.size)
    }

    fun stop() {
        val r = recorder ?: return
        try { r.stop() } catch (_: Throwable) {}
        r.release()
        recorder = null
    }

    fun getSampleRate(): Int = sampleRate
}