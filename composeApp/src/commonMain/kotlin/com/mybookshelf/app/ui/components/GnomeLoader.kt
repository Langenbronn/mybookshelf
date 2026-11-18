package com.mybookshelf.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Loading animation: the gnome reading while walking, with "Looking for your book...". */
@Composable
fun GnomeLoader(
    modifier: Modifier = Modifier,
    message: String = "Looking for your book",
) {
    // "." → ".." → "..." after the message
    val dots by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "dots",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Gnome(GnomePose.Reading)
        val visibleDots = dots.toInt().coerceAtMost(3)
        Text(
            buildAnnotatedString {
                append(message)
                append(".".repeat(visibleDots))
                // Invisible dots keep the text from shifting sideways
                withStyle(SpanStyle(color = Color.Transparent)) { append(".".repeat(3 - visibleDots)) }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The gnome in a [pose] above a centered message. Used for empty pages and errors. */
@Composable
fun GnomeMessage(
    pose: GnomePose,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    isError: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Gnome(pose, size = 130.dp)
        title?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
        Text(
            message,
            textAlign = TextAlign.Center,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
