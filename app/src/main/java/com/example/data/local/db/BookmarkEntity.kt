package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val ayahText: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
