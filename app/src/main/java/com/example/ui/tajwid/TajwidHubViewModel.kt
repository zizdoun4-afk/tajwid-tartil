package com.example.ui.tajwid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.TajwidProgressEntity
import com.example.data.repository.TajwidRepository
import com.example.domain.model.TajwidLesson
import com.example.domain.model.TajwidLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TajwidHubUiState(
    val selectedLevel: TajwidLevel = TajwidLevel.BEGINNER,
    val beginnerLessons: List<TajwidLesson> = emptyList(),
    val intermediateLessons: List<TajwidLesson> = emptyList(),
    val advancedLessons: List<TajwidLesson> = emptyList(),
    val progressMap: Map<String, TajwidProgressEntity> = emptyMap(),
    val totalLessonsCount: Int = 0,
    val completedLessonsCount: Int = 0
)

class TajwidHubViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val tajwidRepository = TajwidRepository(db.tajwidProgressDao())

    private val _uiState = MutableStateFlow(TajwidHubUiState())
    val uiState: StateFlow<TajwidHubUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val allLessons = tajwidRepository.getAllLessons()
        val beginner = allLessons.filter { it.level == TajwidLevel.BEGINNER }
        val intermediate = allLessons.filter { it.level == TajwidLevel.INTERMEDIATE }
        val advanced = allLessons.filter { it.level == TajwidLevel.ADVANCED }

        viewModelScope.launch {
            tajwidRepository.getProgressFlow().collect { progressList ->
                val map = progressList.associateBy { it.lessonId }
                val completedCount = progressList.count { it.isCompleted }

                _uiState.value = TajwidHubUiState(
                    selectedLevel = _uiState.value.selectedLevel,
                    beginnerLessons = beginner,
                    intermediateLessons = intermediate,
                    advancedLessons = advanced,
                    progressMap = map,
                    totalLessonsCount = allLessons.size,
                    completedLessonsCount = completedCount
                )
            }
        }
    }

    fun selectLevel(level: TajwidLevel) {
        _uiState.value = _uiState.value.copy(selectedLevel = level)
    }
}
