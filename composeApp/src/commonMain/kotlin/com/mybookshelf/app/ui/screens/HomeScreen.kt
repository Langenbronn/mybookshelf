package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Home page.
 * @param onOpenLibrary called when the button is pressed (App.kt decides what happens).
 */
@Composable
fun HomeScreen(onOpenLibrary: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome to My Bookshelf", style = MaterialTheme.typography.headlineSmall)
        Text("Use the menu at the bottom to switch pages.")
        Button(onClick = onOpenLibrary) {
            Text("Open my library")
        }
    }
}
