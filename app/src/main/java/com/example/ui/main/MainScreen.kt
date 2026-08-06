package com.example.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LibraryMusic
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
import com.example.ui.recordingslibrary.RecordingsLibraryScreen
import com.example.ui.recordingslibrary.RecordingsLibraryViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.surahlist.SurahListScreen
import com.example.ui.surahlist.SurahListViewModel

sealed class BottomNavRoute(val route: String, val icon: ImageVector) {
    object Surahs : BottomNavRoute("surahs", Icons.AutoMirrored.Filled.MenuBook)
    object Library : BottomNavRoute("library", Icons.Default.LibraryMusic)
    object Settings : BottomNavRoute("settings", Icons.Default.Settings)
}

val BOTTOM_NAV_ITEMS = listOf(
    BottomNavRoute.Surahs,
    BottomNavRoute.Library,
    BottomNavRoute.Settings
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
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    BOTTOM_NAV_ITEMS.forEach { item ->
                        val selected = currentRoute == item.route
                        val labelText = when (item) {
                            BottomNavRoute.Surahs -> strings.navSurahs
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
                                    contentDescription = labelText
                                )
                            },
                            label = { Text(labelText) },
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
                SurahListScreen(
                    viewModel = surahViewModel,
                    onSurahClick = { surahNum ->
                        navController.navigate("reader/$surahNum")
                    }
                )
            }

            composable(BottomNavRoute.Library.route) {
                val recordingsViewModel: RecordingsLibraryViewModel = viewModel()
                RecordingsLibraryScreen(
                    viewModel = recordingsViewModel
                )
            }

            composable(BottomNavRoute.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }

            composable(
                route = "reader/{surahNumber}",
                arguments = listOf(navArgument("surahNumber") { type = NavType.IntType })
            ) { backStackEntry ->
                val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
                val ayahViewModel: AyahReaderViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            val app = (navController.context.applicationContext as android.app.Application)
                            return AyahReaderViewModel(app, surahNumber) as T
                        }
                    }
                )

                AyahReaderScreen(
                    viewModel = ayahViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
