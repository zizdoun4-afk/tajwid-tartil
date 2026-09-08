package com.example.ui.recordingslibrary

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlayerState
import com.example.domain.model.UserRecording
import com.example.ui.components.ReciterNameTextField
import com.example.ui.components.ZelligeHeader
import com.example.ui.i18n.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsLibraryScreen(
    viewModel: RecordingsLibraryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()
    val activeSource by viewModel.audioPlayer.currentSource.collectAsStateWithLifecycle()
    val playPosition by viewModel.audioPlayer.positionMs.collectAsStateWithLifecycle()
    val activeDuration by viewModel.audioPlayer.durationMs.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Zellige Header
        ZelligeHeader(
            title = strings.libraryTitle,
            subtitle = strings.librarySubtitle,
            arabicTitle = strings.libraryHeaderArabic
        )

        // Statistics Summary Dashboard Card
        val allRecordings = remember(uiState.groupedRecordings) {
            uiState.groupedRecordings.flatMap { it.recordings }
        }
        val totalCount = allRecordings.size
        val totalDurationMs = remember(allRecordings) { allRecordings.sumOf { it.durationMs } }
        val uniqueSurahsCount = remember(allRecordings) { allRecordings.map { it.surahNumber }.distinct().size }
        val formattedDuration = remember(totalDurationMs) {
            val totalSeconds = totalDurationMs / 1000
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }

        if (allRecordings.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = strings.statsTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            icon = "🎙️",
                            value = "$totalCount",
                            label = strings.totalRecordingsLabel
                        )
                        StatItem(
                            icon = "⏱️",
                            value = formattedDuration,
                            label = strings.totalDurationLabel
                        )
                        StatItem(
                            icon = "📖",
                            value = "$uniqueSurahsCount",
                            label = strings.practicedSurahsLabel
                        )
                    }
                }
            }

            // Batch Actions Toolbar: Select All / Delete All / Delete Selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Select / Deselect All Button
                val isAllSelected = uiState.selectedRecordingIds.size == allRecordings.size && allRecordings.isNotEmpty()
                TextButton(
                    onClick = {
                        if (isAllSelected) viewModel.deselectAll() else viewModel.selectAll()
                    },
                    modifier = Modifier.testTag("select_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAllSelected) strings.deselectAll else strings.selectAll,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Delete Selected Items Button
                    if (uiState.selectedRecordingIds.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.deleteSelectedRecordings() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("delete_selected_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(strings.deleteSelectedRecordings, uiState.selectedRecordingIds.size),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Delete All (Total Sup) Button
                    Button(
                        onClick = { viewModel.showDeleteAllConfirmDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("delete_all_recordings_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.deleteAllRecordings,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Search Field
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("recordings_search_input"),
            placeholder = { Text(strings.filterPlaceholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (uiState.groupedRecordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) {
                            strings.emptyLibraryMessage
                        } else {
                            String.format(strings.noFilterResults, uiState.searchQuery)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.groupedRecordings.forEach { group ->
                    item(key = group.dateHeader) {
                        Text(
                            text = group.dateHeader,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(
                        items = group.recordings,
                        key = { it.id }
                    ) { recording ->
                        val isActive = activeSource == recording.filePath
                        val isPlayingThis = isActive && playerState is PlayerState.Playing
                        val currentPos = if (isActive) playPosition else 0L
                        val maxDuration = if (isActive && activeDuration > 0) activeDuration else recording.durationMs
                        val isSelected = uiState.selectedRecordingIds.contains(recording.id)

                        RecordingCardItem(
                            recording = recording,
                            isPlaying = isPlayingThis,
                            isActive = isActive,
                            isSelected = isSelected,
                            isSelectionMode = uiState.isSelectionMode,
                            currentPositionMs = currentPos,
                            totalDurationMs = maxDuration,
                            onToggleSelect = { viewModel.toggleSelectRecording(recording.id) },
                            onSeek = { newPos -> viewModel.audioPlayer.seekTo(newPos) },
                            onPlayClick = { viewModel.playRecording(recording) },
                            onShareClick = { viewModel.shareRecording(recording) },
                            onCopyClick = { viewModel.copyToDownloads(recording) },
                            onRenameClick = { viewModel.showRenameDialog(recording) },
                            onDeleteClick = { viewModel.showDeleteDialog(recording) }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    uiState.renameDialogRecording?.let { item ->
        var reciterName by remember { mutableStateOf(item.reciterName) }
        var customLabel by remember { mutableStateOf(item.customLabel ?: "") }

        AlertDialog(
            onDismissRequest = { viewModel.dismissRenameDialog() },
            title = { Text(strings.renameDialogTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReciterNameTextField(
                        value = reciterName,
                        onValueChange = { reciterName = it },
                        label = strings.reciterNameLabel,
                        testTag = "rename_reciter_input"
                    )
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text(strings.customNoteLabel) },
                        singleLine = true,
                        trailingIcon = {
                            if (customLabel.isNotEmpty()) {
                                IconButton(onClick = { customLabel = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextDirection.ContentOrRtl else TextDirection.ContentOrLtr,
                            textAlign = TextAlign.Start
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRename(item.id, reciterName, customLabel) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.validate)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRenameDialog() }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Individual Confirmation Dialog
    uiState.deleteConfirmRecording?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text(strings.deleteConfirmTitle, fontWeight = FontWeight.Bold) },
            text = {
                Text(String.format(strings.deleteConfirmMessageFormat, item.surahName, item.ayahNumber))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.actionDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete All Confirmation Dialog
    if (uiState.showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAllConfirmDialog() },
            title = {
                Text(
                    text = strings.confirmDeleteAllTitle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(text = strings.confirmDeleteAllMessage)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteAllRecordings() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = strings.deleteAllRecordings, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteAllConfirmDialog() }) {
                    Text(text = strings.cancel)
                }
            }
        )
    }
}

@Composable
fun RecordingCardItem(
    recording: UserRecording,
    isPlaying: Boolean,
    isActive: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onToggleSelect: () -> Unit = {},
    onSeek: (Long) -> Unit,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showMenu by remember { mutableStateOf(false) }

    val currentMins = (currentPositionMs / 1000) / 60
    val currentSecs = (currentPositionMs / 1000) % 60
    val formattedCurrent = String.format(Locale.getDefault(), "%02d:%02d", currentMins, currentSecs)

    val elapsedSecs = (if (isActive && totalDurationMs > 0) totalDurationMs else recording.durationMs) / 1000
    val mins = elapsedSecs / 60
    val secs = elapsedSecs % 60
    val durationStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(recording.recordedAtEpochMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_item_${recording.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox for Selection Mode
                if (isSelectionMode || isSelected) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.testTag("recording_checkbox_${recording.id}")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Play Button Icon
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else strings.actionPlay,
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎙 ${recording.surahName} (${recording.ayahNumber}) — ${recording.reciterName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isActive) "$formattedCurrent / $durationStr · $timeStr" else "$durationStr · $timeStr${if (recording.customLabel != null) " • ${recording.customLabel}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // More Menu [⋮]
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.actionPlay) },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onPlayClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.actionRename) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.actionShare) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShareClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.actionCopy) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onCopyClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.actionDelete) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Audio Progress Bar (Interactive Slider for active audio)
            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                val safeMax = if (totalDurationMs > 0) totalDurationMs.toFloat() else 1f
                val safePos = currentPositionMs.coerceIn(0L, totalDurationMs).toFloat()

                Slider(
                    value = safePos,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..safeMax,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_progress_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$icon $value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
