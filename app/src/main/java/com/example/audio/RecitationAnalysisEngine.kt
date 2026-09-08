package com.example.audio

import android.media.MediaMetadataRetriever
import com.example.data.repository.TajwidAnnotation
import com.example.data.repository.TajwidRepository
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pacing assessment of the recitation compared to standard Tartil tempo.
 */
enum class PacingAssessment {
    MEASURED_TARTIL,       // 0.8x - 1.4x of model: good measured pace
    TOO_FAST_HADR,         // < 0.8x: hurried, risk of dropping Madd or letters
    ELONGATED_OR_HESITANT  // > 1.4x: slow or long pauses
}

/**
 * Structured diagnostic produced by on-device acoustic and rhythm analysis.
 */
data class RecitationDiagnostic(
    val durationMs: Long,
    val referenceDurationMs: Long?,
    val pacingRatio: Float?,
    val pacingAssessment: PacingAssessment,
    val pauseCount: Int,
    val prolongedPausesCount: Int,
    val energyStabilityPercent: Int,
    val pacingMessageFr: String,
    val pacingMessageAr: String,
    val pacingMessageEn: String = "",
    val pauseMessageFr: String,
    val pauseMessageAr: String = "",
    val pauseMessageEn: String = "",
    val stabilityMessageFr: String,
    val stabilityMessageAr: String = "",
    val stabilityMessageEn: String = "",
    val tajwidPointsToWatch: List<TajwidAnnotation>,
    val pedagogicalDisclaimerFr: String = "Cette analyse acoustique assistée est un repère indicatif pour votre entraînement personnel et ne remplace pas l'écoute d'un enseignant qualifié (Cheikh / Mouqri).",
    val pedagogicalDisclaimerAr: String = "هذا التحليل الصوتي والإيقاعي وسيلة تدريبية استرشادية ولا يغني عن التلقي والمشافهة على يد شيخ متقن.",
    val pedagogicalDisclaimerEn: String = "This assistive acoustic analysis is an indicative reference for personal practice and does not replace evaluation by a qualified Quran teacher."
)

object RecitationAnalysisEngine {

    /**
     * Estimated baseline Tartil recitation duration per Arabic character in milliseconds.
     * In traditional Tartil (medium pace, e.g. Sheikh Al-Husary / Alafasy), an ayah with
     * ~40 characters takes approx 7-9 seconds (~200ms per character).
     */
    private const val MS_PER_CHAR_ESTIMATE = 220L

    /**
     * Analyzes a recorded audio file for a given ayah.
     *
     * @param audioFile The recorded audio file.
     * @param surahNumber Surah number.
     * @param ayahNumber Ayah number.
     * @param ayahText Arabic text of the ayah (used for baseline duration comparison).
     * @param recordedDbSamples Optional list of dB amplitude samples gathered during recording.
     */
    fun analyzeRecitation(
        audioFile: File?,
        surahNumber: Int,
        ayahNumber: Int,
        ayahText: String,
        recordedDbSamples: List<Float> = emptyList()
    ): RecitationDiagnostic {
        val durationMs = if (audioFile != null && audioFile.exists()) {
            extractDurationMs(audioFile)
        } else {
            0L
        }

        val charCount = ayahText.filter { !it.isWhitespace() }.length.coerceAtLeast(1)
        val estimatedReferenceDurationMs = (charCount * MS_PER_CHAR_ESTIMATE).coerceAtLeast(2000L)

        val ratio = if (durationMs > 0) {
            durationMs.toFloat() / estimatedReferenceDurationMs.toFloat()
        } else {
            1.0f
        }

        val pacingAssessment = when {
            ratio < 0.75f -> PacingAssessment.TOO_FAST_HADR
            ratio > 1.45f -> PacingAssessment.ELONGATED_OR_HESITANT
            else -> PacingAssessment.MEASURED_TARTIL
        }

        val pacingMessageFr = when (pacingAssessment) {
            PacingAssessment.MEASURED_TARTIL ->
                "Tempo mesuré et régulier : Le rythme correspond aux canons du Tartil modéré."
            PacingAssessment.TOO_FAST_HADR ->
                "Tempo rapide (Hadr) : Prenez le temps d'accorder aux prolongations (Madd) et nasalisations (Ghunnah) leur pleine mesure."
            PacingAssessment.ELONGATED_OR_HESITANT ->
                "Tempo prolongé : Assurez la fluidité des enchaînements et évitez les hésitations entre les mots."
        }

        val pacingMessageAr = when (pacingAssessment) {
            PacingAssessment.MEASURED_TARTIL -> "إيقاع ترتيل متزن ومضبوط وفق أصول الترتيل المعتدل."
            PacingAssessment.TOO_FAST_HADR -> "إيقاع حدر سريع - يُرجى إعطاء المدود والغنن حقها من الزمن."
            PacingAssessment.ELONGATED_OR_HESITANT -> "إيقاع بطيء أو متردد - احرص على سلاسة الوصل وتجنب التردد."
        }

        val pacingMessageEn = when (pacingAssessment) {
            PacingAssessment.MEASURED_TARTIL -> "Measured and steady tempo: Pacing matches moderate Tartil standards."
            PacingAssessment.TOO_FAST_HADR -> "Fast tempo (Hadr): Allow full timing for elongations (Madd) and ghunnah."
            PacingAssessment.ELONGATED_OR_HESITANT -> "Elongated tempo: Maintain smooth word transitions and avoid hesitations."
        }

        // Pause analysis from amplitude samples if available, or estimated
        val (pauseCount, prolongedCount) = if (recordedDbSamples.isNotEmpty()) {
            detectPauses(recordedDbSamples)
        } else {
            Pair(0, 0)
        }

        val pauseMessageFr = when {
            pauseCount == 0 -> "Débit continu sans interruption majeure observée."
            prolongedCount > 1 -> "$pauseCount pause(s) observée(s), dont $prolongedCount pause(s) prolongée(s). Veillez à ne vous arrêter que sur les arrêts autorisés (Waqf)."
            else -> "$pauseCount pause(s) observée(s), compatible avec la respiration naturelle."
        }

        val pauseMessageAr = when {
            pauseCount == 0 -> "تلاوة متواصلة بدون توقفات ملحوظة."
            prolongedCount > 1 -> "تمت ملاحظة $pauseCount وقفات، منها $prolongedCount وقفات طويلة. احرص على الوقف الحسن."
            else -> "تمت ملاحظة $pauseCount وقفات متناسقة مع التنفس الطبيعي."
        }

        val pauseMessageEn = when {
            pauseCount == 0 -> "Continuous recitation without significant interruptions observed."
            prolongedCount > 1 -> "$pauseCount pause(s) observed, including $prolongedCount extended pause(s). Ensure stopping only at permitted Waqf."
            else -> "$pauseCount pause(s) observed, compatible with natural breath."
        }

        // Energy stability calculation
        val stabilityPercent = if (recordedDbSamples.size >= 4) {
            calculateStability(recordedDbSamples)
        } else {
            85 // Default healthy baseline
        }

        val stabilityMessageFr = if (stabilityPercent >= 75) {
            "Soutien du souffle régulier et projection vocale stable."
        } else {
            "Fluctuations d'intensité vocale observées : maintenez un appui constant sur le souffle."
        }

        val stabilityMessageAr = if (stabilityPercent >= 75) {
            "دعم تنفسي منتظم ونبرة صوتية مستقرة."
        } else {
            "تذبذب في شدة الصوت: حافظ على استقرار تدفق النفس."
        }

        val stabilityMessageEn = if (stabilityPercent >= 75) {
            "Steady breath support and stable vocal projection."
        } else {
            "Vocal intensity fluctuations observed: maintain steady breath support."
        }

        // Lookup classical Tajwid points for this verse
        val key = "$surahNumber:$ayahNumber"
        val tajwidPoints = TajwidRepository.AYAH_ANNOTATIONS[key] ?: emptyList()

        return RecitationDiagnostic(
            durationMs = durationMs,
            referenceDurationMs = estimatedReferenceDurationMs,
            pacingRatio = ratio,
            pacingAssessment = pacingAssessment,
            pauseCount = pauseCount,
            prolongedPausesCount = prolongedCount,
            energyStabilityPercent = stabilityPercent,
            pacingMessageFr = pacingMessageFr,
            pacingMessageAr = pacingMessageAr,
            pacingMessageEn = pacingMessageEn,
            pauseMessageFr = pauseMessageFr,
            pauseMessageAr = pauseMessageAr,
            pauseMessageEn = pauseMessageEn,
            stabilityMessageFr = stabilityMessageFr,
            stabilityMessageAr = stabilityMessageAr,
            stabilityMessageEn = stabilityMessageEn,
            tajwidPointsToWatch = tajwidPoints
        )
    }

    private fun extractDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            timeStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Detects silence periods where dB is below -45dB.
     * Each sample represents ~250ms (interval in QuranRecorderEngine).
     */
    private fun detectPauses(samples: List<Float>): Pair<Int, Int> {
        val silenceThresholdDb = -42.0f
        var currentSilenceCount = 0
        var totalPauses = 0
        var prolongedPauses = 0

        for (db in samples) {
            if (db < silenceThresholdDb) {
                currentSilenceCount++
            } else {
                if (currentSilenceCount >= 3) { // >= 750ms
                    totalPauses++
                    if (currentSilenceCount >= 7) { // >= 1750ms
                        prolongedPauses++
                    }
                }
                currentSilenceCount = 0
            }
        }

        if (currentSilenceCount >= 3) {
            totalPauses++
            if (currentSilenceCount >= 7) prolongedPauses++
        }

        return Pair(totalPauses, prolongedPauses)
    }

    /**
     * Computes consistency percentage based on RMS deviation of active sound samples.
     */
    private fun calculateStability(samples: List<Float>): Int {
        val activeSamples = samples.filter { it > -45.0f }
        if (activeSamples.size < 4) return 80

        val mean = activeSamples.average()
        val variance = activeSamples.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        // Lower std dev means more stable projection. Range ~3 to 15 dB
        val score = (100 - (stdDev * 4)).toInt().coerceIn(50, 98)
        return score
    }
}
