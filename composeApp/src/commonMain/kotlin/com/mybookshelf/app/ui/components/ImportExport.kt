package com.mybookshelf.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookCsv
import com.mybookshelf.app.data.BookImport
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.files.rememberCsvOpener
import com.mybookshelf.app.files.rememberCsvSaver
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Returns a function `export(books, label)` that saves the books in a CSV file
 * named like "my-bookshelf-living-room-2026-11-19.csv". [onDone] gets a message to show.
 */
@Composable
fun rememberBookExporter(onDone: (message: String) -> Unit): (books: List<BookInfo>, label: String?) -> Unit {
    // How many books are being exported (kept across recompositions)
    val lastCount = remember { intArrayOf(0) }
    val save = rememberCsvSaver { saved ->
        onDone(if (saved) "${countText(lastCount[0])} exported" else "Export cancelled")
    }
    return { books, label ->
        lastCount[0] = books.size
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val slug = label?.lowercase()?.map { if (it.isLetterOrDigit()) it else '-' }?.joinToString("")
            ?.split('-')?.filter { it.isNotEmpty() }?.joinToString("-")?.ifEmpty { null }
        val name = listOfNotNull("my-bookshelf", slug, today.toString()).joinToString("-") + ".csv"
        save(name, BookCsv.export(books))
    }
}

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
                            add("${countText(r.added)} added to your library.")
                            if (r.notFoundOnline > 0) {
                                add("${countText(r.notFoundOnline)} not found online: added with the file's details only.")
                            }
                            if (r.placed > 0) add("${countText(r.placed)} already there, now stored in their place.")
                            if (r.skipped > 0) add("${countText(r.skipped)} already in your library: left as they were.")
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

private fun countText(n: Int) = if (n == 1) "1 book" else "$n books"
