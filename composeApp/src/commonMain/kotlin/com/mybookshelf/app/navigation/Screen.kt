package com.mybookshelf.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The list of pages shown in the bottom menu.
 *
 * To ADD a page:
 *   1. Add an entry here (title + icon).
 *   2. Create its screen in ui/screens/.
 *   3. Map it in the `when` inside App.kt.
 *
 * Browse available icons: type `Icons.Filled.` and let autocompletion help you.
 */
enum class Screen(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Scan("Scan", Icons.Filled.Search),
    Library("Library", Icons.AutoMirrored.Filled.List),
    Favorites("Favorites", Icons.Filled.Favorite),
    Settings("Settings", Icons.Filled.Settings),
}
