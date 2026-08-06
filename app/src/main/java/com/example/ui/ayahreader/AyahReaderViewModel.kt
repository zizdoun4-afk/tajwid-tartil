package com.example.ui.ayahreader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.audio.PlayerState
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.BookmarkEntity
import com.example.data.local.preferences.UserPreferencesRepository
import com.example.data.repository.QuranRepository
import com.example.data.repository.RecordingRepository
import com.example.domain.model.Ayah
import com.example.domain.model.RecitationStyle
import com.example.domain.model.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

data class AyahReaderUiState(
    val isLoadingAyahs: Boolean = true,
    val surah: Surah? = null,
    val ayahs: List<Ayah> = emptyList(),
    val currentAyahIndex: Int = 0,
    val recitationStyle: RecitationStyle = RecitationStyle.TARTIL,
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val recordingAmplitude: Float = 0f,
    val showPostRecordingDialog: Boolean = false,
    val showMetadataDialog: Boolean = false,
    val tempRecordingFile: File? = null,
    val reciterNameInput: String = "Ahmed",
    val isBookmarked: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val repeatCount: Int = 1,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AyahReaderViewModel(
    application: Application,
    val surahNumber: Int
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val quranRepository = QuranRepository(db.quranCacheDao())
    private val recordingRepository = RecordingRepository(application, db.recordingDao())
    private val bookmarkDao = db.bookmarkDao()
    private val prefsRepository = UserPreferencesRepository(application)

    val audioPlayer = AudioPlayer(application)
    val audioRecorder = AudioRecorder(application)

    private val _uiState = MutableStateFlow(AyahReaderUiState())
    val uiState: StateFlow<AyahReaderUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeRecorder()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAyahs = true, errorMessage = null)

            // Load preferred style and reciter name
            val style = prefsRepository.preferredStyle.first()
            val defaultReciter = prefsRepository.defaultReciterName.first()
            _uiState.value = _uiState.value.copy(
                recitationStyle = style,
                reciterNameInput = defaultReciter
            )

            // Load surah info from default list or DB
            val surah = QuranRepository.DEFAULT_SURAHS.find { it.number == surahNumber }
            _uiState.value = _uiState.value.copy(surah = surah)

            // Fetch ayahs
            val result = quranRepository.getSurahAyahs(surahNumber)
            if (result.isSuccess) {
                val ayahs = result.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    isLoadingAyahs = false,
                    ayahs = ayahs,
                    currentAyahIndex = 0
                )
                checkBookmarkStatus()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingAyahs = false,
                    errorMessage = "Impossible de charger les versets (${result.exceptionOrNull()?.localizedMessage})"
                )
            }
        }
    }

    private fun observeRecorder() {
        viewModelScope.launch {
            audioRecorder.isRecording.collect { recording ->
                _uiState.value = _uiState.value.copy(isRecording = recording)
            }
        }
        viewModelScope.launch {
            audioRecorder.elapsedMillis.collect { elapsed ->
                _uiState.value = _uiState.value.copy(recordingElapsedMs = elapsed)
            }
        }
        viewModelScope.launch {
            audioRecorder.amplitudeFlow.collect { amp ->
                _uiState.value = _uiState.value.copy(recordingAmplitude = amp)
            }
        }
    }

    private fun checkBookmarkStatus() {
        val currentAyah = currentAyahOrNull() ?: return
        viewModelScope.launch {
            bookmarkDao.isBookmarked(surahNumber, currentAyah.numberInSurah).collect { isBookmarked ->
                _uiState.value = _uiState.value.copy(isBookmarked = isBookmarked)
            }
        }
    }

    fun toggleBookmark() {
        val currentAyah = currentAyahOrNull() ?: return
        val surahName = _uiState.value.surah?.englishName ?: "Sourate $surahNumber"
        viewModelScope.launch {
            if (_uiState.value.isBookmarked) {
                bookmarkDao.deleteBookmark(surahNumber, currentAyah.numberInSurah)
                _uiState.value = _uiState.value.copy(
                    isBookmarked = false,
                    successMessage = "Retiré des favoris"
                )
            } else {
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        surahNumber = surahNumber,
                        ayahNumber = currentAyah.numberInSurah,
                        surahName = surahName,
                        ayahText = currentAyah.text
                    )
                )
                _uiState.value = _uiState.value.copy(
                    isBookmarked = true,
                    successMessage = "Ajouté aux favoris !"
                )
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun setRepeatCount(count: Int) {
        audioPlayer.setTargetRepeatCount(count)
        _uiState.value = _uiState.value.copy(repeatCount = count)
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
        // If playing reference audio, stop or switch
        audioPlayer.stop()
    }

    fun playReferenceAudio() {
        val ayahs = _uiState.value.ayahs
        val index = _uiState.value.currentAyahIndex
        if (ayahs.isEmpty() || index !in ayahs.indices) return

        val ayah = ayahs[index]
        val url = _uiState.value.recitationStyle.buildAudioUrl(surahNumber, ayah.numberInSurah)
        audioPlayer.togglePlayPause(url)
    }

    fun startRecording() {
        audioPlayer.stop()
        val tempFile = audioRecorder.startRecording()
        if (tempFile != null) {
            _uiState.value = _uiState.value.copy(
                tempRecordingFile = tempFile,
                errorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Erreur lors du démarrage de l'enregistrement micro"
            )
        }
    }

    fun stopRecording() {
        val tempFile = audioRecorder.stopRecording()
        if (tempFile != null && tempFile.exists()) {
            _uiState.value = _uiState.value.copy(
                tempRecordingFile = tempFile,
                showPostRecordingDialog = true
            )
        }
    }

    fun playUserRecording() {
        val tempFile = _uiState.value.tempRecordingFile
        if (tempFile != null && tempFile.exists()) {
            audioPlayer.togglePlayPause(tempFile.absolutePath)
        }
    }

    fun onPostRecordingSaveClicked() {
        audioPlayer.stop()
        _uiState.value = _uiState.value.copy(
            showPostRecordingDialog = false,
            showMetadataDialog = true
        )
    }

    fun onPostRecordingRecommencerClicked() {
        audioPlayer.stop()
        audioRecorder.cancelRecording()
        _uiState.value = _uiState.value.copy(
            showPostRecordingDialog = false,
            tempRecordingFile = null
        )
        startRecording()
    }

    fun onPostRecordingCancelClicked() {
        audioPlayer.stop()
        audioRecorder.cancelRecording()
        _uiState.value = _uiState.value.copy(
            showPostRecordingDialog = false,
            tempRecordingFile = null
        )
    }

    fun onReciterNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(reciterNameInput = name)
    }

    fun confirmSaveRecording() {
        val tempFile = _uiState.value.tempRecordingFile ?: return
        val currentAyah = currentAyahOrNull() ?: return
        val surahName = _uiState.value.surah?.englishName ?: "Sourate $surahNumber"
        val reciter = _uiState.value.reciterNameInput.ifBlank { "Récitateur" }
        val duration = _uiState.value.recordingElapsedMs

        viewModelScope.launch {
            try {
                prefsRepository.setDefaultReciterName(reciter)
                recordingRepository.saveRecording(
                    tempFile = tempFile,
                    reciterName = reciter,
                    surahNumber = surahNumber,
                    surahName = surahName,
                    ayahNumber = currentAyah.numberInSurah,
                    durationMs = duration
                )
                _uiState.value = _uiState.value.copy(
                    showMetadataDialog = false,
                    tempRecordingFile = null,
                    successMessage = "Enregistrement sauvegardé avec succès !"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Erreur de sauvegarde : ${e.localizedMessage}"
                )
            }
        }
    }

    fun dismissMetadataDialog() {
        _uiState.value = _uiState.value.copy(showMetadataDialog = false)
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
            checkBookmarkStatus()
        }
    }

    fun goToPrevAyah() {
        audioPlayer.stop()
        if (_uiState.value.currentAyahIndex > 0) {
            _uiState.value = _uiState.value.copy(
                currentAyahIndex = _uiState.value.currentAyahIndex - 1
            )
            checkBookmarkStatus()
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
        audioRecorder.cancelRecording()
    }
}
