package com.example.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.PlayerState
import com.example.audio.QuranRecorderEngine
import com.example.audio.QuranRecorderEngineImpl
import com.example.audio.RepeatMode
import com.example.data.local.db.AppDatabase
import com.example.data.repository.MemorizationRepository
import com.example.data.repository.QuranRepository
import com.example.data.repository.RecitationsStore
import com.example.domain.model.Ayah
import com.example.domain.model.MemorizationStatus
import com.example.domain.model.RecitationStyle
import com.example.domain.model.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class TrainingStep(val stepNumber: Int) {
    LISTEN_3X(1),
    ACCOMPANIED_READING(2),
    SOLO_RECORDING(3),
    PLAYBACK_REVIEW(4),
    BLIND_TEST(5)
}

data class TrainingSessionUiState(
    val currentStep: TrainingStep = TrainingStep.LISTEN_3X,
    val surah: Surah? = null,
    val ayah: Ayah? = null,
    val recitationStyle: RecitationStyle = RecitationStyle.TARTIL,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val userRecordingFile: File? = null,
    val isTextRevealed: Boolean = false,
    val isSessionCompleted: Boolean = false,
    val status: MemorizationStatus = MemorizationStatus.NEW,
    val message: String? = null
)

class TrainingSessionViewModel(
    application: Application,
    val surahNumber: Int,
    val ayahNumber: Int
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val quranRepository = QuranRepository(db.quranCacheDao(), application)
    private val memorizationRepository = MemorizationRepository(db.memorizationDao())
    private val recitationsStore = RecitationsStore(application, db.recordingDao())

    val audioPlayer = AudioPlayer(application)
    val quranRecorderEngine: QuranRecorderEngine = QuranRecorderEngineImpl(application)

    private val _uiState = MutableStateFlow(TrainingSessionUiState())
    val uiState: StateFlow<TrainingSessionUiState> = _uiState.asStateFlow()

    init {
        loadAyah()
        observeRecorder()
    }

    private fun loadAyah() {
        viewModelScope.launch {
            val surah = quranRepository.getDefaultSurahs().find { it.number == surahNumber }
            _uiState.value = _uiState.value.copy(surah = surah)

            val ayahsResult = quranRepository.getSurahAyahs(surahNumber)
            val ayah = ayahsResult.getOrNull()?.find { it.numberInSurah == ayahNumber }
            _uiState.value = _uiState.value.copy(ayah = ayah)

            val statusEntity = memorizationRepository.getStatusForAyah(surahNumber, ayahNumber)
            statusEntity?.let {
                _uiState.value = _uiState.value.copy(status = it.memorizationStatus)
            }
        }
    }

    private fun observeRecorder() {
        viewModelScope.launch {
            quranRecorderEngine.uiState.collect { recState ->
                _uiState.value = _uiState.value.copy(
                    isRecording = recState.isTaking,
                    recordingDurationMs = recState.elapsedMs
                )
            }
        }
    }

    fun playReferenceAudio(repeatCount: Int = 1) {
        val ayah = _uiState.value.ayah ?: return
        val url = _uiState.value.recitationStyle.buildAudioUrl(surahNumber, ayah.numberInSurah)
        audioPlayer.setRepeatMode(
            when (repeatCount) {
                3 -> RepeatMode.THREE_TIMES
                else -> RepeatMode.ONCE
            }
        )
        audioPlayer.togglePlayPause(url)
    }

    fun startRecording() {
        audioPlayer.stop()
        viewModelScope.launch {
            quranRecorderEngine.startTake()
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            val result = quranRecorderEngine.stopTake()
            if (result != null) {
                val file = File(result.filePath)
                _uiState.value = _uiState.value.copy(
                    userRecordingFile = file,
                    message = "Enregistrement terminé !"
                )

                // Save recording in store
                recitationsStore.saveAyahRecording(
                    tempPath = result.filePath,
                    sura = surahNumber,
                    surahName = _uiState.value.surah?.name ?: "Sourate $surahNumber",
                    aya = ayahNumber,
                    riwaya = "hafs",
                    durationMs = result.durationMs,
                    reciterName = "Entraînement Hifz"
                )
            }
        }
    }

    fun playUserRecording() {
        val file = _uiState.value.userRecordingFile
        if (file != null && file.exists()) {
            audioPlayer.togglePlayPause(file.absolutePath)
        }
    }

    fun toggleTextVisibility() {
        _uiState.value = _uiState.value.copy(isTextRevealed = !_uiState.value.isTextRevealed)
    }

    fun goToNextStep() {
        audioPlayer.stop()
        val next = when (_uiState.value.currentStep) {
            TrainingStep.LISTEN_3X -> TrainingStep.ACCOMPANIED_READING
            TrainingStep.ACCOMPANIED_READING -> TrainingStep.SOLO_RECORDING
            TrainingStep.SOLO_RECORDING -> TrainingStep.PLAYBACK_REVIEW
            TrainingStep.PLAYBACK_REVIEW -> TrainingStep.BLIND_TEST
            TrainingStep.BLIND_TEST -> TrainingStep.BLIND_TEST
        }
        _uiState.value = _uiState.value.copy(
            currentStep = next,
            isTextRevealed = next != TrainingStep.BLIND_TEST
        )
    }

    fun goToPrevStep() {
        audioPlayer.stop()
        val prev = when (_uiState.value.currentStep) {
            TrainingStep.LISTEN_3X -> TrainingStep.LISTEN_3X
            TrainingStep.ACCOMPANIED_READING -> TrainingStep.LISTEN_3X
            TrainingStep.SOLO_RECORDING -> TrainingStep.ACCOMPANIED_READING
            TrainingStep.PLAYBACK_REVIEW -> TrainingStep.SOLO_RECORDING
            TrainingStep.BLIND_TEST -> TrainingStep.PLAYBACK_REVIEW
        }
        _uiState.value = _uiState.value.copy(
            currentStep = prev,
            isTextRevealed = prev != TrainingStep.BLIND_TEST
        )
    }

    fun completeSession() {
        viewModelScope.launch {
            val updated = memorizationRepository.markReviewed(surahNumber, ayahNumber)
            _uiState.value = _uiState.value.copy(
                isSessionCompleted = true,
                status = updated.memorizationStatus,
                message = "Session validée avec succès !"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
