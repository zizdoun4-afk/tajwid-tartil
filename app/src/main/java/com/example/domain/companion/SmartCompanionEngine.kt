package com.example.domain.companion

import com.example.data.local.db.MemorizationDao
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.local.db.TajwidProgressDao
import com.example.data.repository.TajwidRepository
import com.example.domain.model.MemorizationStatus
import kotlinx.coroutines.flow.first

import java.util.Calendar

enum class DailyRoutinePeriod(val periodNameAr: String, val periodNameFr: String, val periodNameEn: String) {
    MORNING("صباحاً", "Matin", "Morning"),
    DAYTIME("بعد ذلك", "Journée", "Daytime"),
    EVENING("مساءً", "Soirée", "Evening"),
    BEDTIME("قبل النوم", "Avant de dormir", "Bedtime")
}

enum class RoutineStepType {
    REVIEW_OVERDUE,
    LEARN_NEW,
    BLIND_TEST,
    NIGHT_REVISION
}

data class DailyRoutineStep(
    val period: DailyRoutinePeriod,
    val titleAr: String,
    val titleFr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionFr: String,
    val descriptionEn: String,
    val stepType: RoutineStepType,
    val targetSurah: Int,
    val targetAyah: Int,
    val isCompleted: Boolean = false,
    val initialTrainingStep: String? = null
)

data class DailyCompanionRoutine(
    val dateEpochMillis: Long,
    val currentPeriod: DailyRoutinePeriod,
    val steps: List<DailyRoutineStep>,
    val quickStartStep: DailyRoutineStep?,
    val completionFraction: Float,
    val allCompleted: Boolean,
    val nextActionSuggestionAr: String,
    val nextActionSuggestionFr: String,
    val nextActionSuggestionEn: String
)

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

    private fun getStartOfDayMillis(currentTimeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun determinePeriod(currentTimeMillis: Long = System.currentTimeMillis()): DailyRoutinePeriod {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> DailyRoutinePeriod.MORNING
            in 11..16 -> DailyRoutinePeriod.DAYTIME
            in 17..20 -> DailyRoutinePeriod.EVENING
            else -> DailyRoutinePeriod.BEDTIME
        }
    }

    /**
     * Generates "رفيق اليوم" — the smart daily companion routine adapting to user's real progress.
     */
    suspend fun computeDailyRoutine(
        memorizationDao: MemorizationDao,
        dailyTarget: Int = 3,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): DailyCompanionRoutine {
        val startOfDay = getStartOfDayMillis(currentTimeMillis)
        val allStatuses = try {
            memorizationDao.getAllStatusesFlow().first()
        } catch (e: Exception) {
            emptyList()
        }

        val currentPeriod = determinePeriod(currentTimeMillis)

        // Partition statuses from real data
        val overdue = allStatuses.filter { it.isOverdue(currentTimeMillis) }
            .sortedBy { it.nextReviewDueEpochMillis ?: Long.MAX_VALUE }
        val weak = allStatuses.filter { it.isWeak }
            .sortedByDescending { it.failedTests }
        val inLearning = allStatuses.filter { it.status == MemorizationStatus.LEARNING.name }
        val memorized = allStatuses.filter { it.status == MemorizationStatus.MEMORIZED.name }
        val inReview = allStatuses.filter { it.status == MemorizationStatus.REVIEW.name }

        // Determine targets for each period
        // 1. Morning: Overdue or due review
        val morningEntity = overdue.firstOrNull() ?: inReview.firstOrNull() ?: memorized.firstOrNull()
        val morningSurah = morningEntity?.surahNumber ?: 1
        val morningAyah = morningEntity?.ayahNumber ?: 1
        val morningDone = morningEntity != null && (morningEntity.lastReviewedAtEpochMillis ?: 0L) >= startOfDay

        // 2. Daytime: Learn new verses
        val daytimeEntity = inLearning.firstOrNull() ?: allStatuses.find { it.status == MemorizationStatus.NEW.name }
        val daytimeSurah = daytimeEntity?.surahNumber ?: (morningSurah)
        val daytimeAyah = daytimeEntity?.ayahNumber ?: (if (morningAyah < 7) morningAyah + 1 else 1)
        val daytimeDone = allStatuses.count { (it.lastReviewedAtEpochMillis ?: 0L) >= startOfDay } >= dailyTarget

        // 3. Evening: Blind test on learned or reviewed verses
        val eveningEntity = inLearning.firstOrNull() ?: morningEntity ?: daytimeEntity
        val eveningSurah = eveningEntity?.surahNumber ?: morningSurah
        val eveningAyah = eveningEntity?.ayahNumber ?: morningAyah
        val eveningDone = allStatuses.any {
            (it.lastReviewedAtEpochMillis ?: 0L) >= startOfDay && it.successfulTests > 0
        }

        // 4. Bedtime: Short anchoring revision of weak verses or daily progress
        val bedtimeEntity = weak.firstOrNull() ?: morningEntity ?: eveningEntity
        val bedtimeSurah = bedtimeEntity?.surahNumber ?: morningSurah
        val bedtimeAyah = bedtimeEntity?.ayahNumber ?: morningAyah
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val isBedtimeHour = cal.get(Calendar.HOUR_OF_DAY) >= 21 || cal.get(Calendar.HOUR_OF_DAY) < 5
        val bedtimeDone = bedtimeEntity != null && (bedtimeEntity.lastReviewedAtEpochMillis ?: 0L) >= startOfDay && isBedtimeHour

        val steps = listOf(
            DailyRoutineStep(
                period = DailyRoutinePeriod.MORNING,
                titleAr = "مراجعة الورد المستحق",
                titleFr = "Révision des versets dus",
                titleEn = "Review due verses",
                descriptionAr = "مراجعة الآيات المستحقة لتثبيتها في الذاكرة قصيرة المدى",
                descriptionFr = "Consolider vos révisions matinales du jour",
                descriptionEn = "Review overdue and due verses in the morning",
                stepType = RoutineStepType.REVIEW_OVERDUE,
                targetSurah = morningSurah,
                targetAyah = morningAyah,
                isCompleted = morningDone,
                initialTrainingStep = null
            ),
            DailyRoutineStep(
                period = DailyRoutinePeriod.DAYTIME,
                titleAr = "تعلم آيات جديدة",
                titleFr = "Apprentissage des nouveaux versets",
                titleEn = "Learn new verses",
                descriptionAr = "حفظ الآيات المقررة حسب وردك اليومي ($dailyTarget آيات)",
                descriptionFr = "Apprendre les nouveaux versets de votre objectif ($dailyTarget / j)",
                descriptionEn = "Memorize new verses towards daily target ($dailyTarget / day)",
                stepType = RoutineStepType.LEARN_NEW,
                targetSurah = daytimeSurah,
                targetAyah = daytimeAyah,
                isCompleted = daytimeDone,
                initialTrainingStep = null
            ),
            DailyRoutineStep(
                period = DailyRoutinePeriod.EVENING,
                titleAr = "اختبار الغيب بدون نظر",
                titleFr = "Validation à l'aveugle",
                titleEn = "Blind test validation",
                descriptionAr = "تسميع الآيات دون النظر في المصحف لاختبار قوة الحفظ",
                descriptionFr = "Réciter sans regarder pour valider l'ancrage",
                descriptionEn = "Recite without looking to test recall",
                stepType = RoutineStepType.BLIND_TEST,
                targetSurah = eveningSurah,
                targetAyah = eveningAyah,
                isCompleted = eveningDone,
                initialTrainingStep = "BLIND_TEST"
            ),
            DailyRoutineStep(
                period = DailyRoutinePeriod.BEDTIME,
                titleAr = "جلسة تثبيت قبل النوم",
                titleFr = "Révision d'ancrage du soir",
                titleEn = "Bedtime anchoring review",
                descriptionAr = "تلاوة هادئة لترسيخ ما حفظته اليوم في الذاكرة الدائمة",
                descriptionFr = "Courte révision calme pour ancrer la mémoire",
                descriptionEn = "Short quiet review to cement today's memorization",
                stepType = RoutineStepType.NIGHT_REVISION,
                targetSurah = bedtimeSurah,
                targetAyah = bedtimeAyah,
                isCompleted = bedtimeDone,
                initialTrainingStep = null
            )
        )

        // Quick start selection: prefer uncompleted step matching current period, or first uncompleted step
        val stepForPeriod = steps.find { it.period == currentPeriod }
        val quickStart = if (stepForPeriod != null && !stepForPeriod.isCompleted) {
            stepForPeriod
        } else {
            steps.firstOrNull { !it.isCompleted } ?: steps.first()
        }

        val completedCount = steps.count { it.isCompleted }
        val completionFraction = completedCount.toFloat() / steps.size.toFloat()
        val allCompleted = completedCount == steps.size

        val (nextAr, nextFr, nextEn) = when {
            allCompleted -> Triple(
                "ما شاء الله! أنجزت روتين اليوم كاملاً. تقبل الله طاعتكم.",
                "Félicitations ! Vous avez accompli toute votre routine du jour.",
                "Alhamdulillah! You completed your full daily routine today."
            )
            !morningDone -> Triple(
                "ابدأ بمراجعة الآيات المستحقة صباحاً لتثبيت حفظك.",
                "Commencez par réviser vos versets du matin.",
                "Start by reviewing your due verses this morning."
            )
            !daytimeDone -> Triple(
                "حان وقت حفظ الآيات الجديدة المحددة في خطتك اليومية.",
                "Passez à l'apprentissage de vos nouveaux versets du jour.",
                "Time to learn your new verses for today's goal."
            )
            !eveningDone -> Triple(
                "مساءً: اختبر حفظك غيباً للتأكد من رسوخ الآيات.",
                "Ce soir : validez votre mémorisation par le test à l'aveugle.",
                "This evening: test your recall with the blind test."
            )
            else -> Triple(
                "قبل النوم: مراجعة خفيفة لتثبيت الآيات قبل الراحة.",
                "Avant de dormir : révision calme d'ancrage nocturne.",
                "Before sleeping: short revision session to anchor memory."
            )
        }

        return DailyCompanionRoutine(
            dateEpochMillis = currentTimeMillis,
            currentPeriod = currentPeriod,
            steps = steps,
            quickStartStep = quickStart,
            completionFraction = completionFraction,
            allCompleted = allCompleted,
            nextActionSuggestionAr = nextAr,
            nextActionSuggestionFr = nextFr,
            nextActionSuggestionEn = nextEn
        )
    }

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
            if (entity.isOverdue(currentTimeMillis)) {
                dueList.add(entity)
            }
            if (entity.isWeak) {
                weakList.add(entity)
            }
            when (entity.memorizationStatus) {
                MemorizationStatus.MEMORIZED -> memorizedCount++
                MemorizationStatus.LEARNING -> {
                    learningCount++
                    learningList.add(entity)
                    surahActivityMap[entity.surahNumber] = (surahActivityMap[entity.surahNumber] ?: 0) + 1
                }
                MemorizationStatus.REVIEW -> {
                    reviewCount++
                    if (!entity.isWeak) {
                        weakList.add(entity)
                    }
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
