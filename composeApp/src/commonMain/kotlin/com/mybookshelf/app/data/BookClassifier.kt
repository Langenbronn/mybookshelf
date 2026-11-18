package com.mybookshelf.app.data

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

enum class BookKind(val label: String) { Fiction("Fiction"), NonFiction("Non-fiction") }

/**
 * Turns the raw data of a [BookInfo] into easy-to-read facts:
 * fiction or non-fiction, genres, and how old the book is.
 *
 * It's a best guess based on keywords in the book's subjects.
 * To recognise a new genre, add a line to [fictionGenres] or [nonFictionGenres].
 */
object BookClassifier {

    // Genre shown → keywords that reveal it (lowercase)
    private val fictionGenres = listOf(
        "Fantasy" to listOf("fantasy"),
        "Science fiction" to listOf("science fiction", "sci-fi"),
        "Mystery" to listOf("mystery", "mysteries", "detective", "crime"),
        "Thriller" to listOf("thriller", "suspense"),
        "Romance" to listOf("romance", "love stories"),
        "Horror" to listOf("horror"),
        "Adventure" to listOf("adventure"),
        "Historical" to listOf("historical"),
        "Humor" to listOf("humor", "humour", "humorous"),
        "Classic" to listOf("classics"),
        "Fairy tale" to listOf("fairy tale"),
        "Poetry" to listOf("poetry"),
        "Children's" to listOf("juvenile", "children"),
        "Young adult" to listOf("young adult"),
    )

    private val nonFictionGenres = listOf(
        "History" to listOf("history"),
        "Science" to listOf("science"),
        "Biography" to listOf("biography", "biographies", "memoir", "autobiography"),
        "Philosophy" to listOf("philosophy"),
        "Religion" to listOf("religion", "spirituality"),
        "Psychology" to listOf("psychology"),
        "Self-help" to listOf("self-help", "self help", "personal development"),
        "Business" to listOf("business", "economics", "finance"),
        "Cooking" to listOf("cooking", "cookbook", "recipes"),
        "Travel" to listOf("travel"),
        "Art" to listOf("art", "photography", "design"),
        "Politics" to listOf("politics", "political"),
        "Health" to listOf("health", "medicine", "fitness"),
        "Technology" to listOf("technology", "computers", "programming"),
        "Children's" to listOf("juvenile", "children"),
    )

    /** Fiction, non-fiction, or null if we can't tell. */
    fun kind(book: BookInfo): BookKind? {
        val subjects = book.subjects.map { it.lowercase().replace("non fiction", "nonfiction") }
        val nonFictionVotes = subjects.count { "nonfiction" in it || "non-fiction" in it }
        // "fiction" on its own (not inside "nonfiction" / "non-fiction")
        val fictionVotes = subjects.count { Regex("""(^|[^n-])fiction""").containsMatchIn(it) }
        return when {
            fictionVotes > nonFictionVotes -> BookKind.Fiction
            nonFictionVotes > fictionVotes -> BookKind.NonFiction
            // Google's categories are standard bookshop shelves: anything not "Fiction" is non-fiction
            book.categories.isNotEmpty() -> BookKind.NonFiction
            else -> null
        }
    }

    /**
     * Up to 4 genres, strongest first, e.g. ["Fantasy", "Young adult", "Children's"].
     * Books can have ~100 subjects, so a genre must appear in several of them to count.
     */
    fun genres(book: BookInfo, kind: BookKind?): List<String> {
        val table = if (kind == BookKind.NonFiction) nonFictionGenres else fictionGenres
        val subjects = book.subjects.map { it.lowercase() }
        val scores = table.map { (genre, keywords) ->
            val regexes = keywords.map { Regex("""\b${Regex.escape(it)}(s|es)?\b""") }
            genre to subjects.count { s -> regexes.any { it.containsMatchIn(s) } }
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        val best = scores.firstOrNull()?.second ?: return book.categories.take(4)
        val threshold = maxOf(2, best / 4)
        val strong = scores.filter { it.second >= threshold }.ifEmpty { scores.take(1) }
        return strong.map { it.first }.take(4)
    }

    /** First 4-digit year found in a date like "2012-09-18" or "September 2012". */
    fun yearOf(date: String?): Int? =
        date?.let { Regex("""\b(1[0-9]{3}|20[0-9]{2})\b""").find(it)?.value?.toInt() }

    fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

    /** "89 years ago", "1 year ago", "this year". */
    fun ageText(year: Int): String = when (val age = currentYear() - year) {
        in Int.MIN_VALUE..0 -> "this year"
        1 -> "1 year ago"
        else -> "$age years ago"
    }
}
