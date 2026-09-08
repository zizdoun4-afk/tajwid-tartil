package com.example.ui.tajwid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.data.local.db.AppDatabase
import com.example.data.repository.QuranRepository
import com.example.domain.model.Ayah
import com.example.domain.model.RecitationStyle
import com.example.domain.model.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ReadWithMeStage {
    LISTENING_MODEL,
    USER_READING
}

data class ReadWithMeUiState(
    val surah: Surah? = null,
    val ayahs: List<Ayah> = emptyList(),
    val currentIndex: Int = 0,
    val stage: ReadWithMeStage = ReadWithMeStage.LISTENING_MODEL,
    val isPlaying: Boolean = false,
    val recitationStyle: RecitationStyle = RecitationStyle.TARTIL
) {
    val currentAyah: Ayah?
        get() = ayahs.getOrNull(currentIndex)
}

class ReadWithMeViewModel(
    application: Application,
    val surahNumber: Int,
    val startAyahIndex: Int = 0
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val quranRepository = QuranRepository(db.quranCacheDao(), application)
    val audioPlayer = AudioPlayer(application)

    private val _uiState = MutableStateFlow(ReadWithMeUiState(currentIndex = startAyahIndex))
    val uiState: StateFlow<ReadWithMeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val surah = quranRepository.getDefaultSurahs().find { it.number == surahNumber }
            val ayahsResult = quranRepository.getSurahAyahs(surahNumber)
            val ayahs = ayahsResult.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                surah = surah,
                ayahs = ayahs,
                currentIndex = startAyahIndex.coerceIn(0, (ayahs.size - 1).coerceAtLeast(0))
            )

            // Auto play model for first ayah
            playCurrentModel()
        }
    }

    fun playCurrentModel() {
        val ayah = _uiState.value.currentAyah ?: return
        val url = _uiState.value.recitationStyle.buildAudioUrl(surahNumber, ayah.numberInSurah)

        _uiState.value = _uiState.value.copy(
            stage = ReadWithMeStage.LISTENING_MODEL,
            isPlaying = true
        )

        audioPlayer.togglePlayPause(url) {
            // When reference finishes playing, move to user reading stage
            _uiState.value = _uiState.value.copy(
                stage = ReadWithMeStage.USER_READING,
                isPlaying = false
            )
        }
    }

    fun repeatModel() {
        audioPlayer.stop()
        playCurrentModel()
    }

    fun nextAyah() {
        audioPlayer.stop()
        val nextIdx = _uiState.value.currentIndex + 1
        if (nextIdx < _uiState.value.ayahs.size) {
            _uiState.value = _uiState.value.copy(
                currentIndex = nextIdx,
                stage = ReadWithMeStage.LISTENING_MODEL
            )
            playCurrentModel()
        }
    }

    fun prevAyah() {
        audioPlayer.stop()
        val prevIdx = _uiState.value.currentIndex - 1
        if (prevIdx >= 0) {
            _uiState.value = _uiState.value.copy(
                currentIndex = prevIdx,
                stage = ReadWithMeStage.LISTENING_MODEL
            )
            playCurrentModel()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
