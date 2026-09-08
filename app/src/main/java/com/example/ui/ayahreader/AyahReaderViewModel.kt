package com.example.ui.ayahreader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.PlayerState
import com.example.audio.QuranRecorderEngine
import com.example.audio.QuranRecorderEngineImpl
import com.example.audio.RepeatMode
import com.example.audio.SessionResult
import com.example.audio.SessionState
import com.example.data.local.db.AppDatabase
import com.example.data.local.preferences.UserPreferencesRepository
import com.example.data.repository.MemorizationRepository
import com.example.data.repository.QuranRepository
import com.example.data.repository.RecitationMeta
import com.example.data.repository.RecitationsStore
import com.example.data.repository.RecordingRepository
import com.example.domain.model.Ayah
import com.example.domain.model.MemorizationStatus
import com.example.domain.model.RecitationStyle
import com.example.domain.model.Surah
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

enum class RecordWorkflowMode { AYAH, SESSION }
enum class TapStyle { TAP, HOLD }

data class AyahReaderUiState(
    val isLoadingAyahs: Boolean = true,
    val surah: Surah? = null,
    val ayahs: List<Ayah> = emptyList(),
    val currentAyahIndex: Int = 0,
    val recitationStyle: RecitationStyle = RecitationStyle.TARTIL,
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val bookmarkedKeys: Set<String> = emptySet(),

    // Recording Engine State
    val recordWorkflowMode: RecordWorkflowMode = RecordWorkflowMode.AYAH,
    val tapStyle: TapStyle = TapStyle.TAP,
    val isTaking: Boolean = false,
    val sessionState: SessionState = SessionState.IDLE,
    val recordingElapsedMs: Long = 0L,
    val recordingAmplitude: Float = 0f,
    val recordedAyahMap: Map<Int, Boolean> = emptyMap(),

    // Memorization / Hifz State
    val memorizationStatusMap: Map<Int, MemorizationStatus> = emptyMap(),
    val showAddToRevisionPrompt: Boolean = false,
    val pendingRevisionAyah: Int? = null,

    // Dialogs & Exit Guard
    val showSessionConfirmDialog: Boolean = false,
    val pendingSessionResult: SessionResult? = null,
    val showExitGuardDialog: Boolean = false,

    val showPostRecordingDialog: Boolean = false,
    val showMetadataDialog: Boolean = false,
    val tempRecordingFile: File? = null,
    val reciterNameInput: String = "Ahmed",
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val isCurrentAyahBookmarked: Boolean
        get() {
            val ayah = ayahs.getOrNull(currentAyahIndex) ?: return false
            return bookmarkedKeys.contains("${surah?.number ?: 0}:${ayah.numberInSurah}")
        }

    val isRecordingActive: Boolean
        get() = isTaking || sessionState != SessionState.IDLE
}

class AyahReaderViewModel(
    application: Application,
    val surahNumber: Int
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val quranRepository = QuranRepository(db.quranCacheDao(), application)
    private val recordingRepository = RecordingRepository(application, db.recordingDao())
    private val prefsRepository = UserPreferencesRepository(application)
    val memorizationRepository = MemorizationRepository(db.memorizationDao())

    val audioPlayer = AudioPlayer(application)
    val quranRecorderEngine: QuranRecorderEngine = QuranRecorderEngineImpl(application)
    val recitationsStore: RecitationsStore = RecitationsStore(application, db.recordingDao())

    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var isRetryingWithFallback = false
    private var lastPlayingAyahNumber: Int? = null

    private val _uiState = MutableStateFlow(AyahReaderUiState())
    val uiState: StateFlow<AyahReaderUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeEngineState()
        observeRecitations()
        observeBookmarks()
        observeMemorizationStatus()
        observeAudioErrorsForFallback()
    }

    private fun observeMemorizationStatus() {
        viewModelScope.launch {
            memorizationRepository.getStatusForSurahFlow(surahNumber).collect { entities ->
                val map = entities.associate { it.ayahNumber to it.memorizationStatus }
                _uiState.value = _uiState.value.copy(memorizationStatusMap = map)
            }
        }
    }

    private fun observeAudioErrorsForFallback() {
        viewModelScope.launch {
            audioPlayer.playerState.collect { state ->
                when (state) {
                    is PlayerState.Error -> {
                        val ayahNum = lastPlayingAyahNumber
                        if (!isRetryingWithFallback && ayahNum != null) {
                            isRetryingWithFallback = true
                            val fallbackUrl = _uiState.value.recitationStyle.buildFallbackAudioUrl(surahNumber, ayahNum)
                            audioPlayer.play(fallbackUrl)
                        } else {
                            _uiState.value = _uiState.value.copy(errorMessage = state.message)
                            isRetryingWithFallback = false
                        }
                    }
                    is PlayerState.Playing -> {
                        isRetryingWithFallback = false
                    }
                    is PlayerState.Idle -> {
                        isRetryingWithFallback = false
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            prefsRepository.bookmarkedAyahs.collect { bookmarks ->
                _uiState.value = _uiState.value.copy(bookmarkedKeys = bookmarks)
            }
        }
    }

    private fun observeEngineState() {
        viewModelScope.launch {
            quranRecorderEngine.uiState.collect { recorderState ->
                _uiState.value = _uiState.value.copy(
                    isTaking = recorderState.isTaking,
                    sessionState = recorderState.sessionState,
                    recordingElapsedMs = recorderState.elapsedMs,
                    recordingAmplitude = recorderState.meteringDb
                )
            }
        }
    }

    private fun observeRecitations() {
        viewModelScope.launch {
            recitationsStore.recitationsFlow.collect {
                val map = recitationsStore.recordedAyahMap(surahNumber)
                _uiState.value = _uiState.value.copy(recordedAyahMap = map)
            }
        }
    }

    fun setRecordWorkflowMode(mode: RecordWorkflowMode) {
        if (_uiState.value.isRecordingActive) return
        _uiState.value = _uiState.value.copy(recordWorkflowMode = mode)
    }

    fun setTapStyle(style: TapStyle) {
        if (_uiState.value.isRecordingActive) return
        _uiState.value = _uiState.value.copy(tapStyle = style)
    }

    fun toggleBookmark() {
        val currentAyah = _uiState.value.ayahs.getOrNull(_uiState.value.currentAyahIndex) ?: return
        viewModelScope.launch {
            prefsRepository.toggleBookmark(surahNumber, currentAyah.numberInSurah)
        }
    }

    fun toggleTranslation() {
        _uiState.value = _uiState.value.copy(showTranslation = !_uiState.value.showTranslation)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setSpeed(speed)
    }

    fun setRepeatMode(mode: RepeatMode) {
        audioPlayer.setRepeatMode(mode)
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAyahs = true, errorMessage = null)

            val style = prefsRepository.preferredStyle.first()
            val defaultReciter = prefsRepository.defaultReciterName.first()
            _uiState.value = _uiState.value.copy(
                recitationStyle = style,
                reciterNameInput = defaultReciter
            )

            val surah = quranRepository.getDefaultSurahs().find { it.number == surahNumber }
            _uiState.value = _uiState.value.copy(surah = surah)

            val result = quranRepository.getSurahAyahs(surahNumber)
            if (result.isSuccess) {
                val ayahs = result.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    isLoadingAyahs = false,
                    ayahs = ayahs,
                    currentAyahIndex = 0
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingAyahs = false,
                    errorMessage = "Impossible de charger les versets"
                )
            }
        }
    }

    fun toggleRecitationStyle() {
        val newStyle = if (_uiState.value.recitationStyle == RecitationStyle.TARTIL) {
            RecitationStyle.TAJWID
        } else {
            RecitationStyle.TARTIL
        }
        _uiState.value = _uiState.value.copy(recitationStyle = newStyle)
        viewModelScope.launch {
            prefsRepository.setPreferredStyle(newStyle)
        }
        audioPlayer.stop()
    }

    fun playReferenceAudio() {
        val ayahs = _uiState.value.ayahs
        val index = _uiState.value.currentAyahIndex
        if (ayahs.isEmpty() || index !in ayahs.indices) return

        val ayah = ayahs[index]
        lastPlayingAyahNumber = ayah.numberInSurah
        isRetryingWithFallback = false
        val url = _uiState.value.recitationStyle.buildAudioUrl(surahNumber, ayah.numberInSurah)
        audioPlayer.togglePlayPause(url)
    }

    // --- Mode Ayah (Take) Engine Operations ---
    fun startTake() {
        audioPlayer.stop()
        viewModelScope.launch {
            val started = quranRecorderEngine.startTake()
            if (!started) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Impossible de démarrer l'enregistrement (vérifiez les permissions micro)"
                )
            }
        }
    }

    fun stopTakeAndSave() {
        viewModelScope.launch {
            val result = quranRecorderEngine.stopTake()
            if (result != null) {
                val currentAyah = currentAyahOrNull()
                if (currentAyah != null) {
                    val reciter = _uiState.value.reciterNameInput.ifBlank { "Mon Enregistrement" }
                    val surahName = _uiState.value.surah?.englishName ?: "Sourate $surahNumber"

                    recitationsStore.saveAyahRecording(
                        tempPath = result.filePath,
                        sura = surahNumber,
                        surahName = surahName,
                        aya = currentAyah.numberInSurah,
                        riwaya = "hafs",
                        durationMs = result.durationMs,
                        reciterName = reciter
                    )

                    // Prompt to add to revision (Hifz), non-intrusive hook
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Verset ${currentAyah.numberInSurah} enregistré avec succès !",
                        showAddToRevisionPrompt = true,
                        pendingRevisionAyah = currentAyah.numberInSurah
                    )
                }
            }
        }
    }

    fun confirmAddToRevision() {
        val ayahNum = _uiState.value.pendingRevisionAyah ?: return
        viewModelScope.launch {
            memorizationRepository.setAyahStatus(surahNumber, ayahNum, MemorizationStatus.LEARNING)
            _uiState.value = _uiState.value.copy(
                showAddToRevisionPrompt = false,
                pendingRevisionAyah = null,
                successMessage = "Verset $ayahNum ajouté à votre programme de mémorisation !"
            )
            goToNextAyah()
        }
    }

    fun dismissAddToRevision() {
        _uiState.value = _uiState.value.copy(
            showAddToRevisionPrompt = false,
            pendingRevisionAyah = null
        )
        goToNextAyah()
    }

    fun discardTake() {
        viewModelScope.launch {
            quranRecorderEngine.discardTake()
        }
    }

    // --- Mode Session Engine Operations ---
    fun startSession() {
        audioPlayer.stop()
        val currentAyah = currentAyahOrNull() ?: return
        viewModelScope.launch {
            val started = quranRecorderEngine.startSession(currentAyah.numberInSurah)
            if (!started) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Erreur au démarrage de la session d'enregistrement"
                )
            }
        }
    }

    fun pauseSession() {
        quranRecorderEngine.pauseSession()
    }

    fun resumeSession() {
        quranRecorderEngine.resumeSession()
    }

    fun markNextAyahInSession() {
        val ayahs = _uiState.value.ayahs
        val currentIndex = _uiState.value.currentAyahIndex
        if (currentIndex < ayahs.size - 1) {
            val nextIndex = currentIndex + 1
            val nextAyah = ayahs[nextIndex]
            _uiState.value = _uiState.value.copy(currentAyahIndex = nextIndex)
            quranRecorderEngine.markAyah(nextAyah.numberInSurah)
        } else {
            // Reached final ayah in session - prompt and stop cleanly
            _uiState.value = _uiState.value.copy(
                errorMessage = "Fin de la sourate atteinte — arrêtez la session"
            )
            requestStopSession()
        }
    }

    fun requestStopSession() {
        viewModelScope.launch {
            val sessionResult = quranRecorderEngine.stopSession()
            if (sessionResult != null) {
                _uiState.value = _uiState.value.copy(
                    pendingSessionResult = sessionResult,
                    showSessionConfirmDialog = true
                )
            }
        }
    }

    fun confirmSaveSession() {
        val sessionResult = _uiState.value.pendingSessionResult ?: return
        val ayahs = _uiState.value.ayahs
        if (ayahs.isEmpty()) return

        val surahName = _uiState.value.surah?.englishName ?: "Sourate $surahNumber"
        val reciter = _uiState.value.reciterNameInput.ifBlank { "Mon Enregistrement" }

        val markers = sessionResult.markers
        val ayaFrom = markers.firstOrNull()?.aya ?: ayahs.first().numberInSurah
        val ayaTo = markers.lastOrNull()?.aya ?: ayahs.last().numberInSurah

        viewModelScope.launch {
            recitationsStore.saveSessionRecording(
                tempPath = sessionResult.filePath,
                sura = surahNumber,
                surahName = surahName,
                ayaFrom = ayaFrom,
                ayaTo = ayaTo,
                riwaya = "hafs",
                durationMs = sessionResult.durationMs,
                markers = markers,
                reciterName = reciter
            )

            _uiState.value = _uiState.value.copy(
                showSessionConfirmDialog = false,
                pendingSessionResult = null,
                successMessage = "Session de récitation (V.$ayaFrom → V.$ayaTo) enregistrée !"
            )
        }
    }

    fun discardSession() {
        viewModelScope.launch {
            _uiState.value.pendingSessionResult?.let {
                File(it.filePath).delete()
            }
            quranRecorderEngine.discardSession()
            _uiState.value = _uiState.value.copy(
                showSessionConfirmDialog = false,
                pendingSessionResult = null
            )
        }
    }

    // --- Navigation Guard ---
    fun onBackPressRequest(): Boolean {
        if (_uiState.value.isRecordingActive) {
            _uiState.value = _uiState.value.copy(showExitGuardDialog = true)
            return false // Intercept navigation
        }
        return true // Allow navigation
    }

    fun confirmExitAndDiscard() {
        viewModelScope.launch {
            if (_uiState.value.recordWorkflowMode == RecordWorkflowMode.AYAH) {
                quranRecorderEngine.discardTake()
            } else {
                quranRecorderEngine.discardSession()
            }
            _uiState.value = _uiState.value.copy(showExitGuardDialog = false)
        }
    }

    fun dismissExitGuard() {
        _uiState.value = _uiState.value.copy(showExitGuardDialog = false)
    }

    fun onReciterNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(reciterNameInput = name)
        viewModelScope.launch {
            prefsRepository.setDefaultReciterName(name)
        }
    }

    fun playUserRecording() {
        val tempFile = _uiState.value.tempRecordingFile
        if (tempFile != null && tempFile.exists()) {
            audioPlayer.togglePlayPause(tempFile.absolutePath)
        }
    }

    fun playRecordedAyah(ayaNumber: Int) {
        val meta = recitationsStore.findAyahRecording(surahNumber, ayaNumber) ?: return
        val audioFile = recitationsStore.getAudioFile(meta.id)
        if (audioFile.exists()) {
            audioPlayer.togglePlayPause(audioFile.absolutePath)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun goToNextAyah() {
        audioPlayer.stop()
        val count = _uiState.value.ayahs.size
        if (count > 0 && _uiState.value.currentAyahIndex < count - 1) {
            _uiState.value = _uiState.value.copy(
                currentAyahIndex = _uiState.value.currentAyahIndex + 1
            )
        }
    }

    fun goToPrevAyah() {
        audioPlayer.stop()
        if (_uiState.value.currentAyahIndex > 0) {
            _uiState.value = _uiState.value.copy(
                currentAyahIndex = _uiState.value.currentAyahIndex - 1
            )
        }
    }

    fun selectAyah(index: Int) {
        audioPlayer.stop()
        if (index in _uiState.value.ayahs.indices) {
            _uiState.value = _uiState.value.copy(currentAyahIndex = index)
        }
    }

    fun currentAyahOrNull(): Ayah? {
        val ayahs = _uiState.value.ayahs
        val index = _uiState.value.currentAyahIndex
        return if (index in ayahs.indices) ayahs[index] else null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        cleanupScope.launch {
            quranRecorderEngine.discardTake()
            quranRecorderEngine.discardSession()
        }
    }
}
