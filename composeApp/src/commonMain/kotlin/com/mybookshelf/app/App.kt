package com.mybookshelf.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mybookshelf.app.navigation.Screen
import com.mybookshelf.app.ui.screens.FavoritesScreen
import com.mybookshelf.app.ui.screens.HomeScreen
import com.mybookshelf.app.ui.screens.LibraryScreen
import com.mybookshelf.app.ui.screens.ScanScreen
import com.mybookshelf.app.ui.screens.SettingsScreen
import com.mybookshelf.app.ui.theme.AppTheme

/**
 * Root of the application, shared by Android and iOS.
 *
 * Layout:
 *   ┌────────────────────┐
 *   │   Top bar (title)  │
 *   ├────────────────────┤
 *   │                    │
 *   │   Current page     │
 *   │                    │
 *   ├────────────────────┤
 *   │ Bottom menu (tabs) │
 *   └────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    AppTheme {
        // Which page is currently selected. "Saveable" = survives screen rotation.
        var currentScreen by rememberSaveable { mutableStateOf(Screen.Home) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text(currentScreen.title) })
            },
            bottomBar = {
                NavigationBar {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = screen == currentScreen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            // innerPadding = space taken by the top bar and bottom menu
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(onOpenLibrary = { currentScreen = Screen.Library })
                    Screen.Scan -> ScanScreen()
                    Screen.Library -> LibraryScreen()
                    Screen.Favorites -> FavoritesScreen()
                    Screen.Settings -> SettingsScreen()
                }
            }
        }
    }
}
