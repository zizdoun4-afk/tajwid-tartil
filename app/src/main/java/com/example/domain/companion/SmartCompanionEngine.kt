package com.example.domain.companion

import com.example.data.local.db.MemorizationDao
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.local.db.TajwidProgressDao
import com.example.data.repository.TajwidRepository
import com.example.domain.model.MemorizationStatus
import kotlinx.coroutines.flow.first

/**
 * Daily personalized study and review plan generated from real Room DB data.
 */
data class DailySmartAgenda(
    val dueForReview: List<MemorizationStatusEntity>,
    val weakAyahs: List<MemorizationStatusEntity>,
    val inLearning: List<MemorizationStatusEntity>,
    val memorizedTotal: Int,
    val learningTotal: Int,
    val reviewTotal: Int,
    val dailyTargetAyahs: Int,
    val recommendedSurahToPractice: Int?,
    val recommendedTajwidLessonId: String?,
    val recommendedTajwidTitleFr: String?,
    val recommendedTajwidTitleAr: String? = null,
    val motivationMessageFr: String,
    val motivationMessageAr: String
)

object SmartCompanionEngine {

    /**
     * Computes the daily smart agenda from real user progress in Room DB.
     */
    suspend fun computeDailyAgenda(
        memorizationDao: MemorizationDao,
        tajwidProgressDao: TajwidProgressDao,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): DailySmartAgenda {
        // 1. Fetch real statuses from Room
        val dueList = mutableListOf<MemorizationStatusEntity>()
        var memorizedCount = 0
        var learningCount = 0
        var reviewCount = 0
        val weakList = mutableListOf<MemorizationStatusEntity>()
        val learningList = mutableListOf<MemorizationStatusEntity>()
        val surahActivityMap = mutableMapOf<Int, Int>()

        val allStatuses = try {
            memorizationDao.getAllStatusesFlow().first()
        } catch (e: Exception) {
            emptyList()
        }

        for (entity in allStatuses) {
            when (entity.memorizationStatus) {
                MemorizationStatus.MEMORIZED -> {
                    memorizedCount++
                    val due = entity.nextReviewDueEpochMillis
                    if (due != null && due <= currentTimeMillis) {
                        dueList.add(entity)
                    }
                }
                MemorizationStatus.LEARNING -> {
                    learningCount++
                    learningList.add(entity)
                    surahActivityMap[entity.surahNumber] = (surahActivityMap[entity.surahNumber] ?: 0) + 1
                }
                MemorizationStatus.REVIEW -> {
                    reviewCount++
                    weakList.add(entity)
                    surahActivityMap[entity.surahNumber] = (surahActivityMap[entity.surahNumber] ?: 0) + 1
                }
                else -> {}
            }
        }

        // 2. Recommend surah to practice (most active learning or default 114 / 1)
        val recommendedSurah = surahActivityMap.maxByOrNull { it.value }?.key
            ?: if (dueList.isNotEmpty()) dueList.first().surahNumber else 1

        // 3. Recommended Tajwid lesson
        var recLessonId: String? = null
        var recLessonTitleFr: String? = null
        var recLessonTitleAr: String? = null
        try {
            val allLessons = TajwidRepository.LESSONS
            for (lesson in allLessons) {
                val progress = tajwidProgressDao.getProgressForLesson(lesson.id)
                if (progress == null || !progress.isCompleted) {
                    recLessonId = lesson.id
                    recLessonTitleFr = lesson.titleFr
                    recLessonTitleAr = lesson.titleAr
                    break
                }
            }
            if (recLessonId == null && allLessons.isNotEmpty()) {
                recLessonId = allLessons.first().id
                recLessonTitleFr = allLessons.first().titleFr
                recLessonTitleAr = allLessons.first().titleAr
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Calculate balanced daily target (1 to 5 ayahs depending on backlog)
        val backlog = dueList.size + weakList.size
        val dailyTarget = when {
            backlog > 10 -> 2  // Reduce new load to prioritize revisions
            backlog in 4..10 -> 3
            else -> 5
        }

        // 5. Contextual encouragement message
        val (msgFr, msgAr) = when {
            dueList.isNotEmpty() -> Pair(
                "Vous avez ${dueList.size} verset(s) à réviser aujourd'hui. Consolidez votre mémorisation avec Rafiq.",
                "لديك ${dueList.size} آية مستحقة للمراجعة اليوم. ثبّت حفظك مع رفيق القرآن."
            )
            learningList.isNotEmpty() -> Pair(
                "Poursuivez l'apprentissage de vos versets en cours avec une écoute attentive et le Tartil.",
                "واصل تعلم آياتك الجديدة بالاستماع المتأني والترتيل المحكم."
            )
            else -> Pair(
                "Toutes vos révisions sont à jour ! Choisissez une nouvelle sourate ou perfectionnez votre Tajwid.",
                "مراجعاتك محدثة بالكامل! اختر سورة جديدة أو طوّر تجويدك."
            )
        }

        return DailySmartAgenda(
            dueForReview = dueList,
            weakAyahs = weakList,
            inLearning = learningList,
            memorizedTotal = memorizedCount,
            learningTotal = learningCount,
            reviewTotal = reviewCount,
            dailyTargetAyahs = dailyTarget,
            recommendedSurahToPractice = recommendedSurah,
            recommendedTajwidLessonId = recLessonId,
            recommendedTajwidTitleFr = recLessonTitleFr,
            recommendedTajwidTitleAr = recLessonTitleAr,
            motivationMessageFr = msgFr,
            motivationMessageAr = msgAr
        )
    }
}
