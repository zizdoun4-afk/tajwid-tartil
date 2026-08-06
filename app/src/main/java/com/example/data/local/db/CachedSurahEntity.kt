package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Ayah
import com.example.domain.model.Surah

@Entity(tableName = "cached_surahs")
data class CachedSurahEntity(
    @PrimaryKey val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String
) {
    fun toDomain(): Surah = Surah(
        number = number,
        name = name,
        englishName = englishName,
        englishNameTranslation = englishNameTranslation,
        numberOfAyahs = numberOfAyahs,
        revelationType = revelationType
    )
}

@Entity(tableName = "cached_ayahs")
data class CachedAyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int
) {
    fun toDomain(): Ayah = Ayah(
        number = number,
        text = text,
        numberInSurah = numberInSurah,
        juz = juz,
        page = page
    )
}
