package com.example.data.repository

import com.example.data.local.db.TajwidProgressDao
import com.example.data.local.db.TajwidProgressEntity
import com.example.domain.model.TajwidExample
import com.example.domain.model.TajwidExercise
import com.example.domain.model.TajwidLesson
import com.example.domain.model.TajwidLevel
import com.example.domain.model.TajwidRule
import com.example.domain.model.TajwidRuleCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class TajwidAnnotation(
    val ruleNameFr: String,
    val ruleNameAr: String,
    val targetSnippet: String,
    val explanationFr: String,
    val explanationAr: String
)

class TajwidRepository(
    private val tajwidProgressDao: TajwidProgressDao
) {

    fun getAllLessons(): List<TajwidLesson> = LESSONS

    fun getLessonsByLevel(level: TajwidLevel): List<TajwidLesson> = LESSONS.filter { it.level == level }

    fun getLessonById(lessonId: String): TajwidLesson? = LESSONS.find { it.id == lessonId }

    fun getProgressFlow(): Flow<List<TajwidProgressEntity>> = tajwidProgressDao.getAllProgressFlow()

    fun getCompletedLessonsCountFlow(): Flow<Int> = tajwidProgressDao.getCompletedLessonsCountFlow()

    fun getProgressForLessonFlow(lessonId: String): Flow<TajwidProgressEntity?> = tajwidProgressDao.getProgressForLessonFlow(lessonId)

    suspend fun markLessonCompleted(lessonId: String, completedExercisesCount: Int) = withContext(Dispatchers.IO) {
        val entity = TajwidProgressEntity(
            lessonId = lessonId,
            completedExercisesCount = completedExercisesCount,
            isCompleted = true,
            lastCompletedAtEpochMillis = System.currentTimeMillis()
        )
        tajwidProgressDao.upsertProgress(entity)
    }

    fun getTajwidAnnotationsForAyah(surahNumber: Int, ayahNumber: Int): List<TajwidAnnotation> {
        return AYAH_ANNOTATIONS["$surahNumber:$ayahNumber"] ?: emptyList()
    }

    companion object {
        val LESSONS = listOf(
            // BEGINNER
            TajwidLesson(
                id = "makharij_basics",
                level = TajwidLevel.BEGINNER,
                titleFr = "Les cinq sorties des lettres (Makhârij)",
                titleAr = "مخارج الحروف الرئيسية",
                descriptionFr = "Découvrez les 5 zones d'articulation phonétique pour prononcer chaque lettre coranique avec précision.",
                descriptionAr = "التعرف على مناطق النطق الخمس الرئيسية لخروج الحروف القرآنية بدقة وإتقان.",
                rules = listOf(
                    TajwidRule(
                        id = "makharij_5",
                        category = TajwidRuleCategory.MAKHAREJ,
                        nameFr = "Les 5 grands points d'articulation",
                        nameAr = "المخارج العامة الخمسة",
                        summaryFr = "Al-Jawf (le vide), Al-Halq (la gorge), Al-Lisan (la langue), Ash-Shafatan (les lèvres), Al-Khayshum (le nez).",
                        summaryAr = "الجوف، الحلق، اللسان، الشفتان، والخيشوم.",
                        detailedExplanationFr = "Chaque lettre de l'alphabet arabe possède un point d'impact précis dans l'appareil phonatoire. Respecter ces sorties est le fondement du Tajwid.",
                        detailedExplanationAr = "لكل حرف قرآني نقطة ارتكاز محددة في جهاز النطق. معرفة المخارج وضبطها هو الركيزة الأولى في علم التجويد.",
                        examples = listOf(
                            TajwidExample(
                                arabicSnippet = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ",
                                highlightedPart = "ح",
                                surahNumber = 1,
                                ayahNumber = 2,
                                explanationFr = "Le Hâ (ح) sort du milieu de la gorge (وسط الحلق).",
                                explanationAr = "حرف الحاء يخرج من وسط الحلق."
                            ),
                            TajwidExample(
                                arabicSnippet = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
                                highlightedPart = "ق",
                                surahNumber = 112,
                                ayahNumber = 1,
                                explanationFr = "Le Qâf (ق) sort du fond de la langue touchant le palais mou (أقصى اللسان).",
                                explanationAr = "حرف القاف يخرج من أقصى اللسان مع ما يحاذيه من الحنك الأعلى."
                            )
                        )
                    )
                ),
                exercises = listOf(
                    TajwidExercise(
                        id = "ex_makharij_1",
                        questionFr = "D'où sort la lettre Ha (ح) dans « ٱلْحَمْدُ » ?",
                        questionAr = "من أين يخرج حرف الحاء في كلمة « ٱلْحَمْدُ » ؟",
                        ayahSnippet = "ٱلْحَمْدُ لِلَّهِ",
                        targetHighlight = "ح",
                        optionsFr = listOf("Du milieu de la gorge", "Des lèvres", "Du bout de la langue"),
                        optionsAr = listOf("من وسط الحلق", "من الشفتين", "من طرف اللسان"),
                        correctOptionIndex = 0,
                        explanationFr = "Le Ha (ح) sort du milieu de la gorge (وسط الحلق).",
                        explanationAr = "حرف الحاء مخرجه وسط الحلق."
                    )
                )
            ),

            TajwidLesson(
                id = "noon_sakinah_overview",
                level = TajwidLevel.BEGINNER,
                titleFr = "Nûn Sâkinah et Tanwîn (Vue d'ensemble)",
                titleAr = "أحكام النون الساكنة والتنوين",
                descriptionFr = "Les 4 règles fondamentales appliquées au Nûn sans voyelle (نْ) ou au Tanwîn.",
                descriptionAr = "القواعد الأربع الكبرى عند تلاقي النون الساكنة أو التنوين بحروف الهجاء.",
                rules = listOf(
                    TajwidRule(
                        id = "noon_rules_4",
                        category = TajwidRuleCategory.NOON_SAKINAH,
                        nameFr = "Les 4 règles du Nûn Sâkin",
                        nameAr = "الأحكام الأربعة",
                        summaryFr = "Izhar (Clarté), Idgham (Fusion), Iqlab (Transformation), Ikhfa (Dissimulation).",
                        summaryAr = "الإظهار، الإدغام، الإقلاب، والإخفاء.",
                        detailedExplanationFr = "Selon la lettre qui suit le Nûn sans voyelle ou le Tanwîn, la prononciation change complètement pour assurer la fluidité de la récitation.",
                        detailedExplanationAr = "حسب الحرف التالي للنون الساكنة أو التنوين، يتحدد الحكم المناسب من إظهار أو إدغام أو إقلاب أو إخفاء.",
                        examples = listOf(
                            TajwidExample(
                                arabicSnippet = "مِنْ خَوْفٍ",
                                highlightedPart = "مِنْ خَ",
                                surahNumber = 106,
                                ayahNumber = 4,
                                explanationFr = "Izhar : Nûn prononcé distinctement devant Khâ (خ).",
                                explanationAr = "إظهار حلقي: نون ساكنة بعدها حرف الخاء."
                            )
                        )
                    )
                ),
                exercises = listOf(
                    TajwidExercise(
                        id = "ex_noon_1",
                        questionFr = "Combien de règles gouvernent le Nûn Sâkinah et le Tanwîn ?",
                        questionAr = "كم عدد أحكام النون الساكنة والتنوين ؟",
                        ayahSnippet = "مِنْ خَوْفٍ",
                        targetHighlight = "نْ",
                        optionsFr = listOf("4 règles", "2 règles", "6 règles"),
                        optionsAr = listOf("أربعة أحكام", "حكمان", "ستة أحكام"),
                        correctOptionIndex = 0,
                        explanationFr = "Il y a 4 règles : Izhar, Idgham, Iqlab et Ikhfa.",
                        explanationAr = "الأحكام أربعة: الإظهار، الإدغام، الإقلاب، الإخفاء."
                    )
                )
            ),

            // INTERMEDIATE
            TajwidLesson(
                id = "izhar_halqi",
                level = TajwidLevel.INTERMEDIATE,
                titleFr = "L'Izhâr Halqî (La Clarté)",
                titleAr = "الإظهار الحلقي",
                descriptionFr = "Prononciation claire et nette du Nûn Sâkin devant les 6 lettres de la gorge (ء هـ ع ح غ خ).",
                descriptionAr = "بيان النون الساكنة والتنوين ونطقها بوضوح دون غنة زائدة عند حروف الحلق الستة.",
                rules = listOf(
                    TajwidRule(
                        id = "rule_izhar",
                        category = TajwidRuleCategory.NOON_SAKINAH,
                        nameFr = "Règle de l'Izhâr Halqî",
                        nameAr = "حكم الإظهار الحلقي",
                        summaryFr = "Lettres : Hamza (ء), Hâ (هـ), 'Ayn (ع), Hâ (ح), Ghayn (غ), Khâ (خ).",
                        summaryAr = "حروفه ستة: همز، فهاء، ثم عين، حاء، مهملتان، ثم غين، خاء.",
                        detailedExplanationFr = "Lorsque le Nûn Sâkinah ou le Tanwîn est suivi par une des six lettres de la gorge, le son du Nûn est prononcé sans dissimulation et sans allongement de grésillement nasal (Ghunnah).",
                        detailedExplanationAr = "إذا وقع بعد النون الساكنة أو التنوين أحد حروف الحلق الستة وجب إظهار النون نطقاً صريحاً دون غنة مفرطة.",
                        examples = listOf(
                            TajwidExample(
                                arabicSnippet = "أَنْعَمْتَ عَلَيْهِمْ",
                                highlightedPart = "أَنْعَ",
                                surahNumber = 1,
                                ayahNumber = 7,
                                explanationFr = "Nûn sâkinah devant 'Ayn (ع) : Izhâr clair.",
                                explanationAr = "نون ساكنة بعدها عين في كلمة واحدة: إظهار حلقي."
                            ),
                            TajwidExample(
                                arabicSnippet = "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ",
                                highlightedPart = "قٍ إِ",
                                surahNumber = 113,
                                ayahNumber = 3,
                                explanationFr = "Tanwîn devant Hamza (إ) : Izhâr clair.",
                                explanationAr = "تنوين بعده همزة: إظهار حلقي."
                            )
                        )
                    )
                ),
                exercises = listOf(
                    TajwidExercise(
                        id = "ex_izhar_1",
                        questionFr = "Quel est le حكم (règle) dans « أَنْعَمْتَ » ?",
                        questionAr = "ما هو الحكم التجويدي في « أَنْعَمْتَ » ؟",
                        ayahSnippet = "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ",
                        targetHighlight = "أَنْعَ",
                        optionsFr = listOf("Izhâr Halqî", "Idgham", "Ikhfa"),
                        optionsAr = listOf("إظهار حلقي", "إدغام", "إخفاء"),
                        correctOptionIndex = 0,
                        explanationFr = "Il s'agit d'un Izhâr Halqî car le Nûn est suivi de la lettre 'Ayn (ع).",
                        explanationAr = "إظهار حلقي لوقوع حرف العين بعد النون الساكنة."
                    )
                )
            ),

            TajwidLesson(
                id = "idgham_rules",
                level = TajwidLevel.INTERMEDIATE,
                titleFr = "L'Idghâm (La Fusion)",
                titleAr = "الإدغام بغنة وبغير غنة",
                descriptionFr = "Fusion du Nûn Sâkin ou Tanwîn dans la lettre suivante (lettres ي ر م ل و ن réparties en غنة et sans غنة).",
                descriptionAr = "إدخال النون الساكنة أو التنوين في الحرف الذي يليها من حروف (يرملون).",
                rules = listOf(
                    TajwidRule(
                        id = "rule_idgham_ghunnah",
                        category = TajwidRuleCategory.NOON_SAKINAH,
                        nameFr = "Idghâm avec Ghunnah (ي ن م و)",
                        nameAr = "الإدغام بغنة (ينمو)",
                        summaryFr = "Fusion avec nasalisation pendant 2 temps devant Ya, Noon, Meem, Waw.",
                        summaryAr = "إدغام النون مع بقاء صفة الغنة بمقدار حركتين عند حروف (ينمو).",
                        detailedExplanationFr = "Le Nûn disparaît dans la lettre suivante tout en conservant une résonance nasale (Ghunnah) de 2 temps.",
                        detailedExplanationAr = "تدغم النون في الحرف التالي مع نطق غنة رنانة من الخيشوم بمقدار حركتين.",
                        examples = listOf(
                            TajwidExample(
                                arabicSnippet = "فَمَن يَعْمَلْ مِثْقَالَ ذَرَّةٍ خَيْرًا يَرَهُۥ",
                                highlightedPart = "مَن يَعْمَلْ",
                                surahNumber = 99,
                                ayahNumber = 7,
                                explanationFr = "Idghâm avec Ghunnah : « ma-yya'mal ».",
                                explanationAr = "إدغام بغنة: نون ساكنة بعدها ياء."
                            )
                        )
                    ),
                    TajwidRule(
                        id = "rule_idgham_no_ghunnah",
                        category = TajwidRuleCategory.NOON_SAKINAH,
                        nameFr = "Idghâm sans Ghunnah (ل ر)",
                        nameAr = "الإدغام بغير غنة (ل، ر)",
                        summaryFr = "Fusion totale sans nasalisation devant Lâm et Râ.",
                        summaryAr = "إدغام كامل للنون والتنوين دون غنة عند حرفي اللام والراء.",
                        detailedExplanationFr = "Le Nûn est complètement absorbé sans aucun résidu nasal.",
                        detailedExplanationAr = "إدغام كامل لا يتبقى معه أي أثر للغنة إطلاقاً.",
                        examples = listOf(
                            TajwidExample(
                                arabicSnippet = "مِن لَّدُنْهُ",
                                highlightedPart = "مِن لَّ",
                                surahNumber = 18,
                                ayahNumber = 2,
                                explanationFr = "Prononcé « milladunhu » directement sans nasalité.",
                                explanationAr = "إدغام بغير غنة: تنطق ميـلَّدنه."
                            )
                        )
                    )
                ),
                exercises = listOf(
                    TajwidExercise(
                        id = "ex_idgham_1",
                        questionFr = "Quelle règle s'applique dans « فَمَن يَعْمَلْ » ?",
                        questionAr = "ما هو الحكم التجويدي في « فَمَن يَعْمَلْ » ؟",
                        ayahSnippet = "فَمَن يَعْمَلْ مِثْقَالَ ذَرَّةٍ",
                        targetHighlight = "مَن يَعْمَلْ",
                        optionsFr = listOf("Idghâm avec Ghunnah", "Idghâm sans Ghunnah", "Izhâr"),
                        optionsAr = listOf("إدغام بغنة", "إدغام بغير غنة", "إظهار"),
                        correctOptionIndex = 0,
                        explanationFr = "Le Nûn est suivi de Yâ (ي), lettre du mot (يَنْمُو), donc Idghâm avec Ghunnah.",
                        explanationAr = "إدغام بغنة لوقوع حرف الياء بعد النون الساكنة."
                    )
                )
            ),

            TajwidLesson(
                id = "qalqalah_rules",
                level = TajwidLevel.INTERMEDIATE,
                titleFr = "La Qalqalah (L'ébranlement)",
                titleAr = "أحكام القلقلة (قطب جد)",
                descriptionFr = "Le rebond sonore sur les 5 lettres (ق ط ب ج د) lorsqu'elles portent un Sukun ou lors d'un arrêt.",
                descriptionAr = "اضطراب الصوت عند النطق بأحد حروف (قطب جد) ساكناً حتى يُسمع له نبرة قوية.",
                rules = listOf(
                    TajwidRule(
                        id = "rule_qalqalah",
                        category = TajwidRuleCategory.QALQALAH,
                        nameFr = "Les 5 lettres de la Qalqalah (ق، ط، ب، ج، د)",
                        nameAr = "حروف القلقلة الخمسة (قُطْبُ جَدّ)",
                        summaryFr = "Qalqalah Sughra (mineure au milieu du mot) et Kubra (majeure à l'arrêt).",
                        summaryAr = "قلقلة صغرى في وسط الكلمة، وقلقلة كبرى عند الوقف.",
                        detailedExplanationFr = "Lorsque l'une de ces 5 lettres est dépourvue de voyelle, elle exige une vibration franche pour libérer la pression articulatoire.",
                        detailedExplanationAr = "هذه الحروف الخمسة شديدة مجهورة تنحبس معها الأصوات، فتتطلب القلقلة لفك احتباس المخرج.",
                        examples = listOf(
                            TajwidExample(
                                arabicSnippet = "قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ",
                                highlightedPart = "ٱلْفَلَقِ",
                                surahNumber = 113,
                                ayahNumber = 1,
                                explanationFr = "Qalqalah Kubra sur le Qâf à l'arrêt.",
                                explanationAr = "قلقلة كبرى عند الوقف على حرف القاف بالسكون."
                            ),
                            TajwidExample(
                                arabicSnippet = "تَبَّتْ يَدَآ أَبِى لَهَبٍ وَتَبَّ",
                                highlightedPart = "وَتَبَّ",
                                surahNumber = 111,
                                ayahNumber = 1,
                                explanationFr = "Qalqalah Kubra accentuée sur le Bâ doublé (Shaddah).",
                                explanationAr = "قلقلة كبرى مشددة عند الوقف على الباء."
                            )
                        )
                    )
                ),
                exercises = listOf(
                    TajwidExercise(
                        id = "ex_qalqalah_1",
                        questionFr = "Quelles sont les 5 lettres de la Qalqalah ?",
                        questionAr = "ما هي حروف القلقلة المجموعة في جملة ؟",
                        ayahSnippet = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
                        targetHighlight = "أَحَدٌ",
                        optionsFr = listOf("ق ط ب ج د (Qutbu Jadd)", "ي ن م و (Yanmou)", "ء هـ ع ح غ خ"),
                        optionsAr = listOf("ق، ط، ب، ج، د (قُطب جَد)", "ي، ن، م، و (ينمو)", "حروف الحلق"),
                        correctOptionIndex = 0,
                        explanationFr = "Les 5 lettres de la Qalqalah sont regroupées dans la formule « Qutbu Jadd » (قُطب جَد).",
                        explanationAr = "حروف القلقلة خمسة تجمعها عبارة (قُطب جَد)."
                    )
                )
            ),

            // ADVANCED
            TajwidLesson(
                id = "madd_categories",
                level = TajwidLevel.ADVANCED,
                titleFr = "Les Catégories de Madd (Prolongations)",
                titleAr = "أقسام المدود وأحكامها",
                descriptionFr = "Madd Muttasil, Munfasil, 'Arid lis-Sukun et Lazim : durées et conditions d'allongement.",
                descriptionAr = "تفصيل مقادير المدود من المد المتصل والمنفصل والعارض للسكون والمد اللازم.",
                rules = listOf(
                    TajwidRule(
                        id = "rule_madd_types",
                        category = TajwidRuleCategory.MADD,
                        nameFr = "Madd Muttasil & Munfasil",
                        nameAr = "المد المتصل والمنفصل",
                        summaryFr = "Muttasil (4-5 temps dans un même mot), Munfasil (4-5 temps entre 2 mots).",
                        summaryAr = "المد الواجب المتصل، والمد الجائز المنفصل بمقدار 4 أو 5 حركات.",
                        detailedExplanationFr = "Lorsque la lettre de prolongation est suivie d'une Hamza, elle est allongée de 4 à 5 temps. Si les deux sont dans le même mot, le Madd est obligatoire (Muttasil).",
                        detailedExplanationAr = "إذا جاء بعد حرف المد همز، يمد بمقدار 4-5 حركات، فإن كانا في كلمة واحدة فهو متصل واجب.",
                        examples = listOf(
                            TajwidExample(
                                arabicSnippet = "إِذَا جَآءَ نَصْرُ ٱللَّهِ وَٱلْفَتْحُ",
                                highlightedPart = "جَآءَ",
                                surahNumber = 110,
                                ayahNumber = 1,
                                explanationFr = "Madd Muttasil : 4 à 5 temps sur « Jâ'a ».",
                                explanationAr = "مد متصل واجب بمقدار 4-5 حركات لوقوع الهمز بعد حرف المد في كلمة واحدة."
                            )
                        )
                    )
                ),
                exercises = listOf(
                    TajwidExercise(
                        id = "ex_madd_1",
                        questionFr = "Quel type de Madd trouve-t-on dans « جَآءَ » ?",
                        questionAr = "ما نوع المد في كلمة « جَآءَ » ؟",
                        ayahSnippet = "إِذَا جَآءَ نَصْرُ ٱللَّهِ",
                        targetHighlight = "جَآءَ",
                        optionsFr = listOf("Madd Muttasil (Obligatoire)", "Madd Munfasil (Permis)", "Madd Tabi'i (2 temps)"),
                        optionsAr = listOf("مد متصل واجب", "مد منفصل جائز", "مد طبيعي"),
                        correctOptionIndex = 0,
                        explanationFr = "C'est un Madd Muttasil car la lettre de Madd et la Hamza sont dans le même mot.",
                        explanationAr = "مد واجب متصل لاجتماع حرف المد والهمز في نفس الكلمة."
                    )
                )
            ),

            TajwidLesson(
                id = "tafkhim_tarqiq",
                level = TajwidLevel.ADVANCED,
                titleFr = "Tafkhîm & Tarqîq (Emphase & Amincissement)",
                titleAr = "أحكام التفخيم والترقيق",
                descriptionFr = "Règles des lettres emphatiques (خ ص ض غ ط ق ظ) et cas particuliers du Râ et du Lâm de majesté.",
                descriptionAr = "حروف الاستعلاء المفخمة دائماً وأحكام الراء ولام لفظ الجلالة ترقيقاً وتفخيماً.",
                rules = listOf(
                    TajwidRule(
                        id = "rule_tafkhim",
                        category = TajwidRuleCategory.TAFKHIM_TARQIQ,
                        nameFr = "Lettres d'Isti'la (خُصَّ ضَغْطٍ قِظْ)",
                        nameAr = "حروف الاستعلاء السبعة",
                        summaryFr = "Toujours emphatiques avec le fond de la langue élevé vers le palais.",
                        summaryAr = "مفخمة دائماً في جميع الأحوال والدرجات.",
                        detailedExplanationFr = "Les 7 lettres d'Isti'la confèrent une sonorité pleine et grave qui ne doit jamais être amincie.",
                        detailedExplanationAr = "هذه الحروف السبعة تتصف باستعلاء أقصى اللسان وتفخيم الصوت بها عند النطق.",
                        examples = listOf(
                            TajwidExample(
                                arabicSnippet = "صِرَاطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ",
                                highlightedPart = "صِرَاطَ",
                                surahNumber = 1,
                                ayahNumber = 7,
                                explanationFr = "Sâd (ص) et Tâ (ط) sont hautement emphatiques.",
                                explanationAr = "حرفا الصاد والطاء من حروف الاستعلاء والإطباق المفخمة."
                            )
                        )
                    )
                ),
                exercises = listOf(
                    TajwidExercise(
                        id = "ex_tafkhim_1",
                        questionFr = "Laquelle de ces lettres est TOUJOURS emphatique (Tafkhîm) ?",
                        questionAr = "أي من هذه الحروف مفخم دائماً (من حروف الاستعلاء) ؟",
                        ayahSnippet = "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                        targetHighlight = "الصِّرَاطَ",
                        optionsFr = listOf("Le Sâd (ص)", "Le Sîn (س)", "Le Tâ (ت)"),
                        optionsAr = listOf("حرف الصاد", "حرف السين", "حرف التاء"),
                        correctOptionIndex = 0,
                        explanationFr = "Le Sâd (ص) fait partie des 7 lettres d'Isti'la (خُصَّ ضَغْطٍ قِظْ).",
                        explanationAr = "الصاد من حروف الاستعلاء السبعة المفخمة دائماً."
                    )
                )
            )
        )

        val AYAH_ANNOTATIONS = mapOf(
            "1:1" to listOf(
                TajwidAnnotation(
                    ruleNameFr = "Tarqîq du Lâm (ل)",
                    ruleNameAr = "ترقيق لام لفظ الجلالة",
                    targetSnippet = "بِسْمِ ٱللَّهِ",
                    explanationFr = "Le Lâm du nom divin est aminci (Tarqîq) car précédé d'une Kasrah.",
                    explanationAr = "ترقيق اللام من اسم الجلالة لكونها مسبوقة بكسر."
                )
            ),
            "1:2" to listOf(
                TajwidAnnotation(
                    ruleNameFr = "Makhraj du Hâ (ح)",
                    ruleNameAr = "مخرج الحاء",
                    targetSnippet = "ٱلْحَمْدُ",
                    explanationFr = "Le Hâ sort du milieu de la gorge (وسط الحلق) avec un souffle doux.",
                    explanationAr = "الحاء تخرج من وسط الحلق مع همس ورخاوة."
                )
            ),
            "1:7" to listOf(
                TajwidAnnotation(
                    ruleNameFr = "Izhâr Halqî",
                    ruleNameAr = "إظهار حلقي",
                    targetSnippet = "أَنْعَمْتَ",
                    explanationFr = "Nûn sâkinah suivi de 'Ayn (ع) : prononciation claire et distincte.",
                    explanationAr = "نون ساكنة بعدها حرف العين من حروف الحلق، فحكمها الإظهار."
                ),
                TajwidAnnotation(
                    ruleNameFr = "Tafkhîm (Emphase)",
                    ruleNameAr = "تفخيم الصاد والطاء",
                    targetSnippet = "صِرَاطَ",
                    explanationFr = "Le Sâd et le Tâ sont deux lettres d'Isti'la prononcées avec emphase.",
                    explanationAr = "الصاد والطاء من حروف الاستعلاء والإطباق المفخمة."
                )
            ),
            "112:1" to listOf(
                TajwidAnnotation(
                    ruleNameFr = "Qalqalah Kubra à l'arrêt",
                    ruleNameAr = "قلقلة كبرى عند الوقف",
                    targetSnippet = "أَحَدٌ",
                    explanationFr = "Rebond sonore marqué sur le Dâl (د) lors de l'arrêt.",
                    explanationAr = "قلقلة كبرى على حرف الدال عند الوقف عليه بالسكون."
                )
            ),
            "113:1" to listOf(
                TajwidAnnotation(
                    ruleNameFr = "Qalqalah Kubra",
                    ruleNameAr = "قلقلة كبرى",
                    targetSnippet = "ٱلْفَلَقِ",
                    explanationFr = "Vibration nette sur le Qâf (ق) lors de l'arrêt.",
                    explanationAr = "قلقلة كبرى على حرف القاف عند الوقف."
                )
            ),
            "114:1" to listOf(
                TajwidAnnotation(
                    ruleNameFr = "Ghunnah sur le Nûn doublé",
                    ruleNameAr = "غنة النون المشددة",
                    targetSnippet = "ٱلنَّاسِ",
                    explanationFr = "Nûn avec Shaddah (نّ) : prolongation de la nasalité pendant 2 temps complets.",
                    explanationAr = "النون المشددة تغن بمقدار حركتين كاملتين."
                )
            )
        )
    }
}
