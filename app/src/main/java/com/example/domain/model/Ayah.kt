package com.example.domain.model

data class Ayah(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val juz: Int = 1,
    val page: Int = 1
)
