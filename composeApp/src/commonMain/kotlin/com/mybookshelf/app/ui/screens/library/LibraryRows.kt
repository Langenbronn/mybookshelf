package com.mybookshelf.app.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.AgeGroup
import com.mybookshelf.app.data.BookClassifier
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.ReadingLevels
import com.mybookshelf.app.ui.components.BookRow
import com.mybookshelf.app.ui.components.Gnome
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose
import com.mybookshelf.app.ui.components.OpenArrow

/**
 * One short row of the Library, with a details line like "Picture book · Animal story · 1969 · Age 0–3".
 * @param checked null = normal row (with ">"), true / false = selection mode (with a checkbox).
 */
@Composable
internal fun LibraryBookRow(book: BookInfo, onClick: () -> Unit, checked: Boolean? = null) {
    BookRow(
        book,
        modifier = Modifier.clickable(onClick = onClick),
        details = shortDetails(book),
        leading = { checked?.let { Checkbox(checked = it, onCheckedChange = null) } },
        trailing = { if (checked == null) OpenArrow() },
    )
}

/** "Picture book · Animal story · 1969 · Age 0–3" */
private fun shortDetails(book: BookInfo): String {
    val kind = BookClassifier.kind(book)
    val format = BookClassifier.format(book, kind) ?: kind?.label
    val genre = BookClassifier.genres(book, kind).firstOrNull()
    val year = book.firstPublishYear ?: BookClassifier.yearOf(book.publishedDate)
    val age = when (val group = ReadingLevels.of(book).ageGroup) {
        AgeGroup.Adult -> "Adult"
        AgeGroup.Unknown -> "Age unknown"
        else -> "Age ${group.label}"
    }
    return listOfNotNull(format, genre, year?.toString(), age).joinToString(" · ")
}

/** "Edit details" and "Remove", side by side under a full book card. */
@Composable
internal fun EditAndRemoveButtons(onEdit: () -> Unit, onRemove: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Edit details")
        }
        OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Remove")
        }
    }
}

/** At the very bottom of the list: the gnome waves goodbye. */
@Composable
internal fun EndOfShelf() {
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

/** Shown instead of the list when there is no book at all. */
@Composable
internal fun EmptyLibrary(onScan: () -> Unit, onImport: () -> Unit) {
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
        OutlinedButton(onClick = onImport) { Text("Import a CSV file") }
    }
}
