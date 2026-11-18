package com.mybookshelf.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The user's library: the list of saved books.
 * Screens read [books]; every change is saved on the phone immediately.
 */
object Library {

    private const val BOOKS_KEY = "library"
    private const val VIEW_KEY = "library_view"
    private val json = Json { ignoreUnknownKeys = true }

    private val _books = MutableStateFlow(loadBooks())

    /** Saved books, most recently added first. Screens update automatically when it changes. */
    val books: StateFlow<List<BookInfo>> = _books.asStateFlow()

    private val _compactView = MutableStateFlow(LibraryStorage.load(VIEW_KEY) == "compact")

    /** true = compact list (one row per book), false = detailed cards. Remembered on the phone. */
    val compactView: StateFlow<Boolean> = _compactView.asStateFlow()

    fun setCompactView(compact: Boolean) {
        _compactView.value = compact
        LibraryStorage.save(VIEW_KEY, if (compact) "compact" else "detailed")
    }

    fun contains(isbn: String): Boolean = _books.value.any { it.isbn == isbn }

    fun add(book: BookInfo) {
        if (contains(book.isbn)) return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        update(listOf(book.copy(addedOn = today)) + _books.value)
    }

    /** Adds several books at once (books already in the library are skipped). */
    fun addAll(books: List<BookInfo>) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val existing = _books.value.map { it.isbn }.toSet()
        val newBooks = books.filter { it.isbn !in existing }.distinctBy { it.isbn }.map { it.copy(addedOn = today) }
        update(newBooks + _books.value)
    }

    fun removeAll() = update(emptyList())

    /** Saves the details corrected by hand for one book (null or empty = back to automatic). */
    fun setCorrections(isbn: String, corrections: Corrections?) {
        val clean = corrections?.takeIf { !it.isEmpty }
        update(_books.value.map { if (it.isbn == isbn) it.copy(corrections = clean) else it })
    }

    fun remove(isbn: String) {
        update(_books.value.filterNot { it.isbn == isbn })
    }

    private fun update(newBooks: List<BookInfo>) {
        _books.value = newBooks
        LibraryStorage.save(BOOKS_KEY, json.encodeToString(newBooks))
    }

    private fun loadBooks(): List<BookInfo> {
        val text = LibraryStorage.load(BOOKS_KEY) ?: return emptyList()
        // If the saved data is unreadable (e.g. after a big app change), start with an empty library
        return runCatching { json.decodeFromString<List<BookInfo>>(text) }.getOrDefault(emptyList())
    }
}
