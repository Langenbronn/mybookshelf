package com.mybookshelf.app.ui.screens.places

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.data.Place
import com.mybookshelf.app.ui.components.BookRow
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose
import com.mybookshelf.app.ui.components.countText

/**
 * View 2 of the Places page: one place and its [books].
 * Top bar: back, name, export, rename, delete. Each book can be taken out with ×.
 */
@Composable
internal fun PlaceDetail(
    place: Place,
    books: List<BookInfo>,
    onBack: () -> Unit,
    onPick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to places")
            }
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    countText(books.size, "book"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onExport, enabled = books.isNotEmpty()) {
                Icon(Icons.Filled.Share, contentDescription = "Export the books of this place")
            }
            IconButton(onClick = onRename) { Icon(Icons.Filled.Edit, contentDescription = "Rename") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete place") }
        }

        Button(onClick = onPick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose books to store here")
        }

        if (books.isEmpty()) {
            GnomeMessage(
                GnomePose.Pointing,
                message = "No book stored here yet. Tap the button above to choose some!",
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(books, key = { it.isbn }) { book ->
                    BookRow(
                        book,
                        trailing = {
                            IconButton(onClick = { Library.setPlace(setOf(book.isbn), null) }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Take out of this place")
                            }
                        },
                    )
                    HorizontalDivider(Modifier.padding(start = 80.dp))
                }
            }
        }
    }
}
