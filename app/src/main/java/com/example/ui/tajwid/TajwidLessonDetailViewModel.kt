package com.example.ui.tajwid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.data.local.db.AppDatabase
import com.example.data.repository.TajwidRepository
import com.example.domain.model.TajwidExample
import com.example.domain.model.TajwidExercise
import com.example.domain.model.TajwidLesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TajwidLessonDetailUiState(
    val lesson: TajwidLesson? = null,
    val isCompleted: Boolean = false,
    val selectedAnswers: Map<String, Int> = emptyMap(),
    val submittedAnswers: Map<String, Boolean> = emptyMap(),
    val isAllExercisesCompleted: Boolean = false,
    val playingExampleUrl: String? = null
)

class TajwidLessonDetailViewModel(
    application: Application,
    val lessonId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val tajwidRepository = TajwidRepository(db.tajwidProgressDao())
    val audioPlayer = AudioPlayer(application)

    private val _uiState = MutableStateFlow(TajwidLessonDetailUiState())
    val uiState: StateFlow<TajwidLessonDetailUiState> = _uiState.asStateFlow()

    init {
        loadLesson()
    }

    private fun loadLesson() {
        val lesson = tajwidRepository.getLessonById(lessonId)
        _uiState.value = _uiState.value.copy(lesson = lesson)

        viewModelScope.launch {
            tajwidRepository.getProgressForLessonFlow(lessonId).collect { entity ->
                val isCompleted = entity?.isCompleted == true
                _uiState.value = _uiState.value.copy(isCompleted = isCompleted)
            }
        }
    }

    fun playExampleAudio(example: TajwidExample) {
        val url = example.getAudioUrl()
        _uiState.value = _uiState.value.copy(playingExampleUrl = url)
        audioPlayer.togglePlayPause(url) {
            _uiState.value = _uiState.value.copy(playingExampleUrl = null)
        }
    }

    fun selectOption(exerciseId: String, optionIndex: Int) {
        val current = _uiState.value.selectedAnswers.toMutableMap()
        current[exerciseId] = optionIndex
        _uiState.value = _uiState.value.copy(selectedAnswers = current)
    }

    fun submitAnswer(exercise: TajwidExercise) {
        val selected = _uiState.value.selectedAnswers[exercise.id] ?: return
        val isCorrect = selected == exercise.correctOptionIndex

        val currentSubmitted = _uiState.value.submittedAnswers.toMutableMap()
        currentSubmitted[exercise.id] = isCorrect

        val lesson = _uiState.value.lesson
        val allExercises = lesson?.exercises ?: emptyList()
        val allDone = allExercises.isNotEmpty() && allExercises.all { currentSubmitted.containsKey(it.id) }

        _uiState.value = _uiState.value.copy(
            submittedAnswers = currentSubmitted,
            isAllExercisesCompleted = allDone
        )

        if (allDone) {
            val correctCount = currentSubmitted.count { it.value }
            viewModelScope.launch {
                tajwidRepository.markLessonCompleted(lessonId, correctCount)
            }
        }
    }

    fun completeLessonManually() {
        viewModelScope.launch {
            tajwidRepository.markLessonCompleted(lessonId, _uiState.value.lesson?.exercises?.size ?: 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
