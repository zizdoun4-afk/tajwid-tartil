package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemorizationDao {

    @Query("SELECT * FROM memorization_status WHERE surahNumber = :surahNumber")
    fun getStatusForSurahFlow(surahNumber: Int): Flow<List<MemorizationStatusEntity>>

    @Query("SELECT * FROM memorization_status WHERE surahNumber = :surahNumber")
    suspend fun getStatusForSurah(surahNumber: Int): List<MemorizationStatusEntity>

    @Query("SELECT * FROM memorization_status WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getStatusForAyah(surahNumber: Int, ayahNumber: Int): MemorizationStatusEntity?

    @Query("SELECT * FROM memorization_status WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber LIMIT 1")
    fun getStatusForAyahFlow(surahNumber: Int, ayahNumber: Int): Flow<MemorizationStatusEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(entity: MemorizationStatusEntity): Long

    @Query("SELECT * FROM memorization_status WHERE nextReviewDueEpochMillis IS NOT NULL AND nextReviewDueEpochMillis <= :nowMillis ORDER BY nextReviewDueEpochMillis ASC")
    fun getDueForReview(nowMillis: Long): Flow<List<MemorizationStatusEntity>>

    @Query("SELECT * FROM memorization_status WHERE status = 'REVIEW' ORDER BY surahNumber ASC, ayahNumber ASC")
    fun getInReviewStatus(): Flow<List<MemorizationStatusEntity>>

    @Query("SELECT * FROM memorization_status WHERE status = 'LEARNING' ORDER BY surahNumber ASC, ayahNumber ASC")
    fun getInLearningStatus(): Flow<List<MemorizationStatusEntity>>

    @Query("SELECT * FROM memorization_status WHERE status = 'MEMORIZED' ORDER BY surahNumber ASC, ayahNumber ASC")
    fun getMemorizedStatus(): Flow<List<MemorizationStatusEntity>>

    @Query("SELECT COUNT(*) FROM memorization_status WHERE lastReviewedAtEpochMillis >= :sinceMillis")
    fun getReviewedSinceCountFlow(sinceMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM memorization_status WHERE lastReviewedAtEpochMillis >= :sinceMillis")
    suspend fun getReviewedSinceCount(sinceMillis: Long): Int

    @Query("SELECT * FROM memorization_status WHERE lastReviewedAtEpochMillis >= :sinceMillis")
    suspend fun getWeeklyStats(sinceMillis: Long): List<MemorizationStatusEntity>

    @Query("SELECT * FROM memorization_status")
    fun getAllStatusesFlow(): Flow<List<MemorizationStatusEntity>>

    @Query("SELECT * FROM memorization_status WHERE failedTests > 0 AND failedTests >= successfulTests ORDER BY failedTests DESC")
    fun getWeakVersesFlow(): Flow<List<MemorizationStatusEntity>>

    @Query("SELECT * FROM memorization_status WHERE failedTests > 0 AND failedTests >= successfulTests ORDER BY failedTests DESC")
    suspend fun getWeakVerses(): List<MemorizationStatusEntity>

    @Query("SELECT * FROM memorization_status WHERE failedTests >= :minFails ORDER BY failedTests DESC")
    fun getFrequentlyFailedVerses(minFails: Int = 2): Flow<List<MemorizationStatusEntity>>

    @Query("SELECT * FROM memorization_status WHERE nextReviewDueEpochMillis > :windowStart AND nextReviewDueEpochMillis <= :windowEnd ORDER BY nextReviewDueEpochMillis ASC")
    fun getVersesApproachingDue(windowStart: Long, windowEnd: Long): Flow<List<MemorizationStatusEntity>>
}
