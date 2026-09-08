package com.example.data.repository

import android.content.Context
import com.example.audio.SessionMarker
import com.example.data.local.db.RecordingDao
import com.example.data.local.db.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

enum class RecitationType { AYAH, SESSION }

data class RecitationMeta(
    val id: String,
    val v: Int = 1,
    val type: RecitationType,
    val riwaya: String = "hafs",
    val sura: Int,
    val ayaFrom: Int,
    val ayaTo: Int,
    val durationMs: Long,
    val createdAt: String,
    val markers: List<SessionMarker>? = null,
    val noteText: String? = null,
    val hasVoiceNote: Boolean = false,
    val reciterName: String = "Mon Enregistrement",
    val filePath: String = "",
    val isBest: Boolean = false
)

/**
 * RecitationsStore is the File/Audio Storage Manager.
 * Room (RecordingEntity) is the single source of truth for metadata.
 * Filesystem is the source of truth for the audio binary (.m4a).
 * Existing index.json entries are migrated to Room once on launch.
 */
class RecitationsStore(
    private val context: Context,
    private val recordingDao: RecordingDao
) {
    private val baseDir = File(context.filesDir, "recitations")
    private val audioDir = File(baseDir, "audio")
    private val notesDir = File(baseDir, "notes")
    private val indexFile = File(baseDir, "index.json")

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _recitationsFlow = MutableStateFlow<List<RecitationMeta>>(emptyList())
    val recitationsFlow: StateFlow<List<RecitationMeta>> = _recitationsFlow.asStateFlow()

    init {
        ensureDirs()
        migrateIndexJsonToRoomIfNeeded()
        observeRoomRecordings()
    }

    private fun ensureDirs() {
        if (!baseDir.exists()) baseDir.mkdirs()
        if (!audioDir.exists()) audioDir.mkdirs()
        if (!notesDir.exists()) notesDir.mkdirs()
    }

    private fun observeRoomRecordings() {
        recordingDao.getAllRecordings().onEach { entities ->
            _recitationsFlow.value = entities.map { entity ->
                val isSession = !entity.markers.isNullOrEmpty() || entity.customLabel?.startsWith("Session") == true
                RecitationMeta(
                    id = entity.id.toString(),
                    type = if (isSession) RecitationType.SESSION else RecitationType.AYAH,
                    sura = entity.surahNumber,
                    ayaFrom = entity.ayahNumber,
                    ayaTo = entity.markers?.lastOrNull()?.aya ?: entity.ayahNumber,
                    durationMs = entity.durationMs,
                    createdAt = formatEpochToIso(entity.recordedAtEpochMillis),
                    markers = entity.markers,
                    noteText = entity.customLabel,
                    reciterName = entity.reciterName,
                    filePath = entity.filePath,
                    isBest = entity.isBest
                )
            }
        }.launchIn(scope)
    }

    /**
     * One-time migration of legacy index.json to Room database
     */
    private fun migrateIndexJsonToRoomIfNeeded() {
        if (!indexFile.exists()) return

        scope.launch {
            try {
                val jsonStr = indexFile.readText()
                val array = JSONArray(jsonStr)

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val audioFile = File(audioDir, "$id.m4a")

                    if (audioFile.exists()) {
                        var markersList: List<SessionMarker>? = null
                        if (obj.has("markers") && !obj.isNull("markers")) {
                            val mArr = obj.getJSONArray("markers")
                            val mList = mutableListOf<SessionMarker>()
                            for (j in 0 until mArr.length()) {
                                val mObj = mArr.getJSONObject(j)
                                mList.add(SessionMarker(mObj.getInt("aya"), mObj.getLong("tMs")))
                            }
                            if (mList.isNotEmpty()) markersList = mList
                        }

                        val sura = obj.getInt("sura")
                        val ayaFrom = obj.getInt("ayaFrom")
                        val ayaTo = obj.optInt("ayaTo", ayaFrom)
                        val durationMs = obj.getLong("durationMs")
                        val reciterName = obj.optString("reciterName", "Mon Enregistrement")
                        val noteText = if (obj.has("noteText") && !obj.isNull("noteText")) obj.getString("noteText") else null
                        val isSession = markersList != null || ayaFrom != ayaTo

                        val entity = RecordingEntity(
                            reciterName = reciterName,
                            surahNumber = sura,
                            surahName = "Sourate $sura",
                            ayahNumber = ayaFrom,
                            filePath = audioFile.absolutePath,
                            durationMs = durationMs,
                            recordedAtEpochMillis = System.currentTimeMillis(),
                            customLabel = noteText ?: if (isSession) "Session V.$ayaFrom → V.$ayaTo" else "Take Ayah $ayaFrom",
                            markers = markersList
                        )
                        recordingDao.insertRecording(entity)
                    }
                }

                // Rename migrated index file so it is never processed again
                val migratedFile = File(baseDir, "index.json.migrated")
                indexFile.renameTo(migratedFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveAyahRecording(
        tempPath: String,
        sura: Int,
        surahName: String = "",
        aya: Int,
        riwaya: String = "hafs",
        durationMs: Long,
        reciterName: String = "Mon Enregistrement",
        isBest: Boolean = false
    ): RecitationMeta = withContext(Dispatchers.IO) {
        ensureDirs()
        val tempFile = File(tempPath)
        val fileId = UUID.randomUUID().toString()
        val destFile = File(audioDir, "$fileId.m4a")

        if (tempFile.exists()) {
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
        }

        if (isBest) {
            recordingDao.clearBestForAyah(sura, aya)
        }

        val entity = RecordingEntity(
            reciterName = reciterName,
            surahNumber = sura,
            surahName = if (surahName.isNotBlank()) surahName else "Sourate $sura",
            ayahNumber = aya,
            filePath = destFile.absolutePath,
            durationMs = durationMs,
            recordedAtEpochMillis = System.currentTimeMillis(),
            customLabel = "Take Ayah $aya",
            markers = null,
            isBest = isBest
        )

        val insertedId = recordingDao.insertRecording(entity)

        RecitationMeta(
            id = insertedId.toString(),
            type = RecitationType.AYAH,
            riwaya = riwaya,
            sura = sura,
            ayaFrom = aya,
            ayaTo = aya,
            durationMs = durationMs,
            createdAt = getCurrentIsoDate(),
            reciterName = reciterName,
            filePath = destFile.absolutePath,
            isBest = isBest
        )
    }

    suspend fun saveSessionRecording(
        tempPath: String,
        sura: Int,
        surahName: String = "",
        ayaFrom: Int,
        ayaTo: Int,
        riwaya: String = "hafs",
        durationMs: Long,
        markers: List<SessionMarker>,
        reciterName: String = "Session Récitation"
    ): RecitationMeta = withContext(Dispatchers.IO) {
        ensureDirs()
        val tempFile = File(tempPath)
        val fileId = UUID.randomUUID().toString()
        val destFile = File(audioDir, "$fileId.m4a")

        if (tempFile.exists()) {
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
        }

        val storedMarkers = if (markers.size > 1) markers else null

        val entity = RecordingEntity(
            reciterName = reciterName,
            surahNumber = sura,
            surahName = if (surahName.isNotBlank()) surahName else "Sourate $sura",
            ayahNumber = ayaFrom,
            filePath = destFile.absolutePath,
            durationMs = durationMs,
            recordedAtEpochMillis = System.currentTimeMillis(),
            customLabel = "Session V.$ayaFrom → V.$ayaTo",
            markers = storedMarkers
        )

        val insertedId = recordingDao.insertRecording(entity)

        RecitationMeta(
            id = insertedId.toString(),
            type = RecitationType.SESSION,
            riwaya = riwaya,
            sura = sura,
            ayaFrom = ayaFrom,
            ayaTo = ayaTo,
            durationMs = durationMs,
            createdAt = getCurrentIsoDate(),
            markers = storedMarkers,
            reciterName = reciterName,
            filePath = destFile.absolutePath
        )
    }

    fun recordedAyahMap(sura: Int): Map<Int, Boolean> {
        return _recitationsFlow.value
            .filter { it.sura == sura }
            .associate { it.ayaFrom to true }
    }

    fun findAyahRecording(sura: Int, aya: Int): RecitationMeta? {
        return _recitationsFlow.value.find {
            it.sura == sura && it.ayaFrom == aya
        }
    }

    fun getAudioFile(idOrPath: String): File {
        val file = File(idOrPath)
        return if (file.exists() && file.isAbsolute) {
            file
        } else {
            File(audioDir, "$idOrPath.m4a")
        }
    }

    suspend fun deleteRecitation(id: String) = withContext(Dispatchers.IO) {
        val idLong = id.toLongOrNull()
        if (idLong != null) {
            val entity = recordingDao.getRecordingById(idLong)
            entity?.let {
                val f = File(it.filePath)
                if (f.exists()) f.delete()
                recordingDao.deleteRecordingById(idLong)
            }
        } else {
            val audioFile = File(audioDir, "$id.m4a")
            if (audioFile.exists()) audioFile.delete()
        }
    }

    private fun getCurrentIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun formatEpochToIso(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMillis))
    }
}
