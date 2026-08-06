package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranCacheDao {
    @Query("SELECT * FROM cached_surahs ORDER BY number ASC")
    fun getAllCachedSurahs(): Flow<List<CachedSurahEntity>>

    @Query("SELECT * FROM cached_surahs WHERE number = :surahNumber")
    suspend fun getCachedSurah(surahNumber: Int): CachedSurahEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<CachedSurahEntity>)

    @Query("SELECT * FROM cached_ayahs WHERE surahNumber = :surahNumber ORDER BY numberInSurah ASC")
    suspend fun getAyahsForSurah(surahNumber: Int): List<CachedAyahEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<CachedAyahEntity>)

    @Query("DELETE FROM cached_ayahs WHERE surahNumber = :surahNumber")
    suspend fun deleteAyahsForSurah(surahNumber: Int)
}
