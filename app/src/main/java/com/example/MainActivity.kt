package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.preferences.UserPreferencesRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.main.MainScreen
import com.example.ui.theme.AVAILABLE_THEMES
import com.example.ui.theme.TajwidTartilTheme
import com.example.ui.theme.ZelligeTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefsRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefsRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val themeId by prefsRepository.selectedThemeId.collectAsStateWithLifecycle(initialValue = ZelligeTheme.id)
            val activeTheme = AVAILABLE_THEMES.find { it.id == themeId } ?: ZelligeTheme

            val language by prefsRepository.selectedLanguage.collectAsStateWithLifecycle(initialValue = AppLanguage.ARABIC)
            val appStrings = getAppStrings(language)

            CompositionLocalProvider(
                LocalAppLanguage provides language,
                LocalAppStrings provides appStrings,
                LocalLayoutDirection provides language.layoutDirection
            ) {
                TajwidTartilTheme(appTheme = activeTheme) {
                    MainScreen()
                }
            }
        }
    }
}
