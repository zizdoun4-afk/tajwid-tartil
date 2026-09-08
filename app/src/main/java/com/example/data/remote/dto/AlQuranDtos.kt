package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BaseApiResponse<T>(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: T
)

@JsonClass(generateAdapter = true)
data class MetaDataDto(
    @Json(name = "surahs") val surahs: SurahsContainerDto? = null
)

@JsonClass(generateAdapter = true)
data class SurahsContainerDto(
    @Json(name = "count") val count: Int? = 0,
    @Json(name = "references") val references: List<SurahMetaDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class SurahMetaDto(
    @Json(name = "number") val number: Int,
    @Json(name = "name") val name: String,
    @Json(name = "englishName") val englishName: String,
    @Json(name = "englishNameTranslation") val englishNameTranslation: String,
    @Json(name = "numberOfAyahs") val numberOfAyahs: Int,
    @Json(name = "revelationType") val revelationType: String
)

@JsonClass(generateAdapter = true)
data class SurahDetailDto(
    @Json(name = "number") val number: Int,
    @Json(name = "name") val name: String,
    @Json(name = "englishName") val englishName: String,
    @Json(name = "englishNameTranslation") val englishNameTranslation: String,
    @Json(name = "revelationType") val revelationType: String,
    @Json(name = "numberOfAyahs") val numberOfAyahs: Int,
    @Json(name = "ayahs") val ayahs: List<AyahDto>
)

@JsonClass(generateAdapter = true)
data class AyahDto(
    @Json(name = "number") val number: Int,
    @Json(name = "text") val text: String,
    @Json(name = "numberInSurah") val numberInSurah: Int,
    @Json(name = "juz") val juz: Int? = 1,
    @Json(name = "page") val page: Int? = 1
)
