package com.example.ui.ayahreader

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlayerState
import com.example.domain.model.RecitationStyle
import com.example.ui.components.TajweedColorizer
import com.example.ui.components.ZelligeHeader
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.QuranTextTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AyahReaderScreen(
    viewModel: AyahReaderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()
    val playPosition by viewModel.audioPlayer.positionMs.collectAsStateWithLifecycle()
    val playDuration by viewModel.audioPlayer.durationMs.collectAsStateWithLifecycle()

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, strings.micPermissionRequired, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    val currentAyah = viewModel.currentAyahOrNull()
    val totalAyahs = uiState.ayahs.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Zellige Header with Back Button & Bookmark Action
        ZelligeHeader(
            title = uiState.surah?.name ?: "سورة ${viewModel.surahNumber}",
            subtitle = uiState.surah?.let { "${it.englishName} • ${String.format(strings.surahVersesCount, it.numberOfAyahs)}" },
            arabicTitle = uiState.surah?.name,
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backButton,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoadingAyahs) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.loadingAyahs,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (currentAyah != null) {
                // Main Verse Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Verset Indicator Banner with Bookmark Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = String.format(strings.ayahHeaderFormat, uiState.surah?.name ?: "", currentAyah.numberInSurah, totalAyahs),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            // Bookmark Toggle Button
                            IconButton(
                                onClick = { viewModel.toggleBookmark() },
                                modifier = Modifier.testTag("bookmark_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Favori",
                                    tint = if (uiState.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Uthmani Arabic Ayah Text with Tajweed Colorization
                        Text(
                            text = TajweedColorizer.colorizeTajweed(currentAyah.text, isTajweedActive = true),
                            style = QuranTextTypography,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )

                        // Legend for Tajweed Colors
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TajweedLegendItem(color = TajweedColorizer.ColorMadd, label = "مد")
                            TajweedLegendItem(color = TajweedColorizer.ColorGhunna, label = "غنة/إخفاء")
                            TajweedLegendItem(color = TajweedColorizer.ColorQalqalah, label = "قلقلة")
                            TajweedLegendItem(color = TajweedColorizer.ColorIdgham, label = "إدغام")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Recitation Style & Audio Speed/Repeat Control Toolbar Row
                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                        val repeatCounts = listOf(1, 3, 5, -1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .horizontalScroll(rememberScrollState())
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StyleChip(
                                label = strings.styleTartilLabel,
                                isSelected = uiState.recitationStyle == RecitationStyle.TARTIL,
                                onClick = { if (uiState.recitationStyle != RecitationStyle.TARTIL) viewModel.toggleRecitationStyle() }
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            StyleChip(
                                label = strings.styleTajwidLabel,
                                isSelected = uiState.recitationStyle == RecitationStyle.TAJWID,
                                onClick = { if (uiState.recitationStyle != RecitationStyle.TAJWID) viewModel.toggleRecitationStyle() }
                            )
                            Spacer(modifier = Modifier.width(4.dp))

                            // Compact Speed Button Chip (⚡ 1.0x)
                            StyleChip(
                                label = "⚡ ${uiState.playbackSpeed}x",
                                isSelected = uiState.playbackSpeed != 1.0f,
                                onClick = {
                                    val currentIdx = speeds.indexOf(uiState.playbackSpeed)
                                    val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % speeds.size else 1
                                    viewModel.setPlaybackSpeed(speeds[nextIdx])
                                }
                            )
                            Spacer(modifier = Modifier.width(2.dp))

                            // Compact Tikrar (تكرار) Button Chip (🔁 تكرار 1x, 3x, 5x, ∞)
                            val repeatLabel = when (uiState.repeatCount) {
                                -1 -> "🔁 تكرار ∞"
                                else -> "🔁 تكرار ${uiState.repeatCount}x"
                            }
                            StyleChip(
                                label = repeatLabel,
                                isSelected = uiState.repeatCount != 1,
                                onClick = {
                                    val currentIdx = repeatCounts.indexOf(uiState.repeatCount)
                                    val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % repeatCounts.size else 0
                                    viewModel.setRepeatCount(repeatCounts[nextIdx])
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reference Audio Player Bar
                        val isRefPlaying = playerState is PlayerState.Playing
                        val isRefLoading = playerState is PlayerState.Loading

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.playReferenceAudio() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("listen_reference_button")
                            ) {
                                if (isRefLoading) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isRefPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isRefPlaying) "Pause" else strings.listenReferenceContentDescription,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format(strings.referenceAudioTitleFormat, uiState.recitationStyle.displayName),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRefPlaying) strings.referencePlaying else strings.referenceSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isRefPlaying && playDuration > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (playPosition.toFloat() / playDuration.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Recording Section
                        if (uiState.isRecording) {
                            // Active Recording Waveform & Timer View
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = strings.recordingInProgress,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )

                                val elapsedSecs = uiState.recordingElapsedMs / 1000
                                val mins = elapsedSecs / 60
                                val secs = elapsedSecs % 60
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                // Real-time Waveform Amplitude Bars
                                AmplitudeWaveform(amplitude = uiState.recordingAmplitude)

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { viewModel.stopRecording() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("stop_recording_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.stopRecording)
                                }
                            }
                        } else {
                            // Record Button
                            Button(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        viewModel.startRecording()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("start_recording_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.recordMyVoice,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prev / Next Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.goToPrevAyah() },
                        enabled = uiState.currentAyahIndex > 0,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("prev_ayah_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.previous)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { viewModel.goToNextAyah() },
                        enabled = uiState.currentAyahIndex < totalAyahs - 1,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("next_ayah_button")
                    ) {
                        Text(strings.next)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }

    // Post-Recording Dialog
    if (uiState.showPostRecordingDialog) {
        val elapsedSecs = uiState.recordingElapsedMs / 1000
        val mins = elapsedSecs / 60
        val secs = elapsedSecs % 60
        val durationStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

        AlertDialog(
            onDismissRequest = { viewModel.onPostRecordingCancelClicked() },
            title = {
                Text(
                    text = String.format(strings.postRecordingTitleFormat, durationStr),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.postRecordingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.playUserRecording() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("prelisten_user_recording")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.playMyRecording)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onPostRecordingSaveClicked() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_recording_dialog_confirm")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.onPostRecordingRecommencerClicked() }) {
                        Text(strings.restart)
                    }
                    TextButton(onClick = { viewModel.onPostRecordingCancelClicked() }) {
                        Text(strings.cancel)
                    }
                }
            }
        )
    }

    // Metadata Dialog
    if (uiState.showMetadataDialog) {
        val todayStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        val elapsedSecs = uiState.recordingElapsedMs / 1000
        val mins = elapsedSecs / 60
        val secs = elapsedSecs % 60
        val durationStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

        var tfValue by remember(uiState.reciterNameInput) {
            mutableStateOf(TextFieldValue(text = uiState.reciterNameInput))
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissMetadataDialog() },
            title = {
                Text(
                    text = strings.saveRecordingTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tfValue,
                        onValueChange = {
                            tfValue = it
                            viewModel.onReciterNameChanged(it.text)
                        },
                        label = { Text(strings.reciterNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    tfValue = tfValue.copy(selection = TextRange(0, tfValue.text.length))
                                }
                            }
                            .testTag("reciter_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(String.format(strings.surahLabelFormat, uiState.surah?.name ?: ""), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(strings.ayahLabelFormat, currentAyah?.numberInSurah ?: 1), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(strings.dateLabelFormat, todayStr), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(strings.durationLabelFormat, durationStr), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSaveRecording() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_save_metadata_button")
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMetadataDialog() }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun StyleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun TajweedLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AmplitudeWaveform(amplitude: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 16
        for (i in 0 until barCount) {
            // Pseudo-random distribution based on current amplitude
            val factor = (Math.sin(i * 0.8) * 0.4 + 0.6).toFloat()
            val heightPercent = (amplitude * factor).coerceIn(0.1f, 1f)
            val animatedHeight by animateFloatAsState(
                targetValue = heightPercent,
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
                label = "amp_bar"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxSize(animatedHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}
