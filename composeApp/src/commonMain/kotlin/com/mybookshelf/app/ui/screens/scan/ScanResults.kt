package com.mybookshelf.app.ui.screens.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookLookup
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.ui.components.BookCard
import com.mybookshelf.app.ui.components.BookRow
import com.mybookshelf.app.ui.components.Gnome
import com.mybookshelf.app.ui.components.GnomePose
import com.mybookshelf.app.ui.components.OpenArrow

/** List of books found by title / author. Tap one to see everything about it. */
@Composable
internal fun SearchResults(state: LookupState.Results, onOpen: (BookLookup.SearchResult) -> Unit) {
    Column {
        Text(
            "${state.results.size} results for \"${state.query}\"",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        state.results.forEach { result ->
            BookRow(
                title = result.title,
                authors = result.authors,
                coverUrl = result.coverUrl,
                modifier = Modifier.clickable { onOpen(result) },
                details = result.firstPublishYear?.toString(),
                titleMaxLines = 2,
                // The page already has a side margin
                contentPadding = PaddingValues(vertical = 8.dp),
                trailing = { OpenArrow() },
            )
            HorizontalDivider(Modifier.padding(start = 64.dp))
        }
    }
}

/** The book found, with a button to add it to the library. */
@Composable
internal fun FoundBook(book: BookInfo) {
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
