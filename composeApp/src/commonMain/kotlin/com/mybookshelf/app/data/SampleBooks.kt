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
import kotlinx.coroutines.withContext

/**
 * 48 real books of every age, to test the app (Settings > "Add test books").
 * Each one is looked up online like a scanned book, so the data is the real thing.
 */
object SampleBooks {

    val isbns = listOf(
        // Babies & toddlers (0–3)
        "9780399226908", // The Very Hungry Caterpillar
        "9780694003617", // Goodnight Moon
        "9780805047905", // Brown Bear, Brown Bear, What Do You See?
        "9781416947370", // Dear Zoo
        "9780763642648", // Guess How Much I Love You
        "9780333903377", // Room on the Broom
        // Picture books (4–5)
        "9780333710937", // The Gruffalo
        "9780007215997", // The Tiger Who Came to Tea
        // Oxford Reading Tree (exact book bands)
        "9780199188192", // Stage 1
        "9780199117062", // Stage 1+
        "9780199169764", // Stage 3
        "9780199183708", // Stage 5
        "9780198482437", // Stage 5: The Magic Key
        "9780199189786", // Stage 7
        // First readers (5–7)
        "9780394800011", // The Cat in the Hat
        "9780394800165", // Green Eggs and Ham
        "9780064440202", // Frog and Toad Are Friends
        // Children (7–11)
        "9780142410349", // Fantastic Mr Fox
        "9780142410394", // The Twits
        "9780064400558", // Charlotte's Web
        "9780142410318", // Charlie and the Chocolate Factory
        "9780064404990", // The Lion, the Witch and the Wardrobe
        "9782070612758", // Le Petit Prince
        "9780142410370", // Matilda
        "9780810993136", // Diary of a Wimpy Kid
        "9780375869020", // Wonder
        "9780440414803", // Holes
        "9780439708180", // Harry Potter and the Sorcerer's Stone
        "9780439064873", // Harry Potter and the Chamber of Secrets
        "9780786838653", // The Lightning Thief
        // Teens (11–15)
        "9780547928227", // The Hobbit
        "9780439023528", // The Hunger Games
        "9780142424179", // The Fault in Our Stars
        "9780062024039", // Divergent
        "9780544336261", // The Giver
        "9780375842207", // The Book Thief
        "9780316015844", // Twilight
        // Adults
        "9780141439518", // Pride and Prejudice
        "9780451524935", // 1984
        "9780061120084", // To Kill a Mockingbird
        "9780743273565", // The Great Gatsby
        "9780062316097", // Sapiens
        "9780307474278", // The Da Vinci Code
        "9780385490818", // The Handmaid's Tale
        "9780735211292", // Atomic Habits
        "9780441172719", // Dune
        "9780062315007", // The Alchemist
        "9782070360024", // L'Étranger
    )

    /** Progress of the download, or null when nothing is running. */
    data class Progress(val done: Int, val total: Int)

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = _progress.asStateFlow()

    // Lives as long as the app: the download continues if you leave the Settings page
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Downloads all the books (6 at a time) and adds them to the library. */
    fun addAllToLibrary() {
        if (_progress.value != null) return
        _progress.value = Progress(0, isbns.size)
        scope.launch {
            val limit = Semaphore(6)
            val books = isbns.map { isbn ->
                async {
                    limit.withPermit {
                        val book = runCatching { BookLookup.findByIsbn(isbn) }.getOrNull()
                        // update {} is safe when several downloads finish at the same time
                        _progress.update { it?.copy(done = it.done + 1) }
                        book
                    }
                }
            }.awaitAll().filterNotNull()
            // The library is only changed from the main thread (see Library)
            withContext(Dispatchers.Main) { Library.addAll(books) }
            _progress.value = null
        }
    }
}
