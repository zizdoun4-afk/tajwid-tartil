package com.example.domain.hifz

import com.example.data.local.db.MemorizationStatusEntity
import com.example.domain.model.MemorizationStatus

/**
 * Breakdown for "خطة اليوم" (Today's Plan).
 */
data class DailyHifzPlan(
    val targetVersesPerDay: Int,
    val dueReviewsCount: Int,
    val weakVersesCount: Int,
    val newVersesCount: Int,
    val completedTodayCount: Int,
    val targetReached: Boolean = completedTodayCount >= targetVersesPerDay,
    val prioritizedAyahs: List<MemorizationStatusEntity> = emptyList()
) {
    val remainingToday: Int
        get() = (targetVersesPerDay - completedTodayCount).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (targetVersesPerDay > 0) {
            (completedTodayCount.toFloat() / targetVersesPerDay).coerceIn(0f, 1f)
        } else 0f
}

/**
 * Status categorization for Spaced Repetition health.
 */
data class SrsHealthSummary(
    val strongCount: Int,
    val weakCount: Int,
    val overdueCount: Int,
    val inProgressCount: Int,
    val memorizedCount: Int
)

/**
 * Juz memorization progress statistics.
 */
data class JuzMemorizationProgress(
    val juzNumber: Int,
    val nameArabic: String,
    val totalAyahs: Int,
    val memorizedCount: Int,
    val learningCount: Int,
    val reviewCount: Int
) {
    val memorizedPercentage: Float
        get() = if (totalAyahs > 0) (memorizedCount.toFloat() / totalAyahs) * 100f else 0f

    val learningPercentage: Float
        get() = if (totalAyahs > 0) (learningCount.toFloat() / totalAyahs) * 100f else 0f

    val reviewPercentage: Float
        get() = if (totalAyahs > 0) (reviewCount.toFloat() / totalAyahs) * 100f else 0f

    val remainingAyahs: Int
        get() = (totalAyahs - memorizedCount).coerceAtLeast(0)
}

object SmartHifzPlanner {

    /**
     * Calculates the smart prioritized review queue according to roadmap requirements:
     * 1. Overdue verses (due date passed)
     * 2. Repeatedly failed verses (failedTests >= 2)
     * 3. Verses approaching due date (due in next 24 hours)
     * 4. Normal reviews (status = REVIEW)
     * 5. New memorization (status = LEARNING or NEW)
     */
    fun buildSmartReviewQueue(
        allStatuses: List<MemorizationStatusEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MemorizationStatusEntity> {
        val window24h = nowMillis + (24L * 60 * 60 * 1000)

        // 1. Overdue verses
        val overdue = allStatuses.filter {
            it.nextReviewDueEpochMillis != null && it.nextReviewDueEpochMillis <= nowMillis
        }.sortedBy { it.nextReviewDueEpochMillis ?: Long.MAX_VALUE }

        val overdueIds = overdue.map { it.id }.toSet()

        // 2. Repeatedly failed verses (not already overdue)
        val repeatedlyFailed = allStatuses.filter {
            !overdueIds.contains(it.id) && it.failedTests >= 2
        }.sortedByDescending { it.failedTests }

        val failedIds = repeatedlyFailed.map { it.id }.toSet()

        // 3. Verses approaching due date (within 24 hours, not already counted)
        val approaching = allStatuses.filter {
            !overdueIds.contains(it.id) &&
            !failedIds.contains(it.id) &&
            it.nextReviewDueEpochMillis != null &&
            it.nextReviewDueEpochMillis > nowMillis &&
            it.nextReviewDueEpochMillis <= window24h
        }.sortedBy { it.nextReviewDueEpochMillis ?: Long.MAX_VALUE }

        val approachingIds = approaching.map { it.id }.toSet()

        // 4. Normal reviews (in REVIEW status, not covered above)
        val normalReviews = allStatuses.filter {
            !overdueIds.contains(it.id) &&
            !failedIds.contains(it.id) &&
            !approachingIds.contains(it.id) &&
            it.status == MemorizationStatus.REVIEW.name
        }.sortedBy { it.lastReviewedAtEpochMillis ?: 0L }

        val normalIds = normalReviews.map { it.id }.toSet()

        // 5. New memorization / in learning
        val learning = allStatuses.filter {
            !overdueIds.contains(it.id) &&
            !failedIds.contains(it.id) &&
            !approachingIds.contains(it.id) &&
            !normalIds.contains(it.id) &&
            (it.status == MemorizationStatus.LEARNING.name || it.status == MemorizationStatus.NEW.name)
        }.sortedWith(compareBy({ it.surahNumber }, { it.ayahNumber }))

        return overdue + repeatedlyFailed + approaching + normalReviews + learning
    }

    /**
     * Computes the daily plan (خطة اليوم) adapted to user daily target and real progress.
     */
    fun computeDailyPlan(
        allStatuses: List<MemorizationStatusEntity>,
        dailyTarget: Int,
        startOfDayMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): DailyHifzPlan {
        val completedToday = allStatuses.count {
            (it.lastReviewedAtEpochMillis ?: 0L) >= startOfDayMillis
        }

        val smartQueue = buildSmartReviewQueue(allStatuses, nowMillis)

        val overdue = allStatuses.filter { it.isOverdue(nowMillis) }
        val weak = allStatuses.filter { it.isWeak }
        val inLearning = allStatuses.filter { it.status == MemorizationStatus.LEARNING.name }

        val dueCount = overdue.size
        val weakCount = weak.size
        val newCount = inLearning.size

        // Take up to dailyTarget items from the smart priority queue
        val planItems = smartQueue.take(dailyTarget)

        return DailyHifzPlan(
            targetVersesPerDay = dailyTarget,
            dueReviewsCount = dueCount,
            weakVersesCount = weakCount,
            newVersesCount = newCount,
            completedTodayCount = completedToday,
            targetReached = completedToday >= dailyTarget,
            prioritizedAyahs = planItems
        )
    }

    /**
     * Analyzes health of the Spaced Repetition system.
     */
    fun computeSrsHealth(
        allStatuses: List<MemorizationStatusEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): SrsHealthSummary {
        var strong = 0
        var weak = 0
        var overdue = 0
        var inProgress = 0
        var memorized = 0

        for (item in allStatuses) {
            if (item.status == MemorizationStatus.MEMORIZED.name) {
                memorized++
            }
            if (item.isOverdue(nowMillis)) {
                overdue++
            }
            if (item.isWeak) {
                weak++
            } else if (item.isStrong) {
                strong++
            } else if (item.status == MemorizationStatus.REVIEW.name || item.status == MemorizationStatus.LEARNING.name) {
                inProgress++
            }
        }

        return SrsHealthSummary(
            strongCount = strong,
            weakCount = weak,
            overdueCount = overdue,
            inProgressCount = inProgress,
            memorizedCount = memorized
        )
    }

    /**
     * 30 Ajza' reference catalog:
     * (juzNumber, startSurah, startAyah, endSurah, endAyah, totalAyahs, nameArabic)
     */
    val JUZ_CATALOG: List<JuzMetadata> = listOf(
        JuzMetadata(1, 1, 1, 2, 141, 148, "الجزء الأول"),
        JuzMetadata(2, 2, 142, 2, 252, 111, "سيقول السفهاء"),
        JuzMetadata(3, 2, 253, 3, 92, 126, "تلك الرسل"),
        JuzMetadata(4, 3, 93, 4, 23, 131, "لن تنالوا"),
        JuzMetadata(5, 4, 24, 4, 147, 124, "والمحصنات"),
        JuzMetadata(6, 4, 148, 5, 81, 110, "لا يحب الله"),
        JuzMetadata(7, 5, 82, 6, 110, 149, "وإذا سمعوا"),
        JuzMetadata(8, 6, 111, 7, 87, 142, "ولو أننا"),
        JuzMetadata(9, 7, 88, 8, 40, 159, "قال الملأ"),
        JuzMetadata(10, 8, 41, 9, 92, 127, "واعلموا"),
        JuzMetadata(11, 9, 93, 11, 5, 151, "يعتذرون"),
        JuzMetadata(12, 11, 6, 12, 52, 170, "وما من دابة"),
        JuzMetadata(13, 12, 53, 14, 52, 154, "وما أبرئ نفسي"),
        JuzMetadata(14, 15, 1, 16, 128, 227, "ربما"),
        JuzMetadata(15, 17, 1, 18, 74, 185, "سبحان الذي"),
        JuzMetadata(16, 18, 75, 20, 135, 269, "قال ألم"),
        JuzMetadata(17, 21, 1, 22, 78, 190, "اقترب للناس"),
        JuzMetadata(18, 23, 1, 25, 20, 202, "قد أفلح"),
        JuzMetadata(19, 25, 21, 27, 55, 339, "وقال الذين لا يرجون"),
        JuzMetadata(20, 27, 56, 29, 45, 171, "فما كان جواب قومه"),
        JuzMetadata(21, 29, 46, 33, 30, 178, "ولا تجادلوا"),
        JuzMetadata(22, 33, 31, 36, 27, 169, "ومن يقنت"),
        JuzMetadata(23, 36, 28, 39, 31, 357, "وما أنزلنا"),
        JuzMetadata(24, 39, 32, 41, 46, 175, "فمن أظلم"),
        JuzMetadata(25, 41, 47, 45, 37, 246, "إليه يرد"),
        JuzMetadata(26, 46, 1, 51, 30, 195, "حم"),
        JuzMetadata(27, 51, 31, 57, 29, 399, "قال فما خطبكم"),
        JuzMetadata(28, 58, 1, 66, 12, 137, "قد سمع الله"),
        JuzMetadata(29, 67, 1, 77, 50, 431, "تبارك الذي"),
        JuzMetadata(30, 78, 1, 114, 6, 564, "عم يتساءلون")
    )

    /**
     * Computes Juz progress from recorded statuses.
     */
    fun computeJuzProgress(allStatuses: List<MemorizationStatusEntity>): List<JuzMemorizationProgress> {
        return JUZ_CATALOG.map { juzMeta ->
            val ayahsInJuz = allStatuses.filter { status ->
                juzMeta.contains(status.surahNumber, status.ayahNumber)
            }
            val memorized = ayahsInJuz.count { it.status == MemorizationStatus.MEMORIZED.name }
            val learning = ayahsInJuz.count { it.status == MemorizationStatus.LEARNING.name }
            val review = ayahsInJuz.count { it.status == MemorizationStatus.REVIEW.name }

            JuzMemorizationProgress(
                juzNumber = juzMeta.juzNumber,
                nameArabic = juzMeta.nameArabic,
                totalAyahs = juzMeta.totalAyahs,
                memorizedCount = memorized,
                learningCount = learning,
                reviewCount = review
            )
        }
    }
}

data class JuzMetadata(
    val juzNumber: Int,
    val startSurah: Int,
    val startAyah: Int,
    val endSurah: Int,
    val endAyah: Int,
    val totalAyahs: Int,
    val nameArabic: String
) {
    fun contains(surah: Int, ayah: Int): Boolean {
        if (surah < startSurah || surah > endSurah) return false
        if (surah == startSurah && ayah < startAyah) return false
        if (surah == endSurah && ayah > endAyah) return false
        return true
    }
}
