package com.mybookshelf.app.files

import androidx.compose.runtime.Composable

/**
 * Saving and opening CSV files. Each platform shows its own file screen:
 *  - Android: androidMain/.../files/CsvFiles.android.kt ("Save to" / "Open" screens of the phone)
 *  - iOS:     iosMain/.../files/CsvFiles.ios.kt (Files app picker)
 */

/**
 * Returns a function `save(fileName, content)` that asks the user where to save the file.
 * [onDone] gets true when the file was saved, false if cancelled or failed.
 */
@Composable
expect fun rememberCsvSaver(onDone: (saved: Boolean) -> Unit): (fileName: String, content: String) -> Unit

/**
 * Returns a function that lets the user pick a file; [onOpened] gets its text
 * (null if cancelled or unreadable).
 */
@Composable
expect fun rememberCsvOpener(onOpened: (text: String?) -> Unit): () -> Unit
