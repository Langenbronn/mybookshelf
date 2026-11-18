package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Settings page: shows how a screen keeps its own state. */
@Composable
fun SettingsScreen() {
    // Local state: the switch value (not saved yet, resets when leaving the page)
    var notificationsEnabled by remember { mutableStateOf(true) }

    Column {
        ListItem(
            headlineContent = { Text("Notifications") },
            trailingContent = {
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                )
            },
        )
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("1.0") },
        )
    }
}
