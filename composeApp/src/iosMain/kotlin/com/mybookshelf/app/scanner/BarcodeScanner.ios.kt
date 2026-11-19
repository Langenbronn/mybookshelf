@file:OptIn(ExperimentalForeignApi::class)

package com.mybookshelf.app.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

/**
 * iOS: shows the camera inside the app with a Cancel button on top.
 * The camera permission text is in iosApp/iosApp/Info.plist (NSCameraUsageDescription).
 */
@Composable
actual fun BarcodeScannerHost(active: Boolean, onResult: (ScanResult) -> Unit) {
    if (!active) return
    val currentOnResult by rememberUpdatedState(onResult)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        UIKitView(
            factory = { CameraScannerView { result -> currentOnResult(result) } },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.stop() },
        )
        Button(
            onClick = { currentOnResult(ScanResult.Cancelled) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        ) {
            Text("Cancel")
        }
    }
}

/** A UIView showing the camera and reporting the first barcode it sees. */
private class CameraScannerView(
    private val onResult: (ScanResult) -> Unit,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {

    private val session = AVCaptureSession()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    private var finished = false

    // Must be kept in a property: AVFoundation only holds a weak reference to it
    private val delegate = MetadataDelegate { code -> finish(ScanResult.Success(code)) }

    init {
        backgroundColor = UIColor.blackColor
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)

        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> setUpCamera()
            AVAuthorizationStatusNotDetermined ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (granted) setUpCamera()
                        else finish(ScanResult.Error("Camera access denied"))
                    }
                }
            else -> finish(ScanResult.Error("Camera access denied. Enable it in Settings > Gnome Library."))
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }

    private fun setUpCamera() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            ?: return finish(ScanResult.Error("No camera available"))
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null)
            as? AVCaptureDeviceInput
            ?: return finish(ScanResult.Error("Cannot open the camera"))

        val output = AVCaptureMetadataOutput()
        if (!session.canAddInput(input) || !session.canAddOutput(output)) {
            return finish(ScanResult.Error("Cannot open the camera"))
        }
        session.addInput(input)
        session.addOutput(output)
        // The barcode types must be set AFTER the output is added to the session
        output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
        output.metadataObjectTypes = listOf(
            AVMetadataObjectTypeEAN13Code,
            AVMetadataObjectTypeEAN8Code,
            AVMetadataObjectTypeUPCECode,
        )
        // startRunning() is slow: Apple recommends calling it off the main thread
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            session.startRunning()
        }
    }

    private fun finish(result: ScanResult) {
        if (finished) return
        finished = true
        stop()
        onResult(result)
    }

    fun stop() {
        if (session.running) {
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                session.stopRunning()
            }
        }
    }
}

private class MetadataDelegate(
    private val onCode: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val code = didOutputMetadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .firstNotNullOfOrNull { it.stringValue }
            ?: return
        onCode(code)
    }
}
