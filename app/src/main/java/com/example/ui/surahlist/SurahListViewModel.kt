package com.example.ui.surahlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.BookmarkEntity
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
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val isShowingBookmarks: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class SurahListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = QuranRepository(db.quranCacheDao())
    private val bookmarkDao = db.bookmarkDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isShowingBookmarks = MutableStateFlow(false)
    val isShowingBookmarks: StateFlow<Boolean> = _isShowingBookmarks.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _allSurahs = MutableStateFlow<List<Surah>>(emptyList())
    private val _allBookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())

    val uiState: StateFlow<SurahListUiState> = combine(
        _allSurahs,
        _allBookmarks,
        _isShowingBookmarks,
        _searchQuery
    ) { surahs, bookmarks, showBookmarks, query ->
        val filteredSurahs = if (query.isBlank()) {
            surahs
        } else {
            surahs.filter { surah ->
                surah.name.contains(query, ignoreCase = true) ||
                surah.englishName.contains(query, ignoreCase = true) ||
                surah.englishNameTranslation.contains(query, ignoreCase = true) ||
                surah.number.toString() == query.trim()
            }
        }
        val filteredBookmarks = if (query.isBlank()) {
            bookmarks
        } else {
            bookmarks.filter { bm ->
                bm.surahName.contains(query, ignoreCase = true) ||
                bm.ayahText.contains(query, ignoreCase = true) ||
                bm.surahNumber.toString() == query.trim()
            }
        }
        SurahListUiState(
            isLoading = surahs.isEmpty(),
            surahs = filteredSurahs,
            bookmarks = filteredBookmarks,
            isShowingBookmarks = showBookmarks,
            searchQuery = query,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = SurahListUiState(isLoading = true)
    )

    init {
        loadSurahs()
        observeBookmarks()
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

    private fun observeBookmarks() {
        viewModelScope.launch {
            bookmarkDao.getAllBookmarks().collect { list ->
                _allBookmarks.value = list
            }
        }
    }

    fun setShowBookmarks(show: Boolean) {
        _isShowingBookmarks.value = show
    }

    fun removeBookmark(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            bookmarkDao.deleteBookmark(surahNumber, ayahNumber)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
