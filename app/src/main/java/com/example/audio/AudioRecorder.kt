package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var tempFile: File? = null
    private var startTimeMillis: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitudeFlow = MutableStateFlow(0f) // 0.0 to 1.0 normalized amplitude
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startRecording(): File? {
        stopRecording()

        try {
            val file = File(context.cacheDir, "temp_rec_${System.currentTimeMillis()}.m4a")
            tempFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            startTimeMillis = System.currentTimeMillis()
            _isRecording.value = true

            // Start amplitude sampling job
            amplitudeJob = scope.launch {
                while (_isRecording.value) {
                    val maxAmp = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    // Normalize amplitude approx max 32767
                    val normAmp = (maxAmp / 32767f).coerceIn(0f, 1f)
                    _amplitudeFlow.value = normAmp
                    _elapsedMillis.value = System.currentTimeMillis() - startTimeMillis
                    delay(100) // ~10 samples per second
                }
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
            return null
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value && recorder == null) return tempFile

        _isRecording.value = false
        amplitudeJob?.cancel()
        amplitudeJob = null

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
        }

        _amplitudeFlow.value = 0f
        return tempFile
    }

    fun cancelRecording() {
        stopRecording()
        tempFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        tempFile = null
        _elapsedMillis.value = 0L
    }
}
