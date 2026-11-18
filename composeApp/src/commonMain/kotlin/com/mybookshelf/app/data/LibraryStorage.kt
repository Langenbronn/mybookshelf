package com.mybookshelf.app.data

/**
 * Saves pieces of text on the phone (each under a name, the "key"), so it survives closing the app.
 *  - Android: androidMain/.../data/LibraryStorage.android.kt (SharedPreferences)
 *  - iOS:     iosMain/.../data/LibraryStorage.ios.kt (NSUserDefaults)
 */
expect object LibraryStorage {
    fun load(key: String): String?
    fun save(key: String, text: String)
}
