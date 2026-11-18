package com.mybookshelf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mybookshelf.app.data.LibraryStorage

/**
 * Android entry point. It only displays the shared [App];
 * all the real UI lives in commonMain.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Open the phone storage used by the library (must happen before the UI starts)
        LibraryStorage.init(this)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

/** Lets you see the app in Android Studio's "Split" / "Design" view. */
@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
