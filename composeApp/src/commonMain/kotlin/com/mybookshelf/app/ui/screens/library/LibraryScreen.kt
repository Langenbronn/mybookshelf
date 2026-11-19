package com.mybookshelf.app.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookBand
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookSearch
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.ui.components.BookBandFilter
import com.mybookshelf.app.ui.components.BookCard
import com.mybookshelf.app.ui.components.EditDetailsDialog
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose
import com.mybookshelf.app.ui.components.RoundSearchField
import com.mybookshelf.app.ui.components.rememberBookExporter
import com.mybookshelf.app.ui.components.rememberBookImporter

/**
 * Library page: every book the user added.
 *
 * Two views: "Detailed" (a full card per book) or "Compact" (one row per book, tap to open).
 * A third mode, "selecting", shows checkboxes to export some books.
 * The pieces of this page are in LibraryHeader.kt and LibraryRows.kt.
 *
 * @param onScan called by the "Scan a book" button when the library is empty.
 */
@Composable
fun LibraryScreen(onScan: () -> Unit) {
    val books by Library.books.collectAsState()
    val compact by Library.compactView.collectAsState()
    // In compact view: the book opened as a full card (null = none)
    var openIsbn by remember { mutableStateOf<String?>(null) }
    // The book waiting for "Remove?" confirmation (null = no dialog)
    var bookToRemove by remember { mutableStateOf<BookInfo?>(null) }
    // The book whose details are being corrected by hand (null = no dialog)
    var bookToEdit by remember { mutableStateOf<BookInfo?>(null) }
    // Text typed in the search bar
    var query by rememberSaveable { mutableStateOf("") }
    // Book bands ticked in the drop-down list (empty = all)
    var bandFilter by remember { mutableStateOf(emptySet<BookBand>()) }
    // Recomputed only when the books, the search text or the bands change
    val shownBooks = remember(books, query, bandFilter) { BookSearch.filter(books, query, bandFilter) }
    // Selection mode: the ISBNs of the ticked books (null = not selecting)
    var selected by remember { mutableStateOf<Set<String>?>(null) }
    // Short messages at the bottom ("12 books exported")
    val snackbar = remember { SnackbarHostState() }
    val export = rememberBookExporter(snackbar)
    val importFile = rememberBookImporter()

    Box(Modifier.fillMaxSize()) {
        if (books.isEmpty()) {
            EmptyLibrary(onScan, onImport = importFile)
        } else {
            Column(Modifier.fillMaxSize()) {
                // Search, filter and selection bar stay at the top while the list scrolls
                RoundSearchField(
                    query,
                    onQueryChange = { query = it },
                    placeholder = "Search title, author, genre…",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                )
                BookBandFilter(
                    selected = bandFilter,
                    onChange = { bandFilter = it },
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
                )
                selected?.let { ticked ->
                    val shownIsbns = shownBooks.map { it.isbn }.toSet()
                    SelectionBar(
                        count = ticked.size,
                        allShownTicked = shownIsbns.isNotEmpty() && ticked.containsAll(shownIsbns),
                        onTickAll = { tick -> selected = if (tick) ticked + shownIsbns else ticked - shownIsbns },
                        onExport = {
                            export(books.filter { it.isbn in ticked }, "selection")
                            selected = null
                        },
                        onClose = { selected = null },
                    )
                }

                // LazyColumn = a scrollable list that only draws visible rows (like RecyclerView / UITableView)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 16.dp),
                ) {
                    item {
                        LibraryHeader(
                            shown = shownBooks.size,
                            total = books.size,
                            compact = compact,
                            onCompactChange = { Library.setCompactView(it) },
                            onSelect = { selected = emptySet() },
                            onExportAll = { export(books, null) },
                            onImport = importFile,
                        )
                    }
                    if (shownBooks.isEmpty()) {
                        item {
                            val forAge = if (bandFilter.isEmpty()) "" else " in these book bands"
                            GnomeMessage(
                                GnomePose.Confused,
                                if (query.isBlank()) "No book$forAge yet." else "No book$forAge matches \"$query\".",
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                            )
                        }
                    }
                    items(shownBooks, key = { it.isbn }) { book ->
                        val ticked = selected
                        val actions: @Composable () -> Unit = {
                            EditAndRemoveButtons(onEdit = { bookToEdit = book }, onRemove = { bookToRemove = book })
                        }
                        when {
                            // Selecting: short rows with a checkbox
                            ticked != null -> {
                                val isTicked = book.isbn in ticked
                                LibraryBookRow(
                                    book,
                                    onClick = { selected = if (isTicked) ticked - book.isbn else ticked + book.isbn },
                                    checked = isTicked,
                                )
                                HorizontalDivider(Modifier.padding(start = 80.dp))
                            }
                            // Detailed view: a full card for every book
                            !compact -> BookCard(
                                book,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                shortDescription = true,
                                actions = actions,
                            )
                            // Compact view, the opened book: full card with a Close button
                            openIsbn == book.isbn -> BookCard(book, Modifier.padding(16.dp), shortDescription = true) {
                                actions()
                                TextButton(onClick = { openIsbn = null }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Close")
                                }
                            }
                            // Compact view, the other books: one short row
                            else -> {
                                LibraryBookRow(book, onClick = { openIsbn = book.isbn })
                                HorizontalDivider(Modifier.padding(start = 80.dp))
                            }
                        }
                    }
                    if (shownBooks.isNotEmpty()) {
                        item(key = "end") { EndOfShelf() }
                    }
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    bookToEdit?.let { book ->
        EditDetailsDialog(
            book = book,
            onSave = { corrections ->
                Library.setCorrections(book.isbn, corrections)
                bookToEdit = null
            },
            onDismiss = { bookToEdit = null },
        )
    }

    bookToRemove?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToRemove = null },
            title = { Text("Remove this book?") },
            text = { Text("\"${book.title}\" will be removed from your library.") },
            confirmButton = {
                TextButton(onClick = {
                    Library.remove(book.isbn)
                    bookToRemove = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { bookToRemove = null }) { Text("Cancel") }
            },
        )
    }
}
