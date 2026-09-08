package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.db.RecordingDao
import com.example.data.local.db.RecordingEntity
import com.example.domain.model.UserRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingRepository(
    private val context: Context,
    private val recordingDao: RecordingDao
) {

    val allRecordings: Flow<List<UserRecording>> = recordingDao.getAllRecordings().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun saveRecording(
        tempFile: File,
        reciterName: String,
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        durationMs: Long,
        customLabel: String? = null,
        isBest: Boolean = false
    ): UserRecording = withContext(Dispatchers.IO) {
        val dateFolderStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val recordingsDir = File(context.filesDir, "recordings/$dateFolderStr")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val destFile = File(recordingsDir, "${surahNumber}_${ayahNumber}_$timestamp.m4a")
        
        // Copy temp file to permanent location
        tempFile.copyTo(destFile, overwrite = true)
        if (tempFile.exists()) {
            tempFile.delete()
        }

        if (isBest) {
            recordingDao.clearBestForAyah(surahNumber, ayahNumber)
        }

        val entity = RecordingEntity(
            reciterName = reciterName,
            surahNumber = surahNumber,
            surahName = surahName,
            ayahNumber = ayahNumber,
            filePath = destFile.absolutePath,
            durationMs = durationMs,
            recordedAtEpochMillis = timestamp,
            customLabel = customLabel,
            isBest = isBest
        )

        val newId = recordingDao.insertRecording(entity)
        entity.copy(id = newId).toDomain()
    }

    suspend fun toggleBestRecording(recording: UserRecording): Boolean = withContext(Dispatchers.IO) {
        val newBest = !recording.isBest
        if (newBest) {
            recordingDao.clearBestForAyah(recording.surahNumber, recording.ayahNumber)
        }
        recordingDao.updateIsBest(recording.id, newBest)
        newBest
    }

    suspend fun setBestRecording(id: Long, surahNumber: Int, ayahNumber: Int) = withContext(Dispatchers.IO) {
        recordingDao.clearBestForAyah(surahNumber, ayahNumber)
        recordingDao.updateIsBest(id, true)
    }

    suspend fun getBestOrLatestRecording(surahNumber: Int, ayahNumber: Int): UserRecording? = withContext(Dispatchers.IO) {
        recordingDao.getRecordingForAyah(surahNumber, ayahNumber)?.toDomain()
    }

    fun getRecordingsForAyah(surahNumber: Int, ayahNumber: Int): Flow<List<UserRecording>> {
        return recordingDao.getRecordingsForAyahFlow(surahNumber, ayahNumber).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun updateRecordingMeta(id: Long, reciterName: String, customLabel: String?) = withContext(Dispatchers.IO) {
        recordingDao.updateRecordingMeta(id, reciterName, customLabel)
    }

    suspend fun deleteRecording(recording: UserRecording) = withContext(Dispatchers.IO) {
        // Delete audio file
        val file = File(recording.filePath)
        if (file.exists()) {
            file.delete()
        }
        // Delete Room DB record
        recordingDao.deleteRecordingById(recording.id)
    }

    suspend fun deleteRecordings(recordings: List<UserRecording>) = withContext(Dispatchers.IO) {
        recordings.forEach { recording ->
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
            recordingDao.deleteRecordingById(recording.id)
        }
    }

    fun shareRecording(recording: UserRecording) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(recording.recordedAtEpochMillis))

        val notePart = if (!recording.customLabel.isNullOrBlank()) "\n• ملاحظة: ${recording.customLabel}" else ""
        val shareBody = """
            |تلاوة للمراجعة والتقويم (رفيق القرآن):
            |• السورة: ${recording.surahName} (${recording.surahNumber})
            |• الآية: ${recording.ayahNumber}
            |• القارئ: ${recording.reciterName}
            |• التاريخ: $dateStr$notePart
        """.trimMargin()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/m4a"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, shareBody)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "تلاوة ${recording.surahName} (آية ${recording.ayahNumber}) - ${recording.reciterName}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "مشاركة التلاوة مع المعلم / Partager")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    suspend fun copyToDownloads(recording: UserRecording): Boolean = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(recording.filePath)
            if (!srcFile.exists()) return@withContext false

            val fileName = "Tajwid_${recording.surahNumber}_${recording.ayahNumber}_${recording.reciterName.replace(" ", "_")}.m4a"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext false

                resolver.openOutputStream(uri)?.use { out ->
                    srcFile.inputStream().use { input -> input.copyTo(out) }
                } ?: return@withContext false

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                true
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val destFile = File(downloadsDir, fileName)
                srcFile.copyTo(destFile, overwrite = true)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
