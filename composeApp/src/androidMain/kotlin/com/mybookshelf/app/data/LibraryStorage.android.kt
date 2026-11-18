package com.mybookshelf.app.data

import android.content.Context
import android.content.SharedPreferences

actual object LibraryStorage {
    private var prefs: SharedPreferences? = null

    /** Called once by MainActivity: Android needs a Context to open the storage. */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("my_bookshelf", Context.MODE_PRIVATE)
    }

    // prefs is null in Android Studio's preview: the library is then simply empty
    actual fun load(key: String): String? = prefs?.getString(key, null)

    actual fun save(key: String, text: String) {
        prefs?.edit()?.putString(key, text)?.apply()
    }
}
