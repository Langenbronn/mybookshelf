package com.mybookshelf.app

import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS entry point. Called from Swift (iosApp/iosApp/ContentView.swift).
 * It only displays the shared [App].
 */
fun MainViewController() = ComposeUIViewController { App() }
