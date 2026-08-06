package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.RecitationStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val RECITER_NAME = stringPreferencesKey("default_reciter_name")
        val PREFERRED_STYLE = stringPreferencesKey("preferred_recitation_style")
        val THEME_ID = stringPreferencesKey("selected_theme_id")
        val LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
    }

    val selectedLanguage: Flow<com.example.ui.i18n.AppLanguage> = context.dataStore.data.map { prefs ->
        val code = prefs[Keys.LANGUAGE_CODE] ?: com.example.ui.i18n.AppLanguage.ARABIC.code
        com.example.ui.i18n.AppLanguage.entries.find { it.code == code } ?: com.example.ui.i18n.AppLanguage.ARABIC
    }

    val defaultReciterName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECITER_NAME] ?: "Ahmed"
    }

    val preferredStyle: Flow<RecitationStyle> = context.dataStore.data.map { prefs ->
        val styleStr = prefs[Keys.PREFERRED_STYLE] ?: RecitationStyle.TARTIL.name
        try {
            RecitationStyle.valueOf(styleStr)
        } catch (e: Exception) {
            RecitationStyle.TARTIL
        }
    }

    val selectedThemeId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_ID] ?: "zellige"
    }

    suspend fun setDefaultReciterName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RECITER_NAME] = name
        }
    }

    suspend fun setPreferredStyle(style: RecitationStyle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_STYLE] = style.name
        }
    }

    suspend fun setSelectedThemeId(themeId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = themeId
        }
    }

    suspend fun setSelectedLanguage(language: com.example.ui.i18n.AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
        }
    }
}
