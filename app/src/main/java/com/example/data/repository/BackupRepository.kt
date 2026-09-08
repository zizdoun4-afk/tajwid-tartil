package com.example.data.repository

import android.content.Context
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.local.preferences.UserPreferencesRepository
import com.example.domain.model.MemorizationStatus
import com.example.domain.model.RecitationStyle
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Handles offline-first, private JSON backup export and import for:
 * - Hifz progress & SRS scheduling
 * - Settings (language, reciter, theme, style)
 * - Bookmarks
 */
class BackupRepository(
    private val database: AppDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val memorizationDao = database.memorizationDao()

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Rafiq Al-Quran (Tajwid & Tartil)")
        root.put("version", 1)
        root.put("exportedAtEpochMillis", System.currentTimeMillis())

        // 1. Memorization status records
        val memorizationList = memorizationDao.getAllStatuses()
        val memArray = JSONArray()
        for (item in memorizationList) {
            val obj = JSONObject()
            obj.put("surahNumber", item.surahNumber)
            obj.put("ayahNumber", item.ayahNumber)
            obj.put("status", item.status)
            obj.put("reviewCount", item.reviewCount)
            obj.put("lastReviewedAt", item.lastReviewedAtEpochMillis ?: -1L)
            obj.put("nextReviewDue", item.nextReviewDueEpochMillis ?: -1L)
            obj.put("successfulTests", item.successfulTests)
            obj.put("failedTests", item.failedTests)
            obj.put("trainingAttempts", item.trainingAttempts)
            memArray.put(obj)
        }
        root.put("memorization", memArray)

        // 2. User Settings & Bookmarks
        val settingsObj = JSONObject()
        val lang = preferencesRepository.selectedLanguage.first()
        val reciter = preferencesRepository.defaultReciterName.first()
        val style = preferencesRepository.preferredStyle.first()
        val theme = preferencesRepository.selectedThemeId.first()
        val bookmarks = preferencesRepository.bookmarkedAyahs.first()

        settingsObj.put("languageCode", lang.code)
        settingsObj.put("reciterName", reciter)
        settingsObj.put("preferredStyle", style.name)
        settingsObj.put("selectedThemeId", theme)

        val bookmarksArray = JSONArray()
        for (bm in bookmarks) {
            bookmarksArray.put(bm)
        }
        settingsObj.put("bookmarks", bookmarksArray)

        root.put("settings", settingsObj)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("memorization")) {
                return@withContext Result.failure(IllegalArgumentException("Format JSON invalide: clé 'memorization' manquante"))
            }

            val memArray = root.getJSONArray("memorization")
            val restoredList = mutableListOf<MemorizationStatusEntity>()

            for (i in 0 until memArray.length()) {
                val obj = memArray.getJSONObject(i)
                val surah = obj.getInt("surahNumber")
                val ayah = obj.getInt("ayahNumber")
                val statusStr = obj.optString("status", MemorizationStatus.NEW.name)
                val revCount = obj.optInt("reviewCount", 0)
                val lastRevRaw = obj.optLong("lastReviewedAt", -1L)
                val nextRevRaw = obj.optLong("nextReviewDue", -1L)
                val success = obj.optInt("successfulTests", 0)
                val fails = obj.optInt("failedTests", 0)
                val attempts = obj.optInt("trainingAttempts", 0)

                val entity = MemorizationStatusEntity(
                    surahNumber = surah,
                    ayahNumber = ayah,
                    status = statusStr,
                    reviewCount = revCount,
                    lastReviewedAtEpochMillis = if (lastRevRaw > 0) lastRevRaw else null,
                    nextReviewDueEpochMillis = if (nextRevRaw > 0) nextRevRaw else null,
                    successfulTests = success,
                    failedTests = fails,
                    trainingAttempts = attempts
                )
                restoredList.add(entity)
            }

            if (restoredList.isNotEmpty()) {
                memorizationDao.upsertStatuses(restoredList)
            }

            // Optional settings restore
            if (root.has("settings")) {
                val settingsObj = root.getJSONObject("settings")
                if (settingsObj.has("languageCode")) {
                    val code = settingsObj.getString("languageCode")
                    AppLanguage.entries.find { it.code == code }?.let {
                        preferencesRepository.setSelectedLanguage(it)
                    }
                }
                if (settingsObj.has("reciterName")) {
                    preferencesRepository.setDefaultReciterName(settingsObj.getString("reciterName"))
                }
                if (settingsObj.has("preferredStyle")) {
                    try {
                        val style = RecitationStyle.valueOf(settingsObj.getString("preferredStyle"))
                        preferencesRepository.setPreferredStyle(style)
                    } catch (_: Exception) {}
                }
                if (settingsObj.has("selectedThemeId")) {
                    preferencesRepository.setSelectedThemeId(settingsObj.getString("selectedThemeId"))
                }
            }

            Result.success(restoredList.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportBackupToFile(context: Context): File = withContext(Dispatchers.IO) {
        val json = exportBackupJson()
        val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, "rafiq_backup_${System.currentTimeMillis()}.json")
        backupFile.writeText(json, Charsets.UTF_8)
        backupFile
    }
}
