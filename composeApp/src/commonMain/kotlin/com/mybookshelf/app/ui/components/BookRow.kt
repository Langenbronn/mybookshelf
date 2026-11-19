package com.mybookshelf.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.BookInfo

/**
 * One short line for a book: small cover, title, authors and an optional grey [details] line.
 * Used everywhere books are listed (Library, Places, Scan results…).
 *
 * @param leading shown before the cover (e.g. a checkbox).
 * @param trailing shown at the end (e.g. [OpenArrow] or a remove button).
 */
@Composable
fun BookRow(
    title: String,
    authors: List<String>,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    details: String? = null,
    titleMaxLines: Int = 1,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        BookCover(coverUrl, title, width = 48.dp, height = 72.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (authors.isNotEmpty()) {
                Text(
                    authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!details.isNullOrEmpty()) {
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

/** Same as above, for a book of the library. */
@Composable
fun BookRow(
    book: BookInfo,
    modifier: Modifier = Modifier,
    details: String? = null,
    titleMaxLines: Int = 1,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) = BookRow(
    title = book.title,
    authors = book.authors,
    coverUrl = book.coverUrl,
    modifier = modifier,
    details = details,
    titleMaxLines = titleMaxLines,
    contentPadding = contentPadding,
    leading = leading,
    trailing = trailing,
)

/** The grey ">" at the end of a row that opens something. */
@Composable
fun OpenArrow() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "Open",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** "1 book", "3 books", "1 place"… ([word] is the singular, an "s" is added for the plural). */
fun countText(count: Int, word: String): String = if (count == 1) "1 $word" else "$count ${word}s"
