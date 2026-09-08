package com.example.ui.memorization

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.local.preferences.UserPreferencesRepository
import com.example.data.repository.MemorizationRepository
import com.example.data.repository.QuranRepository
import com.example.data.repository.SurahMemorizationProgress
import com.example.domain.model.MemorizationStatus
import com.example.domain.model.Surah
import com.example.reminder.HifzReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class HifzDashboardUiState(
    val isLoading: Boolean = true,
    val dueReviews: List<MemorizationStatusEntity> = emptyList(),
    val reviewList: List<MemorizationStatusEntity> = emptyList(),
    val surahsProgress: List<SurahMemorizationProgress> = emptyList(),
    val surahsMap: Map<Int, Surah> = emptyMap(),
    val versesReviewedWeek: Int = 0,
    val versesStartedTotal: Int = 0,
    val versesLearningTotal: Int = 0,
    val versesInReviewTotal: Int = 0,
    val versesMemorizedTotal: Int = 0,
    val dailyTarget: Int = 3,
    val completedToday: Int = 0,
    val nextNewAyah: Pair<Int, Int> = Pair(1, 1),
    val nextTestAyah: Pair<Int, Int>? = null,
    val isReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0
)

class MemorizationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val memorizationRepository = MemorizationRepository(db.memorizationDao())
    private val quranRepository = QuranRepository(db.quranCacheDao(), application)
    private val prefsRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(HifzDashboardUiState())
    val uiState: StateFlow<HifzDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val surahs = quranRepository.getDefaultSurahs()
            val surahMap = surahs.associateBy { it.number }

            combine(
                memorizationRepository.getDueForReview(),
                memorizationRepository.getInReviewStatus(),
                memorizationRepository.getAllStatusesFlow(),
                prefsRepository.dailyMemorizationTarget,
                prefsRepository.hifzReminderEnabled,
                prefsRepository.hifzReminderHour,
                prefsRepository.hifzReminderMinute,
                prefsRepository.lastReadSurah
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val due = args[0] as List<MemorizationStatusEntity>
                @Suppress("UNCHECKED_CAST")
                val inReview = args[1] as List<MemorizationStatusEntity>
                @Suppress("UNCHECKED_CAST")
                val allStatuses = args[2] as List<MemorizationStatusEntity>
                val dailyTarget = args[3] as Int
                val reminderEnabled = args[4] as Boolean
                val reminderHour = args[5] as Int
                val reminderMinute = args[6] as Int
                val lastReadSurah = args[7] as? Int

                val memorizedCount = allStatuses.count { it.status == MemorizationStatus.MEMORIZED.name }
                val learningCount = allStatuses.count { it.status == MemorizationStatus.LEARNING.name }
                val inReviewCount = allStatuses.count { it.status == MemorizationStatus.REVIEW.name }
                val startedCount = allStatuses.count {
                    it.status in listOf(
                        MemorizationStatus.NEW.name,
                        MemorizationStatus.LEARNING.name,
                        MemorizationStatus.REVIEW.name,
                        MemorizationStatus.MEMORIZED.name
                    )
                }

                val startOfDay = getStartOfDayMillis()
                val completedTodayCount = allStatuses.count {
                    val last = it.lastReviewedAtEpochMillis ?: 0L
                    last >= startOfDay
                }

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

                // Calculate next unmemorized ayah to learn for NEW action
                val targetSurahNum = lastReadSurah ?: 1
                val targetSurah = surahMap[targetSurahNum] ?: surahMap[1]
                val surahStatuses = allStatuses.filter { it.surahNumber == (targetSurah?.number ?: 1) }
                val memorizedOrLearningAyahs = surahStatuses.filter {
                    it.status == MemorizationStatus.MEMORIZED.name || it.status == MemorizationStatus.LEARNING.name
                }.map { it.ayahNumber }.toSet()

                val nextAyahInSurah = (1..(targetSurah?.numberOfAyahs ?: 7)).firstOrNull {
                    !memorizedOrLearningAyahs.contains(it)
                } ?: 1

                val nextNew = Pair(targetSurah?.number ?: 1, nextAyahInSurah)

                // Calculate next ayah for TEST action (due review, or first in-review, or first memorized)
                val testAyah = when {
                    due.isNotEmpty() -> Pair(due.first().surahNumber, due.first().ayahNumber)
                    inReview.isNotEmpty() -> Pair(inReview.first().surahNumber, inReview.first().ayahNumber)
                    else -> allStatuses.find { it.status == MemorizationStatus.MEMORIZED.name }?.let {
                        Pair(it.surahNumber, it.ayahNumber)
                    }
                }

                HifzDashboardUiState(
                    isLoading = false,
                    dueReviews = due,
                    reviewList = inReview,
                    surahsProgress = progressList,
                    surahsMap = surahMap,
                    versesReviewedWeek = weeklyReviewed,
                    versesStartedTotal = startedCount,
                    versesLearningTotal = learningCount,
                    versesInReviewTotal = inReviewCount,
                    versesMemorizedTotal = memorizedCount,
                    dailyTarget = dailyTarget,
                    completedToday = completedTodayCount,
                    nextNewAyah = nextNew,
                    nextTestAyah = testAyah,
                    isReminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setDailyTarget(target: Int) {
        viewModelScope.launch {
            prefsRepository.setDailyMemorizationTarget(target)
        }
    }

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setHifzReminderEnabled(enabled)
            val hour = _uiState.value.reminderHour
            val minute = _uiState.value.reminderMinute
            if (enabled) {
                HifzReminderScheduler.scheduleDailyReminder(getApplication(), hour, minute)
            } else {
                HifzReminderScheduler.cancelDailyReminder(getApplication())
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepository.setHifzReminderTime(hour, minute)
            if (_uiState.value.isReminderEnabled) {
                HifzReminderScheduler.scheduleDailyReminder(getApplication(), hour, minute)
            }
        }
    }
}
