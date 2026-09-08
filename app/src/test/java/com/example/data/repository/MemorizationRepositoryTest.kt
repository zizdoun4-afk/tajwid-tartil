package com.example.data.repository

import com.example.data.local.db.MemorizationDao
import com.example.data.local.db.MemorizationStatusEntity
import com.example.domain.model.MemorizationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeMemorizationDao : MemorizationDao {
    val store = mutableMapOf<String, MemorizationStatusEntity>()

    override fun getStatusForSurahFlow(surahNumber: Int): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.filter { it.surahNumber == surahNumber })
    }

    override suspend fun getStatusForSurah(surahNumber: Int): List<MemorizationStatusEntity> {
        return store.values.filter { it.surahNumber == surahNumber }
    }

    override fun getStatusForAyahFlow(surahNumber: Int, ayahNumber: Int): Flow<MemorizationStatusEntity?> {
        return flowOf(store["$surahNumber:$ayahNumber"])
    }

    override suspend fun getStatusForAyah(surahNumber: Int, ayahNumber: Int): MemorizationStatusEntity? {
        return store["$surahNumber:$ayahNumber"]
    }

    override suspend fun upsertStatus(entity: MemorizationStatusEntity): Long {
        store["${entity.surahNumber}:${entity.ayahNumber}"] = entity
        return entity.id
    }

    override fun getDueForReview(nowMillis: Long): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.filter { (it.nextReviewDueEpochMillis ?: Long.MAX_VALUE) <= nowMillis })
    }

    override fun getInReviewStatus(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.filter { it.status == MemorizationStatus.REVIEW.name })
    }

    override fun getInLearningStatus(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.filter { it.status == MemorizationStatus.LEARNING.name })
    }

    override fun getMemorizedStatus(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.filter { it.status == MemorizationStatus.MEMORIZED.name })
    }

    override fun getReviewedSinceCountFlow(sinceMillis: Long): Flow<Int> {
        return flowOf(store.values.count { (it.lastReviewedAtEpochMillis ?: 0L) >= sinceMillis })
    }

    override suspend fun getReviewedSinceCount(sinceMillis: Long): Int {
        return store.values.count { (it.lastReviewedAtEpochMillis ?: 0L) >= sinceMillis }
    }

    override fun getAllStatusesFlow(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.toList())
    }

    override fun getWeakVersesFlow(): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.filter { it.failedTests > 0 && it.failedTests >= it.successfulTests })
    }

    override suspend fun getWeakVerses(): List<MemorizationStatusEntity> {
        return store.values.filter { it.failedTests > 0 && it.failedTests >= it.successfulTests }
    }

    override fun getFrequentlyFailedVerses(minFails: Int): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.filter { it.failedTests >= minFails })
    }

    override fun getVersesApproachingDue(windowStart: Long, windowEnd: Long): Flow<List<MemorizationStatusEntity>> {
        return flowOf(store.values.filter {
            val due = it.nextReviewDueEpochMillis ?: Long.MAX_VALUE
            due > windowStart && due <= windowEnd
        })
    }

    override suspend fun getWeeklyStats(sinceMillis: Long): List<MemorizationStatusEntity> {
        return store.values.filter { (it.lastReviewedAtEpochMillis ?: 0L) >= sinceMillis }
    }
}

class MemorizationRepositoryTest {

    private lateinit var fakeDao: FakeMemorizationDao
    private lateinit var repository: MemorizationRepository

    @Before
    fun setUp() {
        fakeDao = FakeMemorizationDao()
        repository = MemorizationRepository(fakeDao)
    }

    @Test
    fun `test spaced repetition intervals for reviews 1 to 4`() = runTest {
        val oneDayMs = 24L * 60 * 60 * 1000

        // 1st review -> +1 day
        val before1 = System.currentTimeMillis()
        val review1 = repository.markReviewed(1, 1)
        val after1 = System.currentTimeMillis()
        assertEquals(1, review1.reviewCount)
        assertEquals(MemorizationStatus.REVIEW.name, review1.status)
        assertTrue(review1.nextReviewDueEpochMillis!! >= before1 + 1 * oneDayMs)
        assertTrue(review1.nextReviewDueEpochMillis!! <= after1 + 1 * oneDayMs)

        // 2nd review -> +3 days
        val before2 = System.currentTimeMillis()
        val review2 = repository.markReviewed(1, 1)
        val after2 = System.currentTimeMillis()
        assertEquals(2, review2.reviewCount)
        assertEquals(MemorizationStatus.REVIEW.name, review2.status)
        assertTrue(review2.nextReviewDueEpochMillis!! >= before2 + 3 * oneDayMs)
        assertTrue(review2.nextReviewDueEpochMillis!! <= after2 + 3 * oneDayMs)

        // 3rd review -> +7 days
        val before3 = System.currentTimeMillis()
        val review3 = repository.markReviewed(1, 1)
        val after3 = System.currentTimeMillis()
        assertEquals(3, review3.reviewCount)
        assertEquals(MemorizationStatus.REVIEW.name, review3.status)
        assertTrue(review3.nextReviewDueEpochMillis!! >= before3 + 7 * oneDayMs)
        assertTrue(review3.nextReviewDueEpochMillis!! <= after3 + 7 * oneDayMs)

        // 4th review -> +14 days
        val before4 = System.currentTimeMillis()
        val review4 = repository.markReviewed(1, 1)
        val after4 = System.currentTimeMillis()
        assertEquals(4, review4.reviewCount)
        assertTrue(review4.nextReviewDueEpochMillis!! >= before4 + 14 * oneDayMs)
        assertTrue(review4.nextReviewDueEpochMillis!! <= after4 + 14 * oneDayMs)
    }

    @Test
    fun `review count does NOT auto-promote to MEMORIZED`() = runTest {
        // Perform 5 reviews
        for (i in 1..5) {
            repository.markReviewed(2, 255)
        }

        val entity = repository.getStatusForAyah(2, 255)
        assertEquals(5, entity?.reviewCount)
        // Must remain in REVIEW, never auto-promoted to MEMORIZED
        assertNotEquals(MemorizationStatus.MEMORIZED.name, entity?.status)
        assertEquals(MemorizationStatus.REVIEW.name, entity?.status)
    }

    @Test
    fun `markMemorized explicitly validates memorization`() = runTest {
        repository.setAyahStatus(1, 1, MemorizationStatus.LEARNING)
        val memorized = repository.markMemorized(1, 1)

        assertEquals(MemorizationStatus.MEMORIZED.name, memorized.status)
        assertEquals(MemorizationStatus.MEMORIZED, memorized.memorizationStatus)

        // Subsequent review maintains MEMORIZED status
        val afterReview = repository.markReviewed(1, 1)
        assertEquals(MemorizationStatus.MEMORIZED.name, afterReview.status)
    }

    @Test
    fun `recordTestResult failure contracts review interval and demotes status`() = runTest {
        val memorized = repository.markMemorized(1, 1)
        assertEquals(MemorizationStatus.MEMORIZED.name, memorized.status)

        // Test failed
        val failed = repository.recordTestResult(1, 1, succeeded = false)
        assertEquals(MemorizationStatus.REVIEW.name, failed.status)
        assertEquals(1, failed.failedTests)
        assertTrue(failed.isWeak)

        // Due tomorrow (within 24 hours + small margin)
        val oneDayMs = 24L * 60 * 60 * 1000L
        val diff = failed.nextReviewDueEpochMillis!! - System.currentTimeMillis()
        assertTrue(diff in (oneDayMs - 2000L)..(oneDayMs + 2000L))
    }

    @Test
    fun `recordTestResult consecutive successes expand intervals and promote to MEMORIZED`() = runTest {
        // 3 consecutive successes
        val t1 = repository.recordTestResult(1, 2, succeeded = true)
        assertEquals(1, t1.successfulTests)

        val t2 = repository.recordTestResult(1, 2, succeeded = true)
        assertEquals(2, t2.successfulTests)

        val t3 = repository.recordTestResult(1, 2, succeeded = true)
        assertEquals(3, t3.successfulTests)
        assertEquals(0, t3.failedTests)
        assertTrue(t3.isStrong)
        assertEquals(MemorizationStatus.MEMORIZED.name, t3.status)
    }

    @Test
    fun `test daily reviewed count accurately reflects stored timestamps`() = runTest {
        val now = System.currentTimeMillis()
        val startOfToday = now - 1000L // 1 second ago

        // Initially zero
        assertEquals(0, repository.getReviewedSinceCount(startOfToday))

        // Mark 2 ayahs reviewed
        repository.markReviewed(1, 1)
        repository.markReviewed(1, 2)

        assertEquals(2, repository.getReviewedSinceCount(startOfToday))

        // An older review from 2 days ago
        val twoDaysAgo = now - (2L * 24 * 60 * 60 * 1000)
        fakeDao.store["1:3"] = MemorizationStatusEntity(
            surahNumber = 1,
            ayahNumber = 3,
            status = MemorizationStatus.REVIEW.name,
            lastReviewedAtEpochMillis = twoDaysAgo
        )

        // Today's count is still 2
        assertEquals(2, repository.getReviewedSinceCount(startOfToday))
        // Since 3 days ago is 3
        assertEquals(3, repository.getReviewedSinceCount(twoDaysAgo - 1000L))
    }
}
