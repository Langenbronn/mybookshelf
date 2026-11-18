package com.mybookshelf.app.data

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

enum class BookKind(val label: String) { Fiction("Fiction"), NonFiction("Non-fiction") }

/**
 * Turns the raw data of a [BookInfo] into easy-to-read facts:
 * fiction or non-fiction, format, genres, themes, and how old the book is.
 *
 * It's a best guess based on keywords in the book's subjects (tuned on ~50 real books).
 * To recognise a new genre or theme, add a line to [fictionGenres], [schoolSubjects] or [themes].
 */
object BookClassifier {

    // Fiction: what kind of story. Label shown → keywords found in the book's subjects (lowercase)
    private val fictionGenres = listOf(
        "Fairy tale" to listOf("fairy tale", "folklore", "folk tale", "fable"),
        "Myths & legends" to listOf("mythology", "myth", "legend"),
        "Rhyming story" to listOf("stories in rhyme", "rhyme", "nonsense verse"),
        "Bedtime story" to listOf("bedtime", "sleep", "night"),
        "Animal story" to listOf("animal", "animals"),
        "Fantasy & magic" to listOf("fantasy", "magic", "wizard", "witch", "imaginary place"),
        "Adventure" to listOf("adventure", "adventurer", "buried treasure", "pirate", "quest"),
        "Humour" to listOf("humor", "humour", "humorous", "wit and humor", "nonsense", "funny"),
        "Mystery & detective" to listOf("mystery", "mysteries", "detective", "crime"),
        "Science fiction" to listOf("science fiction", "sci-fi", "time travel", "robot"),
        "Dystopian" to listOf("dystopia", "dystopian"),
        "Horror & ghost" to listOf("horror", "ghost", "vampire"),
        "Romance" to listOf("romance", "love stories"),
        "Historical" to listOf("historical fiction", "historical"),
        "School story" to listOf("school stories", "schools", "boarding school", "teacher-student"),
        "Family & friends" to listOf("friendship", "family", "brothers and sisters", "parent and child"),
        "Diary story" to listOf("diaries", "diary"),
        "Sports story" to listOf("sports", "football", "soccer"),
        "Real-life issues" to listOf("social issues", "social themes", "self-acceptance", "bullying", "disabilities"),
        "Poetry" to listOf("poetry", "poems"),
        "Classic" to listOf("classics"),
    )

    // Non-fiction: school subjects and topics
    private val schoolSubjects = listOf(
        "Phonics & reading" to listOf("phonics", "reading", "alphabet", "vocabulary", "spelling"),
        "Maths" to listOf("mathematics", "maths", "math", "arithmetic", "counting", "numbers", "geometry"),
        "Science" to listOf("science", "physics", "chemistry", "experiments"),
        "Nature & animals" to listOf("nature", "animals", "plants", "insects", "birds", "ecology"),
        "Human body & health" to listOf("human body", "anatomy", "health", "nutrition"),
        "Geography" to listOf("geography", "countries", "maps", "atlas", "world"),
        "History" to listOf("history", "historical"),
        "Space" to listOf("astronomy", "space", "planets", "solar system"),
        "Technology" to listOf("technology", "computers", "programming", "coding", "engineering"),
        "Art & craft" to listOf("art", "drawing", "painting", "crafts"),
        "Music" to listOf("music", "songs"),
        "Languages" to listOf("french language", "spanish language", "german language", "bilingual", "foreign language"),
        "Religion" to listOf("religion", "bible", "spirituality"),
        "Feelings & citizenship" to listOf("emotions", "feelings", "behavior", "behaviour", "citizenship", "conduct of life"),
        "Biography" to listOf("biography", "biographies", "memoir", "autobiography"),
        "Philosophy & society" to listOf("philosophy", "society", "civilization", "politics"),
        "Business & money" to listOf("business", "economics", "finance", "money"),
        "Self-help" to listOf("self-help", "self help", "personal development", "habit"),
        "Cooking" to listOf("cooking", "cookbook", "recipes"),
        "Sport" to listOf("sports", "football", "soccer"),
    )

    // Themes (fiction or not): what the book is about, useful for teachers and young readers
    private val themes = listOf(
        "Animals" to listOf("animal", "animals", "pets", "zoo", "farm life"),
        "Friendship" to listOf("friendship"),
        "Family" to listOf("family", "brothers and sisters", "parent and child", "fathers and sons", "mothers and daughters"),
        "Feelings" to listOf("emotions", "feelings", "behavior", "love", "fear", "self-acceptance"),
        "School" to listOf("schools", "school", "teachers", "teacher-student"),
        "Bedtime" to listOf("bedtime", "sleep", "night"),
        "Food" to listOf("food", "eating", "hunger", "fruit"),
        "Colours" to listOf("color", "colors", "colour", "colours"),
        "Counting & numbers" to listOf("counting", "numbers"),
        "Days & seasons" to listOf("days of the week", "seasons", "months", "telling time"),
        "Phonics & words" to listOf("phonics", "alphabet", "vocabulary"),
        "Dinosaurs" to listOf("dinosaurs", "dinosaur"),
        "Dragons" to listOf("dragons", "dragon"),
        "Monsters" to listOf("monsters", "monster"),
        "Witches & wizards" to listOf("witches", "witch", "wizards", "wizard", "magicians"),
        "Princes & princesses" to listOf("princes", "prince", "princesses", "princess", "kings and rulers"),
        "Pirates & treasure" to listOf("pirates", "pirate", "buried treasure"),
        "Space" to listOf("space", "planets", "astronauts"),
        "Sea" to listOf("sea", "ocean", "marine animals"),
        "Insects & minibeasts" to listOf("insects", "caterpillars", "butterflies", "spiders", "bugs"),
        "Holidays" to listOf("christmas", "halloween", "easter", "birthdays"),
        "Vehicles" to listOf("vehicles", "cars", "trains", "trucks"),
        "Disability & difference" to listOf("disabilities", "wheelchair", "deaf", "self-acceptance"),
        "Books & reading" to listOf("books and reading"),
        "Growing up" to listOf("coming of age", "growing up", "teenagers", "adolescence"),
    )

    // Genres that don't make sense for young children (e.g. "love stories" in a bedtime book)
    private val grownUpGenres = setOf("Romance", "Horror & ghost", "Dystopian")

    private val youngGroups = setOf(AgeGroup.Baby, AgeGroup.Age4to5, AgeGroup.Age5to7, AgeGroup.Age7to9)

    /** Keywords turned into regular expressions once: "animal" also matches "animals", not "animalistic". */
    private fun compile(table: List<Pair<String, List<String>>>) =
        table.map { (label, keywords) -> label to keywords.map { Regex("""\b${Regex.escape(it)}(s|es)?\b""") } }

    private val fictionRegexes by lazy { compile(fictionGenres) }
    private val schoolRegexes by lazy { compile(schoolSubjects) }
    private val themeRegexes by lazy { compile(themes) }

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
     * Up to 4 genres (fiction, e.g. "Rhyming story", "Animal story") or
     * school subjects (non-fiction, e.g. "Maths", "Nature & animals"), strongest first.
     */
    fun genres(book: BookInfo, kind: BookKind?): List<String> {
        val table = if (kind == BookKind.NonFiction) schoolRegexes else fictionRegexes
        val found = ranked(table, book, maxItems = 5, minCount = minCount(book))
        val young = ReadingLevels.of(book).ageGroup in youngGroups
        val genres = if (young) found.filter { it !in grownUpGenres } else found
        // Nothing recognised: fall back to the raw categories
        return genres.ifEmpty { book.categories }.take(4)
    }

    /** Up to 5 themes, e.g. ["Animals", "Friendship", "Colours"]. */
    fun themes(book: BookInfo): List<String> {
        // Children's books have few, relevant subjects: one mention is enough
        val young = ReadingLevels.of(book).ageGroup in youngGroups
        return ranked(themeRegexes, book, maxItems = 5, minCount = if (young) 1 else minCount(book))
    }

    /** Big subject lists (up to ~100) contain noise: a label must then appear at least twice. */
    private fun minCount(book: BookInfo) = if (book.subjects.size <= 15) 1 else 2

    private fun ranked(table: List<Pair<String, List<Regex>>>, book: BookInfo, maxItems: Int, minCount: Int): List<String> {
        val subjects = book.subjects.map { it.lowercase() }
        val scores = table.map { (label, regexes) ->
            label to subjects.count { s -> regexes.any { it.containsMatchIn(s) } }
        }.filter { it.second > 0 }.sortedByDescending { it.second }
        val best = scores.firstOrNull()?.second ?: return emptyList()
        val threshold = maxOf(minCount, best / 4)
        val strong = scores.filter { it.second >= threshold }.ifEmpty { scores.take(1) }
        return strong.map { it.first }.take(maxItems)
    }

    /**
     * The physical kind of book: "Board book", "Picture book", "Reading scheme book",
     * "First reader", "Chapter book", "Novel", "Graphic novel", "Activity book"…
     */
    fun format(book: BookInfo, kind: BookKind?): String? {
        val all = book.subjects.joinToString(" | ") { it.lowercase() }
        val level = ReadingLevels.of(book)
        val group = level.ageGroup
        val fiction = kind == BookKind.Fiction
        val littleOnes = group == AgeGroup.Baby || group == AgeGroup.Age4to5
        fun has(vararg words: String) = words.any { it in all }
        fun count(word: String) = Regex(Regex.escape(word)).findAll(all).count()
        return when {
            !level.estimated -> "Reading scheme book (Oxford Reading Tree)"
            // "Toy and movable books" alone is not enough: it's often about one special edition
            has("lift-the-flap", "lift the flap", "pop-up books") && littleOnes -> "Lift-the-flap / pop-up book"
            has("board book") && group == AgeGroup.Baby -> "Board book"
            // Many novels have a comic adaptation: comics must be a big part of the subjects
            (count("graphic novel") + count("comic")).let { it >= 3 && it * 3 >= book.subjects.size } -> "Graphic novel / comic"
            has("activity books", "puzzles", "coloring books", "colouring books", "activities and seat work") &&
                group in youngGroups && !fiction -> "Activity book"
            has("dictionaries", "encyclopedias", "atlases") -> "Reference book"
            has("poetry", "poems") && count("poe") >= 3 && group !in youngGroups -> "Poetry"
            littleOnes -> if (kind == BookKind.NonFiction) "Picture information book" else "Picture book"
            has("beginner", "beginning reader", "first time reader", "readers (primary)", "easy reading", "i can read") ->
                "First reader"
            group == AgeGroup.Age5to7 -> if (kind == BookKind.NonFiction) "Information book" else "First reader"
            // Unknown fiction / non-fiction for older readers: better say nothing than something wrong
            kind == null -> null
            !fiction -> if (group in youngGroups || group == AgeGroup.Age9to11) "Information book" else "Non-fiction book"
            group == AgeGroup.Age7to9 -> "Chapter book"
            else -> "Novel"
        }
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
