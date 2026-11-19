package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.data.SampleBooks
import com.mybookshelf.app.ui.components.Gnome
import com.mybookshelf.app.ui.components.GnomePose

/** Settings page: the gnome gallery and the test data. */
@Composable
fun SettingsScreen() {
    var showGnomes by remember { mutableStateOf(false) }
    var confirmRemoveAll by remember { mutableStateOf(false) }
    val books by Library.books.collectAsState()
    val download by SampleBooks.progress.collectAsState()

    Column(Modifier.verticalScroll(rememberScrollState())) {
        ListItem(
            headlineContent = { Text("Meet the gnome") },
            supportingContent = { Text("All ${GnomePose.entries.size} poses of the bookshelf gnome") },
            leadingContent = { Gnome(GnomePose.Waving, size = 40.dp) },
            modifier = Modifier.clickable { showGnomes = true },
        )

        // ---- Test data ----
        Text(
            "Test data",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
        )
        ListItem(
            headlineContent = { Text("Add ${SampleBooks.isbns.size} test books") },
            supportingContent = {
                val progress = download
                if (progress == null) {
                    Text("Real books of every age, from baby books to adult novels")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Downloading ${progress.done} / ${progress.total}…")
                        LinearProgressIndicator(
                            progress = { progress.done.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            modifier = Modifier.clickable(enabled = download == null) { SampleBooks.addAllToLibrary() },
        )
        ListItem(
            headlineContent = { Text("Remove all books") },
            supportingContent = { Text("Your library has ${books.size} books") },
            modifier = Modifier.clickable(enabled = books.isNotEmpty()) { confirmRemoveAll = true },
        )

        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("1.0") },
        )
    }

    if (showGnomes) GnomeGallery(onClose = { showGnomes = false })

    if (confirmRemoveAll) {
        AlertDialog(
            onDismissRequest = { confirmRemoveAll = false },
            title = { Text("Remove all books?") },
            text = { Text("All ${books.size} books will be removed from your library.") },
            confirmButton = {
                TextButton(onClick = {
                    Library.removeAll()
                    confirmRemoveAll = false
                }) { Text("Remove all") }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveAll = false }) { Text("Cancel") } },
        )
    }
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
