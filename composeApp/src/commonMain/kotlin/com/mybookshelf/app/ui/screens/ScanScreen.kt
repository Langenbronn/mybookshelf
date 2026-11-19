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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import com.mybookshelf.app.data.Place
import com.mybookshelf.app.data.Places
import kotlinx.coroutines.launch

/** What the Scan page is currently showing. */
private sealed interface LookupState {
    data object Idle : LookupState
    data object Loading : LookupState
    data class Found(val book: BookInfo) : LookupState
    data class Results(val query: String, val results: List<BookLookup.SearchResult>) : LookupState
    data class NotFound(val message: String) : LookupState
    data class Failed(val message: String) : LookupState
    /** Quick add: the scanned book went straight into the library. */
    data class QuickAdded(val book: BookInfo, val result: QuickResult) : LookupState
}

/** What happened to a book scanned in quick-add mode. */
private sealed interface QuickResult {
    /** New in the library (in [place], or without a place). */
    data class Added(val place: Place?) : QuickResult
    /** Was already in the library and had no place: now stored in [place]. */
    data class StoredIn(val place: Place) : QuickResult
    /** Was already in the library (in [place], or without a place): nothing changed. */
    data class AlreadyThere(val place: Place?) : QuickResult
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
    // Quick add: a scanned book goes straight into the library, optionally in a place
    var quickAdd by rememberSaveable { mutableStateOf(false) }
    var quickPlaceId by rememberSaveable { mutableStateOf<String?>(null) }
    val places by Places.places.collectAsState()
    // If the chosen place was deleted meanwhile, fall back to "no place"
    val quickPlace = places.firstOrNull { it.id == quickPlaceId }

    /** Quick add: finds the book and puts it in the library (and in [quickPlace]) without asking. */
    fun quickAddIsbn(isbn: String) {
        state = LookupState.Loading
        scope.launch {
            state = try {
                val book = BookLookup.findByIsbn(isbn)
                if (book == null) {
                    LookupState.NotFound("No book found for ISBN $isbn.")
                } else {
                    val existing = Library.books.value.firstOrNull { it.isbn == book.isbn }
                    val currentPlace = Places.byId(existing?.placeId)
                    val result = when {
                        existing == null -> {
                            Library.add(book.copy(placeId = quickPlace?.id))
                            QuickResult.Added(quickPlace)
                        }
                        // A book is only ever in one place: only store it if it has none yet
                        currentPlace == null && quickPlace != null -> {
                            Library.setPlace(setOf(book.isbn), quickPlace.id)
                            QuickResult.StoredIn(quickPlace)
                        }
                        else -> QuickResult.AlreadyThere(currentPlace)
                    }
                    LookupState.QuickAdded(existing ?: book, result)
                }
            } catch (e: Exception) {
                LookupState.Failed(NETWORK_ERROR)
            }
        }
    }

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
            QuickAddOptions(
                enabled = quickAdd,
                onEnabledChange = { quickAdd = it },
                places = places,
                place = quickPlace,
                onPlaceChange = { quickPlaceId = it?.id },
            )

            Button(
                onClick = { scanning = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(if (quickAdd) "Scan & add to my library" else "Scan a barcode")
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
                is LookupState.QuickAdded -> QuickAddedBook(s.book, s.result, onScanNext = { scanning = true })
            }
        }

        // Camera on top of everything while scanning
        BarcodeScannerHost(active = scanning) { result ->
            scanning = false
            when (result) {
                is ScanResult.Success -> {
                    val isbn = BookLookup.normalizeIsbn(result.code)
                    if (quickAdd && isbn != null) {
                        lastResults = null
                        input = isbn
                        quickAddIsbn(isbn)
                    } else {
                        search(result.code)
                    }
                }
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

/** The "Quick add" switch, and the optional place where scanned books are stored. */
@Composable
private fun QuickAddOptions(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    places: List<Place>,
    place: Place?,
    onPlaceChange: (Place?) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quick add", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Scanned books go straight into your library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                var menuOpen by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Store in", modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodyMedium)
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Place, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                place?.name ?: "No place (optional)",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("No place") },
                                onClick = { onPlaceChange(null); menuOpen = false },
                            )
                            places.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
                                    onClick = { onPlaceChange(p); menuOpen = false },
                                )
                            }
                        }
                    }
                }
                if (places.isEmpty()) {
                    Text(
                        "Create places in the Places tab to store books while scanning.",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** After a quick add: what happened, the book, and a button to scan the next one. */
@Composable
private fun QuickAddedBook(book: BookInfo, result: QuickResult, onScanNext: () -> Unit) {
    val (pose, message) = when (result) {
        is QuickResult.Added -> GnomePose.Celebrating to
            (result.place?.let { "Added to your library, in ${it.name}!" } ?: "Added to your library!")
        is QuickResult.StoredIn -> GnomePose.Carrying to
            "Already in your library. Now stored in ${result.place.name}."
        is QuickResult.AlreadyThere -> GnomePose.Shy to
            (result.place?.let { "Already in your library, in ${it.name}." } ?: "Already in your library.")
    }
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Gnome(pose, size = 96.dp)
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BookCover(book, width = 48.dp, height = 72.dp)
                Column(Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Button(onClick = onScanNext, modifier = Modifier.fillMaxWidth()) { Text("Scan the next book") }
        }
    }
}
