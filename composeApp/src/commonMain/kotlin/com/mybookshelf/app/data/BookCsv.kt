package com.mybookshelf.app.data

/**
 * Export / import of books as a CSV file (opens in Excel, Numbers, Google Sheets…).
 *
 * Columns (first line of the file):
 *   isbn, title, subtitle, authors, publisher, published_date, pages, language, place, added_on
 *
 * For import only the "isbn" column is required; every other column is optional and may be
 * empty or missing. Columns can be in any order, separated by "," or ";" (Excel in French) or tabs.
 */
object BookCsv {

    val columns = listOf(
        "isbn", "title", "subtitle", "authors", "publisher",
        "published_date", "pages", "language", "place", "added_on",
    )

    /** One line read from an imported file. Only [isbn] is sure to be there. */
    data class Row(
        val isbn: String,
        val title: String? = null,
        val subtitle: String? = null,
        val authors: List<String> = emptyList(),
        val publisher: String? = null,
        val publishedDate: String? = null,
        val pageCount: Int? = null,
        val language: String? = null,
        val place: String? = null,
        val addedOn: String? = null,
    )

    /** What was read from a file: good rows, and the lines that had no valid ISBN. */
    data class Parsed(val rows: List<Row>, val invalidLines: List<Int>)

    // ---------------------------------------------------------------- export

    fun export(books: List<BookInfo>): String = buildString {
        // "BOM" so that Excel reads accents (é, ü…) correctly
        append('﻿')
        appendLine(columns.joinToString(","))
        books.forEach { book ->
            val values = listOf(
                book.isbn,
                book.title,
                book.subtitle,
                book.authors.joinToString("; "),
                book.publisher,
                book.publishedDate,
                book.pageCount?.toString(),
                book.language,
                Places.byId(book.placeId)?.name,
                book.addedOn,
            )
            appendLine(values.joinToString(",") { quote(it.orEmpty()) })
        }
    }

    /** Puts a value in quotes if it contains a separator, a quote or a line break. */
    private fun quote(value: String): String =
        if (value.any { it == ',' || it == ';' || it == '"' || it == '\n' || it == '\r' || it == '\t' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    // ---------------------------------------------------------------- import

    fun parse(text: String): Parsed {
        val lines = readRecords(text.removePrefix("﻿"))
            .withIndex()
            .filter { (_, cells) -> cells.any { it.isNotBlank() } }
        if (lines.isEmpty()) return Parsed(emptyList(), emptyList())

        // Is the first line a header ("isbn,title,…") or already a book?
        val first = lines.first().value.map { it.trim().lowercase() }
        val hasHeader = first.any { normalizeHeader(it) == "isbn" }
        val index: Map<String, Int> = if (hasHeader) {
            first.withIndex().associate { (i, name) -> normalizeHeader(name) to i }
        } else {
            // No header: the first column is the ISBN
            mapOf("isbn" to 0)
        }
        val isbnColumn = index["isbn"] ?: return Parsed(emptyList(), lines.map { it.index + 1 })

        val rows = mutableListOf<Row>()
        val invalid = mutableListOf<Int>()
        lines.drop(if (hasHeader) 1 else 0).forEach { (lineIndex, cells) ->
            fun cell(name: String) = index[name]?.let { cells.getOrNull(it) }?.trim()?.ifEmpty { null }
            val isbn = cells.getOrNull(isbnColumn)?.let { BookLookup.normalizeIsbn(it) }
            if (isbn == null) {
                invalid += lineIndex + 1
                return@forEach
            }
            rows += Row(
                isbn = isbn,
                title = cell("title"),
                subtitle = cell("subtitle"),
                authors = cell("authors")?.split(';', '|')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
                publisher = cell("publisher"),
                publishedDate = cell("published_date"),
                pageCount = cell("pages")?.toIntOrNull(),
                language = cell("language"),
                place = cell("place"),
                addedOn = cell("added_on"),
            )
        }
        return Parsed(rows.distinctBy { it.isbn }, invalid)
    }

    /** Accepts a few spellings: "ISBN-13", "Author", "Added on"… */
    private fun normalizeHeader(name: String): String {
        val n = name.replace('-', '_').replace(' ', '_')
        return when (n) {
            "isbn13", "isbn_13", "isbn10", "isbn_10", "ean" -> "isbn"
            "author" -> "authors"
            "date", "published" -> "published_date"
            "page_count", "pagecount" -> "pages"
            "location", "storage" -> "place"
            "added", "addedon" -> "added_on"
            else -> n
        }
    }

    /** Splits the text into lines of cells, handling quotes ("a, b") and the separator used. */
    private fun readRecords(text: String): List<List<String>> {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val separator = listOf('\t', ';', ',').maxBy { sep -> firstLine.count { it == sep } }
            .takeIf { sep -> firstLine.contains(sep) } ?: ','

        val records = mutableListOf<List<String>>()
        var cells = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == separator -> { cells += cell.toString(); cell.clear() }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    cells += cell.toString(); cell.clear()
                    records += cells
                    cells = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || cells.isNotEmpty()) {
            cells += cell.toString()
            records += cells
        }
        return records
    }
}
