package com.mybookshelf.app.scanner

import androidx.compose.runtime.Composable

/** What came back from the camera. */
sealed interface ScanResult {
    data class Success(val code: String) : ScanResult
    data object Cancelled : ScanResult
    data class Error(val message: String) : ScanResult
}

/**
 * Opens the camera to read a book barcode while [active] is true,
 * then calls [onResult] once.
 *
 * Each platform has its own version ("actual"):
 *  - Android: androidMain/.../scanner/BarcodeScanner.android.kt (Google code scanner)
 *  - iOS:     iosMain/.../scanner/BarcodeScanner.ios.kt (AVFoundation camera)
 */
@Composable
expect fun BarcodeScannerHost(active: Boolean, onResult: (ScanResult) -> Unit)
