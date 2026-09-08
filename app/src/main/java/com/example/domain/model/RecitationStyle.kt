package com.example.domain.model

enum class RecitationStyle(
    val displayName: String,
    val everyAyahFolder: String,
    val fallbackFolder: String = "Abdul_Basit_Murattal_64kbps"
) {
    TARTIL("Tartil", "Abdul_Basit_Murattal_192kbps"),
    TAJWID("Tajwid", "Abdul_Basit_Mujawwad_128kbps");

    fun buildAudioUrl(surahNumber: Int, ayahNumber: Int): String {
        val surahPart = surahNumber.toString().padStart(3, '0')
        val ayahPart = ayahNumber.toString().padStart(3, '0')
        return "https://everyayah.com/data/$everyAyahFolder/$surahPart$ayahPart.mp3"
    }

    fun buildFallbackAudioUrl(surahNumber: Int, ayahNumber: Int): String {
        val surahPart = surahNumber.toString().padStart(3, '0')
        val ayahPart = ayahNumber.toString().padStart(3, '0')
        return "https://everyayah.com/data/$fallbackFolder/$surahPart$ayahPart.mp3"
    }
}
