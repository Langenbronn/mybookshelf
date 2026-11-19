package com.mybookshelf.app.ui.screens.places

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.mybookshelf.app.data.Library
import com.mybookshelf.app.data.Place
import com.mybookshelf.app.data.Places
import com.mybookshelf.app.ui.components.rememberBookExporter

/**
 * Places page: the places where books are stored (shelves, boxes, rooms…).
 * Storing a book somewhere is optional, and a book is only ever in one place.
 *
 * Three views inside this tab, each in its own file:
 *   1. PlaceList.kt        — the list of places,
 *   2. PlaceDetail.kt      — one place with its books,
 *   3. PlaceBookPicker.kt  — tick one or several books to store them in that place.
 * This file decides which view is shown and holds the dialogs.
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
    // Short messages at the bottom ("5 books exported")
    val snackbar = remember { SnackbarHostState() }
    val export = rememberBookExporter(snackbar)

    // null if no place is open, or if the open place was just deleted
    val openPlace = places.firstOrNull { it.id == openPlaceId }

    Box(Modifier.fillMaxSize()) {
        when {
            openPlace == null -> PlaceList(
                places = places,
                books = books,
                onOpen = { openPlaceId = it.id; picking = false },
                onNew = { showNewPlace = true },
            )
            picking -> PlaceBookPicker(
                place = openPlace,
                places = places,
                books = books,
                onDone = { picking = false },
            )
            else -> {
                val booksHere = books.filter { it.placeId == openPlace.id }
                PlaceDetail(
                    place = openPlace,
                    books = booksHere,
                    onBack = { openPlaceId = null },
                    onPick = { picking = true },
                    onRename = { placeToRename = openPlace },
                    onDelete = { placeToDelete = openPlace },
                    onExport = { export(booksHere, openPlace.name) },
                )
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
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
