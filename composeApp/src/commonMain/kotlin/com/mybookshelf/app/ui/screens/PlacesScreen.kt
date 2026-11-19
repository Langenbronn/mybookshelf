package com.mybookshelf.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookSearch
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.data.Place
import com.mybookshelf.app.data.Places
import com.mybookshelf.app.ui.components.BookCover
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose

/**
 * Places page: the list of places where books are stored (shelves, boxes, rooms…).
 * Storing a book somewhere is optional.
 *
 * Three views inside this tab:
 *   1. the list of places,
 *   2. one place with its books,
 *   3. the book picker: tick one or several books to store them in that place.
 */
@Composable
fun PlacesScreen() {
    val places by Places.places.collectAsState()
    val books by Library.books.collectAsState()
    // The place being looked at (null = list of places)
    var openPlaceId by rememberSaveable { mutableStateOf<String?>(null) }
    // true = choosing which books go in the open place
    var picking by rememberSaveable { mutableStateOf(false) }
    // Dialogs
    var showNewPlace by remember { mutableStateOf(false) }
    var placeToRename by remember { mutableStateOf<Place?>(null) }
    var placeToDelete by remember { mutableStateOf<Place?>(null) }

    val openPlace = places.firstOrNull { it.id == openPlaceId }

    when {
        openPlace == null -> PlaceList(
            places = places,
            books = books,
            onOpen = { openPlaceId = it.id; picking = false },
            onNew = { showNewPlace = true },
        )
        picking -> BookPicker(
            place = openPlace,
            books = books,
            onDone = { picking = false },
        )
        else -> PlaceDetail(
            place = openPlace,
            books = books.filter { it.placeId == openPlace.id },
            onBack = { openPlaceId = null },
            onPick = { picking = true },
            onRename = { placeToRename = openPlace },
            onDelete = { placeToDelete = openPlace },
        )
    }

    if (showNewPlace) {
        PlaceNameDialog(
            title = "New place",
            initial = "",
            onSave = { name ->
                Places.add(name)?.let { openPlaceId = it.id }
                showNewPlace = false
            },
            onDismiss = { showNewPlace = false },
        )
    }

    placeToRename?.let { place ->
        PlaceNameDialog(
            title = "Rename place",
            initial = place.name,
            onSave = { name ->
                Places.rename(place.id, name)
                placeToRename = null
            },
            onDismiss = { placeToRename = null },
        )
    }

    placeToDelete?.let { place ->
        AlertDialog(
            onDismissRequest = { placeToDelete = null },
            title = { Text("Delete this place?") },
            text = { Text("\"${place.name}\" will be deleted. Its books stay in your library, without a place.") },
            confirmButton = {
                TextButton(onClick = {
                    Places.remove(place.id)
                    placeToDelete = null
                    openPlaceId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { placeToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

/** 1. All the places, with how many books each one holds. */
@Composable
private fun PlaceList(
    places: List<Place>,
    books: List<BookInfo>,
    onOpen: (Place) -> Unit,
    onNew: () -> Unit,
) {
    if (places.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GnomeMessage(
                GnomePose.Carrying,
                title = "No places yet",
                message = "Add the places where you keep your books (a shelf, a room, a box…) " +
                    "to always know where to find them. It's optional.",
            )
            Button(onClick = onNew) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add a place")
            }
        }
        return
    }

    val counts = books.groupingBy { it.placeId }.eachCount()
    val notStored = books.count { Places.byId(it.placeId) == null }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (places.size == 1) "1 place" else "${places.size} places",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onNew) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New place")
                }
            }
        }
        items(places, key = { it.id }) { place ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(place) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        bookCount(counts[place.id] ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(Modifier.padding(start = 56.dp))
        }
        if (books.isNotEmpty()) {
            item {
                Text(
                    "${bookCount(notStored)} not stored anywhere",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 2. One place: its books, and buttons to add books, rename or delete it. */
@Composable
private fun PlaceDetail(
    place: Place,
    books: List<BookInfo>,
    onBack: () -> Unit,
    onPick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
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
                    bookCount(books.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    BookRow(book, subtitle = null) {
                        IconButton(onClick = { Library.setPlace(setOf(book.isbn), null) }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Take out of this place")
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 80.dp))
                }
            }
        }
    }
}

/**
 * 3. Tick one or several books to store them in [place].
 * Books already there start ticked; un-ticking one takes it out of the place.
 * Books stored in another place are greyed out (a book can only be in one place).
 */
@Composable
private fun BookPicker(place: Place, books: List<BookInfo>, onDone: () -> Unit) {
    val alreadyHere = remember(place.id) { books.filter { it.placeId == place.id }.map { it.isbn }.toSet() }
    var ticked by remember(place.id) { mutableStateOf(alreadyHere) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(books, query) { BookSearch.filter(books, query, emptySet()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
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

        if (books.isEmpty()) {
            GnomeMessage(
                GnomePose.Sleeping,
                title = "Your library is empty",
                message = "Scan some books first, then come back to store them here.",
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Search title, author…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
        )

        // Select / unselect every book currently shown (respects the search).
        // Books stored in ANOTHER place can't be ticked: a book is only ever in one place.
        val shownIsbns = shown.filter { isFree(it, place) }.map { it.isbn }.toSet()
        val allShownTicked = shownIsbns.isNotEmpty() && ticked.containsAll(shownIsbns)
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { ticked = if (allShownTicked) ticked - shownIsbns else ticked + shownIsbns }
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = allShownTicked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Text(
                if (query.isBlank()) "Select all" else "Select all results",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${ticked.size} selected",
                modifier = Modifier.padding(end = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()

        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.isbn }) { book ->
                val isTicked = book.isbn in ticked
                val currentPlace = Places.byId(book.placeId)
                val free = isFree(book, place)
                val subtitle = when {
                    currentPlace == null -> null
                    currentPlace.id == place.id -> "Already here"
                    else -> "Already in ${currentPlace.name} — take it out there first"
                }
                BookRow(
                    book,
                    subtitle = subtitle,
                    modifier = Modifier
                        .clickable(enabled = free) { ticked = if (isTicked) ticked - book.isbn else ticked + book.isbn }
                        .alpha(if (free) 1f else 0.4f),
                    leading = { Checkbox(checked = isTicked, onCheckedChange = null, enabled = free) },
                )
                HorizontalDivider(Modifier.padding(start = 112.dp))
            }
        }

        // Bottom bar with the confirm button
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        Library.setPlace(ticked, place.id)
                        Library.setPlace(alreadyHere - ticked, null)
                        onDone()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

/** A short book row: [leading] (e.g. a checkbox), cover, title, author, [subtitle], [trailing]. */
@Composable
private fun BookRow(
    book: BookInfo,
    subtitle: String?,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        BookCover(book, width = 48.dp, height = 72.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (book.authors.isNotEmpty()) {
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

/** Dialog with one text field, to name or rename a place. */
@Composable
private fun PlaceNameDialog(title: String, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Living room shelf") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** true if the book can be ticked for [place]: it is in no place, or already in this one. */
private fun isFree(book: BookInfo, place: Place): Boolean {
    val current = Places.byId(book.placeId)
    return current == null || current.id == place.id
}

private fun bookCount(n: Int) = if (n == 1) "1 book" else "$n books"
