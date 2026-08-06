package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun TajwidTartilTheme(
    appTheme: AppThemeDefinition = ZelligeTheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppTheme provides appTheme
    ) {
        MaterialTheme(
            colorScheme = appTheme.colorScheme,
            typography = Typography,
            content = content
        )
    }
}
