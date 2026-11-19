package com.mybookshelf.app.ui.screens.places

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.Place
import com.mybookshelf.app.data.withId
import com.mybookshelf.app.ui.components.GnomeMessage
import com.mybookshelf.app.ui.components.GnomePose
import com.mybookshelf.app.ui.components.OpenArrow
import com.mybookshelf.app.ui.components.countText

/** View 1 of the Places page: every place, with how many books it holds. */
@Composable
internal fun PlaceList(
    places: List<Place>,
    books: List<BookInfo>,
    onOpen: (Place) -> Unit,
    onNew: () -> Unit,
) {
    if (places.isEmpty()) {
        NoPlacesYet(onNew)
        return
    }

    val booksPerPlace = books.groupingBy { it.placeId }.eachCount()
    val notStored = books.count { places.withId(it.placeId) == null }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    countText(places.size, "place"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AddPlaceButton("New place", onNew)
            }
        }
        items(places, key = { it.id }) { place ->
            PlaceRow(place, bookCount = booksPerPlace[place.id] ?: 0, onClick = { onOpen(place) })
            HorizontalDivider(Modifier.padding(start = 56.dp))
        }
        if (books.isNotEmpty()) {
            item {
                Text(
                    "${countText(notStored, "book")} not stored anywhere",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** One place: pin icon, name, "12 books" and ">". */
@Composable
private fun PlaceRow(place: Place, bookCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium)
            Text(
                countText(bookCount, "book"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OpenArrow()
    }
}

/** The gnome explains what places are for, with a button to create the first one. */
@Composable
private fun NoPlacesYet(onNew: () -> Unit) {
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
        AddPlaceButton("Add a place", onNew)
    }
}

@Composable
private fun AddPlaceButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
