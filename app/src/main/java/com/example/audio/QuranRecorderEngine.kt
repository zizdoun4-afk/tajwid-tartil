package com.example.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10

enum class SessionState { IDLE, RECORDING, PAUSED }

data class RecorderUiState(
    val isTaking: Boolean = false,
    val sessionState: SessionState = SessionState.IDLE,
    val elapsedMs: Long = 0L,
    val meteringDb: Float = -160f
)

data class TakeResult(
    val filePath: String,
    val durationMs: Long
)

data class SessionMarker(
    val aya: Int,
    val tMs: Long
)

data class SessionResult(
    val filePath: String,
    val durationMs: Long,
    val markers: List<SessionMarker>
)

interface QuranRecorderEngine {
    val uiState: StateFlow<RecorderUiState>

    suspend fun ensureReady(): Boolean

    // Mode Take (one verse)
    suspend fun startTake(): Boolean
    suspend fun stopTake(): TakeResult?
    suspend fun discardTake()

    // Mode Session (continuous)
    suspend fun startSession(firstAya: Int): Boolean
    fun pauseSession()
    fun resumeSession()
    fun markAyah(aya: Int)
    suspend fun stopSession(): SessionResult?
    suspend fun discardSession()
}

class QuranRecorderEngineImpl(private val context: Context) : QuranRecorderEngine {

    private var mediaRecorder: MediaRecorder? = null
    private var tempFile: File? = null
    private var startTimeMs: Long = 0L
    private var pausedTotalMs: Long = 0L
    private var pauseStartTimeMs: Long = 0L

    private val markersList = mutableListOf<SessionMarker>()

    private val _uiState = MutableStateFlow(RecorderUiState())
    override val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override suspend fun ensureReady(): Boolean {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.mode = AudioManager.MODE_NORMAL
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return permissionGranted
    }

    override suspend fun startTake(): Boolean {
        cleanUp()
        if (!ensureReady()) return false

        try {
            val file = File(context.cacheDir, "take_${System.currentTimeMillis()}.m4a")
            tempFile = file

            setupAndStartRecorder(file)

            startTimeMs = System.currentTimeMillis()
            pausedTotalMs = 0L

            _uiState.value = RecorderUiState(
                isTaking = true,
                sessionState = SessionState.RECORDING,
                elapsedMs = 0L,
                meteringDb = -160f
            )

            startPolling()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            cleanUp()
            return false
        }
    }

    override suspend fun stopTake(): TakeResult? {
        if (!_uiState.value.isTaking && mediaRecorder == null) return null

        val currentTempFile = tempFile
        val duration = _uiState.value.elapsedMs

        stopRecorderInternal()

        return if (currentTempFile != null && currentTempFile.exists() && duration > 0) {
            TakeResult(currentTempFile.absolutePath, duration)
        } else {
            currentTempFile?.delete()
            null
        }
    }

    override suspend fun discardTake() {
        stopRecorderInternal()
        tempFile?.let {
            if (it.exists()) it.delete()
        }
        tempFile = null
    }

    override suspend fun startSession(firstAya: Int): Boolean {
        cleanUp()
        if (!ensureReady()) return false

        try {
            val file = File(context.cacheDir, "session_${System.currentTimeMillis()}.m4a")
            tempFile = file

            setupAndStartRecorder(file)

            startTimeMs = System.currentTimeMillis()
            pausedTotalMs = 0L
            markersList.clear()
            markersList.add(SessionMarker(firstAya, 0L))

            _uiState.value = RecorderUiState(
                isTaking = false,
                sessionState = SessionState.RECORDING,
                elapsedMs = 0L,
                meteringDb = -160f
            )

            startPolling()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            cleanUp()
            return false
        }
    }

    override fun pauseSession() {
        if (_uiState.value.sessionState != SessionState.RECORDING) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
            }
            pauseStartTimeMs = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(sessionState = SessionState.PAUSED)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun resumeSession() {
        if (_uiState.value.sessionState != SessionState.PAUSED) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
            }
            if (pauseStartTimeMs > 0) {
                pausedTotalMs += (System.currentTimeMillis() - pauseStartTimeMs)
                pauseStartTimeMs = 0L
            }
            _uiState.value = _uiState.value.copy(sessionState = SessionState.RECORDING)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun markAyah(aya: Int) {
        val currentMs = _uiState.value.elapsedMs
        markersList.add(SessionMarker(aya, currentMs))
    }

    override suspend fun stopSession(): SessionResult? {
        if (_uiState.value.sessionState == SessionState.IDLE && mediaRecorder == null) return null

        val currentTempFile = tempFile
        val duration = _uiState.value.elapsedMs
        val copyMarkers = markersList.toList()

        stopRecorderInternal()

        return if (currentTempFile != null && currentTempFile.exists() && duration > 0) {
            SessionResult(currentTempFile.absolutePath, duration, copyMarkers)
        } else {
            currentTempFile?.delete()
            null
        }
    }

    override suspend fun discardSession() {
        stopRecorderInternal()
        tempFile?.let {
            if (it.exists()) it.delete()
        }
        tempFile = null
        markersList.clear()
    }

    private fun setupAndStartRecorder(file: File) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (_uiState.value.sessionState != SessionState.IDLE) {
                if (_uiState.value.sessionState == SessionState.RECORDING) {
                    val now = System.currentTimeMillis()
                    val elapsed = (now - startTimeMs) - pausedTotalMs

                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }

                    val db = if (maxAmp > 0) {
                        (20 * log10(maxAmp / 32767.0)).toFloat().coerceIn(-160f, 0f)
                    } else {
                        -160f
                    }

                    _uiState.value = _uiState.value.copy(
                        elapsedMs = elapsed.coerceAtLeast(0L),
                        meteringDb = db
                    )
                }
                delay(250) // Refreshed every 250ms as specified in spec
            }
        }
    }

    private fun stopRecorderInternal() {
        pollingJob?.cancel()
        pollingJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        _uiState.value = RecorderUiState(
            isTaking = false,
            sessionState = SessionState.IDLE,
            elapsedMs = 0L,
            meteringDb = -160f
        )
    }

    private fun cleanUp() {
        stopRecorderInternal()
        tempFile = null
        markersList.clear()
        pausedTotalMs = 0L
        pauseStartTimeMs = 0L
    }
}
