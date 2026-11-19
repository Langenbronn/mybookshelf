package com.mybookshelf.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Book spines on the shelf (always the same pattern, so it doesn't flicker)
private val SpineColors = listOf(
    Color(0xFF2F6B4F), Color(0xFFB8433A), Color(0xFFE0A030), Color(0xFF3F6FB5),
    Color(0xFF7A4B2A), Color(0xFF8E5BA8), Color(0xFF46A758), Color(0xFFD9774A),
)
private val ShelfWood = Color(0xFF8A6A3B)
private val ShelfWoodDark = Color(0xFF6B5030)
private val SpineLine = Color(0x66FFF3D6)

/**
 * A little wooden bookshelf full of colorful books, drawn across the whole width.
 * Used as a decoration under the top bar.
 */
@Composable
fun BookshelfStrip(modifier: Modifier = Modifier, height: Dp = 22.dp) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val plank = size.height * 0.22f
        val booksBottom = size.height - plank
        var x = 2.dp.toPx()
        var i = 0
        while (x < size.width) {
            // Pseudo-random but stable sizes
            val w = (7 + (i * 7) % 5).dp.toPx()
            val h = booksBottom * (0.62f + ((i * 37) % 10) / 27f)
            val color = SpineColors[(i * 3) % SpineColors.size]
            // Every 9th book leans on its neighbour
            if (i % 9 == 4) {
                val leanW = w * 1.15f
                drawRoundRect(color, Offset(x + w * 0.2f, booksBottom - h * 0.92f), Size(leanW, h * 0.92f), CornerRadius(2f))
                x += leanW + 3.dp.toPx()
            } else {
                drawRoundRect(color, Offset(x, booksBottom - h), Size(w, h), CornerRadius(2f))
                // Two small lines on the spine
                drawLine(SpineLine, Offset(x + 2f, booksBottom - h * 0.8f), Offset(x + w - 2f, booksBottom - h * 0.8f), strokeWidth = 2f)
                drawLine(SpineLine, Offset(x + 2f, booksBottom - h * 0.25f), Offset(x + w - 2f, booksBottom - h * 0.25f), strokeWidth = 2f)
                x += w + 1.dp.toPx()
            }
            // Leave a little gap now and then
            if (i % 13 == 12) x += 10.dp.toPx()
            i++
        }
        // The wooden plank
        drawRect(ShelfWood, Offset(0f, booksBottom), Size(size.width, plank))
        drawRect(ShelfWoodDark, Offset(0f, size.height - plank * 0.3f), Size(size.width, plank * 0.3f))
    }
}
