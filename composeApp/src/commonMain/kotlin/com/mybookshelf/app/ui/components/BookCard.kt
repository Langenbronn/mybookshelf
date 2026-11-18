package com.mybookshelf.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mybookshelf.app.data.BookClassifier
import com.mybookshelf.app.data.BookInfo

/**
 * A card with everything we know about a book.
 * Used on the Scan page (after a search) and in the Library list.
 *
 * @param shortDescription show only the first lines of the description, with "Show more".
 * @param actions buttons shown at the bottom of the card (e.g. "Add to my library").
 */
@Composable
fun BookCard(
    book: BookInfo,
    modifier: Modifier = Modifier,
    shortDescription: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookCover(book, width = 96.dp, height = 144.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(book.title, style = MaterialTheme.typography.titleLarge)
                    book.subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(", "),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            HorizontalDivider()

            // Type of book
            val kind = BookClassifier.kind(book)
            val genres = BookClassifier.genres(book, kind)
            kind?.let { DetailRow("Type", it.label) }
            if (genres.isNotEmpty()) DetailRow("Genres", genres.joinToString(", "))

            // Age of the book
            val firstYear = book.firstPublishYear
            val editionYear = BookClassifier.yearOf(book.publishedDate)
            firstYear?.let { DetailRow("First published", "$it · ${BookClassifier.ageText(it)}") }
            if (book.publishedDate != null && editionYear != firstYear) {
                val age = editionYear?.let { " · ${BookClassifier.ageText(it)}" }.orEmpty()
                DetailRow(if (firstYear != null) "This edition" else "Published", book.publishedDate + age)
            }

            DetailRow("ISBN", book.isbn)
            book.publisher?.let { DetailRow("Publisher", it) }
            book.pageCount?.let { DetailRow("Pages", it.toString()) }
            book.language?.let { DetailRow("Language", it.uppercase()) }
            book.addedOn?.let { DetailRow("Added on", it) }

            book.description?.let { Description(it, shortDescription) }

            Text(
                "Source: ${book.source}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            actions()
        }
    }
}

/** The book's cover image, or a grey box saying "No cover". */
@Composable
fun BookCover(book: BookInfo, width: Dp, height: Dp) {
    Surface(
        modifier = Modifier.width(width).height(height).clip(RoundedCornerShape(6.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (book.coverUrl != null) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = "Cover of ${book.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width, height),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text("No cover", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Description(text: String, short: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(!short) }
    HorizontalDivider()
    Text("Description", style = MaterialTheme.typography.titleSmall)
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis,
    )
    if (short) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Show less" else "Show more")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            label,
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
