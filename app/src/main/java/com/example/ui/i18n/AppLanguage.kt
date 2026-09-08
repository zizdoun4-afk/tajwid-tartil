package com.example.ui.i18n

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val layoutDirection: LayoutDirection
) {
    ARABIC("ar", "العربية", LayoutDirection.Rtl),
    FRENCH("fr", "Français", LayoutDirection.Ltr),
    ENGLISH("en", "English", LayoutDirection.Ltr)
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ARABIC }
val LocalAppStrings = compositionLocalOf<AppStrings> { ArabicAppStrings }
