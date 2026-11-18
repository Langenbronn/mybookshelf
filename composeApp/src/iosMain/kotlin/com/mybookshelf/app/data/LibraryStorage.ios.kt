package com.mybookshelf.app.data

import platform.Foundation.NSUserDefaults

actual object LibraryStorage {
    actual fun load(key: String): String? = NSUserDefaults.standardUserDefaults.stringForKey(key)

    actual fun save(key: String, text: String) {
        NSUserDefaults.standardUserDefaults.setObject(text, forKey = key)
    }
}
