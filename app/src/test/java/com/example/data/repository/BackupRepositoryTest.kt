package com.example.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.local.preferences.UserPreferencesRepository
import com.example.domain.model.MemorizationStatus
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class BackupRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupRepository: BackupRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = UserPreferencesRepository(context)
        backupRepository = BackupRepository(db, prefsRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testExportAndImportBackup() = runBlocking {
        // Given initial memorization data
        val entity1 = MemorizationStatusEntity(
            surahNumber = 1,
            ayahNumber = 1,
            status = MemorizationStatus.MEMORIZED.name,
            reviewCount = 5,
            lastReviewedAtEpochMillis = 1000000L,
            nextReviewDueEpochMillis = 2000000L,
            successfulTests = 4,
            failedTests = 0,
            trainingAttempts = 12
        )
        val entity2 = MemorizationStatusEntity(
            surahNumber = 1,
            ayahNumber = 2,
            status = MemorizationStatus.LEARNING.name,
            reviewCount = 1,
            lastReviewedAtEpochMillis = 1050000L,
            nextReviewDueEpochMillis = 1150000L,
            successfulTests = 1,
            failedTests = 1,
            trainingAttempts = 5
        )
        db.memorizationDao().upsertStatuses(listOf(entity1, entity2))

        // When exporting
        val json = backupRepository.exportBackupJson()
        assertTrue(json.isNotBlank())

        val root = JSONObject(json)
        assertEquals(1, root.getInt("version"))
        assertTrue(root.has("memorization"))
        val array = root.getJSONArray("memorization")
        assertEquals(2, array.length())

        // Clear DB
        db.clearAllTables()
        assertEquals(0, db.memorizationDao().getAllStatuses().size)

        // When importing back
        val importResult = backupRepository.importBackupJson(json)
        assertTrue(importResult.isSuccess)
        assertEquals(2, importResult.getOrNull())

        // Then verify restored data in DB
        val restored = db.memorizationDao().getAllStatuses()
        assertEquals(2, restored.size)
        val restored1 = db.memorizationDao().getStatusForAyah(1, 1)
        assertEquals(MemorizationStatus.MEMORIZED.name, restored1?.status)
        assertEquals(5, restored1?.reviewCount)
        assertEquals(4, restored1?.successfulTests)
    }
}
