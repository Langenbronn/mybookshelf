package com.mybookshelf.app.ui.screens.places

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookSearch
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.data.Place
import com.mybookshelf.app.data.withId
import com.mybookshelf.app.ui.components.BookRow
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose
import com.mybookshelf.app.ui.components.RoundSearchField

/**
 * View 3 of the Places page: tick one or several books to store them in [place].
 *
 * - Books already there start ticked; un-ticking one takes it out of the place.
 * - Books stored in ANOTHER place are greyed out: a book can only be in one place,
 *   so it must be taken out of its current place first.
 * - Nothing changes until "Save".
 */
@Composable
internal fun PlaceBookPicker(place: Place, places: List<Place>, books: List<BookInfo>, onDone: () -> Unit) {
    val alreadyHere = remember(place.id) { books.filter { it.placeId == place.id }.map { it.isbn }.toSet() }
    var ticked by remember(place.id) { mutableStateOf(alreadyHere) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(books, query) { BookSearch.filter(books, query) }

    /** true if the book can be ticked: it is in no place, or already in this one. */
    fun canBeTicked(book: BookInfo): Boolean = places.withId(book.placeId).let { it == null || it.id == place.id }

    Column(Modifier.fillMaxSize()) {
        PickerTopBar(place, onBack = onDone)

        if (books.isEmpty()) {
            GnomeMessage(
                GnomePose.Sleeping,
                title = "Your library is empty",
                message = "Scan some books first, then come back to store them here.",
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
            return@Column
        }

        RoundSearchField(
            query,
            onQueryChange = { query = it },
            placeholder = "Search title, author…",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // "Select all": ticks every book shown (respects the search) that can be ticked
        val tickable = shown.filter(::canBeTicked).map { it.isbn }.toSet()
        val allTicked = tickable.isNotEmpty() && ticked.containsAll(tickable)
        SelectAllRow(
            checked = allTicked,
            label = if (query.isBlank()) "Select all" else "Select all results",
            selectedCount = ticked.size,
            onClick = { ticked = if (allTicked) ticked - tickable else ticked + tickable },
        )
        HorizontalDivider()

        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.isbn }) { book ->
                val isTicked = book.isbn in ticked
                val enabled = canBeTicked(book)
                val currentPlace = places.withId(book.placeId)
                BookRow(
                    book,
                    modifier = Modifier
                        .clickable(enabled = enabled) { ticked = if (isTicked) ticked - book.isbn else ticked + book.isbn }
                        .alpha(if (enabled) 1f else 0.4f),
                    details = when {
                        currentPlace == null -> null
                        currentPlace.id == place.id -> "Already here"
                        else -> "Already in ${currentPlace.name} — take it out there first"
                    },
                    leading = { Checkbox(checked = isTicked, onCheckedChange = null, enabled = enabled) },
                )
                HorizontalDivider(Modifier.padding(start = 112.dp))
            }
        }

        SaveBar(
            onCancel = onDone,
            onSave = {
                Library.setPlace(ticked, place.id)
                Library.setPlace(alreadyHere - ticked, null)   // un-ticked: taken out of the place
                onDone()
            },
        )
    }
}

@Composable
private fun PickerTopBar(place: Place, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
        }
        Text(
            "Store in \"${place.name}\"",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** [x] Select all ............ 3 selected */
@Composable
private fun SelectAllRow(checked: Boolean, label: String, selectedCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Text(
            "$selectedCount selected",
            modifier = Modifier.padding(end = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Bottom bar: Cancel / Save. */
@Composable
private fun SaveBar(onCancel: () -> Unit, onSave: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
        }
    }
}
