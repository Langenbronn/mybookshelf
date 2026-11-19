package com.mybookshelf.app.ui.screens.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.data.Place
import com.mybookshelf.app.data.Places
import com.mybookshelf.app.ui.components.BookRow
import com.mybookshelf.app.ui.components.Gnome
import com.mybookshelf.app.ui.components.GnomePose

/*
 * "Quick add" on the Scan page: a scanned book goes straight into the library,
 * optionally stored in a chosen place, without any confirmation screen.
 */

/** What happened to a book scanned in quick-add mode. */
internal sealed interface QuickResult {
    /** New in the library (in [place], or without a place). */
    data class Added(val place: Place?) : QuickResult
    /** Was already in the library and had no place: now stored in [place]. */
    data class StoredIn(val place: Place) : QuickResult
    /** Was already in the library (in [place], or without a place): nothing changed. */
    data class AlreadyThere(val place: Place?) : QuickResult
}

/**
 * The quick-add rules. Returns the book to show (the library's copy if it was already there)
 * and what happened.
 *  - New book: added, in [place] if one is chosen.
 *  - Already in the library without a place: stored in [place].
 *  - Already in a place: left there (a book is only ever in one place).
 */
internal fun storeScannedBook(book: BookInfo, place: Place?): Pair<BookInfo, QuickResult> {
    val existing = Library.books.value.firstOrNull { it.isbn == book.isbn }
        ?: run {
            Library.add(book.copy(placeId = place?.id))
            return book to QuickResult.Added(place)
        }
    val currentPlace = Places.byId(existing.placeId)
    return if (currentPlace == null && place != null) {
        Library.setPlace(setOf(book.isbn), place.id)
        existing to QuickResult.StoredIn(place)
    } else {
        existing to QuickResult.AlreadyThere(currentPlace)
    }
}

/** The "Quick add" switch, and the optional place where scanned books are stored. */
@Composable
internal fun QuickAddOptions(
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Store in", modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodyMedium)
                    PlaceChooser(places, place, onPlaceChange, Modifier.weight(1f))
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

/** A button showing the chosen place; tapping it opens the list of places (+ "No place"). */
@Composable
private fun PlaceChooser(places: List<Place>, place: Place?, onPlaceChange: (Place?) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    // The Box keeps the menu next to its button
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
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
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("No place") },
                onClick = { onPlaceChange(null); open = false },
            )
            places.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name) },
                    leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
                    onClick = { onPlaceChange(p); open = false },
                )
            }
        }
    }
}

/** After a quick add: what happened, the book, and a button to scan the next one. */
@Composable
internal fun QuickAddedBook(book: BookInfo, result: QuickResult, onScanNext: () -> Unit) {
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
            BookRow(book, titleMaxLines = 2, contentPadding = PaddingValues(0.dp))
            Button(onClick = onScanNext, modifier = Modifier.fillMaxWidth()) { Text("Scan the next book") }
        }
    }
}
