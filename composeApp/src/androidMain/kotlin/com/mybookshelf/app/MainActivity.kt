package com.mybookshelf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Android entry point. It only displays the shared [App];
 * all the real UI lives in commonMain.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
