package com.mybookshelf.app.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.mybookshelf.app.data.BookClassifier
import com.mybookshelf.app.data.AgeGroup
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookKind
import com.mybookshelf.app.data.Origin
import com.mybookshelf.app.data.ReadingLevel
import com.mybookshelf.app.data.ReadingLevels

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

            // Type of book. Each line says where it comes from:
            // "estimated" = guessed by the app, "set by you" = corrected by hand, nothing = from the database
            val kind = BookClassifier.kind(book)
            val genres = BookClassifier.genres(book, kind)
            val themes = BookClassifier.themes(book)
            DetailRow("Type", kind?.label, BookClassifier.kindOrigin(book))
            DetailRow("Format", BookClassifier.format(book, kind), BookClassifier.formatOrigin(book, kind))
            DetailRow(
                if (kind == BookKind.NonFiction) "Subjects" else "Genres",
                genres.joinToString(", ").ifEmpty { null },
                BookClassifier.genresOrigin(book, kind),
            )
            DetailRow("Themes", themes.joinToString(", ").ifEmpty { null }, BookClassifier.themesOrigin(book))

            // Reading age and UK book band
            val level = ReadingLevels.of(book)
            DetailRow(
                "Reading age",
                readingAgeText(level).takeIf { level.ageGroup != AgeGroup.Unknown },
                level.origin,
            )
            BandRow(level)

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
                "Source: ${book.source}. \"estimated\" = guessed by the app from the book's subjects " +
                    "and number of pages. \"set by you\" = corrected by hand (Library > Edit details).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            actions()
        }
    }
}

/** "4–5 years", "Adult", "Unknown". */
fun readingAgeText(level: ReadingLevel): String = when (level.ageGroup) {
    AgeGroup.Adult, AgeGroup.Unknown -> level.ageGroup.label
    else -> "${level.ageGroup.label} years"
}

/** The book's cover image, or a grey box saying "No cover". */
@Composable
fun BookCover(book: BookInfo, width: Dp, height: Dp) = BookCover(book.coverUrl, book.title, width, height)

@Composable
fun BookCover(coverUrl: String?, title: String, width: Dp, height: Dp) {
    Surface(
        modifier = Modifier.width(width).height(height).clip(RoundedCornerShape(6.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (coverUrl != null) {
            SubcomposeAsyncImage(
                model = coverUrl,
                contentDescription = "Cover of $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width, height),
                // While the image downloads: the gnome paints the cover
                loading = { CoverGnome(GnomePose.Painting, width, height, text = null) },
                // Download failed: same as no cover
                error = { CoverGnome(GnomePose.Shy, width, height, text = "No cover") },
            )
        } else {
            CoverGnome(GnomePose.Shy, width, height, text = "No cover")
        }
    }
}

/** A gnome filling the cover space, with a small text under it on big covers. */
@Composable
private fun CoverGnome(pose: GnomePose, width: Dp, height: Dp, text: String?) {
    Column(
        modifier = Modifier.size(width, height),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Gnome(pose, size = width)
        // Only on big covers: on small ones the text would be too tiny
        if (text != null && height >= 120.dp) {
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/** "Book band" + the text + one colored dot per possible band. */
@Composable
private fun BandRow(level: ReadingLevel) {
    Row {
        Text(
            "Book band",
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                withOrigin(level.bookBand.takeIf { level.ageGroup != AgeGroup.Unknown }, level.origin),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (level.bands.any { it.argb != null }) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    level.bands.forEach { BandSwatch(it) }
                }
            }
        }
    }
}

/** A line of facts (always from the database). */
@Composable
private fun DetailRow(label: String, value: String) = DetailRow(label, value, Origin.Database)

/**
 * A line whose value may be guessed or corrected: adds a small "estimated" / "set by you" after it.
 * A null value is shown as a grey "Unknown".
 */
@Composable
private fun DetailRow(label: String, value: String?, origin: Origin) {
    Row {
        Text(
            label,
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(withOrigin(value, origin), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * "Fantasy, Adventure · estimated" (grey), "Picture book · set by you" (colored),
 * or a grey italic "Unknown" when there is no value.
 */
@Composable
private fun withOrigin(value: String?, origin: Origin): AnnotatedString {
    val small = MaterialTheme.typography.labelSmall.fontSize
    val grey = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        if (value == null) {
            withStyle(SpanStyle(color = grey, fontStyle = FontStyle.Italic)) { append("Unknown") }
            return@buildAnnotatedString
        }
        append(value)
        when (origin) {
            Origin.Estimated -> withStyle(SpanStyle(color = grey, fontStyle = FontStyle.Italic, fontSize = small)) {
                append("  · estimated")
            }
            Origin.User -> withStyle(SpanStyle(color = primary, fontStyle = FontStyle.Italic, fontSize = small)) {
                append("  · set by you")
            }
            Origin.Database, Origin.Unknown -> Unit
        }
    }
}
