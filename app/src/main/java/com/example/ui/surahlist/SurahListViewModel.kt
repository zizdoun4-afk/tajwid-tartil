package com.example.ui.surahlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.AppDatabase
import com.example.data.repository.QuranRepository
import com.example.domain.model.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SurahListUiState(
    val isLoading: Boolean = false,
    val surahs: List<Surah> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class SurahListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = QuranRepository(db.quranCacheDao(), application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _allSurahs = MutableStateFlow<List<Surah>>(emptyList())

    val uiState: StateFlow<SurahListUiState> = combine(
        _isLoading,
        _allSurahs,
        _searchQuery,
        _errorMessage
    ) { loading, surahs, query, error ->
        val filtered = if (query.isBlank()) {
            surahs
        } else {
            surahs.filter { surah ->
                surah.name.contains(query, ignoreCase = true) ||
                surah.englishName.contains(query, ignoreCase = true) ||
                surah.englishNameTranslation.contains(query, ignoreCase = true) ||
                surah.number.toString() == query.trim()
            }
        }
        SurahListUiState(
            isLoading = loading,
            surahs = filtered,
            searchQuery = query,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = SurahListUiState(isLoading = true)
    )

    init {
        loadSurahs()
    }

    fun loadSurahs() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Observe local database or fallback immediately
            repository.getSurahsFlow().collect { localList ->
                _allSurahs.value = localList
                _isLoading.value = false
            }
        }

        // Trigger remote sync in background
        viewModelScope.launch {
            val result = repository.fetchAndCacheSurahList()
            if (result.isFailure) {
                // Keep displaying fallback list silently or show subtle notice if list empty
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
