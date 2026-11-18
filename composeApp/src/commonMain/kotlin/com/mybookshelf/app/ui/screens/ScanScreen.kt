package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookLookup
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.scanner.BarcodeScannerHost
import com.mybookshelf.app.scanner.ScanResult
import com.mybookshelf.app.ui.components.BookCard
import com.mybookshelf.app.ui.components.BookCover
import com.mybookshelf.app.ui.components.Gnome
import com.mybookshelf.app.ui.components.GnomeLoader
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose
import kotlinx.coroutines.launch

/** What the Scan page is currently showing. */
private sealed interface LookupState {
    data object Idle : LookupState
    data object Loading : LookupState
    data class Found(val book: BookInfo) : LookupState
    data class Results(val query: String, val results: List<BookLookup.SearchResult>) : LookupState
    data class NotFound(val message: String) : LookupState
    data class Failed(val message: String) : LookupState
}

private const val NETWORK_ERROR = "Could not reach the book servers. Check your internet connection."

/**
 * Scan page: scan a book's barcode, or type an ISBN, a title or an author,
 * and show what we know about the book.
 */
@Composable
fun ScanScreen() {
    var scanning by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<LookupState>(LookupState.Idle) }
    // The last list of results, to come back to it after opening a book
    var lastResults by remember { mutableStateOf<LookupState.Results?>(null) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    /** Loads one book from its ISBN. */
    fun openIsbn(isbn: String) {
        state = LookupState.Loading
        scope.launch {
            state = try {
                BookLookup.findByIsbn(isbn)?.let { LookupState.Found(it) }
                    ?: LookupState.NotFound("No book found for ISBN $isbn.")
            } catch (e: Exception) {
                LookupState.Failed(NETWORK_ERROR)
            }
        }
    }

    /** Typed text: an ISBN opens the book, anything else searches by title / author. */
    fun search(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        keyboard?.hide()
        val isbn = BookLookup.normalizeIsbn(text)
        if (isbn != null) {
            lastResults = null
            input = isbn
            openIsbn(isbn)
            return
        }
        state = LookupState.Loading
        scope.launch {
            state = try {
                val results = BookLookup.searchByText(text)
                if (results.isEmpty()) {
                    LookupState.NotFound("No book found for \"$text\".")
                } else {
                    LookupState.Results(text, results).also { lastResults = it }
                }
            } catch (e: Exception) {
                LookupState.Failed(NETWORK_ERROR)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = { scanning = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Scan a barcode")
            }

            // Manual search: ISBN, title or author
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("…or type an ISBN, a title or an author") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search(input) }),
                trailingIcon = {
                    IconButton(onClick = { search(input) }, enabled = input.isNotBlank()) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            val messageModifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            when (val s = state) {
                LookupState.Idle -> GnomeMessage(
                    GnomePose.Searching,
                    "Scan the barcode on the back of a book, or search by ISBN, title or author.",
                    messageModifier,
                )
                LookupState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                    GnomeLoader()
                }
                is LookupState.Results -> SearchResults(s, onOpen = { openIsbn(it.isbn) })
                is LookupState.Found -> {
                    lastResults?.let { results ->
                        TextButton(onClick = { state = results }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Back to results")
                        }
                    }
                    FoundBook(s.book)
                }
                is LookupState.NotFound -> GnomeMessage(GnomePose.Confused, s.message, messageModifier)
                is LookupState.Failed -> GnomeMessage(GnomePose.Confused, s.message, messageModifier, isError = true)
            }
        }

        // Camera on top of everything while scanning
        BarcodeScannerHost(active = scanning) { result ->
            scanning = false
            when (result) {
                is ScanResult.Success -> search(result.code)
                is ScanResult.Error -> state = LookupState.Failed(result.message)
                ScanResult.Cancelled -> Unit
            }
        }
    }
}

/** List of books found by title / author. Tap one to see everything about it. */
@Composable
private fun SearchResults(state: LookupState.Results, onOpen: (BookLookup.SearchResult) -> Unit) {
    Column {
        Text(
            "${state.results.size} results for \"${state.query}\"",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        state.results.forEach { result ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(result) }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BookCover(result.coverUrl, result.title, width = 48.dp, height = 72.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        result.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (result.authors.isNotEmpty()) {
                        Text(
                            result.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    result.firstPublishYear?.let {
                        Text(
                            it.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(Modifier.padding(start = 64.dp))
        }
    }
}

/** The book found, with a button to add it to the library. */
@Composable
private fun FoundBook(book: BookInfo) {
    // Re-draws automatically when the library changes
    val savedBooks by Library.books.collectAsState()
    val inLibrary = savedBooks.any { it.isbn == book.isbn }

    BookCard(book) {
        if (inLibrary) {
            // The gnome celebrates the new book
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Gnome(GnomePose.Celebrating, size = 72.dp)
                Text(
                    "In your library!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Button(onClick = { Library.add(book) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add to my library")
            }
        }
    }
}
