package com.example.domain.companion

import com.example.audio.PacingAssessment
import com.example.audio.RecitationAnalysisEngine
import com.example.data.local.db.MemorizationDao
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.local.db.TajwidProgressDao
import com.example.data.local.db.TajwidProgressEntity
import com.example.domain.model.MemorizationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeMemorizationDao : MemorizationDao {
    val items = mutableListOf<MemorizationStatusEntity>()

    override fun getStatusForSurahFlow(surahNumber: Int): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items.filter { it.surahNumber == surahNumber })
    }

    override suspend fun getStatusForSurah(surahNumber: Int): List<MemorizationStatusEntity> {
        return items.filter { it.surahNumber == surahNumber }
    }

    override suspend fun getStatusForAyah(surahNumber: Int, ayahNumber: Int): MemorizationStatusEntity? {
        return items.find { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
    }

    override fun getStatusForAyahFlow(surahNumber: Int, ayahNumber: Int): Flow<MemorizationStatusEntity?> {
        return flowOf(items.find { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber })
    }

    override suspend fun upsertStatus(entity: MemorizationStatusEntity): Long {
        items.removeAll { it.surahNumber == entity.surahNumber && it.ayahNumber == entity.ayahNumber }
        items.add(entity)
        return 1L
    }

    override fun getDueForReview(nowMillis: Long): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items.filter { it.nextReviewDueEpochMillis != null && it.nextReviewDueEpochMillis <= nowMillis })
    }

    override fun getInReviewStatus(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items.filter { it.status == "REVIEW" })
    }

    override fun getInLearningStatus(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items.filter { it.status == "LEARNING" })
    }

    override fun getMemorizedStatus(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items.filter { it.status == "MEMORIZED" })
    }

    override fun getReviewedSinceCountFlow(sinceMillis: Long): Flow<Int> {
        return flowOf(items.count { (it.lastReviewedAtEpochMillis ?: 0L) >= sinceMillis })
    }

    override suspend fun getReviewedSinceCount(sinceMillis: Long): Int {
        return items.count { (it.lastReviewedAtEpochMillis ?: 0L) >= sinceMillis }
    }

    override suspend fun getWeeklyStats(sinceMillis: Long): List<MemorizationStatusEntity> {
        return items.filter { (it.lastReviewedAtEpochMillis ?: 0L) >= sinceMillis }
    }

    override fun getAllStatusesFlow(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items)
    }

    override fun getWeakVersesFlow(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items.filter { it.failedTests > 0 && it.failedTests >= it.successfulTests })
    }

    override suspend fun getWeakVerses(): List<MemorizationStatusEntity> {
        return items.filter { it.failedTests > 0 && it.failedTests >= it.successfulTests }
    }

    override fun getFrequentlyFailedVerses(minFails: Int): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items.filter { it.failedTests >= minFails })
    }

    override fun getVersesApproachingDue(windowStart: Long, windowEnd: Long): Flow<List<MemorizationStatusEntity>> {
        return flowOf(items.filter {
            val due = it.nextReviewDueEpochMillis ?: Long.MAX_VALUE
            due > windowStart && due <= windowEnd
        })
    }
}

class FakeTajwidDao : TajwidProgressDao {
    val store = mutableMapOf<String, TajwidProgressEntity>()

    override fun getAllProgressFlow(): Flow<List<TajwidProgressEntity>> = flowOf(store.values.toList())
    override suspend fun getProgressForLesson(lessonId: String): TajwidProgressEntity? = store[lessonId]
    override fun getProgressForLessonFlow(lessonId: String): Flow<TajwidProgressEntity?> = flowOf(store[lessonId])
    override suspend fun upsertProgress(entity: TajwidProgressEntity): Long {
        store[entity.lessonId] = entity
        return 1L
    }
    override fun getCompletedLessonsCountFlow(): Flow<Int> = flowOf(store.values.count { it.isCompleted })
}

class SmartCompanionEngineTest {

    private lateinit var memDao: FakeMemorizationDao
    private lateinit var tajwidDao: FakeTajwidDao

    @Before
    fun setUp() {
        memDao = FakeMemorizationDao()
        tajwidDao = FakeTajwidDao()
    }

    @Test
    fun `test computeDailyAgenda identifies due and weak ayahs correctly`() = runTest {
        val now = 1_000_000L

        // Ayah due for review
        memDao.items.add(
            MemorizationStatusEntity(
                surahNumber = 1,
                ayahNumber = 1,
                status = MemorizationStatus.MEMORIZED.name,
                nextReviewDueEpochMillis = now - 1000L // overdue
            )
        )

        // Ayah in learning
        memDao.items.add(
            MemorizationStatusEntity(
                surahNumber = 1,
                ayahNumber = 2,
                status = MemorizationStatus.LEARNING.name
            )
        )

        // Ayah in review
        memDao.items.add(
            MemorizationStatusEntity(
                surahNumber = 1,
                ayahNumber = 3,
                status = MemorizationStatus.REVIEW.name
            )
        )

        val agenda = SmartCompanionEngine.computeDailyAgenda(memDao, tajwidDao, currentTimeMillis = now)

        assertEquals(1, agenda.dueForReview.size)
        assertEquals(1, agenda.inLearning.size)
        assertEquals(1, agenda.weakAyahs.size)
        assertEquals(1, agenda.memorizedTotal)
        assertEquals(1, agenda.learningTotal)
        assertEquals(1, agenda.reviewTotal)
        assertNotNull(agenda.recommendedTajwidLessonId)
    }

    @Test
    fun `test RecitationAnalysisEngine baseline pacing evaluation`() {
        val ayahText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"

        // Null file safe fallback
        val diagNull = RecitationAnalysisEngine.analyzeRecitation(
            audioFile = null,
            surahNumber = 1,
            ayahNumber = 1,
            ayahText = ayahText
        )

        assertNotNull(diagNull)
        assertTrue(diagNull.tajwidPointsToWatch.isNotEmpty()) // Fatiha 1:1 has annotations
        assertNotNull(diagNull.pedagogicalDisclaimerFr)
    }

    @Test
    fun `test RecitationAnalysisEngine Tajwid points integration`() {
        val diag = RecitationAnalysisEngine.analyzeRecitation(
            audioFile = null,
            surahNumber = 112,
            ayahNumber = 1,
            ayahText = "قُلْ هُوَ اللَّهُ أَحَدٌ"
        )

        // Surah 112:1 has Qalqalah annotations in TajwidRepository
        assertTrue(diag.tajwidPointsToWatch.any { it.ruleNameFr.contains("Qalqalah") || it.targetSnippet.contains("أَحَد") })
    }

    @Test
    fun `test Rafiq validate mastered marks MEMORIZED with SRS schedule`() = runTest {
        val repo = com.example.data.repository.MemorizationRepository(memDao)
        memDao.items.add(
            MemorizationStatusEntity(
                surahNumber = 1,
                ayahNumber = 5,
                status = MemorizationStatus.LEARNING.name,
                reviewCount = 1
            )
        )

        // When user explicitly validates mastery in Rafiq:
        val result = repo.markMemorized(1, 5)

        assertEquals(MemorizationStatus.MEMORIZED.name, result.status)
        assertEquals(MemorizationStatus.MEMORIZED, memDao.getStatusForAyah(1, 5)?.memorizationStatus)
        assertNotNull(result.nextReviewDueEpochMillis)
        assertTrue(result.nextReviewDueEpochMillis!! > System.currentTimeMillis())
    }

    @Test
    fun `test Rafiq validate failed keeps or sets REVIEW and does NOT mark MEMORIZED`() = runTest {
        val repo = com.example.data.repository.MemorizationRepository(memDao)
        memDao.items.add(
            MemorizationStatusEntity(
                surahNumber = 1,
                ayahNumber = 6,
                status = MemorizationStatus.LEARNING.name,
                reviewCount = 0
            )
        )

        // When user does NOT master the ayah in Rafiq:
        repo.setAyahStatus(1, 6, MemorizationStatus.REVIEW)

        val updated = memDao.getStatusForAyah(1, 6)
        assertEquals(MemorizationStatus.REVIEW, updated?.memorizationStatus)
        assertTrue(updated?.memorizationStatus != MemorizationStatus.MEMORIZED)
    }

    @Test
    fun `test computeDailyRoutine returns 4 steps`() = runTest {
        val now = System.currentTimeMillis()
        val routine = SmartCompanionEngine.computeDailyRoutine(
            memorizationDao = memDao,
            dailyTarget = 3,
            currentTimeMillis = now
        )
        assertEquals(4, routine.steps.size)
    }

    @Test
    fun `test computeDailyRoutine period detection morning`() {
        // 7 AM
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 7)
        }
        val period = SmartCompanionEngine.determinePeriod(calendar.timeInMillis)
        assertEquals(com.example.domain.companion.DailyRoutinePeriod.MORNING, period)
    }

    @Test
    fun `test computeDailyRoutine period detection evening`() {
        // 7 PM
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 19)
        }
        val period = SmartCompanionEngine.determinePeriod(calendar.timeInMillis)
        assertEquals(com.example.domain.companion.DailyRoutinePeriod.EVENING, period)
    }

    @Test
    fun `test computeDailyRoutine period detection bedtime`() {
        // 11 PM
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
        }
        val period = SmartCompanionEngine.determinePeriod(calendar.timeInMillis)
        assertEquals(com.example.domain.companion.DailyRoutinePeriod.BEDTIME, period)
    }

    @Test
    fun `test computeDailyRoutine quickStartStep is first uncompleted step`() = runTest {
        val now = System.currentTimeMillis()
        val routine = SmartCompanionEngine.computeDailyRoutine(
            memorizationDao = memDao,
            currentTimeMillis = now
        )
        val quickStart = routine.quickStartStep
        assertNotNull(quickStart)
        // Quick start should not be completed if any step is incomplete
        val hasIncomplete = routine.steps.any { !it.isCompleted }
        if (hasIncomplete) {
            assertTrue(quickStart == null || !quickStart.isCompleted)
        }
    }

    @Test
    fun `test computeDailyRoutine nextActionSuggestion not empty`() = runTest {
        val now = System.currentTimeMillis()
        val routine = SmartCompanionEngine.computeDailyRoutine(
            memorizationDao = memDao,
            currentTimeMillis = now
        )
        assertTrue(routine.nextActionSuggestionFr.isNotBlank())
        assertTrue(routine.nextActionSuggestionAr.isNotBlank())
    }

    @Test
    fun `test computeDailyRoutine completionFraction is zero when no steps done`() = runTest {
        val now = System.currentTimeMillis()
        val routine = SmartCompanionEngine.computeDailyRoutine(
            memorizationDao = memDao, // empty db
            currentTimeMillis = now
        )
        assertEquals(0f, routine.completionFraction, 0.01f)
    }

    @Test
    fun `test computeDailyRoutine with real overdue data sets morningStep`() = runTest {
        val now = System.currentTimeMillis()
        memDao.items.add(
            MemorizationStatusEntity(
                surahNumber = 2,
                ayahNumber = 255,
                status = MemorizationStatus.MEMORIZED.name,
                nextReviewDueEpochMillis = now - 100_000L // overdue
            )
        )
        val routine = SmartCompanionEngine.computeDailyRoutine(
            memorizationDao = memDao,
            currentTimeMillis = now
        )
        val morningStep = routine.steps.first()
        assertEquals(2, morningStep.targetSurah)
        assertEquals(255, morningStep.targetAyah)
    }
}

