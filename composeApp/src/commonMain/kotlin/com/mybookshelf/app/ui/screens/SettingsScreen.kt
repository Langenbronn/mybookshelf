package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.ui.components.Gnome
import com.mybookshelf.app.ui.components.GnomePose

/** Settings page: shows how a screen keeps its own state. */
@Composable
fun SettingsScreen() {
    // Local state: the switch value (not saved yet, resets when leaving the page)
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showGnomes by remember { mutableStateOf(false) }

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
            headlineContent = { Text("Meet the gnome") },
            supportingContent = { Text("All the poses of the bookshelf gnome") },
            leadingContent = { Gnome(GnomePose.Waving, size = 40.dp) },
            modifier = Modifier.clickable { showGnomes = true },
        )
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("1.0") },
        )
    }

    if (showGnomes) GnomeGallery(onClose = { showGnomes = false })
}

/** Every gnome pose, two per row. */
@Composable
private fun GnomeGallery(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("The bookshelf gnome") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GnomePose.entries.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        pair.forEach { pose ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Gnome(pose, size = 100.dp)
                                Text(pose.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
    )
}
