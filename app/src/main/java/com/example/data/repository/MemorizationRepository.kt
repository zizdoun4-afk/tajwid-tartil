package com.example.data.repository

import com.example.data.local.db.MemorizationDao
import com.example.data.local.db.MemorizationStatusEntity
import com.example.domain.model.MemorizationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class SurahMemorizationProgress(
    val surahNumber: Int,
    val memorizedCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
    val totalAyahs: Int
) {
    val percentage: Float
        get() = if (totalAyahs > 0) (memorizedCount.toFloat() / totalAyahs) * 100f else 0f

    val remainingAyahs: Int
        get() = (totalAyahs - memorizedCount).coerceAtLeast(0)

    val learningPercentage: Float
        get() = if (totalAyahs > 0) (learningCount.toFloat() / totalAyahs) * 100f else 0f

    val reviewPercentage: Float
        get() = if (totalAyahs > 0) (reviewCount.toFloat() / totalAyahs) * 100f else 0f
}

class MemorizationRepository(
    private val memorizationDao: MemorizationDao
) {

    fun getStatusForSurahFlow(surahNumber: Int): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getStatusForSurahFlow(surahNumber)
    }

    fun getStatusForAyahFlow(surahNumber: Int, ayahNumber: Int): Flow<MemorizationStatusEntity?> {
        return memorizationDao.getStatusForAyahFlow(surahNumber, ayahNumber)
    }

    suspend fun getStatusForAyah(surahNumber: Int, ayahNumber: Int): MemorizationStatusEntity? = withContext(Dispatchers.IO) {
        memorizationDao.getStatusForAyah(surahNumber, ayahNumber)
    }

    suspend fun setAyahStatus(
        surahNumber: Int,
        ayahNumber: Int,
        status: MemorizationStatus
    ) = withContext(Dispatchers.IO) {
        val existing = memorizationDao.getStatusForAyah(surahNumber, ayahNumber)
        val entity = existing?.copy(status = status.name) ?: MemorizationStatusEntity(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            status = status.name,
            lastReviewedAtEpochMillis = System.currentTimeMillis(),
            reviewCount = 0
        )
        memorizationDao.upsertStatus(entity)
    }

    suspend fun markReviewed(
        surahNumber: Int,
        ayahNumber: Int
    ): MemorizationStatusEntity = withContext(Dispatchers.IO) {
        val existing = memorizationDao.getStatusForAyah(surahNumber, ayahNumber)
        val now = System.currentTimeMillis()
        val nextCount = (existing?.reviewCount ?: 0) + 1

        // Spaced repetition schedule based on review count
        val oneDayMs = 24L * 60 * 60 * 1000
        val intervalDays = when (nextCount) {
            1 -> 1L
            2 -> 3L
            3 -> 7L
            else -> 14L
        }
        val nextDue = now + (intervalDays * oneDayMs)

        val currentStatus = existing?.memorizationStatus
        val newStatus = if (currentStatus == MemorizationStatus.MEMORIZED) {
            MemorizationStatus.MEMORIZED
        } else {
            MemorizationStatus.REVIEW
        }

        val entity = (existing ?: MemorizationStatusEntity(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            status = newStatus.name
        )).copy(
            status = newStatus.name,
            lastReviewedAtEpochMillis = now,
            nextReviewDueEpochMillis = nextDue,
            reviewCount = nextCount
        )

        memorizationDao.upsertStatus(entity)
        entity
    }

    suspend fun markMemorized(
        surahNumber: Int,
        ayahNumber: Int
    ): MemorizationStatusEntity = withContext(Dispatchers.IO) {
        val existing = memorizationDao.getStatusForAyah(surahNumber, ayahNumber)
        val now = System.currentTimeMillis()
        val nextCount = (existing?.reviewCount ?: 0) + 1
        val oneDayMs = 24L * 60 * 60 * 1000
        val nextDue = now + (14L * oneDayMs) // Maintenance review due in 14 days

        val entity = (existing ?: MemorizationStatusEntity(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            status = MemorizationStatus.MEMORIZED.name
        )).copy(
            status = MemorizationStatus.MEMORIZED.name,
            lastReviewedAtEpochMillis = now,
            nextReviewDueEpochMillis = nextDue,
            reviewCount = nextCount,
            successfulTests = (existing?.successfulTests ?: 0) + 1
        )

        memorizationDao.upsertStatus(entity)
        entity
    }

    /**
     * Records a test attempt (blind test or quiz) using adaptive Spaced Repetition.
     * - Success expands future review interval based on history and marks strong.
     * - Failure contracts future interval to 1 day, marks weak, and demotes from MEMORIZED to REVIEW.
     */
    suspend fun recordTestResult(
        surahNumber: Int,
        ayahNumber: Int,
        succeeded: Boolean
    ): MemorizationStatusEntity = withContext(Dispatchers.IO) {
        val existing = memorizationDao.getStatusForAyah(surahNumber, ayahNumber)
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000L

        val prevSuccess = existing?.successfulTests ?: 0
        val prevFails = existing?.failedTests ?: 0
        val prevAttempts = existing?.trainingAttempts ?: 0
        val prevReviewCount = existing?.reviewCount ?: 0

        val newSuccess = if (succeeded) prevSuccess + 1 else prevSuccess
        val newFails = if (!succeeded) prevFails + 1 else prevFails
        val newAttempts = prevAttempts + 1
        val newReviewCount = prevReviewCount + 1

        val intervalDays: Long
        val newStatus: MemorizationStatus

        if (succeeded) {
            // Adaptive schedule: consecutive success expands review intervals
            intervalDays = when {
                newFails > 0 -> 2L // Still recovering from past mistakes
                newSuccess == 1 -> 1L
                newSuccess == 2 -> 3L
                newSuccess == 3 -> 7L
                newSuccess == 4 -> 14L
                else -> 30L
            }

            newStatus = MemorizationStatus.MEMORIZED
        } else {
            // Failure: reset interval to 1 day for reinforcement
            intervalDays = 1L
            // If failed, verse is weak and needs REVIEW, cannot remain MEMORIZED
            newStatus = MemorizationStatus.REVIEW
        }

        val nextDue = now + (intervalDays * oneDayMs)

        val entity = (existing ?: MemorizationStatusEntity(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            status = newStatus.name
        )).copy(
            status = newStatus.name,
            lastReviewedAtEpochMillis = now,
            nextReviewDueEpochMillis = nextDue,
            reviewCount = newReviewCount,
            successfulTests = newSuccess,
            failedTests = newFails,
            trainingAttempts = newAttempts
        )

        memorizationDao.upsertStatus(entity)
        entity
    }

    /**
     * Records a training attempt (listen, accompany, record) without affecting test pass/fail.
     */
    suspend fun recordTrainingAttempt(
        surahNumber: Int,
        ayahNumber: Int
    ): MemorizationStatusEntity = withContext(Dispatchers.IO) {
        val existing = memorizationDao.getStatusForAyah(surahNumber, ayahNumber)
        val attempts = (existing?.trainingAttempts ?: 0) + 1
        val entity = (existing ?: MemorizationStatusEntity(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            status = MemorizationStatus.LEARNING.name
        )).copy(
            trainingAttempts = attempts,
            lastReviewedAtEpochMillis = System.currentTimeMillis()
        )
        memorizationDao.upsertStatus(entity)
        entity
    }

    fun getDueForReview(nowMillis: Long = System.currentTimeMillis()): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getDueForReview(nowMillis)
    }

    fun getInReviewStatus(): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getInReviewStatus()
    }

    fun getInLearningStatus(): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getInLearningStatus()
    }

    fun getMemorizedStatus(): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getMemorizedStatus()
    }

    fun getWeakVersesFlow(): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getWeakVersesFlow()
    }

    suspend fun getWeakVerses(): List<MemorizationStatusEntity> = withContext(Dispatchers.IO) {
        memorizationDao.getWeakVerses()
    }

    fun getFrequentlyFailedVerses(minFails: Int = 2): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getFrequentlyFailedVerses(minFails)
    }

    fun getVersesApproachingDue(windowHours: Long = 24): Flow<List<MemorizationStatusEntity>> {
        val now = System.currentTimeMillis()
        val windowEnd = now + (windowHours * 60 * 60 * 1000)
        return memorizationDao.getVersesApproachingDue(now, windowEnd)
    }

    fun getReviewedSinceCountFlow(sinceMillis: Long): Flow<Int> {
        return memorizationDao.getReviewedSinceCountFlow(sinceMillis)
    }

    suspend fun getReviewedSinceCount(sinceMillis: Long): Int = withContext(Dispatchers.IO) {
        memorizationDao.getReviewedSinceCount(sinceMillis)
    }

    fun getAllStatusesFlow(): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getAllStatusesFlow()
    }

    suspend fun getWeeklyStats(sinceMillis: Long = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000): List<MemorizationStatusEntity> = withContext(Dispatchers.IO) {
        memorizationDao.getWeeklyStats(sinceMillis)
    }
}
