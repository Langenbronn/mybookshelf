package com.mybookshelf.app.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookLookup
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.scanner.BarcodeScannerHost
import com.mybookshelf.app.ui.components.BookCard
import com.mybookshelf.app.ui.components.GnomeLoader
import com.mybookshelf.app.scanner.ScanResult
import kotlinx.coroutines.launch

/** What the Scan page is currently showing. */
private sealed interface LookupState {
    data object Idle : LookupState
    data class Loading(val isbn: String) : LookupState
    data class Found(val book: BookInfo) : LookupState
    data class NotFound(val isbn: String) : LookupState
    data class Failed(val message: String) : LookupState
}

/**
 * Scan page: scan a book's barcode (or type its ISBN) and show what we know about it.
 */
@Composable
fun ScanScreen() {
    var scanning by remember { mutableStateOf(false) }
    var isbnInput by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<LookupState>(LookupState.Idle) }
    val scope = rememberCoroutineScope()

    fun lookUp(raw: String) {
        val isbn = BookLookup.normalizeIsbn(raw)
        if (isbn == null) {
            state = LookupState.Failed("\"$raw\" is not a valid ISBN (10 or 13 digits).")
            return
        }
        isbnInput = isbn
        state = LookupState.Loading(isbn)
        scope.launch {
            state = try {
                BookLookup.findByIsbn(isbn)?.let { LookupState.Found(it) } ?: LookupState.NotFound(isbn)
            } catch (e: Exception) {
                LookupState.Failed("Could not reach the book servers. Check your internet connection.")
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

            // Manual entry: useful on the emulator or when the barcode is damaged
            OutlinedTextField(
                value = isbnInput,
                onValueChange = { isbnInput = it },
                label = { Text("…or type the ISBN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { lookUp(isbnInput) }),
                trailingIcon = {
                    IconButton(onClick = { lookUp(isbnInput) }, enabled = isbnInput.isNotBlank()) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            when (val s = state) {
                LookupState.Idle -> Hint("Scan the barcode on the back of a book to see its details.")
                is LookupState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                    GnomeLoader()
                }
                is LookupState.Found -> FoundBook(s.book)
                is LookupState.NotFound -> Hint("No book found for ISBN ${s.isbn}.")
                is LookupState.Failed -> Hint(s.message, isError = true)
            }
        }

        // Camera on top of everything while scanning
        BarcodeScannerHost(active = scanning) { result ->
            scanning = false
            when (result) {
                is ScanResult.Success -> lookUp(result.code)
                is ScanResult.Error -> state = LookupState.Failed(result.message)
                ScanResult.Cancelled -> Unit
            }
        }
    }
}

@Composable
private fun Hint(text: String, isError: Boolean = false) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        textAlign = TextAlign.Center,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The book found, with a button to add it to the library. */
@Composable
private fun FoundBook(book: BookInfo) {
    // Re-draws automatically when the library changes
    val savedBooks by Library.books.collectAsState()
    val inLibrary = savedBooks.any { it.isbn == book.isbn }

    BookCard(book) {
        if (inLibrary) {
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("In your library")
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
