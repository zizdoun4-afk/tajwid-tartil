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
import com.example.domain.hifz.DailyHifzPlan
import com.example.domain.hifz.JuzMemorizationProgress
import com.example.domain.hifz.SmartHifzPlanner
import com.example.domain.hifz.SrsHealthSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HifzDashboardTab {
    TODAY_PLAN,
    SMART_QUEUE,
    WEAK_VERSES,
    SURAHS,
    JUZ
}

data class HifzDashboardUiState(
    val isLoading: Boolean = true,
    val dueReviews: List<MemorizationStatusEntity> = emptyList(),
    val reviewList: List<MemorizationStatusEntity> = emptyList(),
    val smartReviewQueue: List<MemorizationStatusEntity> = emptyList(),
    val weakVersesList: List<MemorizationStatusEntity> = emptyList(),
    val frequentlyFailedList: List<MemorizationStatusEntity> = emptyList(),
    val dailyPlan: DailyHifzPlan? = null,
    val srsHealth: SrsHealthSummary? = null,
    val surahsProgress: List<SurahMemorizationProgress> = emptyList(),
    val juzProgressList: List<JuzMemorizationProgress> = emptyList(),
    val surahsMap: Map<Int, Surah> = emptyMap(),
    val versesReviewedWeek: Int = 0,
    val versesReviewedMonth: Int = 0,
    val selectedPeriodDays: Int = 7,
    val lastReadSurahNumber: Int = 1,
    val lastReadAyahNumber: Int = 1,
    val totalRecordingsCount: Int = 0,
    val tajwidLessonsCompleted: Int = 0,
    val totalTajwidLessons: Int = 3,
    val versesStartedTotal: Int = 0,
    val versesLearningTotal: Int = 0,
    val versesInReviewTotal: Int = 0,
    val versesMemorizedTotal: Int = 0,
    val dailyTarget: Int = 3,
    val completedToday: Int = 0,
    val currentStreakDays: Int = 0,
    val nextNewAyah: Pair<Int, Int> = Pair(1, 1),
    val nextTestAyah: Pair<Int, Int>? = null,
    val isReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val activeTab: HifzDashboardTab = HifzDashboardTab.TODAY_PLAN
)

class MemorizationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val memorizationRepository = MemorizationRepository(db.memorizationDao())
    private val quranRepository = QuranRepository(db.quranCacheDao(), application)
    private val prefsRepository = UserPreferencesRepository(application)
    private val recordingDao = db.recordingDao()
    private val tajwidProgressDao = db.tajwidProgressDao()

    private val _selectedPeriodDays = MutableStateFlow(7)

    private val _uiState = MutableStateFlow(HifzDashboardUiState())
    val uiState: StateFlow<HifzDashboardUiState> = _uiState.asStateFlow()

    fun setPeriodDays(days: Int) {
        _selectedPeriodDays.value = days
    }

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

    private fun computeStreakDays(allStatuses: List<MemorizationStatusEntity>): Int {
        val reviewDates = allStatuses.mapNotNull { it.lastReviewedAtEpochMillis }.toSet()
        if (reviewDates.isEmpty()) return 0

        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val reviewDays = reviewDates.map { dayFormat.format(Date(it)) }.toSet()

        val cal = Calendar.getInstance()
        val todayStr = dayFormat.format(cal.time)

        var streak = 0
        if (reviewDays.contains(todayStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dayFormat.format(cal.time)
            if (!reviewDays.contains(yesterdayStr)) {
                return 0
            }
        }

        while (true) {
            val checkStr = dayFormat.format(cal.time)
            if (reviewDays.contains(checkStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun selectTab(tab: HifzDashboardTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
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
                prefsRepository.lastReadSurah,
                prefsRepository.lastReadAyahIndex,
                recordingDao.getAllRecordings(),
                tajwidProgressDao.getCompletedLessonsCountFlow(),
                _selectedPeriodDays
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
                val lastReadAyahIdx = args[8] as Int
                @Suppress("UNCHECKED_CAST")
                val allRecordings = args[9] as List<com.example.data.local.db.RecordingEntity>
                val completedTajwidCount = args[10] as Int
                val periodDays = args[11] as Int

                val now = System.currentTimeMillis()
                val startOfDay = getStartOfDayMillis()

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

                val completedTodayCount = allStatuses.count {
                    val last = it.lastReviewedAtEpochMillis ?: 0L
                    last >= startOfDay
                }

                // Smart Hifz Planner computations
                val dailyPlan = SmartHifzPlanner.computeDailyPlan(
                    allStatuses = allStatuses,
                    dailyTarget = dailyTarget,
                    startOfDayMillis = startOfDay,
                    nowMillis = now
                )

                val smartQueue = SmartHifzPlanner.buildSmartReviewQueue(
                    allStatuses = allStatuses,
                    nowMillis = now
                )

                val srsHealth = SmartHifzPlanner.computeSrsHealth(
                    allStatuses = allStatuses,
                    nowMillis = now
                )

                val weakVerses = allStatuses.filter { it.isWeak }
                val frequentlyFailed = allStatuses.filter { it.failedTests >= 2 }
                val juzProgress = SmartHifzPlanner.computeJuzProgress(allStatuses)
                val streakDays = computeStreakDays(allStatuses)

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
                    last >= (now - 7L * 24 * 60 * 60 * 1000)
                }

                val monthlyReviewed = allStatuses.count {
                    val last = it.lastReviewedAtEpochMillis ?: 0L
                    last >= (now - 30L * 24 * 60 * 60 * 1000)
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

                // Priority for next TEST action: first item in smartQueue or due or inReview
                val testAyah = smartQueue.firstOrNull()?.let {
                    Pair(it.surahNumber, it.ayahNumber)
                } ?: when {
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
                    smartReviewQueue = smartQueue,
                    weakVersesList = weakVerses,
                    frequentlyFailedList = frequentlyFailed,
                    dailyPlan = dailyPlan,
                    srsHealth = srsHealth,
                    surahsProgress = progressList,
                    juzProgressList = juzProgress,
                    surahsMap = surahMap,
                    versesReviewedWeek = weeklyReviewed,
                    versesReviewedMonth = monthlyReviewed,
                    selectedPeriodDays = periodDays,
                    lastReadSurahNumber = targetSurahNum,
                    lastReadAyahNumber = lastReadAyahIdx + 1,
                    totalRecordingsCount = allRecordings.size,
                    tajwidLessonsCompleted = completedTajwidCount,
                    totalTajwidLessons = 3,
                    versesStartedTotal = startedCount,
                    versesLearningTotal = learningCount,
                    versesInReviewTotal = inReviewCount,
                    versesMemorizedTotal = memorizedCount,
                    dailyTarget = dailyTarget,
                    completedToday = completedTodayCount,
                    currentStreakDays = streakDays,
                    nextNewAyah = nextNew,
                    nextTestAyah = testAyah,
                    isReminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    activeTab = _uiState.value.activeTab
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
