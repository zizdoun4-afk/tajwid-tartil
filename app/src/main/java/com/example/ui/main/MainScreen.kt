package com.example.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ayahreader.AyahReaderScreen
import com.example.ui.ayahreader.AyahReaderViewModel
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.memorization.MemorizationScreen
import com.example.ui.memorization.MemorizationViewModel
import com.example.ui.recordingslibrary.RecordingsLibraryScreen
import com.example.ui.recordingslibrary.RecordingsLibraryViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.surahlist.SurahListScreen
import com.example.ui.surahlist.SurahListViewModel
import com.example.ui.training.TrainingSessionScreen
import com.example.ui.training.TrainingSessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.School

sealed class BottomNavRoute(val route: String, val icon: ImageVector) {
    object Surahs : BottomNavRoute("surahs", Icons.AutoMirrored.Filled.MenuBook)
    object Tajwid : BottomNavRoute("tajwid", Icons.Default.School)
    object Memorization : BottomNavRoute("memorization", Icons.Default.Psychology)
    object Library : BottomNavRoute("library", Icons.Default.LibraryMusic)
    object Settings : BottomNavRoute("settings", Icons.Default.Settings)
}

val BOTTOM_NAV_ITEMS = listOf(
    BottomNavRoute.Surahs,
    BottomNavRoute.Tajwid,
    BottomNavRoute.Memorization,
    BottomNavRoute.Library
)

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val strings = LocalAppStrings.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in BOTTOM_NAV_ITEMS.map { it.route }

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 2.dp
                ) {
                    BOTTOM_NAV_ITEMS.forEach { item ->
                        val selected = currentRoute == item.route
                        val labelText = when (item) {
                            BottomNavRoute.Surahs -> strings.navSurahs
                            BottomNavRoute.Tajwid -> strings.navTajwid
                            BottomNavRoute.Memorization -> strings.navMemorization
                            BottomNavRoute.Library -> strings.navLibrary
                            BottomNavRoute.Settings -> strings.navSettings
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = labelText,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { 
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${item.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavRoute.Surahs.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavRoute.Surahs.route) {
                val surahViewModel: SurahListViewModel = viewModel()
                val coroutineScope = rememberCoroutineScope()
                SurahListScreen(
                    viewModel = surahViewModel,
                    onSurahClick = { surahNum ->
                        navController.navigate("reader/$surahNum/0")
                    },
                    onContinueReadingClick = { surahNum ->
                        coroutineScope.launch {
                            val ayahIndex = surahViewModel.lastReadAyahIndex.first()
                            navController.navigate("reader/$surahNum/$ayahIndex")
                        }
                    },
                    onSettingsClick = {
                        navController.navigate(BottomNavRoute.Settings.route)
                    }
                )
            }

            composable(BottomNavRoute.Library.route) {
                val recordingsViewModel: RecordingsLibraryViewModel = viewModel()
                RecordingsLibraryScreen(
                    viewModel = recordingsViewModel,
                    onOpenInReader = { surahNum, ayahIndex ->
                        navController.navigate("reader/$surahNum/$ayahIndex")
                    }
                )
            }

            composable(BottomNavRoute.Memorization.route) {
                val memorizationViewModel: MemorizationViewModel = viewModel()
                MemorizationScreen(
                    viewModel = memorizationViewModel,
                    onStartTraining = { surahNum, ayahNum, initialStep ->
                        val route = if (initialStep != null) {
                            "training/$surahNum/$ayahNum?step=$initialStep"
                        } else {
                            "training/$surahNum/$ayahNum"
                        }
                        navController.navigate(route)
                    }
                )
            }

            composable(BottomNavRoute.Tajwid.route) {
                val tajwidViewModel: com.example.ui.tajwid.TajwidHubViewModel = viewModel()
                com.example.ui.tajwid.TajwidHubScreen(
                    viewModel = tajwidViewModel,
                    onOpenLesson = { lessonId ->
                        navController.navigate("tajwid_lesson/$lessonId")
                    }
                )
            }

            composable(
                route = "tajwid_lesson/{lessonId}",
                arguments = listOf(
                    navArgument("lessonId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
                val lessonDetailViewModel: com.example.ui.tajwid.TajwidLessonDetailViewModel = viewModel(
                    key = "lesson_$lessonId",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            val app = (navController.context.applicationContext as android.app.Application)
                            return com.example.ui.tajwid.TajwidLessonDetailViewModel(app, lessonId) as T
                        }
                    }
                )

                com.example.ui.tajwid.TajwidLessonDetailScreen(
                    viewModel = lessonDetailViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(BottomNavRoute.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }

            composable(
                route = "reader/{surahNumber}/{startAyahIndex}",
                arguments = listOf(
                    navArgument("surahNumber") { type = NavType.IntType },
                    navArgument("startAyahIndex") { type = NavType.IntType; defaultValue = 0 }
                )
            ) { backStackEntry ->
                val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
                val startAyahIndex = backStackEntry.arguments?.getInt("startAyahIndex") ?: 0
                val ayahViewModel: AyahReaderViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            val app = (navController.context.applicationContext as android.app.Application)
                            return AyahReaderViewModel(app, surahNumber, startAyahIndex) as T
                        }
                    }
                )

                AyahReaderScreen(
                    viewModel = ayahViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onStartTraining = { sNum, aNum ->
                        navController.navigate("training/$sNum/$aNum")
                    },
                    onReadWithMeClick = { sNum, aIdx ->
                        navController.navigate("read_with_me/$sNum/$aIdx")
                    }
                )
            }

            composable(
                route = "read_with_me/{surahNumber}/{ayahIndex}",
                arguments = listOf(
                    navArgument("surahNumber") { type = NavType.IntType },
                    navArgument("ayahIndex") { type = NavType.IntType; defaultValue = 0 }
                )
            ) { backStackEntry ->
                val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
                val ayahIndex = backStackEntry.arguments?.getInt("ayahIndex") ?: 0
                val readWithMeViewModel: com.example.ui.tajwid.ReadWithMeViewModel = viewModel(
                    key = "read_with_me_${surahNumber}_$ayahIndex",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            val app = (navController.context.applicationContext as android.app.Application)
                            return com.example.ui.tajwid.ReadWithMeViewModel(app, surahNumber, ayahIndex) as T
                        }
                    }
                )

                com.example.ui.tajwid.ReadWithMeScreen(
                    viewModel = readWithMeViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "training/{surahNumber}/{ayahNumber}?step={step}",
                arguments = listOf(
                    navArgument("surahNumber") { type = NavType.IntType },
                    navArgument("ayahNumber") { type = NavType.IntType },
                    navArgument("step") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
                val ayahNumber = backStackEntry.arguments?.getInt("ayahNumber") ?: 1
                val stepArg = backStackEntry.arguments?.getString("step")
                val initialStep = when (stepArg?.uppercase()) {
                    "BLIND_TEST" -> com.example.ui.training.TrainingStep.BLIND_TEST
                    else -> com.example.ui.training.TrainingStep.LISTEN_3X
                }
                val trainingViewModel: TrainingSessionViewModel = viewModel(
                    key = "training_${surahNumber}_${ayahNumber}_$stepArg",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            val app = (navController.context.applicationContext as android.app.Application)
                            return TrainingSessionViewModel(app, surahNumber, ayahNumber, initialStep) as T
                        }
                    }
                )

                TrainingSessionScreen(
                    viewModel = trainingViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
