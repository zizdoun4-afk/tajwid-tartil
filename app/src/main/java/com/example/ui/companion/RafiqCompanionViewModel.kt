package com.example.ui.companion

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.PacingAssessment
import com.example.audio.QuranRecorderEngine
import com.example.audio.QuranRecorderEngineImpl
import com.example.audio.RecitationAnalysisEngine
import com.example.audio.RecitationDiagnostic
import com.example.audio.TakeResult
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.repository.MemorizationRepository
import com.example.data.repository.QuranRepository
import com.example.domain.companion.DailyCompanionRoutine
import com.example.domain.companion.DailySmartAgenda
import com.example.domain.companion.SmartCompanionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class RafiqCompanionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val memorizationDao = db.memorizationDao()
    private val tajwidProgressDao = db.tajwidProgressDao()
    private val quranCacheDao = db.quranCacheDao()

    private val memorizationRepository = MemorizationRepository(memorizationDao)
    private val quranRepository = QuranRepository(quranCacheDao, application)

    val recorderEngine: QuranRecorderEngine = QuranRecorderEngineImpl(application)
    val audioPlayer = AudioPlayer(application)

    private var userPlayer: MediaPlayer? = null

    // Smart Agenda
    private val _agenda = MutableStateFlow<DailySmartAgenda?>(null)
    val agenda: StateFlow<DailySmartAgenda?> = _agenda.asStateFlow()

    // Smart Daily Companion Routine
    private val _dailyRoutine = MutableStateFlow<DailyCompanionRoutine?>(null)
    val dailyRoutine: StateFlow<DailyCompanionRoutine?> = _dailyRoutine.asStateFlow()

    private val _nextActionSuggestion = MutableStateFlow<String?>(null)
    val nextActionSuggestion: StateFlow<String?> = _nextActionSuggestion.asStateFlow()

    // Smart Session Queue
    private val _sessionQueue = MutableStateFlow<List<MemorizationStatusEntity>>(emptyList())
    val sessionQueue: StateFlow<List<MemorizationStatusEntity>> = _sessionQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    // Active Ayah under study
    private val _currentSurahNumber = MutableStateFlow(1)
    val currentSurahNumber: StateFlow<Int> = _currentSurahNumber.asStateFlow()

    private val _currentAyahNumber = MutableStateFlow(1)
    val currentAyahNumber: StateFlow<Int> = _currentAyahNumber.asStateFlow()

    private val _currentAyahText = MutableStateFlow("")
    val currentAyahText: StateFlow<String> = _currentAyahText.asStateFlow()

    private val _isTextRevealed = MutableStateFlow(false)
    val isTextRevealed: StateFlow<Boolean> = _isTextRevealed.asStateFlow()

    // Audio & Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _lastTakeResult = MutableStateFlow<TakeResult?>(null)
    val lastTakeResult: StateFlow<TakeResult?> = _lastTakeResult.asStateFlow()

    private val recordedDbSamples = mutableListOf<Float>()
    private var sampleCollectorJob: Job? = null

    // Diagnostic
    private val _diagnostic = MutableStateFlow<RecitationDiagnostic?>(null)
    val diagnostic: StateFlow<RecitationDiagnostic?> = _diagnostic.asStateFlow()

    // Audio playback status
    private val _isPlayingModel = MutableStateFlow(false)
    val isPlayingModel: StateFlow<Boolean> = _isPlayingModel.asStateFlow()

    private val _isPlayingUser = MutableStateFlow(false)
    val isPlayingUser: StateFlow<Boolean> = _isPlayingUser.asStateFlow()

    init {
        loadAgenda()
        loadAyahText(1, 1)
    }

    fun loadAgenda() {
        viewModelScope.launch {
            val smartAgenda = SmartCompanionEngine.computeDailyAgenda(
                memorizationDao = memorizationDao,
                tajwidProgressDao = tajwidProgressDao
            )
            _agenda.value = smartAgenda

            val routine = SmartCompanionEngine.computeDailyRoutine(
                memorizationDao = memorizationDao
            )
            _dailyRoutine.value = routine
            _nextActionSuggestion.value = routine.nextActionSuggestionFr

            // If queue is empty, populate from due or weak
            if (_sessionQueue.value.isEmpty()) {
                val queue = when {
                    smartAgenda.dueForReview.isNotEmpty() -> smartAgenda.dueForReview
                    smartAgenda.weakAyahs.isNotEmpty() -> smartAgenda.weakAyahs
                    smartAgenda.inLearning.isNotEmpty() -> smartAgenda.inLearning
                    else -> emptyList()
                }
                _sessionQueue.value = queue
                if (queue.isNotEmpty()) {
                    selectAyah(queue.first().surahNumber, queue.first().ayahNumber)
                }
            }
        }
    }

    fun selectAyah(surahNumber: Int, ayahNumber: Int) {
        _currentSurahNumber.value = surahNumber
        _currentAyahNumber.value = ayahNumber
        _isTextRevealed.value = false
        _diagnostic.value = null
        _lastTakeResult.value = null
        loadAyahText(surahNumber, ayahNumber)
    }

    private fun loadAyahText(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            try {
                val result = quranRepository.getSurahAyahs(surahNumber)
                if (result.isSuccess) {
                    val ayahs = result.getOrNull() ?: emptyList()
                    val target = ayahs.find { it.numberInSurah == ayahNumber }
                    if (target != null) {
                        _currentAyahText.value = target.text
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleRevealText() {
        _isTextRevealed.value = !_isTextRevealed.value
    }

    fun startRecording() {
        viewModelScope.launch {
            stopAudio()
            recordedDbSamples.clear()
            val started = recorderEngine.startTake()
            if (started) {
                _isRecording.value = true
                _diagnostic.value = null
                // Collect amplitude metering
                sampleCollectorJob?.cancel()
                sampleCollectorJob = launch {
                    recorderEngine.uiState.collect { state ->
                        if (state.isTaking) {
                            recordedDbSamples.add(state.meteringDb)
                        }
                    }
                }
            }
        }
    }

    fun stopRecordingAndAnalyze() {
        viewModelScope.launch {
            sampleCollectorJob?.cancel()
            sampleCollectorJob = null
            _isRecording.value = false

            val result = recorderEngine.stopTake()
            _lastTakeResult.value = result

            if (result != null) {
                val file = File(result.filePath)
                val diag = RecitationAnalysisEngine.analyzeRecitation(
                    audioFile = file,
                    surahNumber = _currentSurahNumber.value,
                    ayahNumber = _currentAyahNumber.value,
                    ayahText = _currentAyahText.value,
                    recordedDbSamples = recordedDbSamples.toList()
                )
                _diagnostic.value = diag
                // Auto-reveal text after recitation to allow comparison
                _isTextRevealed.value = true
            }
        }
    }

    fun playModelRecitation() {
        viewModelScope.launch {
            stopAudio()
            try {
                _isPlayingModel.value = true
                val surah = _currentSurahNumber.value
                val ayah = _currentAyahNumber.value
                // Streaming from EveryAyah Alafasy
                val formattedSurah = surah.toString().padStart(3, '0')
                val formattedAyah = ayah.toString().padStart(3, '0')
                val url = "https://everyayah.com/data/Alafasy_128kbps/$formattedSurah$formattedAyah.mp3"

                audioPlayer.togglePlayPause(url) {
                    _isPlayingModel.value = false
                }
            } catch (e: Exception) {
                _isPlayingModel.value = false
            }
        }
    }

    fun playUserRecording() {
        val result = _lastTakeResult.value ?: return
        val file = File(result.filePath)
        if (!file.exists()) return

        stopAudio()
        try {
            _isPlayingUser.value = true
            userPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    _isPlayingUser.value = false
                    releasePlayer()
                }
            }
        } catch (e: Exception) {
            _isPlayingUser.value = false
            releasePlayer()
        }
    }

    fun stopAudio() {
        try {
            audioPlayer.stop()
            _isPlayingModel.value = false
        } catch (e: Exception) {
            // Ignore
        }
        releasePlayer()
    }

    private fun releasePlayer() {
        try {
            userPlayer?.stop()
            userPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            userPlayer = null
            _isPlayingUser.value = false
        }
    }

    /**
     * Completes review of current ayah with self-validation.
     * Updates SRS interval in Room DB and advances to next ayah in queue.
     */
    fun validateCurrentAyah(isMastered: Boolean) {
        viewModelScope.launch {
            val surah = _currentSurahNumber.value
            val ayah = _currentAyahNumber.value

            memorizationRepository.recordTestResult(surah, ayah, isMastered)

            // Reload agenda stats
            loadAgenda()

            // Advance in queue
            val nextIndex = _currentQueueIndex.value + 1
            if (nextIndex < _sessionQueue.value.size) {
                _currentQueueIndex.value = nextIndex
                val nextEntity = _sessionQueue.value[nextIndex]
                selectAyah(nextEntity.surahNumber, nextEntity.ayahNumber)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        audioPlayer.release()
        sampleCollectorJob?.cancel()
    }
}
