package com.mybookshelf.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.mybookshelf.app.ui.components.GnomePose

/**
 * The list of pages shown in the bottom menu (the first one is shown when the app opens).
 * [gnome] = what the little gnome in the top bar is doing on that page.
 *
 * To ADD a page:
 *   1. Add an entry here (title + icon + gnome).
 *   2. Create its screen in ui/screens/.
 *   3. Map it in the `when` inside App.kt.
 *
 * Browse available icons: type `Icons.Filled.` and let autocompletion help you.
 */
enum class Screen(val title: String, val icon: ImageVector, val gnome: GnomePose) {
    Library("Library", Icons.AutoMirrored.Filled.List, GnomePose.Reading),
    Scan("Scan", Icons.Filled.Search, GnomePose.Searching),
    Places("Places", Icons.Filled.Place, GnomePose.Carrying),
    Favorites("Favorites", Icons.Filled.Favorite, GnomePose.Love),
    Settings("Settings", Icons.Filled.Settings, GnomePose.Tea),
}
