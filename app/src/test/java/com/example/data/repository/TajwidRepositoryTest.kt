package com.example.data.repository

import com.example.data.local.db.TajwidProgressDao
import com.example.data.local.db.TajwidProgressEntity
import com.example.domain.model.TajwidLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTajwidProgressDao : TajwidProgressDao {
    val store = mutableMapOf<String, TajwidProgressEntity>()

    override fun getAllProgressFlow(): Flow<List<TajwidProgressEntity>> {
        return flowOf(store.values.toList())
    }

    override suspend fun getProgressForLesson(lessonId: String): TajwidProgressEntity? {
        return store[lessonId]
    }

    override fun getProgressForLessonFlow(lessonId: String): Flow<TajwidProgressEntity?> {
        return flowOf(store[lessonId])
    }

    override suspend fun upsertProgress(entity: TajwidProgressEntity): Long {
        store[entity.lessonId] = entity
        return 1L
    }

    override fun getCompletedLessonsCountFlow(): Flow<Int> {
        return flowOf(store.values.count { it.isCompleted })
    }
}

class TajwidRepositoryTest {

    private lateinit var fakeDao: FakeTajwidProgressDao
    private lateinit var repository: TajwidRepository

    @Before
    fun setUp() {
        fakeDao = FakeTajwidProgressDao()
        repository = TajwidRepository(fakeDao)
    }

    @Test
    fun `test all lessons are loaded across three levels`() {
        val allLessons = repository.getAllLessons()
        assertTrue(allLessons.isNotEmpty())

        val beginner = repository.getLessonsByLevel(TajwidLevel.BEGINNER)
        val intermediate = repository.getLessonsByLevel(TajwidLevel.INTERMEDIATE)
        val advanced = repository.getLessonsByLevel(TajwidLevel.ADVANCED)

        assertTrue(beginner.isNotEmpty())
        assertTrue(intermediate.isNotEmpty())
        assertTrue(advanced.isNotEmpty())

        // Verify that every lesson has rules and examples
        for (lesson in allLessons) {
            assertTrue("Lesson ${lesson.id} must have rules", lesson.rules.isNotEmpty())
            for (rule in lesson.rules) {
                assertTrue("Rule ${rule.id} must have a name", rule.nameFr.isNotBlank())
                assertTrue("Rule ${rule.id} must have an Arabic name", rule.nameAr.isNotBlank())
            }
        }
    }

    @Test
    fun `test lesson completion and progress flow`() = runTest {
        assertEquals(0, repository.getCompletedLessonsCountFlow().first())

        repository.markLessonCompleted("makharij_basics", 1)

        val progress = repository.getProgressFlow().first()
        assertEquals(1, progress.size)
        assertTrue(progress[0].isCompleted)
        assertEquals("makharij_basics", progress[0].lessonId)
        assertEquals(1, repository.getCompletedLessonsCountFlow().first())
    }

    @Test
    fun `test verified tajwid annotations for key ayahs`() {
        val annotations1_7 = repository.getTajwidAnnotationsForAyah(1, 7)
        assertTrue(annotations1_7.isNotEmpty())
        assertTrue(annotations1_7.any { it.ruleNameFr.contains("Izhâr", ignoreCase = true) })

        val annotations112_1 = repository.getTajwidAnnotationsForAyah(112, 1)
        assertTrue(annotations112_1.isNotEmpty())
        assertTrue(annotations112_1.any { it.ruleNameFr.contains("Qalqalah", ignoreCase = true) })
    }
}
