package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tajwid_progress")
data class TajwidProgressEntity(
    @PrimaryKey
    val lessonId: String,
    val completedExercisesCount: Int = 0,
    val isCompleted: Boolean = false,
    val lastCompletedAtEpochMillis: Long = System.currentTimeMillis()
)
