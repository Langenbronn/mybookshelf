package com.mybookshelf.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Imports books from a CSV file (see [BookCsv]).
 * Each new ISBN is looked up online like a scanned book; the file's own columns
 * fill in whatever the lookup doesn't know (or everything, if the book isn't found online).
 */
object BookImport {

    sealed interface State {
        data class Running(val done: Int, val total: Int) : State
        data class Finished(val report: Report) : State
        data class Failed(val message: String) : State
    }

    data class Report(
        /** New books added to the library. */
        val added: Int,
        /** Of the added books, those unknown online (added with the file's data only). */
        val notFoundOnline: Int,
        /** Already in the library, now stored in the place given in the file. */
        val placed: Int,
        /** Already in the library, left as they were. */
        val skipped: Int,
        /** Line numbers without a valid ISBN. */
        val invalidLines: List<Int>,
    )

    private val _state = MutableStateFlow<State?>(null)
    /** null = no import running or to report. */
    val state: StateFlow<State?> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun dismiss() {
        if (_state.value !is State.Running) _state.value = null
    }

    fun start(text: String) {
        if (_state.value is State.Running) return
        val parsed = BookCsv.parse(text)
        if (parsed.rows.isEmpty()) {
            _state.value = State.Failed(
                "No book found in this file. The first line must name the columns, " +
                    "with at least an \"isbn\" column."
            )
            return
        }

        // Places named in the file: reuse existing ones (same name), create the others
        val placeIds: Map<String, String> = parsed.rows.mapNotNull { it.place }
            .distinctBy { it.lowercase() }
            .associate { name ->
                val existing = Places.places.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
                name.lowercase() to (existing ?: Places.add(name)!!).id
            }
        fun placeIdOf(row: BookCsv.Row) = row.place?.let { placeIds[it.lowercase()] }

        // Books already in the library: only give them a place if they have none (one place per book)
        val inLibrary = Library.books.value.associateBy { it.isbn }
        val known = parsed.rows.filter { it.isbn in inLibrary }
        val toPlace = known.filter { row ->
            placeIdOf(row) != null && Places.byId(inLibrary.getValue(row.isbn).placeId) == null
        }
        toPlace.groupBy { placeIdOf(it)!! }.forEach { (placeId, rows) ->
            Library.setPlace(rows.map { it.isbn }.toSet(), placeId)
        }
        val newRows = parsed.rows.filter { it.isbn !in inLibrary }

        _state.value = State.Running(0, newRows.size)
        scope.launch {
            val limit = Semaphore(4)   // at most 4 lookups at the same time
            val found = newRows.map { row ->
                async {
                    limit.withPermit {
                        val online = runCatching { BookLookup.findByIsbn(row.isbn) }.getOrNull()
                        _state.update { (it as? State.Running)?.copy(done = it.done + 1) ?: it }
                        merge(row, online).copy(placeId = placeIdOf(row)) to (online != null)
                    }
                }
            }.awaitAll()
            Library.importBooks(found.map { it.first })
            _state.value = State.Finished(
                Report(
                    added = found.size,
                    notFoundOnline = found.count { !it.second },
                    placed = toPlace.size,
                    skipped = known.size - toPlace.size,
                    invalidLines = parsed.invalidLines,
                )
            )
        }
    }

    /** The online data, completed with the file's columns (or only the file's data if not found online). */
    private fun merge(row: BookCsv.Row, online: BookInfo?): BookInfo {
        val base = online ?: BookInfo(
            isbn = row.isbn,
            title = row.title ?: "ISBN ${row.isbn}",
            source = "Imported file",
        )
        return base.copy(
            subtitle = base.subtitle ?: row.subtitle,
            authors = base.authors.ifEmpty { row.authors },
            publisher = base.publisher ?: row.publisher,
            publishedDate = base.publishedDate ?: row.publishedDate,
            pageCount = base.pageCount ?: row.pageCount,
            language = base.language ?: row.language,
            addedOn = row.addedOn,
        )
    }
}
