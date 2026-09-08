package com.example.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.MemorizationStatus

@Entity(
    tableName = "memorization_status",
    indices = [Index(value = ["surahNumber", "ayahNumber"], unique = true)]
)
data class MemorizationStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val status: String = MemorizationStatus.NEW.name,
    val lastReviewedAtEpochMillis: Long? = null,
    val nextReviewDueEpochMillis: Long? = null,
    val reviewCount: Int = 0
) {
    val memorizationStatus: MemorizationStatus
        get() = MemorizationStatus.fromString(status)
}
