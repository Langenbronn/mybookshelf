package com.mybookshelf.app.data

/**
 * Age groups, with the matching UK Book Bands and Oxford Reading Tree (ORT) stages.
 *
 * School years → age → bands:
 *   Reception (4–5)  Lilac, Pink, Red, Yellow        ORT 1–3
 *   Year 1–2  (5–7)  Blue, Green, Orange, Turquoise, Purple, Gold   ORT 4–9
 *   Year 3–4  (7–9)  White, Lime, Brown, Grey       ORT 10–14
 *   Year 5–6  (9–11) Dark Blue, Dark Red            ORT 15–20
 */
enum class AgeGroup(val label: String, val bookBand: String) {
    Baby("0–3", "Before book bands (read aloud)"),
    Age4to5("4–5", "Lilac to Yellow (ORT 1–3)"),
    Age5to7("5–7", "Blue to Gold (ORT 4–9)"),
    Age7to9("7–9", "White to Grey (ORT 10–14)"),
    Age9to11("9–11", "Dark Blue to Dark Red (ORT 15–20)"),
    Teen("11–15", "Free reader (beyond book bands)"),
    Adult("Adult", "Free reader (beyond book bands)"),
    /** No clue at all (e.g. the database has no subjects for this book): correct it by hand. */
    Unknown("Unknown", "Unknown"),
}

/**
 * UK Book Bands with their Oxford Reading Tree stage and official color (ARGB),
 * plus "before bands" (babies) and "free reader" / "adult" for older readers.
 */
enum class BookBand(val label: String, val ort: String?, val ageGroup: AgeGroup, val argb: Long?) {
    BeforeBands("Before book bands", null, AgeGroup.Baby, null),
    Lilac("Lilac", "1", AgeGroup.Age4to5, 0xFFC8A2C8),
    Pink("Pink", "1+", AgeGroup.Age4to5, 0xFFF48FB1),
    Red("Red", "2", AgeGroup.Age4to5, 0xFFE53935),
    Yellow("Yellow", "3", AgeGroup.Age4to5, 0xFFFDD835),
    Blue("Blue", "4", AgeGroup.Age5to7, 0xFF1E88E5),
    Green("Green", "5", AgeGroup.Age5to7, 0xFF43A047),
    Orange("Orange", "6", AgeGroup.Age5to7, 0xFFFB8C00),
    Turquoise("Turquoise", "7", AgeGroup.Age5to7, 0xFF26C6DA),
    Purple("Purple", "8", AgeGroup.Age5to7, 0xFF8E24AA),
    Gold("Gold", "9", AgeGroup.Age5to7, 0xFFD4AF37),
    White("White", "10", AgeGroup.Age7to9, 0xFFFFFFFF),
    Lime("Lime", "11", AgeGroup.Age7to9, 0xFFC0CA33),
    Brown("Brown", "12", AgeGroup.Age7to9, 0xFF795548),
    Grey("Grey", "13–14", AgeGroup.Age7to9, 0xFF9E9E9E),
    DarkBlue("Dark Blue", "15–16", AgeGroup.Age9to11, 0xFF1A237E),
    DarkRed("Dark Red", "17–20", AgeGroup.Age9to11, 0xFF8B0000),
    FreeReader("Free reader", null, AgeGroup.Teen, null),
    Adult("Adult", null, AgeGroup.Adult, null),
    Unknown("Unknown", null, AgeGroup.Unknown, null);

    /** "Yellow (ORT 3)", "Free reader". */
    val fullLabel: String get() = if (ort != null) "$label (ORT $ort)" else label

    companion object {
        fun forAge(group: AgeGroup): List<BookBand> = entries.filter { it.ageGroup == group }
    }
}

/**
 * @param bookBand e.g. "Lilac to Yellow (ORT 1–3)", or "Yellow (ORT Stage 3)" for an Oxford Reading Tree book.
 * @param bands the possible bands: all the bands of the age group, or exactly one for an ORT book.
 * @param origin Database = printed on the book (Oxford Reading Tree stage), Estimated = guessed,
 *               User = corrected by hand, Unknown = no clue.
 */
data class ReadingLevel(
    val ageGroup: AgeGroup,
    val bookBand: String,
    val bands: List<BookBand>,
    val origin: Origin,
) {
    /** true when guessed by the app (to show "estimated"). */
    val estimated: Boolean get() = origin == Origin.Estimated
}

/**
 * Guesses the reading age of a book. No free database gives it directly, so we use clues:
 *  1. An Oxford Reading Tree stage in the title ("Oxford Reading Tree: Stage 3") → exact.
 *  2. Subjects like "Board books", "Picture books", "Beginner Books", "Juvenile fiction",
 *     "Young adult fiction", "Reading Level-Grade 8"…
 *  3. The number of pages (a 30-page children's book is for younger readers than a 300-page one).
 * Tested on ~50 well-known books; it's a good guess, not an official rating.
 */
object ReadingLevels {

    fun of(book: BookInfo): ReadingLevel {
        // 1. Corrected by hand: always wins
        book.corrections?.ageGroup?.let { group ->
            val band = book.corrections.band?.takeIf { it.ageGroup == group }
            return if (band != null) {
                ReadingLevel(group, band.fullLabel, listOf(band), Origin.User)
            } else {
                ReadingLevel(group, group.bookBand, BookBand.forAge(group), Origin.User)
            }
        }
        // 2. Written in the title (Oxford Reading Tree)
        orStage(book)?.let { return it }
        // 3. Guessed from the subjects and pages
        val group = estimateGroup(book)
        val origin = if (group == AgeGroup.Unknown) Origin.Unknown else Origin.Estimated
        return ReadingLevel(group, group.bookBand, BookBand.forAge(group), origin)
    }

    // ---------- Oxford Reading Tree ----------

    /** true when the title contains an Oxford Reading Tree stage. */
    fun isOxfordReadingTree(book: BookInfo): Boolean = orStage(book) != null

    private val stageRegex = Regex("""(oxford reading tree|read with oxford|biff, chip).*?(stage|level)\s*(\d+)(\+?)""")

    private fun orStage(book: BookInfo): ReadingLevel? {
        val text = listOfNotNull(book.title, book.subtitle).joinToString(" ").lowercase()
        val match = stageRegex.find(text) ?: return null
        val stage = match.groupValues[3].toInt()
        val plus = match.groupValues[4] == "+"
        val band = when {
            stage <= 1 && plus -> BookBand.Pink
            stage <= 1 -> BookBand.Lilac
            stage == 2 -> BookBand.Red
            stage == 3 -> BookBand.Yellow
            stage == 4 -> BookBand.Blue
            stage == 5 -> BookBand.Green
            stage == 6 -> BookBand.Orange
            stage == 7 -> BookBand.Turquoise
            stage == 8 -> BookBand.Purple
            stage == 9 -> BookBand.Gold
            stage == 10 -> BookBand.White
            stage == 11 -> BookBand.Lime
            stage == 12 -> BookBand.Brown
            stage <= 14 -> BookBand.Grey
            stage <= 16 -> BookBand.DarkBlue
            else -> BookBand.DarkRed
        }
        val stageText = "$stage${if (plus) "+" else ""}"
        return ReadingLevel(band.ageGroup, "${band.label} (ORT Stage $stageText)", listOf(band), Origin.Database)
    }

    // ---------- Estimate from subjects and pages ----------

    private val kidsWords = listOf("juvenile", "children", "kindergarten", "preschool")
    private val teenWords = listOf("young adult", "teen", "12-up")
    private val babyWords = listOf("board book", "baby", "toddler")
    private val pictureWords = listOf("picture book", "stories in rhyme", "kindergarten")
    private val earlyReaderWords = listOf(
        "beginner", "beginning reader", "first time reader", "readers (primary)", "easy reading", "i can read",
    )
    private val gradeRegex = Regex("""reading level-grade (\d+)""")

    private fun estimateGroup(book: BookInfo): AgeGroup {
        if (book.maturityRating == "MATURE") return AgeGroup.Adult
        // No subjects at all: nothing to guess from (better "Unknown" than a wrong "Adult")
        if (book.subjects.isEmpty()) return AgeGroup.Unknown

        val subjects = book.subjects.map { it.lowercase() }
        val all = subjects.joinToString(" | ")
        val pages = book.pageCount
        val kids = subjects.count { s -> kidsWords.any { it in s } }
        val teen = subjects.count { s -> teenWords.any { it in s } }
        val manySubjects = maxOf(1, subjects.size) * 0.1

        // Not (clearly) a children's book → adult.
        // A few stray "juvenile"/"young adult" subjects are often about adaptations (comics, school editions).
        val childrenSignals = kids + teen
        val earlyReader = earlyReaderWords.any { it in all }
        if (!earlyReader && childrenSignals < 3 && childrenSignals < manySubjects) return AgeGroup.Adult
        if (!earlyReader && kids == 0 && teen < manySubjects) return AgeGroup.Adult

        return when {
            babyWords.any { it in all } && (pages ?: 0) <= 32 -> AgeGroup.Baby
            pictureWords.any { it in all } && (pages ?: 0) <= 48 -> AgeGroup.Age4to5
            earlyReader || (pages != null && pages <= 70) -> AgeGroup.Age5to7
            teen >= 3 || (teen >= 1 && teen * 2 >= kids) -> AgeGroup.Teen
            // School reading levels: grade 8+ (age 13+) with young-adult subjects
            teen >= 2 && (gradeRegex.findAll(all).minOfOrNull { it.groupValues[1].toInt() } ?: 0) >= 8 -> AgeGroup.Teen
            pages == null || pages <= 190 -> AgeGroup.Age7to9
            else -> AgeGroup.Age9to11
        }
    }
}
