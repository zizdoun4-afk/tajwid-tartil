package com.example.ui.memorization

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.repository.MemorizationRepository
import com.example.data.repository.QuranRepository
import com.example.data.repository.SurahMemorizationProgress
import com.example.domain.model.MemorizationStatus
import com.example.domain.model.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HifzDashboardUiState(
    val isLoading: Boolean = true,
    val dueReviews: List<MemorizationStatusEntity> = emptyList(),
    val reviewList: List<MemorizationStatusEntity> = emptyList(),
    val surahsProgress: List<SurahMemorizationProgress> = emptyList(),
    val surahsMap: Map<Int, Surah> = emptyMap(),
    val versesReviewedWeek: Int = 0,
    val versesMemorizedTotal: Int = 0,
    val versesLearningTotal: Int = 0
)

class MemorizationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val memorizationRepository = MemorizationRepository(db.memorizationDao())
    private val quranRepository = QuranRepository(db.quranCacheDao(), application)

    private val _uiState = MutableStateFlow(HifzDashboardUiState())
    val uiState: StateFlow<HifzDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val surahs = quranRepository.getDefaultSurahs()
            val surahMap = surahs.associateBy { it.number }

            combine(
                memorizationRepository.getDueForReview(),
                memorizationRepository.getInReviewStatus(),
                memorizationRepository.getAllStatusesFlow()
            ) { due, inReview, allStatuses ->
                val memorizedCount = allStatuses.count { it.status == MemorizationStatus.MEMORIZED.name }
                val learningCount = allStatuses.count { it.status == MemorizationStatus.LEARNING.name }

                // Group by surah to calculate progress %
                val groupedBySurah = allStatuses.groupBy { it.surahNumber }
                val progressList = groupedBySurah.mapNotNull { (surahNum, statusList) ->
                    val surah = surahMap[surahNum] ?: return@mapNotNull null
                    val mem = statusList.count { it.status == MemorizationStatus.MEMORIZED.name }
                    val learn = statusList.count { it.status == MemorizationStatus.LEARNING.name }
                    val rev = statusList.count { it.status == MemorizationStatus.REVIEW.name }
                    SurahMemorizationProgress(
                        surahNumber = surahNum,
                        memorizedCount = mem,
                        learningCount = learn,
                        reviewCount = rev,
                        totalAyahs = surah.numberOfAyahs
                    )
                }.sortedByDescending { it.percentage }

                val weeklyReviewed = allStatuses.count {
                    val last = it.lastReviewedAtEpochMillis ?: 0L
                    last >= (System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
                }

                HifzDashboardUiState(
                    isLoading = false,
                    dueReviews = due,
                    reviewList = inReview,
                    surahsProgress = progressList,
                    surahsMap = surahMap,
                    versesReviewedWeek = weeklyReviewed,
                    versesMemorizedTotal = memorizedCount,
                    versesLearningTotal = learningCount
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
