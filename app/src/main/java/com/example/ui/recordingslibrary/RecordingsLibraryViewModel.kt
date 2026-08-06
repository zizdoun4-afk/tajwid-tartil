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
    private val _renameDialogRecording = MutableStateFlow<UserRecording?>(null)
    private val _deleteConfirmRecording = MutableStateFlow<UserRecording?>(null)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecordingsLibraryUiState> = combine(
        _allRecordings,
        _searchQuery,
        _renameDialogRecording,
        _deleteConfirmRecording,
        _message
    ) { recordings, query, renameItem, deleteItem, msg ->
        val filtered = if (query.isBlank()) {
            recordings
        } else {
            recordings.filter { rec ->
                rec.reciterName.contains(query, ignoreCase = true) ||
                rec.surahName.contains(query, ignoreCase = true) ||
                (rec.customLabel?.contains(query, ignoreCase = true) == true)
            }
        }

        // Group by Date Header
        val grouped = groupRecordingsByDate(filtered)

        RecordingsLibraryUiState(
            isLoading = false,
            groupedRecordings = grouped,
            searchQuery = query,
            renameDialogRecording = renameItem,
            deleteConfirmRecording = deleteItem,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = RecordingsLibraryUiState(isLoading = true)
    )

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
                else -> "📅 ${SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH).format(Date(item.recordedAtEpochMillis)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }}"
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
