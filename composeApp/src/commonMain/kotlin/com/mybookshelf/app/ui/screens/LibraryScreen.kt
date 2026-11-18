package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Example data. Later this could come from a database or the internet.
private val sampleBooks = listOf(
    "The Hobbit" to "J.R.R. Tolkien",
    "Dune" to "Frank Herbert",
    "1984" to "George Orwell",
    "The Little Prince" to "Antoine de Saint-Exupéry",
)

/** Library page: a scrollable list of books. */
@Composable
fun LibraryScreen() {
    // LazyColumn = a scrollable list that only draws visible rows (like RecyclerView / UITableView)
    LazyColumn {
        items(sampleBooks) { (title, author) ->
            ListItem(
                headlineContent = { Text(title) },
                supportingContent = { Text(author) },
            )
            HorizontalDivider()
        }
    }
}
