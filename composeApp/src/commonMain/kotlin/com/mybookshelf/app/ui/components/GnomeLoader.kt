package com.mybookshelf.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Gnome colors (the same in light and dark mode)
private val HatRed = Color(0xFFD84343)
private val HatBand = Color(0xFFA92F2F)
private val Skin = Color(0xFFF2C6A0)
private val Nose = Color(0xFFE8927C)
private val Beard = Color(0xFFF7F7F7)
private val BeardEdge = Color(0xFFCFCFCF)
private val Eyes = Color(0xFF2B2B2B)
private val Tunic = Color(0xFF3F6FB5)
private val Boots = Color(0xFF5A3A22)
private val BookCover = Color(0xFF7A4B2A)
private val Page = Color(0xFFFFF8E7)
private val PageTurning = Color(0xFFEFE4CC)
private val TextLine = Color(0xFFCBBFA6)

/**
 * Loading animation: a little gnome walking on the spot while reading a book.
 * Drawn with code (no image files), on a 100 x 100 grid scaled to [size].
 */
@Composable
fun GnomeLoader(
    modifier: Modifier = Modifier,
    message: String = "Looking for your book",
) {
    val transition = rememberInfiniteTransition(label = "gnome")
    // One step cycle: drives the bounce and the feet
    val walk by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "walk",
    )
    // Hat swinging left and right (degrees)
    val hatAngle by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hat",
    )
    // Eyes: open most of the time, a quick blink every ~3 seconds
    val eyeOpen by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                1f at 2900
                0.1f at 3000
                1f at 3100
            }
        ),
        label = "blink",
    )
    // A page turning from right to left
    val pageTurn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing)),
        label = "page",
    )
    // "." → ".." → "..." after the message
    val dots by transition.animateFloat(
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
        Canvas(Modifier.size(140.dp)) {
            scale(size.minDimension / 100f, pivot = Offset.Zero) {
                drawGnome(walk, hatAngle, eyeOpen, pageTurn)
            }
        }
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

/** Draws the gnome on a 100 x 100 grid. */
private fun DrawScope.drawGnome(walk: Float, hatAngle: Float, eyeOpen: Float, pageTurn: Float) {
    val step = sin(walk)
    val bounce = -abs(step) * 3f          // the body goes up at each step
    val leftFootLift = max(0f, step) * 4f
    val rightFootLift = max(0f, -step) * 4f

    // Shadow: smaller when the gnome is up
    val shadowWidth = 40f + bounce * 2f
    drawOval(
        Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(50f - shadowWidth / 2f, 92.5f),
        size = Size(shadowWidth, 5f),
    )

    // Boots
    drawRoundRect(Boots, Offset(36f, 86f - leftFootLift), Size(12f, 7f), CornerRadius(3f))
    drawRoundRect(Boots, Offset(52f, 86f - rightFootLift), Size(12f, 7f), CornerRadius(3f))

    translate(top = bounce) {
        // Tunic
        val body = Path().apply {
            moveTo(40f, 55f)
            lineTo(60f, 55f)
            lineTo(67f, 88f)
            quadraticTo(50f, 91f, 33f, 88f)
            close()
        }
        drawPath(body, Tunic)

        // Face
        drawCircle(Skin, radius = 9f, center = Offset(50f, 44f))

        // Beard (with a light outline so it shows on a white background)
        val beard = Path().apply {
            moveTo(40f, 46f)
            quadraticTo(36f, 64f, 50f, 74f)
            quadraticTo(64f, 64f, 60f, 46f)
            quadraticTo(50f, 52f, 40f, 46f)
            close()
        }
        drawPath(beard, Beard)
        drawPath(beard, BeardEdge, style = Stroke(width = 0.8f))

        // Eyes (squashed vertically when blinking)
        val eyeHeight = 3.6f * eyeOpen
        drawOval(Eyes, Offset(44.4f, 42f - eyeHeight / 2f), Size(3.2f, eyeHeight))
        drawOval(Eyes, Offset(52.4f, 42f - eyeHeight / 2f), Size(3.2f, eyeHeight))

        // Big round nose
        drawCircle(Nose, radius = 3.6f, center = Offset(50f, 46.5f))

        // Pointy hat, swinging around its base
        rotate(degrees = hatAngle, pivot = Offset(50f, 39f)) {
            val hat = Path().apply {
                moveTo(38f, 39f)
                quadraticTo(42f, 20f, 53f, 7f)
                quadraticTo(56f, 22f, 62f, 39f)
                close()
            }
            drawPath(hat, HatRed)
            drawRoundRect(HatBand, Offset(37f, 35.5f), Size(26f, 5f), CornerRadius(2.5f))
        }

        drawBook(pageTurn)
    }
}

/** Open book held in both hands, with one page turning. */
private fun DrawScope.drawBook(pageTurn: Float) {
    drawRoundRect(BookCover, Offset(36.5f, 65f), Size(27f, 14.5f), CornerRadius(1.5f))
    drawRect(Page, Offset(38f, 66f), Size(11.7f, 12.5f))
    drawRect(Page, Offset(50.3f, 66f), Size(11.7f, 12.5f))
    for (i in 0..3) {
        val y = 68.8f + i * 2.6f
        drawLine(TextLine, Offset(40f, y), Offset(48f, y), strokeWidth = 0.6f)
        drawLine(TextLine, Offset(52f, y), Offset(60f, y), strokeWidth = 0.6f)
    }

    // The turning page: full width on the right, shrinks to the spine, then grows on the left
    val pageWidth = 11.7f * cos(PI.toFloat() * pageTurn)
    val left = minOf(50f, 50f + pageWidth)
    drawRect(PageTurning, Offset(left, 66f), Size(abs(pageWidth), 12.5f))

    // Hands
    drawCircle(Skin, radius = 2.8f, center = Offset(36.5f, 72f))
    drawCircle(Skin, radius = 2.8f, center = Offset(63.5f, 72f))
}
