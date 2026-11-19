package com.mybookshelf.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.navigation.Screen
import com.mybookshelf.app.ui.components.BookshelfStrip
import com.mybookshelf.app.ui.components.Gnome
import com.mybookshelf.app.ui.screens.FavoritesScreen
import com.mybookshelf.app.ui.screens.LibraryScreen
import com.mybookshelf.app.ui.screens.PlacesScreen
import com.mybookshelf.app.ui.screens.ScanScreen
import com.mybookshelf.app.ui.screens.SettingsScreen
import com.mybookshelf.app.ui.theme.AppTheme

/**
 * Root of the application, shared by Android and iOS.
 *
 * Layout:
 *   ┌────────────────────┐
 *   │ Gnome + title      │  green top bar, the gnome changes with the page
 *   │ Bookshelf strip    │  decoration
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
        // Which page is currently selected (the app opens on the Library). "Saveable" = survives screen rotation.
        var currentScreen by rememberSaveable { mutableStateOf(Screen.Library) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Gnome(currentScreen.gnome, size = 44.dp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(currentScreen.title, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "My Bookshelf",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    BookshelfStrip()
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = screen == currentScreen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            // innerPadding = space taken by the top bar and bottom menu
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentScreen) {
                    Screen.Scan -> ScanScreen()
                    Screen.Library -> LibraryScreen(onScan = { currentScreen = Screen.Scan })
                    Screen.Places -> PlacesScreen()
                    Screen.Favorites -> FavoritesScreen()
                    Screen.Settings -> SettingsScreen()
                }
            }
        }
    }
}
