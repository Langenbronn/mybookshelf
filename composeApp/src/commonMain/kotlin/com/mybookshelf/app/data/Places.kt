package com.mybookshelf.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A place where books can be stored, e.g. "Living room shelf" or "Box in the attic". */
@Serializable
data class Place(
    /** Never changes, so renaming a place keeps its books. */
    val id: String,
    val name: String,
)

/**
 * The user's list of places. Each book can optionally be stored in one place
 * ([BookInfo.placeId]). Every change is saved on the phone immediately.
 */
object Places {

    private const val PLACES_KEY = "places"
    private val json = Json { ignoreUnknownKeys = true }

    private val _places = MutableStateFlow(loadPlaces())

    /** Places in alphabetical order. */
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    fun byId(id: String?): Place? = id?.let { wanted -> _places.value.firstOrNull { it.id == wanted } }

    /** Adds a place and returns it (or null if the name is empty). */
    fun add(name: String): Place? {
        val clean = name.trim().ifEmpty { return null }
        val place = Place(id = "p" + Clock.System.now().toEpochMilliseconds(), name = clean)
        update(_places.value + place)
        return place
    }

    fun rename(id: String, name: String) {
        val clean = name.trim().ifEmpty { return }
        update(_places.value.map { if (it.id == id) it.copy(name = clean) else it })
    }

    /** Deletes a place. Its books stay in the library, just without a place. */
    fun remove(id: String) {
        Library.setPlace(Library.books.value.filter { it.placeId == id }.map { it.isbn }.toSet(), null)
        update(_places.value.filterNot { it.id == id })
    }

    private fun update(newPlaces: List<Place>) {
        val sorted = newPlaces.sortedBy { it.name.lowercase() }
        _places.value = sorted
        LibraryStorage.save(PLACES_KEY, json.encodeToString(sorted))
    }

    private fun loadPlaces(): List<Place> {
        val text = LibraryStorage.load(PLACES_KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Place>>(text) }.getOrDefault(emptyList())
    }
}
