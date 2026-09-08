package com.example.domain.model

import com.example.audio.SessionMarker

data class UserRecording(
    val id: Long = 0,
    val reciterName: String,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val filePath: String,
    val durationMs: Long,
    val recordedAtEpochMillis: Long,
    val customLabel: String? = null,
    val markers: List<SessionMarker>? = null,
    val isBest: Boolean = false
)
