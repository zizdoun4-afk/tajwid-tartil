package com.example.ui.training

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PlayerState
import com.example.ui.components.ZelligeHeader
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.QuranTextTypography

@Composable
fun TrainingSessionScreen(
    viewModel: TrainingSessionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, strings.micPermissionRequired, Toast.LENGTH_SHORT).show()
        }
    }

    // Intercept back navigation
    BackHandler(enabled = true) {
        if (uiState.isSessionCompleted) {
            onBackClick()
        } else {
            viewModel.showExitConfirmation()
        }
    }

    // Exit confirmation dialog
    if (uiState.showExitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExitConfirmation() },
            title = { Text(strings.trainingExitConfirmTitle) },
            text = { Text(strings.trainingExitConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.abandonSession()
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.quitButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExitConfirmation() }) {
                    Text(strings.continueTrainingButton)
                }
            }
        )
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        ZelligeHeader(
            title = String.format(strings.trainingTitleFormat, uiState.surah?.name ?: "سورة ${viewModel.surahNumber}", viewModel.ayahNumber),
            subtitle = uiState.status.name,
            navigationIcon = {
                IconButton(onClick = {
                    if (uiState.isSessionCompleted) {
                        onBackClick()
                    } else {
                        viewModel.showExitConfirmation()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backButton,
                        tint = Color.White
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Step Progress Indicator (1 to 5)
            StepProgressBar(
                currentStep = uiState.currentStep.stepNumber,
                totalSteps = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current Step Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (uiState.currentStep) {
                            TrainingStep.LISTEN_3X -> strings.trainingStep1Title
                            TrainingStep.ACCOMPANIED_READING -> strings.trainingStep2Title
                            TrainingStep.SOLO_RECORDING -> strings.trainingStep3Title
                            TrainingStep.PLAYBACK_REVIEW -> strings.trainingStep4Title
                            TrainingStep.BLIND_TEST -> strings.trainingStep5Title
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (uiState.currentStep) {
                            TrainingStep.LISTEN_3X -> strings.trainingStep1Desc
                            TrainingStep.ACCOMPANIED_READING -> strings.trainingStep2Desc
                            TrainingStep.SOLO_RECORDING -> strings.trainingStep3Desc
                            TrainingStep.PLAYBACK_REVIEW -> strings.trainingStep4Desc
                            TrainingStep.BLIND_TEST -> strings.trainingStep5Desc
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quranic Text Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val ayah = uiState.ayah
                    val isBlurred = uiState.currentStep == TrainingStep.BLIND_TEST && !uiState.isTextRevealed

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ayah?.text ?: "...",
                            style = QuranTextTypography.copy(
                                fontSize = 26.sp,
                                lineHeight = 44.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = if (isBlurred) Modifier.blur(16.dp) else Modifier
                        )

                        if (isBlurred) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickable { viewModel.toggleTextVisibility() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = strings.revealTextButton,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.revealTextButton,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.currentStep == TrainingStep.BLIND_TEST && uiState.isTextRevealed) {
                        TextButton(onClick = { viewModel.toggleTextVisibility() }) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = strings.hideTextButton,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.hideTextButton)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step-specific Interactive Panel
            when (uiState.currentStep) {
                TrainingStep.LISTEN_3X -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { viewModel.playReferenceAudio(repeatCount = 3) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            val isPlaying = playerState is PlayerState.Playing
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.Repeat,
                                contentDescription = strings.listen3xButton
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) strings.pauseButton else strings.listen3xButton,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (uiState.isStep1Completed) {
                            Text(
                                text = strings.step1CompletedBadge,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = strings.step1RequiredToProceed,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                TrainingStep.ACCOMPANIED_READING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { viewModel.playReferenceAudio(repeatCount = 1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            val isPlaying = playerState is PlayerState.Playing
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = strings.listenAccompaniedButton
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) strings.pauseButton else strings.listenAccompaniedButton,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val isPlaying = playerState is PlayerState.Playing
                        if (isPlaying) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = strings.step2ReadingInProgress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (uiState.isStep2Completed) {
                            Text(
                                text = strings.step2CompletedBadge,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = strings.step2RequiredToProceed,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                TrainingStep.SOLO_RECORDING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!uiState.isRecording) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = strings.recordSoloButton)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.userRecordingFile != null) strings.reRecordButton else strings.recordSoloButton,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.stopRecording() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                val sec = (uiState.recordingDurationMs / 1000).toInt()
                                Text(
                                    text = String.format(strings.stopRecordingDurationFormat, sec),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (uiState.userRecordingFile != null && !uiState.isRecording) {
                            Text(
                                text = "✓ ${strings.postRecordingMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                TrainingStep.PLAYBACK_REVIEW -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Play reference
                        OutlinedButton(
                            onClick = { viewModel.playReferenceAudio(repeatCount = 1) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.compareModelLabel, maxLines = 1)
                        }

                        // Play user
                        Button(
                            onClick = { viewModel.playUserRecording() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.compareMyVoiceLabel, maxLines = 1)
                        }
                    }
                }

                TrainingStep.BLIND_TEST -> {
                    if (!uiState.isSessionCompleted) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!uiState.isTextRevealed) {
                                Text(
                                    text = strings.blindTestPrompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = { viewModel.toggleTextVisibility() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.revealTextButton,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Revealed: User validates result
                                Text(
                                    text = strings.testSelfHelp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Retry button
                                    OutlinedButton(
                                        onClick = { viewModel.validateBlindTest(succeeded = false) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.blindTestRetryButton, maxLines = 1)
                                    }

                                    // Success / Memorized button
                                    Button(
                                        onClick = { viewModel.validateBlindTest(succeeded = true) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.blindTestSuccessButton, maxLines = 1, color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        // Success screen
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = strings.markMemorizedSuccess,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onBackClick,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text(strings.backButton, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation between steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.currentStep.stepNumber > 1 && !uiState.isSessionCompleted) {
                    TextButton(onClick = { viewModel.goToPrevStep() }) {
                        Text(strings.prevStepButton)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (uiState.currentStep.stepNumber < 5 && !uiState.isSessionCompleted) {
                    val canProceed = viewModel.canProceedToNextStep()
                    Button(
                        onClick = {
                            if (canProceed) {
                                viewModel.goToNextStep()
                            } else {
                                val msg = when (uiState.currentStep) {
                                    TrainingStep.LISTEN_3X -> strings.step1RequiredToProceed
                                    TrainingStep.ACCOMPANIED_READING -> strings.step2RequiredToProceed
                                    TrainingStep.SOLO_RECORDING -> strings.recordRequiredToProceed
                                    TrainingStep.PLAYBACK_REVIEW -> strings.recordRequiredToProceed
                                    TrainingStep.BLIND_TEST -> ""
                                }
                                if (msg.isNotEmpty()) {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = canProceed,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.nextStepButton)
                    }
                }
            }
        }
    }
}

@Composable
fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val isCompleted = i < currentStep
            val isCurrent = i == currentStep

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> Color(0xFF2E7D32)
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = i.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (i < totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .padding(horizontal = 4.dp)
                        .background(
                            if (i < currentStep) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}
