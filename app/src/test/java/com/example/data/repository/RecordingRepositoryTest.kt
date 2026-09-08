package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.RecordingDao
import com.example.data.local.db.RecordingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

class FakeRecordingDao : RecordingDao {
    private val records = mutableMapOf<Long, RecordingEntity>()
    private var nextId = 1L

    override fun getAllRecordings(): Flow<List<RecordingEntity>> {
        return flowOf(records.values.sortedByDescending { it.recordedAtEpochMillis })
    }

    override suspend fun insertRecording(recording: RecordingEntity): Long {
        val id = if (recording.id == 0L) nextId++ else recording.id
        records[id] = recording.copy(id = id)
        return id
    }

    override suspend fun deleteRecordingById(id: Long) {
        records.remove(id)
    }

    override suspend fun updateRecordingMeta(id: Long, reciterName: String, customLabel: String?) {
        records[id]?.let {
            records[id] = it.copy(reciterName = reciterName, customLabel = customLabel)
        }
    }

    override suspend fun getRecordingById(id: Long): RecordingEntity? = records[id]

    override fun getRecordingsBySurah(surahNumber: Int): Flow<List<RecordingEntity>> {
        return flowOf(records.values.filter { it.surahNumber == surahNumber }.sortedByDescending { it.recordedAtEpochMillis })
    }

    override suspend fun getRecordingForAyah(surahNumber: Int, ayahNumber: Int): RecordingEntity? {
        return records.values
            .filter { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
            .sortedWith(compareByDescending<RecordingEntity> { it.isBest }.thenByDescending { it.recordedAtEpochMillis })
            .firstOrNull()
    }

    override fun getRecordedAyahNumbers(surahNumber: Int): Flow<List<Int>> {
        return flowOf(records.values.filter { it.surahNumber == surahNumber }.map { it.ayahNumber }.distinct())
    }

    override suspend fun updateIsBest(id: Long, isBest: Boolean) {
        records[id]?.let {
            records[id] = it.copy(isBest = isBest)
        }
    }

    override suspend fun clearBestForAyah(surahNumber: Int, ayahNumber: Int) {
        records.values.filter { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }.forEach {
            records[it.id] = it.copy(isBest = false)
        }
    }

    override fun getRecordingsForAyahFlow(surahNumber: Int, ayahNumber: Int): Flow<List<RecordingEntity>> {
        return flowOf(records.values
            .filter { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
            .sortedWith(compareByDescending<RecordingEntity> { it.isBest }.thenByDescending { it.recordedAtEpochMillis }))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingRepositoryTest {

    private lateinit var context: Context
    private lateinit var fakeDao: FakeRecordingDao
    private lateinit var repository: RecordingRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeDao = FakeRecordingDao()
        repository = RecordingRepository(context, fakeDao)
    }

    @Test
    fun `saveRecording moves file permanently and deletes temp file`() = runTest {
        val tempFile = File(context.cacheDir, "test_take.m4a")
        tempFile.writeText("audio dummy content")
        assertTrue(tempFile.exists())

        val saved = repository.saveRecording(
            tempFile = tempFile,
            reciterName = "Ahmed",
            surahNumber = 1,
            surahName = "Al-Fatiha",
            ayahNumber = 1,
            durationMs = 4500L,
            customLabel = "Prise 1",
            isBest = true
        )

        // Temp file must be deleted (no duplicate/orphan)
        assertFalse("Temp file should be deleted after save", tempFile.exists())

        // Permanent file must exist
        val permFile = File(saved.filePath)
        assertTrue("Permanent file should exist", permFile.exists())
        assertEquals("audio dummy content", permFile.readText())

        // Metadata must match
        assertEquals(1, saved.surahNumber)
        assertEquals(1, saved.ayahNumber)
        assertEquals(4500L, saved.durationMs)
        assertTrue(saved.isBest)

        // Cleanup
        permFile.delete()
    }

    @Test
    fun `toggleBestRecording sets and unsets best take properly`() = runTest {
        val tempFile1 = File(context.cacheDir, "take1.m4a").apply { writeText("audio 1") }
        val rec1 = repository.saveRecording(tempFile1, "User", 1, "Al-Fatiha", 1, 3000L, isBest = false)

        val tempFile2 = File(context.cacheDir, "take2.m4a").apply { writeText("audio 2") }
        val rec2 = repository.saveRecording(tempFile2, "User", 1, "Al-Fatiha", 1, 3200L, isBest = false)

        // Initially neither is best
        assertFalse(rec1.isBest)
        assertFalse(rec2.isBest)

        // Toggle rec1 as best
        val isBest1 = repository.toggleBestRecording(rec1)
        assertTrue(isBest1)
        val bestNow1 = repository.getBestOrLatestRecording(1, 1)
        assertEquals(rec1.id, bestNow1?.id)
        assertTrue(bestNow1?.isBest == true)

        // Toggle rec2 as best -> rec1 is cleared, rec2 becomes best
        val isBest2 = repository.toggleBestRecording(rec2)
        assertTrue(isBest2)
        val bestNow2 = repository.getBestOrLatestRecording(1, 1)
        assertEquals(rec2.id, bestNow2?.id)
        assertTrue(bestNow2?.isBest == true)

        // Verify rec1 is no longer best in DAO
        val rec1InDao = fakeDao.getRecordingById(rec1.id)
        assertFalse(rec1InDao?.isBest == true)

        // Cleanup
        File(rec1.filePath).delete()
        File(rec2.filePath).delete()
    }

    @Test
    fun `deleteRecording removes both audio file and database entry`() = runTest {
        val tempFile = File(context.cacheDir, "to_delete.m4a").apply { writeText("audio to delete") }
        val saved = repository.saveRecording(tempFile, "User", 1, "Al-Fatiha", 2, 2000L)

        val file = File(saved.filePath)
        assertTrue("File should exist before delete", file.exists())
        assertNotNull("Record should exist before delete", fakeDao.getRecordingById(saved.id))

        repository.deleteRecording(saved)

        assertFalse("File should be deleted on disk", file.exists())
        assertNull("Record should be deleted in Room", fakeDao.getRecordingById(saved.id))
    }
}
