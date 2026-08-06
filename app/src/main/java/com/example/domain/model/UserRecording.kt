package com.example.domain.model

data class UserRecording(
    val id: Long = 0,
    val reciterName: String,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val filePath: String,
    val durationMs: Long,
    val recordedAtEpochMillis: Long,
    val customLabel: String? = null
)
