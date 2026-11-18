package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose

/** Favorites page (empty for now). */
@Composable
fun FavoritesScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GnomeMessage(GnomePose.Love, "No favorite books yet.")
    }
}
