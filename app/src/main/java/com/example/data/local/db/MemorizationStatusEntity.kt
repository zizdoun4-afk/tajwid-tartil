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
    val reviewCount: Int = 0,
    val successfulTests: Int = 0,
    val failedTests: Int = 0,
    val trainingAttempts: Int = 0
) {
    val memorizationStatus: MemorizationStatus
        get() = MemorizationStatus.fromString(status)

    val isWeak: Boolean
        get() = failedTests > 0 && failedTests >= successfulTests

    val isStrong: Boolean
        get() = successfulTests >= 3 && failedTests == 0

    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return nextReviewDueEpochMillis != null && nextReviewDueEpochMillis <= nowMillis
    }
}
