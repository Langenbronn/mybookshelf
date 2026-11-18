package com.mybookshelf.app.data

/**
 * Filters books with a search text, e.g. "tolkien", "fantasy 1937", "petit prince".
 * Every word typed must appear somewhere in the book (title, author, genre, year, ISBN…).
 * Ignores upper/lower case and accents ("é" = "e").
 */
object BookSearch {

    fun filter(books: List<BookInfo>, query: String): List<BookInfo> {
        val words = normalize(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return books
        return books.filter { book ->
            val text = searchableText(book)
            words.all { it in text }
        }
    }

    /** Everything we can search in, as one lowercase text without accents. */
    private fun searchableText(book: BookInfo): String {
        val kind = BookClassifier.kind(book)
        val parts = listOfNotNull(
            book.title,
            book.subtitle,
            book.authors.joinToString(" "),
            book.publisher,
            book.isbn,
            book.publishedDate,
            book.firstPublishYear?.toString(),
            book.language,
            kind?.label,
            BookClassifier.genres(book, kind).joinToString(" "),
            book.categories.joinToString(" "),
        )
        return normalize(parts.joinToString(" "))
    }

    private val accents = mapOf(
        'à' to 'a', 'á' to 'a', 'â' to 'a', 'ä' to 'a', 'ã' to 'a', 'å' to 'a',
        'ç' to 'c',
        'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e',
        'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
        'ñ' to 'n',
        'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'ö' to 'o', 'õ' to 'o', 'ø' to 'o',
        'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
        'ý' to 'y', 'ÿ' to 'y',
        '’' to '\'',
    )

    private fun normalize(text: String): String =
        text.lowercase().map { accents[it] ?: it }.joinToString("")
}
