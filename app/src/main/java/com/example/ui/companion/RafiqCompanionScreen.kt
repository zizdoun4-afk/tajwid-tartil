package com.example.ui.companion

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.PacingAssessment
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.QuranTextTypography

enum class RafiqTab {
    SMART_SESSION,
    QUICK_ANALYSIS,
    RECOMMENDATIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RafiqCompanionScreen(
    strings: AppStrings,
    onBackClick: () -> Unit,
    onNavigateToTajwidLesson: ((String) -> Unit)? = null,
    viewModel: RafiqCompanionViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(RafiqTab.SMART_SESSION) }
    val currentLanguage = LocalAppLanguage.current

    val agenda by viewModel.agenda.collectAsState()
    val sessionQueue by viewModel.sessionQueue.collectAsState()
    val queueIndex by viewModel.currentQueueIndex.collectAsState()

    val currentSurah by viewModel.currentSurahNumber.collectAsState()
    val currentAyah by viewModel.currentAyahNumber.collectAsState()
    val ayahText by viewModel.currentAyahText.collectAsState()
    val isRevealed by viewModel.isTextRevealed.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val lastTake by viewModel.lastTakeResult.collectAsState()
    val diagnostic by viewModel.diagnostic.collectAsState()

    val isPlayingModel by viewModel.isPlayingModel.collectAsState()
    val isPlayingUser by viewModel.isPlayingUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.rafiqTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.rafiqHeaderArabic,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backButton
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == RafiqTab.SMART_SESSION,
                    onClick = { selectedTab = RafiqTab.SMART_SESSION },
                    text = { Text(strings.rafiqTabSmartSession, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == RafiqTab.QUICK_ANALYSIS,
                    onClick = { selectedTab = RafiqTab.QUICK_ANALYSIS },
                    text = { Text(strings.rafiqTabAnalyzer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == RafiqTab.RECOMMENDATIONS,
                    onClick = { selectedTab = RafiqTab.RECOMMENDATIONS },
                    text = { Text(strings.rafiqTabProgram, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    RafiqTab.SMART_SESSION -> {
                        // Spiritual motivation banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🌿", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.rafiqDailySmartSession,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val motivation = when (currentLanguage) {
                                    AppLanguage.ARABIC -> agenda?.motivationMessageAr ?: strings.rafiqDailySmartSessionDesc
                                    else -> agenda?.motivationMessageFr ?: strings.rafiqDailySmartSessionDesc
                                }
                                Text(
                                    text = motivation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Queue indicator
                        if (sessionQueue.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.getDefault(), strings.rafiqQueueProgressFormat, queueIndex + 1, sessionQueue.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format(java.util.Locale.getDefault(), strings.rafiqSurahAyahFormat, currentSurah, currentAyah),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Ayah Box (Blind or Revealed)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isRevealed && ayahText.isNotBlank()) {
                                    Text(
                                        text = ayahText,
                                        style = QuranTextTypography.copy(fontSize = 24.sp, textAlign = TextAlign.Center),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = strings.rafiqBlindModeTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = strings.rafiqBlindModeSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { viewModel.toggleRevealText() },
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(if (isRevealed) strings.hideTextButton else strings.revealTextButton, fontSize = 12.sp)
                                }
                            }
                        }

                        // Recording & Acoustic Analyzer Section
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = strings.rafiqVoiceAnalyzerTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (isRecording) {
                                    Text(
                                        text = strings.rafiqRecordingInProgress,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.stopRecordingAndAnalyze() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text(strings.rafiqStopAndAnalyze)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.startRecording() },
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text(if (lastTake != null) strings.rafiqReRecord else strings.rafiqRecordMicrophone)
                                    }
                                }

                                // Audio comparison controls
                                if (lastTake != null && !isRecording) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                if (isPlayingUser) viewModel.stopAudio() else viewModel.playUserRecording()
                                            },
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(if (isPlayingUser) strings.rafiqStopAudio else strings.rafiqPlayMyVoice, fontSize = 12.sp)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                if (isPlayingModel) viewModel.stopAudio() else viewModel.playModelRecitation()
                                            },
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(if (isPlayingModel) strings.rafiqStopAudio else strings.rafiqPlayModelSheikh, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Diagnostic Card (when recitation is analyzed)
                        diagnostic?.let { diag ->
                            val pacingMsg = when (currentLanguage) {
                                AppLanguage.ARABIC -> diag.pacingMessageAr
                                AppLanguage.ENGLISH -> diag.pacingMessageEn.ifBlank { diag.pacingMessageFr }
                                else -> diag.pacingMessageFr
                            }
                            val pauseMsg = when (currentLanguage) {
                                AppLanguage.ARABIC -> diag.pauseMessageAr
                                AppLanguage.ENGLISH -> diag.pauseMessageEn.ifBlank { diag.pauseMessageFr }
                                else -> diag.pauseMessageFr
                            }
                            val stabilityMsg = when (currentLanguage) {
                                AppLanguage.ARABIC -> diag.stabilityMessageAr
                                AppLanguage.ENGLISH -> diag.stabilityMessageEn.ifBlank { diag.stabilityMessageFr }
                                else -> diag.stabilityMessageFr
                            }
                            val disclaimerMsg = when (currentLanguage) {
                                AppLanguage.ARABIC -> diag.pedagogicalDisclaimerAr
                                AppLanguage.ENGLISH -> diag.pedagogicalDisclaimerEn
                                else -> diag.pedagogicalDisclaimerFr
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.rafiqAcousticDiagnosticTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = when (diag.pacingAssessment) {
                                                PacingAssessment.MEASURED_TARTIL -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                                                PacingAssessment.TOO_FAST_HADR -> Color(0xFFE65100).copy(alpha = 0.2f)
                                                PacingAssessment.ELONGATED_OR_HESITANT -> Color(0xFF1565C0).copy(alpha = 0.2f)
                                            }
                                        ) {
                                            Text(
                                                text = when (diag.pacingAssessment) {
                                                    PacingAssessment.MEASURED_TARTIL -> strings.rafiqPacingTartil
                                                    PacingAssessment.TOO_FAST_HADR -> strings.rafiqPacingHadr
                                                    PacingAssessment.ELONGATED_OR_HESITANT -> strings.rafiqPacingElongated
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pacingMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "• $pauseMsg",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val stabilityText = String.format(java.util.Locale.getDefault(), strings.rafiqStabilityFormat, diag.energyStabilityPercent)
                                    Text(
                                        text = "• $stabilityMsg ($stabilityText)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Tajwid Points to Watch
                                    if (diag.tajwidPointsToWatch.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = strings.rafiqTajwidPointsHeader,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        diag.tajwidPointsToWatch.forEach { point ->
                                            val ruleName = if (currentLanguage == AppLanguage.ARABIC) point.ruleNameAr else point.ruleNameFr
                                            val explanation = if (currentLanguage == AppLanguage.ARABIC) point.explanationAr else point.explanationFr
                                            Text(
                                                text = "✦ $ruleName : ${point.targetSnippet} — $explanation",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = disclaimerMsg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Self-validation buttons
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = strings.rafiqSelfValidationPrompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.validateCurrentAyah(false) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.rafiqMarkNeedsWork, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { viewModel.validateCurrentAyah(true) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.rafiqMarkMastered, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    RafiqTab.QUICK_ANALYSIS -> {
                        // Direct Surah / Ayah Selector & Quick Analyzer
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = strings.rafiqQuickAnalyzerTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strings.rafiqQuickAnalyzerDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.selectAyah(1, 1) },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("1:1", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.selectAyah(112, 1) },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("112:1", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.selectAyah(114, 1) },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("114:1", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = String.format(java.util.Locale.getDefault(), strings.rafiqSurahAyahFormat, currentSurah, currentAyah),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (ayahText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = ayahText,
                                        style = QuranTextTypography.copy(fontSize = 20.sp, textAlign = TextAlign.Center),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                if (isRecording) {
                                    Button(
                                        onClick = { viewModel.stopRecordingAndAnalyze() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text(strings.rafiqStopAndAnalyze)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.startRecording() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text(if (lastTake != null) strings.rafiqReRecord else strings.rafiqRecordMicrophone)
                                    }
                                }
                            }
                        }

                        // Diagnostic view if available
                        diagnostic?.let { diag ->
                            val pacingMsg = when (currentLanguage) {
                                AppLanguage.ARABIC -> diag.pacingMessageAr
                                AppLanguage.ENGLISH -> diag.pacingMessageEn.ifBlank { diag.pacingMessageFr }
                                else -> diag.pacingMessageFr
                            }
                            val pauseMsg = when (currentLanguage) {
                                AppLanguage.ARABIC -> diag.pauseMessageAr
                                AppLanguage.ENGLISH -> diag.pauseMessageEn.ifBlank { diag.pauseMessageFr }
                                else -> diag.pauseMessageFr
                            }
                            val stabilityMsg = when (currentLanguage) {
                                AppLanguage.ARABIC -> diag.stabilityMessageAr
                                AppLanguage.ENGLISH -> diag.stabilityMessageEn.ifBlank { diag.stabilityMessageFr }
                                else -> diag.stabilityMessageFr
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = pacingMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "• $pauseMsg", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "• $stabilityMsg", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    RafiqTab.RECOMMENDATIONS -> {
                        // Smart Agenda breakdown
                        agenda?.let { ag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = strings.rafiqStrengthenRecommendationsTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = strings.rafiqDueVersesLabel, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "${ag.dueForReview.size}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = strings.rafiqWeakVersesLabel, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "${ag.weakAyahs.size}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = strings.rafiqTotalMemorizedLabel, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "${ag.memorizedTotal}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = strings.rafiqDailyGoalLabel, style = MaterialTheme.typography.bodySmall)
                                        val targetFormat = String.format(java.util.Locale.getDefault(), strings.rafiqDailyGoalFormat, ag.dailyTargetAyahs)
                                        Text(
                                            text = targetFormat,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Tajwid recommendation
                                    ag.recommendedTajwidLessonId?.let { lessonId ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = strings.rafiqRecommendedTajwidLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val tajwidTitle = when (currentLanguage) {
                                            AppLanguage.ARABIC -> ag.recommendedTajwidTitleAr ?: ag.recommendedTajwidTitleFr ?: ""
                                            else -> ag.recommendedTajwidTitleFr ?: ag.recommendedTajwidTitleAr ?: ""
                                        }
                                        Text(
                                            text = tajwidTitle,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (onNavigateToTajwidLesson != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedButton(
                                                onClick = { onNavigateToTajwidLesson(lessonId) },
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text(strings.rafiqOpenLesson, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
