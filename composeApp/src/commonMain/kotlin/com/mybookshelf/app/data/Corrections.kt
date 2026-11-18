package com.mybookshelf.app.data

import kotlinx.serialization.Serializable

/** Where a piece of information comes from. Shown on the book card. */
enum class Origin {
    /** Given by the book database (or written on the book, like an Oxford Reading Tree stage). */
    Database,
    /** Guessed by the app from the book's subjects and number of pages. */
    Estimated,
    /** Corrected by hand in the app ("set by you"). */
    User,
    /** No clue at all. */
    Unknown,
}

/**
 * Details corrected by hand (Library > "Edit details"). null = automatic (not corrected).
 * Saved with the book, so they stay after closing the app.
 */
@Serializable
data class Corrections(
    val kind: BookKind? = null,
    val format: String? = null,
    val genres: List<String>? = null,
    val themes: List<String>? = null,
    val ageGroup: AgeGroup? = null,
    /** An exact band inside [ageGroup], or null for "any band of this age". */
    val band: BookBand? = null,
) {
    val isEmpty: Boolean get() = this == Corrections()
}
