package com.mybookshelf.app.ui.screens.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookLookup
import com.mybookshelf.app.data.Places
import com.mybookshelf.app.data.withId
import com.mybookshelf.app.scanner.BarcodeScannerHost
import com.mybookshelf.app.scanner.ScanResult
import com.mybookshelf.app.ui.components.GnomeLoader
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose
import kotlinx.coroutines.launch

/** What the Scan page is currently showing. */
internal sealed interface LookupState {
    data object Idle : LookupState
    data object Loading : LookupState
    data class Found(val book: BookInfo) : LookupState
    data class Results(val query: String, val results: List<BookLookup.SearchResult>) : LookupState
    data class NotFound(val message: String) : LookupState
    data class Failed(val message: String) : LookupState
    /** Quick add: the scanned book went straight into the library. */
    data class QuickAdded(val book: BookInfo, val result: QuickResult) : LookupState
}

private const val NETWORK_ERROR = "Could not reach the book servers. Check your internet connection."

/**
 * Scan page: scan a book's barcode, or type an ISBN, a title or an author,
 * and show what we know about the book.
 *
 * With "Quick add" on, a scanned book goes straight into the library (see QuickAdd.kt).
 * The search results and the found book are drawn in ScanResults.kt.
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
    // null if no place was chosen, or if the chosen place was deleted meanwhile
    val quickPlace = places.withId(quickPlaceId)

    /** Shows "Loading", runs [lookup] (a network call), then shows its result or a network error. */
    fun load(lookup: suspend () -> LookupState) {
        state = LookupState.Loading
        scope.launch {
            state = try {
                lookup()
            } catch (e: Exception) {
                LookupState.Failed(NETWORK_ERROR)
            }
        }
    }

    /** Opens one book from its ISBN. */
    fun openIsbn(isbn: String) = load {
        BookLookup.findByIsbn(isbn)?.let { LookupState.Found(it) }
            ?: LookupState.NotFound("No book found for ISBN $isbn.")
    }

    /** Quick add: finds the book and puts it in the library (and in [quickPlace]) without asking. */
    fun quickAddIsbn(isbn: String) = load {
        val book = BookLookup.findByIsbn(isbn)
        if (book == null) {
            LookupState.NotFound("No book found for ISBN $isbn.")
        } else {
            val (shownBook, result) = storeScannedBook(book, quickPlace)
            LookupState.QuickAdded(shownBook, result)
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
        load {
            val results = BookLookup.searchByText(text)
            if (results.isEmpty()) {
                LookupState.NotFound("No book found for \"$text\".")
            } else {
                LookupState.Results(text, results).also { lastResults = it }
            }
        }
    }

    /** A barcode was read: quick add if it's on and the code is an ISBN, otherwise a normal search. */
    fun onScanned(code: String) {
        val isbn = BookLookup.normalizeIsbn(code)
        if (quickAdd && isbn != null) {
            lastResults = null
            input = isbn
            quickAddIsbn(isbn)
        } else {
            search(code)
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
                is ScanResult.Success -> onScanned(result.code)
                is ScanResult.Error -> state = LookupState.Failed(result.message)
                ScanResult.Cancelled -> Unit
            }
        }
    }
}
