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

        // Never automatically promote to MEMORIZED solely based on review count.
        // Status remains REVIEW (or retains MEMORIZED if it was already validated).
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
            reviewCount = nextCount
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

    fun getAllStatusesFlow(): Flow<List<MemorizationStatusEntity>> {
        return memorizationDao.getAllStatusesFlow()
    }

    suspend fun getWeeklyStats(sinceMillis: Long = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000): List<MemorizationStatusEntity> = withContext(Dispatchers.IO) {
        memorizationDao.getWeeklyStats(sinceMillis)
    }
}
