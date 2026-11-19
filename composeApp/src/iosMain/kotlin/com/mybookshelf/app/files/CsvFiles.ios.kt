@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.mybookshelf.app.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeText
import platform.darwin.NSObject

/** iOS: writes the file in a temporary folder, then lets the user copy it with the Files picker. */
@Composable
actual fun rememberCsvSaver(onDone: (saved: Boolean) -> Unit): (fileName: String, content: String) -> Unit {
    val currentOnDone by rememberUpdatedState(onDone)
    val delegate = remember { PickerDelegate() }
    return remember {
        { fileName, content ->
            val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
            val written = NSString.create(string = content)
                .writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
            if (!written) {
                currentOnDone(false)
            } else {
                delegate.onPicked = { urls -> currentOnDone(urls.isNotEmpty()) }
                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                picker.delegate = delegate
                topViewController()?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

/** iOS: the Files picker, for a CSV or text file. */
@Composable
actual fun rememberCsvOpener(onOpened: (text: String?) -> Unit): () -> Unit {
    val currentOnOpened by rememberUpdatedState(onOpened)
    val delegate = remember { PickerDelegate() }
    return remember {
        {
            delegate.onPicked = { urls ->
                val text = urls.firstOrNull()?.let {
                    NSString.stringWithContentsOfURL(it, encoding = NSUTF8StringEncoding, error = null)
                }
                currentOnOpened(text)
            }
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeCommaSeparatedText, UTTypePlainText, UTTypeText),
                asCopy = true,
            )
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

/** Receives the picker's answer. Kept in a `remember` because UIKit only holds a weak reference. */
private class PickerDelegate : NSObject(), UIDocumentPickerDelegateProtocol {
    var onPicked: (List<NSURL>) -> Unit = {}

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onPicked(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked(emptyList())
    }
}

/** The screen currently shown, to present the picker on top of it. */
private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
