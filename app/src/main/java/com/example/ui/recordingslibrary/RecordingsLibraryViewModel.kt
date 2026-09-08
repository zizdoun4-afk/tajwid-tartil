package com.example.ui.recordingslibrary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.data.local.db.AppDatabase
import com.example.data.repository.RecordingRepository
import com.example.domain.model.UserRecording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DateGroupedRecordings(
    val dateHeader: String,
    val recordings: List<UserRecording>
)

data class RecordingsLibraryUiState(
    val isLoading: Boolean = false,
    val groupedRecordings: List<DateGroupedRecordings> = emptyList(),
    val searchQuery: String = "",
    val selectedRecordingIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showDeleteAllConfirmDialog: Boolean = false,
    val activePlayingRecordingId: Long? = null,
    val isPlaying: Boolean = false,
    val renameDialogRecording: UserRecording? = null,
    val deleteConfirmRecording: UserRecording? = null,
    val message: String? = null
)

class RecordingsLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val recordingRepository = RecordingRepository(application, db.recordingDao())

    val audioPlayer = AudioPlayer(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allRecordings = MutableStateFlow<List<UserRecording>>(emptyList())
    private val _selectedRecordingIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)
    private val _showDeleteAllConfirm = MutableStateFlow(false)
    private val _renameDialogRecording = MutableStateFlow<UserRecording?>(null)
    private val _deleteConfirmRecording = MutableStateFlow<UserRecording?>(null)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecordingsLibraryUiState> = combine(
        combine(_allRecordings, _searchQuery, _selectedRecordingIds, _isSelectionMode) { recs, q, sel, mode ->
            Quad(recs, q, sel, mode)
        },
        combine(_showDeleteAllConfirm, _renameDialogRecording, _deleteConfirmRecording, _message) { showDelAll, rename, del, msg ->
            Quad(showDelAll, rename, del, msg)
        }
    ) { (recs, q, sel, mode), (showDelAll, rename, del, msg) ->
        val filtered = if (q.isBlank()) {
            recs
        } else {
            recs.filter { rec ->
                rec.reciterName.contains(q, ignoreCase = true) ||
                rec.surahName.contains(q, ignoreCase = true) ||
                (rec.customLabel?.contains(q, ignoreCase = true) == true)
            }
        }

        // Group by Date Header
        val grouped = groupRecordingsByDate(filtered)

        RecordingsLibraryUiState(
            isLoading = false,
            groupedRecordings = grouped,
            searchQuery = q,
            selectedRecordingIds = sel,
            isSelectionMode = mode,
            showDeleteAllConfirmDialog = showDelAll,
            renameDialogRecording = rename,
            deleteConfirmRecording = del,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = RecordingsLibraryUiState(isLoading = true)
    )

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    init {
        loadRecordings()
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            recordingRepository.allRecordings.collect { list ->
                _allRecordings.value = list
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun playRecording(recording: UserRecording) {
        audioPlayer.togglePlayPause(recording.filePath) {
            // on complete
        }
    }

    fun shareRecording(recording: UserRecording) {
        recordingRepository.shareRecording(recording)
    }

    fun copyToDownloads(recording: UserRecording) {
        viewModelScope.launch {
            val success = recordingRepository.copyToDownloads(recording)
            _message.value = if (success) {
                "Fichier copié dans le dossier Téléchargements !"
            } else {
                "Erreur lors de la copie du fichier"
            }
        }
    }

    fun showRenameDialog(recording: UserRecording) {
        _renameDialogRecording.value = recording
    }

    fun dismissRenameDialog() {
        _renameDialogRecording.value = null
    }

    fun confirmRename(id: Long, newReciterName: String, newLabel: String?) {
        viewModelScope.launch {
            recordingRepository.updateRecordingMeta(id, newReciterName, newLabel.takeIf { !it.isNull_or_blank() })
            _renameDialogRecording.value = null
            _message.value = "Enregistrement renommé"
        }
    }

    fun showDeleteDialog(recording: UserRecording) {
        _deleteConfirmRecording.value = recording
    }

    fun dismissDeleteDialog() {
        _deleteConfirmRecording.value = null
    }

    fun confirmDelete(recording: UserRecording) {
        viewModelScope.launch {
            audioPlayer.stop()
            recordingRepository.deleteRecording(recording)
            _deleteConfirmRecording.value = null
            _message.value = "Enregistrement supprimé"
        }
    }

    // Selection & Bulk Delete
    fun toggleSelectionMode() {
        val currentMode = _isSelectionMode.value
        _isSelectionMode.value = !currentMode
        if (currentMode) {
            _selectedRecordingIds.value = emptySet()
        }
    }

    fun toggleSelectRecording(id: Long) {
        val set = _selectedRecordingIds.value.toMutableSet()
        if (set.contains(id)) {
            set.remove(id)
        } else {
            set.add(id)
        }
        _selectedRecordingIds.value = set
        if (set.isNotEmpty()) {
            _isSelectionMode.value = true
        }
    }

    fun selectAll() {
        val allIds = _allRecordings.value.map { it.id }.toSet()
        _selectedRecordingIds.value = allIds
        _isSelectionMode.value = true
    }

    fun deselectAll() {
        _selectedRecordingIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelectedRecordings() {
        viewModelScope.launch {
            val selectedIds = _selectedRecordingIds.value
            val toDelete = _allRecordings.value.filter { selectedIds.contains(it.id) }
            audioPlayer.stop()
            recordingRepository.deleteRecordings(toDelete)
            _selectedRecordingIds.value = emptySet()
            _isSelectionMode.value = false
            _message.value = "${toDelete.size} enregistrement(s) supprimé(s)"
        }
    }

    fun showDeleteAllConfirmDialog() {
        _showDeleteAllConfirm.value = true
    }

    fun dismissDeleteAllConfirmDialog() {
        _showDeleteAllConfirm.value = false
    }

    fun confirmDeleteAllRecordings() {
        viewModelScope.launch {
            audioPlayer.stop()
            val all = _allRecordings.value
            recordingRepository.deleteRecordings(all)
            _selectedRecordingIds.value = emptySet()
            _isSelectionMode.value = false
            _showDeleteAllConfirm.value = false
            _message.value = "Tous les enregistrements ont été supprimés"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun groupRecordingsByDate(list: List<UserRecording>): List<DateGroupedRecordings> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val yesterdayMillis = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(yesterdayMillis))

        val map = LinkedHashMap<String, MutableList<UserRecording>>()

        for (item in list) {
            val itemDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.recordedAtEpochMillis))
            val headerTitle = when (itemDateStr) {
                todayStr -> "📅 Aujourd'hui — ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(item.recordedAtEpochMillis))}"
                yesterdayStr -> "📅 Hier — ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(item.recordedAtEpochMillis))}"
                else -> "📅 ${SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH).format(Date(item.recordedAtEpochMillis)).capitalize(Locale.FRENCH)}"
            }

            if (!map.containsKey(headerTitle)) {
                map[headerTitle] = mutableListOf()
            }
            map[headerTitle]?.add(item)
        }

        return map.map { (header, items) ->
            DateGroupedRecordings(dateHeader = header, recordings = items)
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
