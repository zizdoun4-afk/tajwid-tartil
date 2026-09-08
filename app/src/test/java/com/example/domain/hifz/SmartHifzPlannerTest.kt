package com.example.domain.hifz

import com.example.data.local.db.MemorizationStatusEntity
import com.example.domain.model.MemorizationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartHifzPlannerTest {

    @Test
    fun `test buildSmartReviewQueue prioritizes correctly`() {
        val now = System.currentTimeMillis()

        // 1. Overdue ayah (due 2 hours ago)
        val overdueAyah = MemorizationStatusEntity(
            id = 1,
            surahNumber = 1,
            ayahNumber = 1,
            status = MemorizationStatus.REVIEW.name,
            nextReviewDueEpochMillis = now - (2L * 60 * 60 * 1000)
        )

        // 2. Repeatedly failed ayah (failed 3 times, not yet overdue)
        val failedAyah = MemorizationStatusEntity(
            id = 2,
            surahNumber = 1,
            ayahNumber = 2,
            status = MemorizationStatus.REVIEW.name,
            failedTests = 3,
            nextReviewDueEpochMillis = now + (48L * 60 * 60 * 1000)
        )

        // 3. Approaching due ayah (due in 5 hours)
        val approachingAyah = MemorizationStatusEntity(
            id = 3,
            surahNumber = 1,
            ayahNumber = 3,
            status = MemorizationStatus.REVIEW.name,
            nextReviewDueEpochMillis = now + (5L * 60 * 60 * 1000)
        )

        // 4. Normal review ayah (due in 5 days)
        val normalReviewAyah = MemorizationStatusEntity(
            id = 4,
            surahNumber = 1,
            ayahNumber = 4,
            status = MemorizationStatus.REVIEW.name,
            nextReviewDueEpochMillis = now + (5L * 24 * 60 * 60 * 1000)
        )

        // 5. In learning ayah
        val learningAyah = MemorizationStatusEntity(
            id = 5,
            surahNumber = 1,
            ayahNumber = 5,
            status = MemorizationStatus.LEARNING.name
        )

        val inputList = listOf(learningAyah, normalReviewAyah, approachingAyah, failedAyah, overdueAyah)
        val prioritizedQueue = SmartHifzPlanner.buildSmartReviewQueue(inputList, now)

        assertEquals(5, prioritizedQueue.size)
        assertEquals(1L, prioritizedQueue[0].id) // Overdue first
        assertEquals(2L, prioritizedQueue[1].id) // Repeatedly failed second
        assertEquals(3L, prioritizedQueue[2].id) // Approaching 24h third
        assertEquals(4L, prioritizedQueue[3].id) // Normal review fourth
        assertEquals(5L, prioritizedQueue[4].id) // Learning fifth
    }

    @Test
    fun `test computeDailyPlan respects dailyTarget and calculates completion`() {
        val now = System.currentTimeMillis()
        val startOfDay = now - (2L * 60 * 60 * 1000) // 2 hours ago

        val reviewedToday = MemorizationStatusEntity(
            id = 1,
            surahNumber = 1,
            ayahNumber = 1,
            status = MemorizationStatus.REVIEW.name,
            lastReviewedAtEpochMillis = now - (1L * 60 * 60 * 1000)
        )

        val unreviewedOverdue = MemorizationStatusEntity(
            id = 2,
            surahNumber = 1,
            ayahNumber = 2,
            status = MemorizationStatus.REVIEW.name,
            nextReviewDueEpochMillis = now - (10L * 60 * 1000)
        )

        val weakAyah = MemorizationStatusEntity(
            id = 3,
            surahNumber = 1,
            ayahNumber = 3,
            status = MemorizationStatus.REVIEW.name,
            failedTests = 2,
            successfulTests = 1
        )

        val allStatuses = listOf(reviewedToday, unreviewedOverdue, weakAyah)

        val plan = SmartHifzPlanner.computeDailyPlan(
            allStatuses = allStatuses,
            dailyTarget = 3,
            startOfDayMillis = startOfDay,
            nowMillis = now
        )

        assertEquals(3, plan.targetVersesPerDay)
        assertEquals(1, plan.completedTodayCount)
        assertEquals(1, plan.dueReviewsCount)
        assertEquals(1, plan.weakVersesCount)
        assertFalse(plan.targetReached)
        assertEquals(2, plan.remainingToday)
    }

    @Test
    fun `test computeSrsHealth identifies strong, weak, overdue, and memorized`() {
        val now = System.currentTimeMillis()

        val strong = MemorizationStatusEntity(
            id = 1,
            surahNumber = 1,
            ayahNumber = 1,
            status = MemorizationStatus.MEMORIZED.name,
            successfulTests = 4,
            failedTests = 0
        )

        val weak = MemorizationStatusEntity(
            id = 2,
            surahNumber = 1,
            ayahNumber = 2,
            status = MemorizationStatus.REVIEW.name,
            failedTests = 2,
            successfulTests = 1
        )

        val overdue = MemorizationStatusEntity(
            id = 3,
            surahNumber = 1,
            ayahNumber = 3,
            status = MemorizationStatus.REVIEW.name,
            nextReviewDueEpochMillis = now - 5000L
        )

        val health = SmartHifzPlanner.computeSrsHealth(listOf(strong, weak, overdue), now)

        assertEquals(1, health.strongCount)
        assertEquals(1, health.weakCount)
        assertEquals(1, health.overdueCount)
        assertEquals(1, health.memorizedCount)
    }

    @Test
    fun `test computeJuzProgress covers all 30 Ajza`() {
        val statuses = listOf(
            MemorizationStatusEntity(id = 1, surahNumber = 1, ayahNumber = 1, status = MemorizationStatus.MEMORIZED.name),
            MemorizationStatusEntity(id = 2, surahNumber = 114, ayahNumber = 1, status = MemorizationStatus.MEMORIZED.name)
        )

        val juzProgress = SmartHifzPlanner.computeJuzProgress(statuses)

        assertEquals(30, juzProgress.size)
        assertEquals(1, juzProgress[0].juzNumber)
        assertEquals(1, juzProgress[0].memorizedCount) // In Juz 1 (Al-Fatiha)
        assertEquals(30, juzProgress[29].juzNumber)
        assertEquals(1, juzProgress[29].memorizedCount) // In Juz 30 (An-Nas)
    }
}
