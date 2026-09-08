package com.example.ui.ayahreader

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlayerState
import com.example.audio.RepeatMode
import com.example.audio.SessionState
import com.example.domain.model.RecitationStyle
import com.example.domain.model.MemorizationStatus
import com.example.ui.components.ReciterNameTextField
import com.example.ui.components.ZelligeHeader
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.QuranTextTypography
import java.util.Locale

@Composable
fun AyahReaderScreen(
    viewModel: AyahReaderViewModel,
    onBackClick: () -> Unit,
    onStartTraining: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()
    val activeSource by viewModel.audioPlayer.currentSource.collectAsStateWithLifecycle()
    val playPosition by viewModel.audioPlayer.positionMs.collectAsStateWithLifecycle()
    val playDuration by viewModel.audioPlayer.durationMs.collectAsStateWithLifecycle()

    // Keep screen awake while studio screen is active
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Intercept back navigation if recording is in progress
    BackHandler(enabled = true) {
        if (viewModel.onBackPressRequest()) {
            onBackClick()
        }
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (uiState.recordWorkflowMode == RecordWorkflowMode.AYAH) {
                viewModel.startTake()
            } else {
                viewModel.startSession()
            }
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
        // Zellige Header with Intercepting Back Button
        ZelligeHeader(
            title = uiState.surah?.name ?: "سورة ${viewModel.surahNumber}",
            subtitle = uiState.surah?.let { "${it.englishName} • ${String.format(strings.surahVersesCount, it.numberOfAyahs)}" },
            arabicTitle = uiState.surah?.name,
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (viewModel.onBackPressRequest()) {
                            onBackClick()
                        }
                    },
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
                // Workflow Mode Selector (Pills: Mode Ayah / Mode Session)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val isAyahMode = uiState.recordWorkflowMode == RecordWorkflowMode.AYAH
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !uiState.isRecordingActive) {
                                viewModel.setRecordWorkflowMode(RecordWorkflowMode.AYAH)
                            },
                        color = if (isAyahMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = strings.modeAyahLabel,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isAyahMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAyahMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !uiState.isRecordingActive) {
                                viewModel.setRecordWorkflowMode(RecordWorkflowMode.SESSION)
                            },
                        color = if (!isAyahMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = strings.modeSessionLabel,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (!isAyahMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isAyahMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Verse Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                        // Verset Indicator Banner + Recorded Badge + Bookmark Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = String.format(strings.ayahHeaderFormat, uiState.surah?.name ?: "", currentAyah.numberInSurah, totalAyahs),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }

                                // Recorded badge indicator
                                val isRecorded = uiState.recordedAyahMap[currentAyah.numberInSurah] == true
                                if (isRecorded) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.clickable {
                                            viewModel.playRecordedAyah(currentAyah.numberInSurah)
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Enregistré",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = strings.myRecordingBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Memorization Status Badge (Section 3.4)
                                val memStatus = uiState.memorizationStatusMap[currentAyah.numberInSurah]
                                if (memStatus != null && memStatus != MemorizationStatus.NEW) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val (badgeBg, badgeText, badgeLabel) = when (memStatus) {
                                        MemorizationStatus.LEARNING -> Triple(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            MaterialTheme.colorScheme.onSecondaryContainer,
                                            strings.statusLearning
                                        )
                                        MemorizationStatus.REVIEW -> Triple(
                                            MaterialTheme.colorScheme.tertiaryContainer,
                                            MaterialTheme.colorScheme.onTertiaryContainer,
                                            strings.statusReview
                                        )
                                        MemorizationStatus.MEMORIZED -> Triple(
                                            Color(0xFF2E7D32).copy(alpha = 0.15f),
                                            Color(0xFF1B5E20),
                                            strings.statusMemorized
                                        )
                                        else -> Triple(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                            strings.statusNew
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = badgeBg,
                                        modifier = Modifier.clickable {
                                            onStartTraining?.invoke(viewModel.surahNumber, currentAyah.numberInSurah)
                                        }
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeText,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Bookmark Toggle Button
                            IconButton(
                                onClick = {
                                    viewModel.toggleBookmark()
                                    val msg = if (uiState.isCurrentAyahBookmarked) strings.bookmarkRemoved else strings.bookmarkAdded
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isCurrentAyahBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = strings.bookmarksTitle,
                                    tint = if (uiState.isCurrentAyahBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Uthmani Arabic Ayah Text
                        Text(
                            text = currentAyah.text,
                            style = QuranTextTypography,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Recitation Style Selector (Tartil / Tajwid Toggle)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StyleChip(
                                label = strings.styleTartilLabel,
                                isSelected = uiState.recitationStyle == RecitationStyle.TARTIL,
                                onClick = { if (uiState.recitationStyle != RecitationStyle.TARTIL) viewModel.toggleRecitationStyle() }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            StyleChip(
                                label = strings.styleTajwidLabel,
                                isSelected = uiState.recitationStyle == RecitationStyle.TAJWID,
                                onClick = { if (uiState.recitationStyle != RecitationStyle.TAJWID) viewModel.toggleRecitationStyle() }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reference Audio Player Bar
                        val isRefPlaying = playerState is PlayerState.Playing && viewModel.audioPlayer.currentSource.value?.startsWith("http") == true
                        val isRefLoading = playerState is PlayerState.Loading && viewModel.audioPlayer.currentSource.value?.startsWith("http") == true

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.playReferenceAudio() },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .testTag("listen_reference_button")
                                    ) {
                                        if (isRefLoading) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp),
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

                                // Playback Speed & Repeat Controls
                                val currentSpeed by viewModel.audioPlayer.playbackSpeed.collectAsStateWithLifecycle()
                                val currentRepeat by viewModel.audioPlayer.repeatMode.collectAsStateWithLifecycle()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Speed Button with Icon
                                    FilterChip(
                                        selected = currentSpeed != 1.0f,
                                        onClick = {
                                            val nextSpeed = when (currentSpeed) {
                                                1.0f -> 1.25f
                                                1.25f -> 1.5f
                                                1.5f -> 0.75f
                                                else -> 1.0f
                                            }
                                            viewModel.setPlaybackSpeed(nextSpeed)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = strings.speedLabel,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text("${currentSpeed}x", style = MaterialTheme.typography.labelSmall)
                                        }
                                    )

                                    // Repeat selector
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "🔁 ",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        FilterChip(
                                            selected = currentRepeat != RepeatMode.OFF,
                                            onClick = {
                                                val nextMode = when (currentRepeat) {
                                                    RepeatMode.OFF -> RepeatMode.ONCE
                                                    RepeatMode.ONCE -> RepeatMode.THREE_TIMES
                                                    RepeatMode.THREE_TIMES -> RepeatMode.LOOP
                                                    RepeatMode.LOOP -> RepeatMode.OFF
                                                }
                                                viewModel.setRepeatMode(nextMode)
                                            },
                                            label = {
                                                val modeText = when (currentRepeat) {
                                                    RepeatMode.OFF -> strings.repeatModeOff
                                                    RepeatMode.ONCE -> strings.repeatMode1x
                                                    RepeatMode.THREE_TIMES -> strings.repeatMode3x
                                                    RepeatMode.LOOP -> strings.repeatModeLoop
                                                }
                                                Text(modeText, style = MaterialTheme.typography.labelSmall)
                                            }
                                        )
                                    }
                                }

                                // Auto-play next ayah toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = uiState.autoPlayNext,
                                        onClick = { viewModel.toggleAutoPlay() },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.SkipNext,
                                                contentDescription = strings.autoPlayNextAyah,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (uiState.autoPlayNext) strings.autoPlayOn else strings.autoPlayOff,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.testTag("auto_play_toggle")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- RECORDER CONTROLS PANEL ---
                        if (uiState.recordWorkflowMode == RecordWorkflowMode.AYAH) {
                            // MODE AYAH (TAKE UNIQUE)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.isTaking) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    }
                                ),
                                border = if (uiState.isTaking) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Segmented selector for Tap / Hold style
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isTap = uiState.tapStyle == TapStyle.TAP
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(enabled = !uiState.isTaking) { viewModel.setTapStyle(TapStyle.TAP) },
                                            color = if (isTap) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = strings.tapModeOption,
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isTap) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isTap) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(enabled = !uiState.isTaking) { viewModel.setTapStyle(TapStyle.HOLD) },
                                            color = if (!isTap) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = strings.holdModeOption,
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (!isTap) FontWeight.Bold else FontWeight.Medium,
                                                color = if (!isTap) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    val isCurrentAyahRecorded = uiState.recordedAyahMap[currentAyah.numberInSurah] == true

                                    if (uiState.isTaking) {
                                        // Active Take view
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.error)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = String.format(strings.recordingVerseFormat, currentAyah.numberInSurah),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        val elapsedSecs = uiState.recordingElapsedMs / 1000
                                        val mins = elapsedSecs / 60
                                        val secs = elapsedSecs % 60
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        AmplitudeWaveform(amplitude = (uiState.recordingAmplitude + 160f) / 160f)

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.discardTake() },
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(strings.discardTakeButton)
                                            }

                                            Button(
                                                onClick = { viewModel.stopTake() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1.3f).testTag("stop_take_button")
                                            ) {
                                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(strings.stopRecording, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else if (uiState.reviewTakeResult != null) {
                                        // REVIEW FLOW: Listen to Take, Listen to Model, Retake, Keep
                                        val reviewResult = uiState.reviewTakeResult!!
                                        val isReviewTakePlaying = activeSource == reviewResult.filePath && playerState is PlayerState.Playing
                                        val modelUrl = uiState.recitationStyle.buildAudioUrl(viewModel.surahNumber, currentAyah.numberInSurah)
                                        val isModelPlaying = activeSource == modelUrl && playerState is PlayerState.Playing
                                        val durationSecs = (reviewResult.durationMs / 1000).coerceAtLeast(1)

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = strings.reviewTakeTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "${durationSecs}s",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = strings.reviewTakeSubtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                            )

                                            // Dual Playback Row: Listen to Take vs Listen to Model
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // My Take Player
                                                Button(
                                                    onClick = { viewModel.playReviewTake() },
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isReviewTakePlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = if (isReviewTakePlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                                    ),
                                                    modifier = Modifier.weight(1f).testTag("play_review_take_button")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isReviewTakePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(strings.playTakeButton, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                }

                                                // Reference Model Player
                                                OutlinedButton(
                                                    onClick = { viewModel.playReviewModel() },
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(1.dp, if (isModelPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline),
                                                    modifier = Modifier.weight(1f).testTag("play_review_model_button")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isModelPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = if (isModelPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = strings.playModelButton,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isModelPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Action Buttons: Retake, Discard, Keep as Best, Keep
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.discardReviewTake() },
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier.weight(1f).testTag("discard_review_take_button")
                                                ) {
                                                    Text(strings.discardTakeButton, style = MaterialTheme.typography.labelMedium)
                                                }

                                                OutlinedButton(
                                                    onClick = { viewModel.retake() },
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier.weight(1f).testTag("retake_button")
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(strings.retakeButton, style = MaterialTheme.typography.labelMedium)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.keepReviewTake(markAsBest = true) },
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                                                    modifier = Modifier.weight(1f).testTag("keep_as_best_button")
                                                ) {
                                                    Text(
                                                        text = strings.keepAsBestButton,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Button(
                                                    onClick = { viewModel.keepReviewTake(markAsBest = false) },
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    modifier = Modifier.weight(1f).testTag("keep_take_button")
                                                ) {
                                                    Text(
                                                        text = strings.keepTakeButton,
                                                        color = MaterialTheme.colorScheme.onSecondary,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // A/B Comparison & Saved Recording Section (if recorded)
                                        val savedRecording = uiState.currentAyahRecording
                                        if (savedRecording != null) {
                                            val isUserPlaying = activeSource == savedRecording.filePath && playerState is PlayerState.Playing
                                            val modelUrl = uiState.recitationStyle.buildAudioUrl(viewModel.surahNumber, currentAyah.numberInSurah)
                                            val isModelPlaying = activeSource == modelUrl && playerState is PlayerState.Playing

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = strings.compareHeaderTitle,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )

                                                        if (savedRecording.isBest) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                                            ) {
                                                                Text(
                                                                    text = strings.bestRecordingBadge,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    // Side-by-side Comparison Row
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        // Reference Model Column
                                                        Surface(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .clickable { viewModel.playAudio() },
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = if (isModelPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                                            border = BorderStroke(1.dp, if (isModelPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (isModelPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Column {
                                                                    Text(
                                                                        text = strings.activeSourceModel,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    Text(
                                                                        text = if (isModelPlaying) strings.referencePlaying else "Audio",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // User Recording Column
                                                        Surface(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .clickable { viewModel.playRecordedAyah(currentAyah.numberInSurah) },
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = if (isUserPlaying) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                                            border = BorderStroke(1.dp, if (isUserPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (isUserPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.secondary,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Column {
                                                                    Text(
                                                                        text = strings.activeSourceUser,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    val secs = savedRecording.durationMs / 1000
                                                                    Text(
                                                                        text = "${secs}s • ${savedRecording.reciterName}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    // Best Toggle & Delete row
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        TextButton(onClick = { viewModel.toggleBestCurrentRecording() }) {
                                                            Text(
                                                                text = if (savedRecording.isBest) "⭐ Meilleure prise" else "☆ Marquer meilleure",
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        TextButton(
                                                            onClick = { viewModel.deleteCurrentRecording() },
                                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                        ) {
                                                            Text(strings.actionDelete, style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Start Take Controls
                                        if (uiState.tapStyle == TapStyle.TAP) {
                                            Button(
                                                onClick = {
                                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                        viewModel.startTake()
                                                    } else {
                                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    }
                                                },
                                                shape = RoundedCornerShape(18.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary,
                                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .testTag("start_take_button")
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isCurrentAyahRecorded) strings.reRecordThisVerse else strings.recordThisVerse,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Text(
                                                text = strings.tapToStartHelp,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        } else {
                                            // Hold mode button
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onPress = {
                                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                                    viewModel.startTake()
                                                                    tryAwaitRelease()
                                                                    viewModel.stopTake()
                                                                } else {
                                                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                                }
                                                            }
                                                        )
                                                    }
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isCurrentAyahRecorded) strings.holdToReRecord else strings.holdToRecord,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSecondary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Text(
                                                text = strings.holdToRecordHelp,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // MODE SESSION (CONTINU)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (uiState.sessionState) {
                                        SessionState.RECORDING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                        SessionState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                ),
                                border = if (uiState.sessionState == SessionState.RECORDING) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val sessionActive = uiState.sessionState != SessionState.IDLE

                                    if (sessionActive) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (uiState.sessionState == SessionState.RECORDING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (uiState.sessionState == SessionState.RECORDING) String.format(strings.sessionInProgressFormat, currentAyah.numberInSurah) else strings.sessionPaused,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (uiState.sessionState == SessionState.RECORDING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        val elapsedSecs = uiState.recordingElapsedMs / 1000
                                        val mins = elapsedSecs / 60
                                        val secs = elapsedSecs % 60
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        AmplitudeWaveform(
                                            amplitude = if (uiState.sessionState == SessionState.RECORDING) (uiState.recordingAmplitude + 160f) / 160f else 0.05f
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Verset suivant marker button
                                        if (uiState.sessionState == SessionState.RECORDING && uiState.currentAyahIndex < totalAyahs - 1) {
                                            Button(
                                                onClick = { viewModel.markNextAyahInSession() },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(String.format(strings.markNextVerseFormat, currentAyah.numberInSurah + 1), fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Pause / Resume toggle
                                            OutlinedButton(
                                                onClick = {
                                                    if (uiState.sessionState == SessionState.RECORDING) viewModel.pauseSession() else viewModel.resumeSession()
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (uiState.sessionState == SessionState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (uiState.sessionState == SessionState.RECORDING) strings.pauseButton else strings.resumeButton)
                                            }

                                            // Stop session button
                                            Button(
                                                onClick = { viewModel.requestStopSession() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1f).testTag("stop_session_button")
                                            ) {
                                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(strings.endSessionButton)
                                            }
                                        }
                                    } else {
                                        // Start Session Button
                                        Button(
                                            onClick = {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                    viewModel.startSession()
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
                                                .height(52.dp)
                                                .testTag("start_session_button")
                                        ) {
                                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = String.format(strings.startContinuousSessionFormat, currentAyah.numberInSurah),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sticky Prev / Next Navigation Bar
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.goToPrevAyah() },
                            enabled = uiState.currentAyahIndex > 0 && !uiState.isRecordingActive,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("prev_ayah_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.previous, style = MaterialTheme.typography.labelMedium)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.goToNextAyah() },
                            enabled = uiState.currentAyahIndex < totalAyahs - 1 && !uiState.isRecordingActive,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("next_ayah_button")
                        ) {
                            Text(strings.next, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // Session Confirmation Dialog
    if (uiState.showSessionConfirmDialog && uiState.pendingSessionResult != null) {
        val result = uiState.pendingSessionResult!!
        val elapsedSecs = result.durationMs / 1000
        val mins = elapsedSecs / 60
        val secs = elapsedSecs % 60
        val durationStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

        val markers = result.markers
        val ayaFrom = markers.firstOrNull()?.aya ?: 1
        val ayaTo = markers.lastOrNull()?.aya ?: 1

        AlertDialog(
            onDismissRequest = { viewModel.discardSession() },
            title = {
                Text(
                    text = strings.saveSessionDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.saveSessionDialogMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(String.format(strings.surahLabelTextFormat, uiState.surah?.name ?: "سورة ${viewModel.surahNumber}"), fontWeight = FontWeight.Bold)
                            Text(String.format(strings.verseRangeLabelFormat, ayaFrom, ayaTo))
                            Text(String.format(strings.totalDurationTextFormat, durationStr))
                            Text(String.format(strings.timeMarkersCountFormat, markers.size))
                        }
                    }

                    ReciterNameTextField(
                        value = uiState.reciterNameInput,
                        onValueChange = { viewModel.onReciterNameChanged(it) },
                        label = strings.reciterNameLabel,
                        testTag = "session_reciter_input"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSaveSession() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_session_confirm_button")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.discardSession() }) {
                    Text(strings.discardTakeButton)
                }
            }
        )
    }

    // Exit Guard Dialog
    if (uiState.showExitGuardDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExitGuard() },
            title = {
                Text(
                    text = strings.recordingActiveTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = strings.recordingActiveExitMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmExitAndDiscard()
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.discardAndExit)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExitGuard() }) {
                    Text(strings.continueRecording)
                }
            }
        )
    }

    // Dialog for adding verse to revision / Hifz (Section 3.2)
    if (uiState.showAddToRevisionPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddToRevision() },
            title = {
                Text(
                    text = strings.addToRevisionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.addToRevisionMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAddToRevision() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.addToRevisionYes)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAddToRevision() }) {
                    Text(strings.addToRevisionLater)
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
