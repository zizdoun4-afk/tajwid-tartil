package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TajwidProgressDao {

    @Query("SELECT * FROM tajwid_progress")
    fun getAllProgressFlow(): Flow<List<TajwidProgressEntity>>

    @Query("SELECT * FROM tajwid_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getProgressForLesson(lessonId: String): TajwidProgressEntity?

    @Query("SELECT * FROM tajwid_progress WHERE lessonId = :lessonId LIMIT 1")
    fun getProgressForLessonFlow(lessonId: String): Flow<TajwidProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: TajwidProgressEntity): Long

    @Query("SELECT COUNT(*) FROM tajwid_progress WHERE isCompleted = 1")
    fun getCompletedLessonsCountFlow(): Flow<Int>
}
