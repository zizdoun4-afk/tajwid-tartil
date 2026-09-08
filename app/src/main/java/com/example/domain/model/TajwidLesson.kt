package com.example.domain.model

enum class TajwidLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

enum class TajwidRuleCategory {
    MAKHAREJ,
    VOWELS_SUKUN,
    NOON_SAKINAH,
    MEEM_SAKINAH,
    QALQALAH,
    MADD,
    TAFKHIM_TARQIQ,
    WAQF
}

data class TajwidExample(
    val arabicSnippet: String,
    val highlightedPart: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val explanationFr: String,
    val explanationAr: String
) {
    fun getAudioUrl(): String {
        return "https://cdn.islamic.network/quran/audio/128/ar.alafasy/${calculateGlobalAyahNumber(surahNumber, ayahNumber)}.mp3"
    }

    private fun calculateGlobalAyahNumber(surah: Int, ayah: Int): Int {
        val surahAyahCounts = listOf(
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
            123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
            112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
            34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
            54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
            60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
            14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
            28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
            29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
            15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
            11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
            5, 4, 5, 6
        )
        var globalIndex = 0
        for (i in 0 until (surah - 1).coerceAtMost(surahAyahCounts.size)) {
            globalIndex += surahAyahCounts[i]
        }
        return globalIndex + ayah
    }
}

data class TajwidRule(
    val id: String,
    val category: TajwidRuleCategory,
    val nameFr: String,
    val nameAr: String,
    val summaryFr: String,
    val summaryAr: String,
    val detailedExplanationFr: String,
    val detailedExplanationAr: String,
    val examples: List<TajwidExample>
)

data class TajwidExercise(
    val id: String,
    val questionFr: String,
    val questionAr: String,
    val ayahSnippet: String,
    val targetHighlight: String,
    val optionsFr: List<String>,
    val optionsAr: List<String>,
    val correctOptionIndex: Int,
    val explanationFr: String,
    val explanationAr: String
)

data class TajwidLesson(
    val id: String,
    val level: TajwidLevel,
    val titleFr: String,
    val titleAr: String,
    val descriptionFr: String,
    val descriptionAr: String,
    val rules: List<TajwidRule>,
    val exercises: List<TajwidExercise>
)
