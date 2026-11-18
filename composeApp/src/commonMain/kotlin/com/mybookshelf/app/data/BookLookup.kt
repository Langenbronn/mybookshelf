package com.mybookshelf.app.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Everything we know about a book, whatever the source.
 * @Serializable = can be saved as text (JSON) in the library.
 */
@Serializable
data class BookInfo(
    val isbn: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val categories: List<String> = emptyList(),
    /** Detailed subjects from Open Library (used to guess fiction/non-fiction and genres). */
    val subjects: List<String> = emptyList(),
    /** Year the work first came out (any edition), e.g. 1937 for The Hobbit. */
    val firstPublishYear: Int? = null,
    val language: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val source: String,
    /** Date the book was added to the library, e.g. "2026-11-18" (null if not in the library). */
    val addedOn: String? = null,
)

/**
 * Finds a book from its ISBN (the number under the barcode).
 * Tries Google Books first (it has descriptions), then Open Library.
 */
object BookLookup {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Keeps only digits (and a final X for old ISBN-10).
     * Returns null if it doesn't look like an ISBN.
     */
    fun normalizeIsbn(raw: String): String? {
        val cleaned = raw.uppercase().filter { it.isDigit() || it == 'X' }
        return cleaned.takeIf { it.length == 10 || it.length == 13 }
    }

    /** Returns the book, or null if no source knows it. Throws on network errors. */
    suspend fun findByIsbn(isbn: String): BookInfo? = coroutineScope {
        // Fetched at the same time as the main lookup: first publication year + subjects
        val extras = async { runCatching { openLibraryExtras(isbn) }.getOrNull() }
        val book = findMainInfo(isbn) ?: return@coroutineScope null
        val (firstYear, subjects) = extras.await() ?: (null to emptyList())
        book.copy(
            firstPublishYear = firstYear,
            subjects = (book.categories + subjects).distinct(),
        )
    }

    private suspend fun findMainInfo(isbn: String): BookInfo? {
        var networkError: Throwable? = null
        for (source in listOf(::fromGoogleBooks, ::fromOpenLibrary)) {
            try {
                source(isbn)?.let { return it }
            } catch (e: Exception) {
                networkError = e
            }
        }
        // Only report an error if no source could answer at all
        networkError?.let { throw it }
        return null
    }

    // ---------- Google Books ----------

    private suspend fun fromGoogleBooks(isbn: String): BookInfo? {
        val response: GoogleResponse = client.get("https://www.googleapis.com/books/v1/volumes") {
            parameter("q", "isbn:$isbn")
        }.body()
        val info = response.items?.firstOrNull()?.volumeInfo ?: return null
        return BookInfo(
            isbn = isbn,
            title = info.title ?: return null,
            subtitle = info.subtitle,
            authors = info.authors.orEmpty(),
            publisher = info.publisher,
            publishedDate = info.publishedDate,
            pageCount = info.pageCount?.takeIf { it > 0 },
            categories = info.categories.orEmpty(),
            language = info.language,
            description = info.description,
            // Google returns http:// links; Android blocks those by default
            coverUrl = (info.imageLinks?.thumbnail ?: info.imageLinks?.smallThumbnail)
                ?.replace("http://", "https://"),
            source = "Google Books",
        )
    }

    @Serializable
    private data class GoogleResponse(val items: List<GoogleItem>? = null)

    @Serializable
    private data class GoogleItem(val volumeInfo: GoogleVolumeInfo? = null)

    @Serializable
    private data class GoogleVolumeInfo(
        val title: String? = null,
        val subtitle: String? = null,
        val authors: List<String>? = null,
        val publisher: String? = null,
        val publishedDate: String? = null,
        val description: String? = null,
        val pageCount: Int? = null,
        val categories: List<String>? = null,
        val language: String? = null,
        val imageLinks: GoogleImageLinks? = null,
    )

    @Serializable
    private data class GoogleImageLinks(val thumbnail: String? = null, val smallThumbnail: String? = null)

    // ---------- Open Library ----------

    private suspend fun fromOpenLibrary(isbn: String): BookInfo? {
        val response: Map<String, OpenLibraryBook> = client.get("https://openlibrary.org/api/books") {
            parameter("bibkeys", "ISBN:$isbn")
            parameter("format", "json")
            parameter("jscmd", "data")
        }.body()
        val book = response["ISBN:$isbn"] ?: return null
        return BookInfo(
            isbn = isbn,
            title = book.title ?: return null,
            subtitle = book.subtitle,
            authors = book.authors.orEmpty().map { it.name },
            publisher = book.publishers?.firstOrNull()?.name,
            publishedDate = book.publish_date,
            pageCount = book.number_of_pages,
            categories = book.subjects.orEmpty().take(5).map { it.name },
            coverUrl = book.cover?.large ?: book.cover?.medium,
            source = "Open Library",
        )
    }

    @Suppress("PropertyName")
    @Serializable
    private data class OpenLibraryBook(
        val title: String? = null,
        val subtitle: String? = null,
        val authors: List<Named>? = null,
        val publishers: List<Named>? = null,
        val publish_date: String? = null,
        val number_of_pages: Int? = null,
        val subjects: List<Named>? = null,
        val cover: OpenLibraryCover? = null,
    )

    @Serializable
    private data class Named(val name: String)

    /** Open Library's search knows the first publication year of the work and many subjects. */
    private suspend fun openLibraryExtras(isbn: String): Pair<Int?, List<String>> {
        val response: OpenLibrarySearch = client.get("https://openlibrary.org/search.json") {
            parameter("isbn", isbn)
            parameter("fields", "first_publish_year,subject")
            parameter("limit", 1)
        }.body()
        val doc = response.docs.firstOrNull() ?: return null to emptyList()
        return doc.first_publish_year to doc.subject.orEmpty()
    }

    @Serializable
    private data class OpenLibrarySearch(val docs: List<OpenLibrarySearchDoc> = emptyList())

    @Suppress("PropertyName")
    @Serializable
    private data class OpenLibrarySearchDoc(
        val first_publish_year: Int? = null,
        val subject: List<String>? = null,
    )

    @Serializable
    private data class OpenLibraryCover(val medium: String? = null, val large: String? = null)
}
