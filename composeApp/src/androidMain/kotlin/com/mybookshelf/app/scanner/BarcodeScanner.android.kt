package com.mybookshelf.app.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Android: uses Google's ready-made scanner screen (Google Play services).
 * No camera permission is needed: Google's screen handles the camera itself.
 */
@Composable
actual fun BarcodeScannerHost(active: Boolean, onResult: (ScanResult) -> Unit) {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        val options = GmsBarcodeScannerOptions.Builder()
            // Book barcodes are EAN-13 (ISBN); the others are common on older books
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
        GmsBarcodeScanning.getClient(context, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue
                currentOnResult(
                    if (value != null) ScanResult.Success(value) else ScanResult.Error("Unreadable barcode")
                )
            }
            .addOnCanceledListener { currentOnResult(ScanResult.Cancelled) }
            .addOnFailureListener { e ->
                currentOnResult(ScanResult.Error(e.message ?: "Scanner unavailable"))
            }
    }
}
