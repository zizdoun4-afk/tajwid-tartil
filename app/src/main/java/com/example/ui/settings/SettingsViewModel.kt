package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.preferences.UserPreferencesRepository
import com.example.domain.model.RecitationStyle
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.AVAILABLE_THEMES
import com.example.ui.theme.AppThemeDefinition
import com.example.ui.theme.ZelligeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val selectedLanguage: AppLanguage = AppLanguage.ARABIC,
    val defaultReciterName: String = "Ahmed",
    val preferredStyle: RecitationStyle = RecitationStyle.TARTIL,
    val selectedTheme: AppThemeDefinition = ZelligeTheme,
    val cacheSizeMB: Double = 0.0,
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = UserPreferencesRepository(application)

    private val _cacheSize = MutableStateFlow(0.0)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        prefsRepository.selectedLanguage,
        prefsRepository.defaultReciterName,
        prefsRepository.preferredStyle,
        prefsRepository.selectedThemeId,
        _cacheSize,
        _message
    ) { args: Array<Any?> ->
        val lang = args[0] as AppLanguage
        val name = args[1] as String
        val style = args[2] as RecitationStyle
        val themeId = args[3] as String
        val cache = args[4] as Double
        val msg = args[5] as? String

        val themeDef = AVAILABLE_THEMES.find { it.id == themeId } ?: ZelligeTheme
        SettingsUiState(
            selectedLanguage = lang,
            defaultReciterName = name,
            preferredStyle = style,
            selectedTheme = themeDef,
            cacheSizeMB = cache,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        calculateCacheSize()
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            prefsRepository.setSelectedLanguage(language)
        }
    }

    fun onReciterNameChanged(name: String) {
        viewModelScope.launch {
            prefsRepository.setDefaultReciterName(name)
        }
    }

    fun onPreferredStyleChanged(style: RecitationStyle) {
        viewModelScope.launch {
            prefsRepository.setPreferredStyle(style)
        }
    }

    fun onThemeSelected(themeId: String) {
        viewModelScope.launch {
            prefsRepository.setSelectedThemeId(themeId)
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val cacheDir = app.cacheDir
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()

                val audioCacheDir = File(app.filesDir, "audio_cache")
                if (audioCacheDir.exists()) {
                    audioCacheDir.deleteRecursively()
                }

                calculateCacheSize()
                _message.value = "Cache audio vidé avec succès"
            } catch (e: Exception) {
                _message.value = "Erreur lors du vidage du cache : ${e.localizedMessage}"
            }
        }
    }

    fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            var bytes = getFolderSize(app.cacheDir)
            val audioCacheDir = File(app.filesDir, "audio_cache")
            if (audioCacheDir.exists()) {
                bytes += getFolderSize(audioCacheDir)
            }
            val mb = bytes / (1024.0 * 1024.0)
            _cacheSize.value = mb
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        if (file.isDirectory) {
            val files = file.listFiles() ?: return 0
            for (f in files) {
                size += if (f.isDirectory) getFolderSize(f) else f.length()
            }
        } else {
            size = file.length()
        }
        return size
    }
}
