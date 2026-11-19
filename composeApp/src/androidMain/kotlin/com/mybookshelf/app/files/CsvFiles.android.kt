package com.mybookshelf.app.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Android: the system "Save to…" screen (Downloads, Google Drive…). */
@Composable
actual fun rememberCsvSaver(onDone: (saved: Boolean) -> Unit): (fileName: String, content: String) -> Unit {
    val context = LocalContext.current
    val currentOnDone by rememberUpdatedState(onDone)
    // The text waiting for the user to choose where to save it
    var pending by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val content = pending
        pending = null
        if (uri == null || content == null) {
            currentOnDone(false)
            return@rememberLauncherForActivityResult
        }
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        }.isSuccess
        currentOnDone(ok)
    }
    return remember(launcher) {
        { fileName, content ->
            pending = content
            launcher.launch(fileName)
        }
    }
}

/** Android: the system "Open" screen. */
@Composable
actual fun rememberCsvOpener(onOpened: (text: String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnOpened by rememberUpdatedState(onOpened)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            currentOnOpened(null)
            return@rememberLauncherForActivityResult
        }
        val text = runCatching {
            context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
        currentOnOpened(text)
    }
    // CSV files are sometimes typed as plain text or as Excel files depending on the app that made them
    return remember(launcher) {
        {
            launcher.launch(
                arrayOf("text/csv", "text/comma-separated-values", "text/plain", "text/*", "application/vnd.ms-excel")
            )
        }
    }
}
