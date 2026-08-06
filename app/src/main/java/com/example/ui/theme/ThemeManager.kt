package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppThemeDefinition(
    val id: String,
    val displayNameFr: String,
    val descriptionFr: String,
    val displayNameAr: String,
    val descriptionAr: String,
    val isDark: Boolean,
    val colorScheme: ColorScheme
)

val ZelligeTheme = AppThemeDefinition(
    id = "zellige",
    displayNameFr = "Zellige Marocain",
    descriptionFr = "Vert émeraude profond & Or traditionnel",
    displayNameAr = "الزليج المغربي",
    descriptionAr = "أخضر زمردي عميق وذهب تقليدي",
    isDark = false,
    colorScheme = lightColorScheme(
        primary = ZelligeEmeraldPrimary,
        onPrimary = Color.White,
        primaryContainer = ZelligeEmeraldLight,
        onPrimaryContainer = Color.White,
        secondary = ZelligeGoldSecondary,
        onSecondary = ZelligeEmeraldDark,
        secondaryContainer = ZelligeGoldLight,
        onSecondaryContainer = ZelligeEmeraldDark,
        background = ZelligeIvoryBackground,
        onBackground = ZelligeTextPrimary,
        surface = ZelligeParchmentSurface,
        onSurface = ZelligeTextPrimary,
        surfaceVariant = ZelligeSurfaceVariant,
        onSurfaceVariant = ZelligeTextSecondary
    )
)

val NuitTheme = AppThemeDefinition(
    id = "nuit",
    displayNameFr = "Nuit Émeraude",
    descriptionFr = "Sombre élégant & Or lumineux",
    displayNameAr = "الليل الزمردي",
    descriptionAr = "داكن أنيق وذهب مضيء",
    isDark = true,
    colorScheme = darkColorScheme(
        primary = NuitGoldPrimary,
        onPrimary = NuitDarkBackground,
        primaryContainer = NuitDarkSurfaceVariant,
        onPrimaryContainer = NuitGoldPrimary,
        secondary = NuitGoldSecondary,
        onSecondary = NuitDarkBackground,
        secondaryContainer = NuitDarkSurfaceVariant,
        onSecondaryContainer = NuitGoldSecondary,
        background = NuitDarkBackground,
        onBackground = NuitTextPrimary,
        surface = NuitDarkSurface,
        onSurface = NuitTextPrimary,
        surfaceVariant = NuitDarkSurfaceVariant,
        onSurfaceVariant = NuitTextSecondary
    )
)

val DesertTheme = AppThemeDefinition(
    id = "desert",
    displayNameFr = "Sable & Oasis",
    descriptionFr = "Ocre chaleureux & Oasis émeraude",
    displayNameAr = "الرمل والواحة",
    descriptionAr = "طبيعي دافئ وواحة زمردية",
    isDark = false,
    colorScheme = lightColorScheme(
        primary = DesertOchrePrimary,
        onPrimary = Color.White,
        primaryContainer = DesertGoldSecondary,
        onPrimaryContainer = Color.White,
        secondary = DesertOasisAccent,
        onSecondary = Color.White,
        secondaryContainer = DesertSandBackground,
        onSecondaryContainer = DesertOasisAccent,
        background = DesertSandBackground,
        onBackground = DesertTextPrimary,
        surface = DesertSandSurface,
        onSurface = DesertTextPrimary,
        surfaceVariant = Color(0xFFEFE8DA),
        onSurfaceVariant = DesertTextSecondary
    )
)

val AVAILABLE_THEMES = listOf(ZelligeTheme, NuitTheme, DesertTheme)

val LocalAppTheme = compositionLocalOf { ZelligeTheme }
