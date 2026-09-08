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
}
