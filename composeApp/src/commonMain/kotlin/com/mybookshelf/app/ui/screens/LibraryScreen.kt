package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.AgeGroup
import com.mybookshelf.app.data.BookClassifier
import com.mybookshelf.app.data.BookBand
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookSearch
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.data.ReadingLevels
import com.mybookshelf.app.ui.components.BookBandFilter
import com.mybookshelf.app.ui.components.BookCard
import com.mybookshelf.app.ui.components.BookCover
import com.mybookshelf.app.ui.components.EditDetailsDialog
import com.mybookshelf.app.ui.components.Gnome
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose

/**
 * Library page: every book the user added from the Scan page.
 * Two views: "Detailed" (a full card per book) or "Compact" (one row per book, tap to open).
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

    if (books.isEmpty()) {
        EmptyLibrary(onScan)
    } else Column(Modifier.fillMaxSize()) {
        // Stays at the top while the list scrolls
        SearchBar(query, onQueryChange = { query = it })
        BookBandFilter(
            selected = bandFilter,
            onChange = { bandFilter = it },
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
        )

        // LazyColumn = a scrollable list that only draws visible rows (like RecyclerView / UITableView)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 16.dp),
        ) {
            item {
                Header(
                    shown = shownBooks.size,
                    total = books.size,
                    compact = compact,
                    onCompactChange = { Library.setCompactView(it) },
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
                // "Edit details" + "Remove" side by side, under each full card
                val removeButton: @Composable () -> Unit = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { bookToEdit = book }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit details")
                        }
                        OutlinedButton(onClick = { bookToRemove = book }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Remove")
                        }
                    }
                }
                when {
                    !compact -> BookCard(
                        book,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        shortDescription = true,
                        actions = removeButton,
                    )
                    openIsbn == book.isbn -> BookCard(
                        book,
                        modifier = Modifier.padding(16.dp),
                        shortDescription = true,
                    ) {
                        removeButton()
                        TextButton(onClick = { openIsbn = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("Close")
                        }
                    }
                    else -> {
                        CompactBookRow(book, onClick = { openIsbn = book.isbn })
                        HorizontalDivider(Modifier.padding(start = 80.dp))
                    }
                }
            }
            // At the very bottom: the gnome waves goodbye
            if (shownBooks.isNotEmpty()) {
                item(key = "end") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Gnome(GnomePose.Waving, size = 80.dp)
                        Text(
                            "That's the end of the shelf!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
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

/** Search field with a clear (×) button. */
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        placeholder = { Text("Search title, author, genre…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

/** "3 books" (or "2 of 3 books" when searching) + the Compact / Detailed switch. */
@Composable
private fun Header(shown: Int, total: Int, compact: Boolean, onCompactChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when {
                shown != total -> "$shown of $total books"
                total == 1 -> "1 book"
                else -> "$total books"
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = compact,
                onClick = { onCompactChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Compact") }
            SegmentedButton(
                selected = !compact,
                onClick = { onCompactChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Detailed") }
        }
    }
}

/** One short row: small cover, title, author, "Fiction · Fantasy · 1943". */
@Composable
private fun CompactBookRow(book: BookInfo, onClick: () -> Unit) {
    val kind = BookClassifier.kind(book)
    val genre = BookClassifier.genres(book, kind).firstOrNull()
    val year = book.firstPublishYear ?: BookClassifier.yearOf(book.publishedDate)
    val age = ReadingLevels.of(book).ageGroup.let {
        when (it) {
            AgeGroup.Adult -> "Adult"
            AgeGroup.Unknown -> "Age unknown"
            else -> "Age ${it.label}"
        }
    }
    // e.g. "Picture book · Animal story · 1969 · Age 0–3"
    val format = BookClassifier.format(book, kind) ?: kind?.label
    val details = listOfNotNull(format, genre, year?.toString(), age).joinToString(" · ")

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book, width = 48.dp, height = 72.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.authors.isNotEmpty()) {
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (details.isNotEmpty()) {
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyLibrary(onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The gnome sleeps: nothing to read yet!
        GnomeMessage(
            GnomePose.Sleeping,
            title = "Your library is empty",
            message = "Scan a book's barcode, then tap \"Add to my library\".",
        )
        Button(onClick = onScan) { Text("Scan a book") }
    }
}
