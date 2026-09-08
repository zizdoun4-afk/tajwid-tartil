package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY recordedAtEpochMillis DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("UPDATE recordings SET reciterName = :reciterName, customLabel = :customLabel WHERE id = :id")
    suspend fun updateRecordingMeta(id: Long, reciterName: String, customLabel: String?)

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE surahNumber = :surahNumber ORDER BY recordedAtEpochMillis DESC")
    fun getRecordingsBySurah(surahNumber: Int): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber ORDER BY recordedAtEpochMillis DESC LIMIT 1")
    suspend fun getRecordingForAyah(surahNumber: Int, ayahNumber: Int): RecordingEntity?

    @Query("SELECT DISTINCT ayahNumber FROM recordings WHERE surahNumber = :surahNumber")
    fun getRecordedAyahNumbers(surahNumber: Int): Flow<List<Int>>
}
