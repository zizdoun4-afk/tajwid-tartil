package com.example.data.remote

import com.example.data.remote.dto.BaseApiResponse
import com.example.data.remote.dto.MetaDataDto
import com.example.data.remote.dto.SurahDetailDto
import com.example.data.remote.dto.SurahMetaDto
import retrofit2.http.GET
import retrofit2.http.Path

interface AlQuranApi {
    @GET("v1/meta")
    suspend fun getMeta(): BaseApiResponse<MetaDataDto>

    @GET("v1/surah")
    suspend fun getAllSurahs(): BaseApiResponse<List<SurahMetaDto>>

    @GET("v1/surah/{surahNumber}/quran-uthmani")
    suspend fun getSurahUthmani(@Path("surahNumber") surahNumber: Int): BaseApiResponse<SurahDetailDto>

    companion object {
        const val BASE_URL = "https://api.alquran.cloud/"
    }
}
