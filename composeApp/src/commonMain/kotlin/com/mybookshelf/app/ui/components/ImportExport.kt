package com.mybookshelf.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookCsv
import com.mybookshelf.app.data.BookImport
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.files.rememberCsvOpener
import com.mybookshelf.app.files.rememberCsvSaver
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Returns a function `export(books, label)` that saves the books in a CSV file
 * named like "gnome-library-living-room-2026-11-19.csv",
 * then shows "12 books exported" (or "Export cancelled") in [snackbar].
 */
@Composable
fun rememberBookExporter(snackbar: SnackbarHostState): (books: List<BookInfo>, label: String?) -> Unit {
    val scope = rememberCoroutineScope()
    // How many books are being exported (kept across recompositions, read when the file is saved)
    val exportedCount = remember { intArrayOf(0) }
    val save = rememberCsvSaver { saved ->
        val message = if (saved) "${countText(exportedCount[0], "book")} exported" else "Export cancelled"
        scope.launch { snackbar.showSnackbar(message) }
    }
    return { books, label ->
        exportedCount[0] = books.size
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val name = listOfNotNull("gnome-library", label?.let(::fileNamePart), today).joinToString("-") + ".csv"
        save(name, BookCsv.export(books))
    }
}

/** "Living room (top)" -> "living-room-top": only letters, digits and dashes. Null if nothing is left. */
private fun fileNamePart(label: String): String? =
    label.lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }   // spaces, brackets, dots… become spaces
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }
        .joinToString("-")
        .ifEmpty { null }

/** Returns a function that lets the user pick a CSV file and imports it ([BookImport]). */
@Composable
fun rememberBookImporter(): () -> Unit = rememberCsvOpener { text ->
    if (text != null) BookImport.start(text)
}

/** Progress and result of an import. Shown on top of every page while there is something to say. */
@Composable
fun ImportStatusDialog() {
    val state by BookImport.state.collectAsState()
    val s = state ?: return
    AlertDialog(
        onDismissRequest = { BookImport.dismiss() },
        title = {
            Text(
                when (s) {
                    is BookImport.State.Running -> "Importing books…"
                    is BookImport.State.Finished -> "Import finished"
                    is BookImport.State.Failed -> "Import failed"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (s) {
                    is BookImport.State.Running -> {
                        Gnome(GnomePose.Carrying, size = 110.dp)
                        Text("Looking up ${s.done} / ${s.total} books online")
                        LinearProgressIndicator(
                            progress = { if (s.total == 0) 1f else s.done.toFloat() / s.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is BookImport.State.Finished -> {
                        val r = s.report
                        Gnome(if (r.added + r.placed > 0) GnomePose.Celebrating else GnomePose.Shy, size = 110.dp)
                        val lines = buildList {
                            add("${countText(r.added, "book")} added to your library.")
                            if (r.notFoundOnline > 0) {
                                add("${countText(r.notFoundOnline, "book")} not found online: added with the file's details only.")
                            }
                            if (r.placed > 0) add("${countText(r.placed, "book")} already there, now stored in their place.")
                            if (r.skipped > 0) add("${countText(r.skipped, "book")} already in your library: left as they were.")
                            if (r.invalidLines.isNotEmpty()) {
                                add("Lines without a valid ISBN, skipped: ${r.invalidLines.take(10).joinToString(", ")}" +
                                    if (r.invalidLines.size > 10) "…" else ".")
                            }
                        }
                        lines.forEach { Text(it, textAlign = TextAlign.Center) }
                    }
                    is BookImport.State.Failed -> {
                        Gnome(GnomePose.Confused, size = 110.dp)
                        Text(s.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (s !is BookImport.State.Running) {
                TextButton(onClick = { BookImport.dismiss() }) { Text("OK") }
            }
        },
    )
}
