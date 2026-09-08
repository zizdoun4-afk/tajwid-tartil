package com.example.ui.memorization

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.repository.SurahMemorizationProgress
import com.example.domain.model.Surah
import com.example.ui.components.ZelligeHeader
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import java.util.Locale

@Composable
fun MemorizationScreen(
    viewModel: MemorizationViewModel,
    onStartTraining: (surahNumber: Int, ayahNumber: Int, initialStep: String?) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToRafiq: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val isArabic = currentLang.code == "ar"
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showCustomTargetDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }

    // Dialog for custom target
    if (showCustomTargetDialog) {
        var customInput by remember { mutableStateOf(uiState.dailyTarget.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomTargetDialog = false },
            title = { Text(strings.hifzPlanSetCustomTitle) },
            text = {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) customInput = it },
                    label = { Text("Nombre de versets par jour") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = customInput.toIntOrNull()?.coerceAtLeast(1) ?: 3
                    viewModel.setDailyTarget(target)
                    showCustomTargetDialog = false
                }) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTargetDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Dialog for reminder hour
    if (showReminderTimeDialog) {
        var selectedHour by remember { mutableIntStateOf(uiState.reminderHour) }
        AlertDialog(
            onDismissRequest = { showReminderTimeDialog = false },
            title = { Text(strings.hifzReminderTimePrompt) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sélectionnez l'heure du rappel quotidien :", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(8, 13, 18, 20, 21, 22).forEach { hour ->
                            FilterChip(
                                selected = selectedHour == hour,
                                onClick = { selectedHour = hour },
                                label = { Text(String.format(Locale.getDefault(), "%02d:00", hour)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setReminderTime(selectedHour, 0)
                    showReminderTimeDialog = false
                }) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimeDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        ZelligeHeader(
            title = strings.memorizationTitle,
            subtitle = strings.memorizationSubtitle,
            arabicTitle = strings.memorizationHeaderArabic
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rafiq Al-Quran Smart Companion Hero Card
                if (onNavigateToRafiq != null) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToRafiq() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🎙", fontSize = 20.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = strings.rafiqTitle + " — " + strings.rafiqHeaderArabic,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = strings.rafiqDailySmartSessionDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text("▶", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // SECTION 1: Three Primary Actions (NEW / REVIEW / TEST)
                item {
                    HifzThreeActionsSection(
                        nextNewAyah = uiState.nextNewAyah,
                        dueReviewsCount = uiState.dueReviews.size,
                        nextTestAyah = uiState.nextTestAyah,
                        surahsMap = uiState.surahsMap,
                        onNewClick = {
                            onStartTraining(uiState.nextNewAyah.first, uiState.nextNewAyah.second, null)
                        },
                        onReviewClick = {
                            if (uiState.dueReviews.isNotEmpty()) {
                                val item = uiState.dueReviews.first()
                                onStartTraining(item.surahNumber, item.ayahNumber, null)
                            } else if (uiState.reviewList.isNotEmpty()) {
                                val item = uiState.reviewList.first()
                                onStartTraining(item.surahNumber, item.ayahNumber, null)
                            } else {
                                Toast.makeText(context, strings.hifzNoDueReviews, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTestClick = {
                            val test = uiState.nextTestAyah
                            if (test != null) {
                                onStartTraining(test.first, test.second, "BLIND_TEST")
                            } else {
                                onStartTraining(uiState.nextNewAyah.first, uiState.nextNewAyah.second, "BLIND_TEST")
                            }
                        }
                    )
                }

                // SECTION 2: Daily Memorization Plan Card
                // SECTION 2: Daily Memorization Plan Card with Quick Start
                item {
                    val highestPriorityAyah = uiState.dailyPlan?.prioritizedAyahs?.firstOrNull()
                        ?: uiState.smartReviewQueue.firstOrNull()
                        ?: uiState.dueReviews.firstOrNull()

                    DailyPlanCard(
                        dailyPlan = uiState.dailyPlan,
                        dailyTarget = uiState.dailyTarget,
                        completedToday = uiState.completedToday,
                        onSelectTarget = { target -> viewModel.setDailyTarget(target) },
                        onCustomTargetClick = { showCustomTargetDialog = true },
                        onQuickStartSession = {
                            if (highestPriorityAyah != null) {
                                onStartTraining(highestPriorityAyah.surahNumber, highestPriorityAyah.ayahNumber, null)
                            } else {
                                onStartTraining(uiState.nextNewAyah.first, uiState.nextNewAyah.second, null)
                            }
                        }
                    )
                }

                // SECTION 2.5: SRS Health & Streak Dashboard Card
                item {
                    SrsHealthAndStreakCard(
                        streakDays = uiState.currentStreakDays,
                        weeklyReviewed = uiState.versesReviewedWeek,
                        srsHealth = uiState.srsHealth
                    )
                }

                // SECTION 3: Visual Progress Journey (Started -> Learning -> Reviewing -> Memorized)
                item {
                    ProgressJourneyCard(
                        startedCount = uiState.versesStartedTotal,
                        learningCount = uiState.versesLearningTotal,
                        reviewingCount = uiState.versesInReviewTotal,
                        memorizedCount = uiState.versesMemorizedTotal
                    )
                }

                // SECTION 4: Local Daily Reminder Switch Card
                item {
                    LocalReminderCard(
                        isEnabled = uiState.isReminderEnabled,
                        hour = uiState.reminderHour,
                        minute = uiState.reminderMinute,
                        onToggle = { viewModel.toggleReminder(it) },
                        onTimeClick = { showReminderTimeDialog = true }
                    )
                }

                // SECTION 5: Sub-tabs (File intelligente / Versets faibles / Mes sourates / Les 30 Ajza')
                item {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings.hifzTabSmartQueue, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                    if (uiState.smartReviewQueue.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${uiState.smartReviewQueue.size}",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings.hifzTabWeakVerses, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                    if (uiState.weakVersesList.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${uiState.weakVersesList.size}",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text(strings.hifzMySurahs, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                        )
                        Tab(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            text = { Text(strings.hifzTabJuz, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                        )
                        Tab(
                            selected = selectedTabIndex == 4,
                            onClick = { selectedTabIndex = 4 },
                            text = { Text(strings.hifzTabJourney, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> {
                        // Smart Review Queue (Prioritized: Overdue -> Failed -> Approaching -> Normal -> Learning)
                        if (uiState.smartReviewQueue.isEmpty()) {
                            item {
                                EmptyStateCard(message = strings.hifzNoDueReviews)
                            }
                        } else {
                            items(uiState.smartReviewQueue, key = { "smart_${it.surahNumber}:${it.ayahNumber}" }) { item ->
                                val surah = uiState.surahsMap[item.surahNumber]
                                SmartQueueCardItem(
                                    entity = item,
                                    surah = surah,
                                    onStartTraining = { onStartTraining(item.surahNumber, item.ayahNumber, null) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Weak verses needing reinforcement
                        if (uiState.weakVersesList.isEmpty()) {
                            item {
                                EmptyStateCard(message = "ما شاء الله! لا توجد آيات ضعيفة حالياً.")
                            }
                        } else {
                            items(uiState.weakVersesList, key = { "weak_${it.surahNumber}:${it.ayahNumber}" }) { item ->
                                val surah = uiState.surahsMap[item.surahNumber]
                                WeakVerseCardItem(
                                    entity = item,
                                    surah = surah,
                                    onStartTraining = { onStartTraining(item.surahNumber, item.ayahNumber, null) }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Mes sourates: Progress bars per surah with Learning %, Review %, Remaining
                        if (uiState.surahsProgress.isEmpty()) {
                            item {
                                EmptyStateCard(message = strings.hifzNoSurahsInProgress)
                            }
                        } else {
                            items(uiState.surahsProgress, key = { it.surahNumber }) { progress ->
                                val surah = uiState.surahsMap[progress.surahNumber]
                                SurahProgressCardItem(
                                    progress = progress,
                                    surah = surah,
                                    onOpenSurah = { onStartTraining(progress.surahNumber, 1, null) }
                                )
                            }
                        }
                    }

                    3 -> {
                        // Les 30 Ajza' progress
                        items(uiState.juzProgressList, key = { it.juzNumber }) { juzProgress ->
                            JuzProgressCardItem(
                                progress = juzProgress,
                                onOpenJuz = {
                                    val metadata = com.example.domain.hifz.SmartHifzPlanner.JUZ_CATALOG.find { it.juzNumber == juzProgress.juzNumber }
                                    if (metadata != null) {
                                        onStartTraining(metadata.startSurah, metadata.startAyah, null)
                                    }
                                }
                            )
                        }
                    }

                    4 -> {
                        // Professional Quran Journey (مسار رحلتي)
                        item {
                            QuranJourneyDashboard(
                                uiState = uiState,
                                strings = strings,
                                isArabic = isArabic,
                                onPeriodChange = { viewModel.setPeriodDays(it) },
                                onOpenTraining = onStartTraining
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HifzThreeActionsSection(
    nextNewAyah: Pair<Int, Int>,
    dueReviewsCount: Int,
    nextTestAyah: Pair<Int, Int>?,
    surahsMap: Map<Int, Surah>,
    onNewClick: () -> Unit,
    onReviewClick: () -> Unit,
    onTestClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val newSurah = surahsMap[nextNewAyah.first]?.name ?: "Sourate ${nextNewAyah.first}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ACTION 1: NEW (Apprendre)
        ActionCardItem(
            modifier = Modifier.weight(1f),
            title = strings.hifzActionNew,
            subtitle = "$newSurah (${nextNewAyah.second})",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            accentColor = Color(0xFF1B5E20),
            backgroundColor = Color(0xFFE8F5E9),
            badge = null,
            onClick = onNewClick
        )

        // ACTION 2: REVIEW (Réviser)
        ActionCardItem(
            modifier = Modifier.weight(1f),
            title = strings.hifzActionReview,
            subtitle = if (dueReviewsCount > 0) "$dueReviewsCount verset(s)" else "À jour",
            icon = Icons.Default.Repeat,
            accentColor = Color(0xFFE65100),
            backgroundColor = Color(0xFFFFF3E0),
            badge = if (dueReviewsCount > 0) "$dueReviewsCount" else null,
            onClick = onReviewClick
        )

        // ACTION 3: TEST (Tester)
        ActionCardItem(
            modifier = Modifier.weight(1f),
            title = strings.hifzActionTest,
            subtitle = "Sans regarder",
            icon = Icons.Default.Psychology,
            accentColor = Color(0xFF4A148C),
            backgroundColor = Color(0xFFF3E5F5),
            badge = null,
            onClick = onTestClick
        )
    }
}

@Composable
fun ActionCardItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    backgroundColor: Color,
    badge: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            if (badge != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyPlanCard(
    dailyPlan: com.example.domain.hifz.DailyHifzPlan?,
    dailyTarget: Int,
    completedToday: Int,
    onSelectTarget: (Int) -> Unit,
    onCustomTargetClick: () -> Unit,
    onQuickStartSession: () -> Unit
) {
    val strings = LocalAppStrings.current
    val progress = if (dailyTarget > 0) (completedToday.toFloat() / dailyTarget).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.hifzPlanTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = String.format(strings.hifzPlanTargetFormat, dailyTarget),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown pills: due reviews, weak verses, new verses
            if (dailyPlan != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE65100).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${dailyPlan.dueReviewsCount} à réviser",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${dailyPlan.weakVersesCount} faibles",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1B5E20).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${dailyPlan.newVersesCount} nouveaux",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "dailyProgress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF2E7D32),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = String.format(strings.hifzPlanTodayProgressFormat, completedToday, dailyTarget),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Start CTA button: "ابدأ جلسة اليوم"
            Button(
                onClick = onQuickStartSession,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.hifzStartTodaySession,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Plan target selector chips: 1, 3, 5, 10, Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    1 to strings.hifzPlanTarget1,
                    3 to strings.hifzPlanTarget3,
                    5 to strings.hifzPlanTarget5,
                    10 to strings.hifzPlanTarget10
                ).forEach { (target, label) ->
                    val isSelected = dailyTarget == target
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTarget(target) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Custom button
                val isCustom = dailyTarget !in listOf(1, 3, 5, 10)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCustomTargetClick() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isCustom) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isCustom) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SrsHealthAndStreakCard(
    streakDays: Int,
    weeklyReviewed: Int,
    srsHealth: com.example.domain.hifz.SrsHealthSummary?
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF6F00).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(strings.hifzStreakFormat, streakDays),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                // Weekly progress badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📈", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(strings.hifzWeeklyReviewedFormat, weeklyReviewed),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (srsHealth != null) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SrsHealthPill(
                        modifier = Modifier.weight(1f),
                        label = strings.hifzSrsStrongLabel,
                        count = srsHealth.strongCount,
                        color = Color(0xFF2E7D32)
                    )
                    SrsHealthPill(
                        modifier = Modifier.weight(1f),
                        label = strings.hifzSrsInProgressLabel,
                        count = srsHealth.inProgressCount,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SrsHealthPill(
                        modifier = Modifier.weight(1f),
                        label = strings.hifzSrsWeakLabel,
                        count = srsHealth.weakCount,
                        color = MaterialTheme.colorScheme.error
                    )
                    SrsHealthPill(
                        modifier = Modifier.weight(1f),
                        label = strings.hifzSrsOverdueLabel,
                        count = srsHealth.overdueCount,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}

@Composable
fun SrsHealthPill(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ProgressJourneyCard(
    startedCount: Int,
    learningCount: Int,
    reviewingCount: Int,
    memorizedCount: Int
) {
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = strings.hifzJourneyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JourneyStagePill(
                    modifier = Modifier.weight(1f),
                    stageName = strings.hifzJourneyStarted,
                    count = startedCount,
                    accentColor = MaterialTheme.colorScheme.primary
                )
                JourneyStagePill(
                    modifier = Modifier.weight(1f),
                    stageName = strings.hifzJourneyLearning,
                    count = learningCount,
                    accentColor = MaterialTheme.colorScheme.secondary
                )
                JourneyStagePill(
                    modifier = Modifier.weight(1f),
                    stageName = strings.hifzJourneyReviewing,
                    count = reviewingCount,
                    accentColor = Color(0xFFE65100)
                )
                JourneyStagePill(
                    modifier = Modifier.weight(1f),
                    stageName = strings.hifzJourneyMemorized,
                    count = memorizedCount,
                    accentColor = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun JourneyStagePill(
    modifier: Modifier = Modifier,
    stageName: String,
    count: Int,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stageName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LocalReminderCard(
    isEnabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.hifzReminderSettingsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isEnabled) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d — Appuyez pour modifier", hour, minute),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onTimeClick() }
                    )
                } else {
                    Text(
                        text = strings.hifzReminderToggle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun DueReviewCardItem(
    entity: MemorizationStatusEntity,
    surah: Surah?,
    onStartTraining: () -> Unit
) {
    val strings = LocalAppStrings.current
    val now = System.currentTimeMillis()
    val dueEpoch = entity.nextReviewDueEpochMillis ?: now
    val diffDays = ((dueEpoch - now) / (24L * 60 * 60 * 1000)).toInt()

    val badgeText = when {
        diffDays < 0 -> String.format(strings.hifzDueBadgeOverdueFormat, -diffDays)
        diffDays == 0 -> strings.hifzDueBadgeToday
        else -> String.format(strings.hifzDueBadgeDaysFormat, diffDays)
    }

    val badgeColor = when {
        diffDays < 0 -> MaterialTheme.colorScheme.error
        diffDays == 0 -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entity.ayahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah?.name ?: "Sourate ${entity.surahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Répétition #${entity.reviewCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onStartTraining,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.hifzStartTraining)
            }
        }
    }
}

@Composable
fun SurahProgressCardItem(
    progress: SurahMemorizationProgress,
    surah: Surah?,
    onOpenSurah: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSurah() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = surah?.name ?: "Sourate ${progress.surahNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = surah?.englishName ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${progress.percentage.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (progress.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF2E7D32),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(strings.hifzProgressFormat, progress.memorizedCount, progress.totalAyahs, progress.percentage.toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(strings.hifzRemainingVersesFormat, progress.remainingAyahs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SmartQueueCardItem(
    entity: MemorizationStatusEntity,
    surah: Surah?,
    onStartTraining: () -> Unit
) {
    val strings = LocalAppStrings.current
    val now = System.currentTimeMillis()
    val dueEpoch = entity.nextReviewDueEpochMillis ?: now
    val diffDays = ((dueEpoch - now) / (24L * 60 * 60 * 1000)).toInt()

    // Determine priority label & color
    val (priorityLabel, badgeColor) = when {
        entity.isOverdue(now) -> {
            val days = (-diffDays).coerceAtLeast(1)
            String.format(strings.hifzDueBadgeOverdueFormat, days) to MaterialTheme.colorScheme.error
        }
        entity.failedTests >= 2 -> {
            "Échec répété (${entity.failedTests}x)" to MaterialTheme.colorScheme.error
        }
        entity.nextReviewDueEpochMillis != null && entity.nextReviewDueEpochMillis <= (now + 24L * 60 * 60 * 1000) -> {
            "Dans les 24h" to Color(0xFFE65100)
        }
        entity.status == com.example.domain.model.MemorizationStatus.REVIEW.name -> {
            "Révision #${entity.reviewCount}" to MaterialTheme.colorScheme.primary
        }
        else -> {
            "Nouveau / En cours" to Color(0xFF1B5E20)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entity.ayahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah?.name ?: "Sourate ${entity.surahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = priorityLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (entity.successfulTests > 0 || entity.failedTests > 0) {
                        Text(
                            text = "${entity.successfulTests}✓ ${entity.failedTests}✗",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = onStartTraining,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.hifzStartTraining)
            }
        }
    }
}

@Composable
fun WeakVerseCardItem(
    entity: MemorizationStatusEntity,
    surah: Surah?,
    onStartTraining: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entity.ayahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah?.name ?: "Sourate ${entity.surahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Échecs : ${entity.failedTests} • Succès : ${entity.successfulTests}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Button(
                onClick = onStartTraining,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Renforcer")
            }
        }
    }
}

@Composable
fun JuzProgressCardItem(
    progress: com.example.domain.hifz.JuzMemorizationProgress,
    onOpenJuz: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenJuz() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${progress.juzNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = progress.nameArabic,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Juz ${progress.juzNumber} • ${progress.totalAyahs} versets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${progress.memorizedPercentage.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (progress.memorizedPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF2E7D32),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${progress.memorizedCount} / ${progress.totalAyahs} mémorisés",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(strings.hifzRemainingVersesFormat, progress.remainingAyahs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QuranJourneyDashboard(
    uiState: HifzDashboardUiState,
    strings: AppStrings,
    isArabic: Boolean,
    onPeriodChange: (Int) -> Unit,
    onOpenTraining: (Int, Int, String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visual Stage Progression (بدأت → أتعلم → أراجع → أتقن)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌱", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.journeyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JourneyStageChip(
                        title = strings.journeyStageStarted,
                        count = uiState.versesStartedTotal,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text("›", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    JourneyStageChip(
                        title = strings.journeyStageLearning,
                        count = uiState.versesLearningTotal,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text("›", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    JourneyStageChip(
                        title = strings.journeyStageReview,
                        count = uiState.versesInReviewTotal,
                        color = Color(0xFFFFF3E0),
                        textColor = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                    Text("›", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    JourneyStageChip(
                        title = strings.journeyStageMastered,
                        count = uiState.versesMemorizedTotal,
                        color = Color(0xFFE8F5E9),
                        textColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Period Switcher (7 days vs 30 days)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.selectedPeriodDays == 7) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.clickable { onPeriodChange(7) }
                    ) {
                        Text(
                            text = strings.journeyPeriod7Days,
                            color = if (uiState.selectedPeriodDays == 7) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.selectedPeriodDays == 30) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.clickable { onPeriodChange(30) }
                    ) {
                        Text(
                            text = strings.journeyPeriod30Days,
                            color = if (uiState.selectedPeriodDays == 30) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Card 1: Hifz & Spaced Review
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.journeyHifzStatsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JourneyStatTile(
                        value = "${uiState.versesMemorizedTotal}",
                        label = strings.hifzMemorizedVerses,
                        modifier = Modifier.weight(1f)
                    )
                    val reviewedPeriodCount = if (uiState.selectedPeriodDays == 7) uiState.versesReviewedWeek else uiState.versesReviewedMonth
                    JourneyStatTile(
                        value = "$reviewedPeriodCount",
                        label = if (uiState.selectedPeriodDays == 7) strings.journeyPeriod7Days else strings.journeyPeriod30Days,
                        modifier = Modifier.weight(1f)
                    )
                    JourneyStatTile(
                        value = "${uiState.currentStreakDays} 🔥",
                        label = strings.hifzStreakLabel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card 2: Quran Reading & Recordings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.journeyQuranStatsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JourneyStatTile(
                        value = "${uiState.lastReadSurahNumber}:${uiState.lastReadAyahNumber}",
                        label = strings.continueReading,
                        modifier = Modifier.weight(1f)
                    )
                    JourneyStatTile(
                        value = "${uiState.totalRecordingsCount}",
                        label = strings.journeyRecitationStatsTitle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card 3: Tajwid Academy Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.journeyTajwidStatsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JourneyStatTile(
                        value = "${uiState.tajwidLessonsCompleted} / ${uiState.totalTajwidLessons}",
                        label = strings.tajwidCompletedBadge,
                        modifier = Modifier.weight(1f)
                    )
                    val tajwidPercent = if (uiState.totalTajwidLessons > 0) {
                        (uiState.tajwidLessonsCompleted * 100) / uiState.totalTajwidLessons
                    } else 0
                    JourneyStatTile(
                        value = "$tajwidPercent%",
                        label = strings.progressLabel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun JourneyStageChip(
    title: String,
    count: Int,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = textColor,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun JourneyStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

