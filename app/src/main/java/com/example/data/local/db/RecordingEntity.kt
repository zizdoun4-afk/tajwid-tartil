package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.audio.SessionMarker
import com.example.domain.model.UserRecording

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reciterName: String,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val filePath: String,
    val durationMs: Long,
    val recordedAtEpochMillis: Long,
    val customLabel: String? = null,
    val markers: List<SessionMarker>? = null
) {
    fun toDomain(): UserRecording = UserRecording(
        id = id,
        reciterName = reciterName,
        surahNumber = surahNumber,
        surahName = surahName,
        ayahNumber = ayahNumber,
        filePath = filePath,
        durationMs = durationMs,
        recordedAtEpochMillis = recordedAtEpochMillis,
        customLabel = customLabel,
        markers = markers
    )

    companion object {
        fun fromDomain(domain: UserRecording): RecordingEntity = RecordingEntity(
            id = domain.id,
            reciterName = domain.reciterName,
            surahNumber = domain.surahNumber,
            surahName = domain.surahName,
            ayahNumber = domain.ayahNumber,
            filePath = domain.filePath,
            durationMs = domain.durationMs,
            recordedAtEpochMillis = domain.recordedAtEpochMillis,
            customLabel = domain.customLabel,
            markers = domain.markers
        )
    }
}
