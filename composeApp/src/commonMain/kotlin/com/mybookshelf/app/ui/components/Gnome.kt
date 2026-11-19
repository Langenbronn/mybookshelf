package com.mybookshelf.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** What the gnome is doing. */
enum class GnomePose(val label: String) {
    Reading("Reading"),
    Waving("Waving"),
    Searching("Searching"),
    Confused("Confused"),
    Celebrating("Celebrating"),
    Sleeping("Sleeping"),
    Love("Love"),
    Painting("Painting"),
    Shy("Shy"),
    Carrying("Carrying books"),
    Tea("Tea time"),
    Pointing("Pointing"),
}

// Gnome colors (the same in light and dark mode)
private val HatRed = Color(0xFFD84343)
private val HatBand = Color(0xFFA92F2F)
private val Skin = Color(0xFFF2C6A0)
private val Nose = Color(0xFFE8927C)
private val Beard = Color(0xFFF7F7F7)
private val BeardEdge = Color(0xFFCFCFCF)
private val Eyes = Color(0xFF2B2B2B)
private val Tunic = Color(0xFF3F6FB5)
private val Sleeve = Color(0xFF35609F)
private val Boots = Color(0xFF5A3A22)
private val Wood = Color(0xFF8A6A3B)
private val Glass = Color(0x66BFE3F5)
private val Amber = Color(0xFFE0A030)
private val SleepBlue = Color(0xFF7E8FB3)
private val HeartRed = Color(0xFFE5484D)
private val Blush = Color(0x80F28B8B)
private val Drop = Color(0xFF9CCFEA)
private val PaletteWood = Color(0xFFD9B382)
private val PaintBlue = Color(0xFF7FB8E6)
private val PaintYellow = Color(0xFFF6C744)
private val PaintGreen = Color(0xFF6DBE6A)
private val Confetti = listOf(Color(0xFFE5484D), Color(0xFFE0A030), Color(0xFF3F6FB5), Color(0xFF46A758))
private val BookCover = Color(0xFF7A4B2A)
private val Page = Color(0xFFFFF8E7)
private val PageTurning = Color(0xFFEFE4CC)
private val TextLine = Color(0xFFCBBFA6)
private val StackBooks = listOf(Color(0xFF2F6B4F), Color(0xFFB8433A), Color(0xFFE0A030))
private val Cup = Color(0xFFFFFDF7)
private val CupEdge = Color(0xFFB9A88E)
private val TeaColor = Color(0xFFB0703A)
private val Steam = Color(0xFFB8B8B8)
private val Sparkle = Color(0xFFF6C744)

private const val TWO_PI = (2 * PI).toFloat()

/**
 * The bookshelf gnome, animated, in one of several [GnomePose]s.
 * Drawn with code (no image files) on a 100 x 100 grid scaled to [size].
 */
@Composable
fun Gnome(pose: GnomePose, modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val transition = rememberInfiniteTransition(label = "gnome")
    // Fast cycle (steps, waving, jumping, heartbeat)
    val fast by transition.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(700, easing = LinearEasing)), label = "fast",
    )
    // Slow cycle (hat swing, breathing, floating things)
    val slow by transition.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "slow",
    )
    // Eyes: open most of the time, a quick blink every ~3 seconds
    val blink by transition.animateFloat(
        1f, 1f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                1f at 2900
                0.1f at 3000
                1f at 3100
            }
        ),
        label = "blink",
    )
    // Page turning (Reading pose)
    val page by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "page",
    )

    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            when (pose) {
                GnomePose.Reading -> drawReading(fast, slow, blink, page)
                GnomePose.Waving -> drawWaving(fast, slow, blink)
                GnomePose.Searching -> drawSearching(slow, blink)
                GnomePose.Confused -> drawConfused(slow, blink)
                GnomePose.Celebrating -> drawCelebrating(fast, slow)
                GnomePose.Sleeping -> drawSleeping(slow)
                GnomePose.Love -> drawLove(fast, slow)
                GnomePose.Painting -> drawPainting(fast, slow, blink)
                GnomePose.Shy -> drawShy(slow, blink)
                GnomePose.Carrying -> drawCarrying(fast, slow, blink)
                GnomePose.Tea -> drawTea(slow, blink)
                GnomePose.Pointing -> drawPointing(fast, slow, blink)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Poses
// ---------------------------------------------------------------------------

private fun DrawScope.drawReading(fast: Float, slow: Float, blink: Float, page: Float) {
    val step = sin(fast)
    drawGnome(
        Look(
            bounce = -abs(step) * 3f,
            leftFootLift = max(0f, step) * 4f,
            rightFootLift = max(0f, -step) * 4f,
            hatAngle = sin(slow) * 8f,
            eyeOpen = blink,
        ),
        front = { drawBook(page) },
    )
}

private fun DrawScope.drawWaving(fast: Float, slow: Float, blink: Float) {
    drawGnome(
        Look(bounce = -(1f + sin(slow)) * 0.6f, hatAngle = sin(slow) * 4f, eyeOpen = blink),
        arms = {
            drawArm(Offset(41f, 60f), Offset(35f, 77f))
            // Right arm swinging between ~25° and ~55° from vertical
            val angle = (40f + 15f * sin(fast)) * PI.toFloat() / 180f
            drawArm(Offset(59f, 60f), Offset(59f + sin(angle) * 16f, 60f - cos(angle) * 16f))
        },
    )
}

private fun DrawScope.drawSearching(slow: Float, blink: Float) {
    val sweep = sin(slow)   // -1..1: looking left / right
    val hand = Offset(63f + 4f * sweep, 66f)
    drawGnome(
        Look(bounce = -(1f + sin(slow * 2f)) * 0.4f, hatAngle = sweep * 5f, eyeOpen = blink, lookX = sweep * 1.2f),
        arms = {
            drawArm(Offset(41f, 60f), Offset(35f, 77f))
            drawArm(Offset(59f, 60f), hand)
        },
        front = {
            // Magnifying glass
            val lens = Offset(hand.x + 3f, hand.y - 12f)
            drawLine(Wood, hand, Offset(lens.x - 1.5f, lens.y + 5f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawCircle(Glass, radius = 5.5f, center = lens)
            drawCircle(Wood, radius = 5.5f, center = lens, style = Stroke(width = 1.6f))
            drawCircle(Color.White.copy(alpha = 0.7f), radius = 1.2f, center = Offset(lens.x - 2f, lens.y - 2f))
            drawCircle(Skin, radius = 2.8f, center = hand)
        },
    )
}

private fun DrawScope.drawConfused(slow: Float, blink: Float) {
    val shrug = (1f + sin(slow * 2f)) / 2f   // 0..1
    drawGnome(
        Look(bounce = -shrug, hatAngle = 10f, eyeOpen = blink),
        arms = {
            drawArm(Offset(41f, 60f), Offset(29f, 63f - shrug * 5f))
            drawArm(Offset(59f, 60f), Offset(71f, 63f - shrug * 5f))
        },
    )
    drawQuestionMark(Offset(72f, 20f + sin(slow) * 2f))
}

private fun DrawScope.drawCelebrating(fast: Float, slow: Float) {
    drawConfetti(slow)
    val jump = max(0f, sin(fast)) * 7f
    val wiggle = sin(fast * 2f) * 1.5f
    drawGnome(
        Look(bounce = -jump, leftFootLift = jump, rightFootLift = jump, hatAngle = sin(fast) * 6f, eyes = EyeStyle.Happy),
        arms = {
            drawArm(Offset(41f, 60f), Offset(31f, 43f + wiggle))
            drawArm(Offset(59f, 60f), Offset(69f, 43f - wiggle))
        },
    )
}

private fun DrawScope.drawSleeping(slow: Float) {
    val breath = sin(slow)
    drawGnome(
        Look(bounce = breath * 0.7f, hatAngle = -14f + breath * 2f, eyes = EyeStyle.Closed),
        arms = {
            // Hands resting together under the beard
            drawArm(Offset(41f, 60f), Offset(46f, 76f))
            drawArm(Offset(59f, 60f), Offset(54f, 76f))
        },
    )
    // Three "Z" floating up and fading
    val t = slow / TWO_PI
    for (i in 0..2) {
        val p = (t + i / 3f) % 1f
        val zSize = 3f + p * 4f
        val x = 64f + p * 16f
        val y = 34f - p * 24f
        val z = Path().apply {
            moveTo(x, y)
            lineTo(x + zSize, y)
            lineTo(x, y + zSize)
            lineTo(x + zSize, y + zSize)
        }
        drawPath(
            z,
            SleepBlue.copy(alpha = sin(p * PI.toFloat())),
            style = Stroke(width = 1.1f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawLove(fast: Float, slow: Float) {
    val beat = 1f + 0.12f * max(0f, sin(fast))
    val leftHand = Offset(42.5f, 73f)
    val rightHand = Offset(57.5f, 73f)
    drawGnome(
        Look(bounce = -(1f + sin(slow)) * 0.5f, hatAngle = sin(slow) * 4f, eyes = EyeStyle.Happy),
        arms = {
            drawArm(Offset(41f, 60f), leftHand)
            drawArm(Offset(59f, 60f), rightHand)
        },
        front = {
            drawHeart(Offset(50f, 70f), 15f * beat)
            drawCircle(Skin, radius = 2.8f, center = leftHand)
            drawCircle(Skin, radius = 2.8f, center = rightHand)
        },
    )
}

/** Painting a little picture on an easel (used while a cover is loading). */
private fun DrawScope.drawPainting(fast: Float, slow: Float, blink: Float) {
    drawEasel(progress = slow / TWO_PI)
    // The gnome stands a bit to the left of the easel; the brush goes up and down
    val hand = Offset(78f + 1.5f * sin(fast), 53f + 4f * sin(fast))
    val paletteHand = Offset(34f, 72f)
    drawGnome(
        Look(shiftX = -12f, bounce = -(1f + sin(slow * 2f)) * 0.4f, hatAngle = 4f + sin(slow) * 3f, eyeOpen = blink, lookX = 1f),
        arms = {
            drawArm(Offset(41f, 60f), paletteHand)
            drawArm(Offset(59f, 60f), hand)
        },
        front = {
            // Palette with paint dots in the left hand
            drawOval(PaletteWood, Offset(paletteHand.x - 7f, paletteHand.y - 1f), Size(11f, 6.5f))
            drawCircle(HeartRed, 0.9f, Offset(paletteHand.x - 4.5f, paletteHand.y + 1.5f))
            drawCircle(PaintYellow, 0.9f, Offset(paletteHand.x - 2f, paletteHand.y + 3.5f))
            drawCircle(PaintBlue, 0.9f, Offset(paletteHand.x + 1f, paletteHand.y + 3.8f))
            drawCircle(Skin, radius = 2.8f, center = paletteHand)
            // Brush
            val tip = Offset(hand.x + 4f, hand.y - 5f)
            drawLine(Wood, hand, tip, strokeWidth = 1.4f, cap = StrokeCap.Round)
            drawCircle(PaintBlue, radius = 1.1f, center = tip)
            drawCircle(Skin, radius = 2.8f, center = hand)
        },
    )
}

/** Easel with a picture that gets painted little by little ([progress] 0..1). */
private fun DrawScope.drawEasel(progress: Float) {
    drawLine(Wood, Offset(70f, 92f), Offset(74f, 34f), strokeWidth = 1.6f, cap = StrokeCap.Round)
    drawLine(Wood, Offset(86f, 92f), Offset(80f, 34f), strokeWidth = 1.6f, cap = StrokeCap.Round)
    drawRect(Page, Offset(64f, 36f), Size(24f, 26f))
    drawRect(Wood, Offset(64f, 36f), Size(24f, 26f), style = Stroke(width = 1f))
    drawRect(Wood, Offset(63f, 62f), Size(26f, 2f))
    // 1st third: sky, 2nd: sun, 3rd: hill
    val sky = (progress * 3f).coerceIn(0f, 1f)
    val sun = (progress * 3f - 1f).coerceIn(0f, 1f)
    val hill = (progress * 3f - 2f).coerceIn(0f, 1f)
    drawRect(PaintBlue, Offset(65.5f, 37.5f), Size(21f * sky, 12f))
    if (sun > 0f) drawCircle(PaintYellow, radius = 3.5f * sun, center = Offset(81f, 42f))
    drawRect(PaintGreen, Offset(65.5f, 52f), Size(21f * hill, 8.5f))
}

/** Shy and sorry (used when a book has no cover): eyes down, pink cheeks, hands together. */
private fun DrawScope.drawShy(slow: Float, blink: Float) {
    val sway = sin(slow)
    drawGnome(
        Look(shiftX = sway * 1.2f, bounce = 0.5f, hatAngle = 6f + sway * 2f, eyeOpen = blink, lookY = 0.8f, blush = true),
        arms = {
            drawArm(Offset(41f, 60f), Offset(47f, 78f))
            drawArm(Offset(59f, 60f), Offset(53f, 78f))
        },
    )
    // A little drop of embarrassment sliding down next to the hat
    val p = (slow / TWO_PI * 2f) % 1f
    val drop = Offset(66f + sway * 1.2f, 30f + p * 8f)
    val alpha = 1f - p
    val dropPath = Path().apply {
        moveTo(drop.x, drop.y - 3f)
        quadraticTo(drop.x + 1.6f, drop.y - 0.5f, drop.x + 1.4f, drop.y + 0.4f)
        quadraticTo(drop.x, drop.y + 2.2f, drop.x - 1.4f, drop.y + 0.4f)
        quadraticTo(drop.x - 1.6f, drop.y - 0.5f, drop.x, drop.y - 3f)
        close()
    }
    drawPath(dropPath, Drop.copy(alpha = alpha))
}

/** Walking with a stack of three books in his arms (used for places / storing books). */
private fun DrawScope.drawCarrying(fast: Float, slow: Float, blink: Float) {
    val step = sin(fast)
    val wobble = sin(fast) * 1.5f   // the stack tilts a little with each step
    val leftHand = Offset(37f, 74f)
    val rightHand = Offset(63f, 74f)
    drawGnome(
        Look(
            bounce = -abs(step) * 2.5f,
            leftFootLift = max(0f, step) * 4f,
            rightFootLift = max(0f, -step) * 4f,
            hatAngle = sin(slow) * 6f,
            eyeOpen = blink,
            lookY = -0.6f,
        ),
        arms = {
            drawArm(Offset(41f, 60f), leftHand)
            drawArm(Offset(59f, 60f), rightHand)
        },
        front = {
            rotate(degrees = wobble, pivot = Offset(50f, 80f)) {
                // Three books, each one a bit shifted
                val widths = listOf(26f, 23f, 24f)
                val shifts = listOf(0f, 1.5f, -1f)
                for (i in 0..2) {
                    val y = 75f - i * 5.2f
                    val left = 50f - widths[i] / 2f + shifts[i]
                    drawRoundRect(StackBooks[i], Offset(left, y), Size(widths[i], 5f), CornerRadius(1f))
                    // Pages on the side
                    drawRect(Page, Offset(left + widths[i] - 3f, y + 0.8f), Size(2.2f, 3.4f))
                    // Little gold line on the spine
                    drawLine(Sparkle, Offset(left + 3f, y + 2.5f), Offset(left + 9f, y + 2.5f), strokeWidth = 0.6f)
                }
            }
            drawCircle(Skin, radius = 2.8f, center = leftHand)
            drawCircle(Skin, radius = 2.8f, center = rightHand)
        },
    )
}

/** Cosy tea break: sipping from a steaming cup, eyes closed with pleasure. */
private fun DrawScope.drawTea(slow: Float, blink: Float) {
    // Every few seconds the cup goes up to the beard for a sip
    val sip = max(0f, sin(slow)).let { it * it }   // 0..1
    val hand = Offset(60f - sip * 4f, 72f - sip * 10f)
    drawGnome(
        Look(
            bounce = -(1f + sin(slow * 2f)) * 0.3f,
            hatAngle = -6f + sin(slow) * 3f,
            eyes = if (sip > 0.4f) EyeStyle.Closed else EyeStyle.Open,
            eyeOpen = blink,
            blush = sip > 0.4f,
        ),
        arms = {
            drawArm(Offset(41f, 60f), Offset(44f, 77f))
            drawArm(Offset(59f, 60f), hand)
        },
        front = {
            val cupTop = Offset(hand.x - 3f, hand.y - 7f)
            // Handle
            drawArc(
                CupEdge, startAngle = -90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cupTop.x + 7f, cupTop.y + 1.5f), size = Size(4f, 4.5f),
                style = Stroke(width = 1.2f),
            )
            // Cup and tea
            drawRoundRect(Cup, cupTop, Size(9f, 8f), CornerRadius(1.5f, 3f))
            drawRoundRect(CupEdge, cupTop, Size(9f, 8f), CornerRadius(1.5f, 3f), style = Stroke(width = 0.6f))
            drawOval(TeaColor, Offset(cupTop.x + 0.8f, cupTop.y + 0.3f), Size(7.4f, 1.8f))
            drawCircle(Skin, radius = 2.8f, center = hand)
            // Steam: two wavy lines going up and fading
            val t = slow / TWO_PI
            for (i in 0..1) {
                val p = (t * 2f + i * 0.5f) % 1f
                val x = cupTop.x + 2.5f + i * 4f
                val y = cupTop.y - 2f - p * 10f
                val steam = Path().apply {
                    moveTo(x, y + 5f)
                    quadraticTo(x - 2f, y + 2.5f, x, y)
                    quadraticTo(x + 2f, y - 2.5f, x, y - 5f)
                }
                drawPath(steam, Steam.copy(alpha = 0.8f * sin(p * PI.toFloat())), style = Stroke(width = 0.9f, cap = StrokeCap.Round))
            }
        },
    )
}

/** Pointing up to the right with a twinkle ("look here!"), the other hand on the hip. */
private fun DrawScope.drawPointing(fast: Float, slow: Float, blink: Float) {
    val nudge = max(0f, sin(fast)) * 2f   // the arm pokes forward
    val hand = Offset(74f + nudge, 44f - nudge * 0.6f)
    drawGnome(
        Look(bounce = -(1f + sin(slow)) * 0.5f, hatAngle = 6f + sin(slow) * 3f, eyeOpen = blink, lookX = 1.2f, lookY = -0.5f),
        arms = {
            drawArm(Offset(41f, 60f), Offset(36f, 70f))   // hand on the hip
            drawArm(Offset(59f, 60f), hand)
        },
        front = {
            // Pointing finger
            drawLine(Skin, hand, Offset(hand.x + 3.2f, hand.y - 2f), strokeWidth = 1.6f, cap = StrokeCap.Round)
        },
    )
    // Twinkling star where he points
    val star = Offset(86f, 30f)
    val twinkle = 0.6f + 0.4f * sin(slow * 3f)
    drawStar(star, 5f * twinkle)
}

/** A 4-point sparkle. */
private fun DrawScope.drawStar(center: Offset, size: Float) {
    val (x, y) = center
    val star = Path().apply {
        moveTo(x, y - size)
        quadraticTo(x, y, x + size, y)
        quadraticTo(x, y, x, y + size)
        quadraticTo(x, y, x - size, y)
        quadraticTo(x, y, x, y - size)
        close()
    }
    drawPath(star, Sparkle)
}

// ---------------------------------------------------------------------------
// The gnome itself (shared by every pose)
// ---------------------------------------------------------------------------

private enum class EyeStyle { Open, Closed, Happy }

private class Look(
    val bounce: Float = 0f,          // negative = up
    val leftFootLift: Float = 0f,
    val rightFootLift: Float = 0f,
    val hatAngle: Float = 0f,        // degrees
    val eyes: EyeStyle = EyeStyle.Open,
    val eyeOpen: Float = 1f,         // 1 = open, 0.1 = blinking
    val lookX: Float = 0f,           // eyes looking left/right
    val lookY: Float = 0f,           // eyes looking down (positive)
    val blush: Boolean = false,      // pink cheeks
    val shiftX: Float = 0f,          // move the whole gnome left/right
)

/**
 * Draws the gnome. [arms] are drawn behind the face and beard,
 * [front] on top of everything (book, heart, magnifying glass…).
 */
private fun DrawScope.drawGnome(
    look: Look,
    arms: DrawScope.() -> Unit = {},
    front: DrawScope.() -> Unit = {},
) = translate(left = look.shiftX) { drawGnomeAtCenter(look, arms, front) }

private fun DrawScope.drawGnomeAtCenter(
    look: Look,
    arms: DrawScope.() -> Unit,
    front: DrawScope.() -> Unit,
) {
    // Shadow: smaller when the gnome is up
    val shadowWidth = (40f + look.bounce * 2f).coerceAtLeast(20f)
    drawOval(
        Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(50f - shadowWidth / 2f, 92.5f),
        size = Size(shadowWidth, 5f),
    )

    // Boots
    drawRoundRect(Boots, Offset(36f, 86f - look.leftFootLift), Size(12f, 7f), CornerRadius(3f))
    drawRoundRect(Boots, Offset(52f, 86f - look.rightFootLift), Size(12f, 7f), CornerRadius(3f))

    translate(top = look.bounce) {
        // Tunic
        val body = Path().apply {
            moveTo(40f, 55f)
            lineTo(60f, 55f)
            lineTo(67f, 88f)
            quadraticTo(50f, 91f, 33f, 88f)
            close()
        }
        drawPath(body, Tunic)

        arms()

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

        drawEyes(look)
        if (look.blush) {
            drawCircle(Blush, radius = 2.4f, center = Offset(43.2f, 47f))
            drawCircle(Blush, radius = 2.4f, center = Offset(56.8f, 47f))
        }

        // Big round nose
        drawCircle(Nose, radius = 3.6f, center = Offset(50f, 46.5f))

        // Pointy hat, swinging around its base
        rotate(degrees = look.hatAngle, pivot = Offset(50f, 39f)) {
            val hat = Path().apply {
                moveTo(38f, 39f)
                quadraticTo(42f, 20f, 53f, 7f)
                quadraticTo(56f, 22f, 62f, 39f)
                close()
            }
            drawPath(hat, HatRed)
            drawRoundRect(HatBand, Offset(37f, 35.5f), Size(26f, 5f), CornerRadius(2.5f))
        }

        front()
    }
}

private fun DrawScope.drawEyes(look: Look) {
    for (x in listOf(44.4f, 52.4f)) {
        val left = x + look.lookX
        val y = 42f + look.lookY
        when (look.eyes) {
            EyeStyle.Open -> {
                val h = 3.6f * look.eyeOpen
                drawOval(Eyes, Offset(left, y - h / 2f), Size(3.2f, h))
            }
            // ‿ closed, peaceful
            EyeStyle.Closed -> drawArc(
                Eyes, startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(left - 0.2f, y - 1.5f), size = Size(3.6f, 3f),
                style = Stroke(width = 0.9f, cap = StrokeCap.Round),
            )
            // ^ happy
            EyeStyle.Happy -> drawArc(
                Eyes, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(left - 0.2f, y - 1f), size = Size(3.6f, 3f),
                style = Stroke(width = 0.9f, cap = StrokeCap.Round),
            )
        }
    }
}

/** A sleeve from the shoulder to the hand, and the hand. */
private fun DrawScope.drawArm(shoulder: Offset, hand: Offset) {
    drawLine(Sleeve, shoulder, hand, strokeWidth = 4.5f, cap = StrokeCap.Round)
    drawCircle(Skin, radius = 2.8f, center = hand)
}

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

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
    drawRect(PageTurning, Offset(minOf(50f, 50f + pageWidth), 66f), Size(abs(pageWidth), 12.5f))
    // Hands
    drawCircle(Skin, radius = 2.8f, center = Offset(36.5f, 72f))
    drawCircle(Skin, radius = 2.8f, center = Offset(63.5f, 72f))
}

private fun DrawScope.drawQuestionMark(at: Offset) {
    val (x, y) = at
    val hook = Path().apply {
        moveTo(x - 3f, y - 1f)
        quadraticTo(x - 3f, y - 5f, x, y - 5f)
        quadraticTo(x + 3.5f, y - 5f, x + 3f, y - 1.5f)
        quadraticTo(x + 2.5f, y + 0.5f, x, y + 1.5f)
        lineTo(x, y + 3.5f)
    }
    drawPath(hook, Amber, style = Stroke(width = 1.8f, cap = StrokeCap.Round))
    drawCircle(Amber, radius = 1.1f, center = Offset(x, y + 6.8f))
}

private fun DrawScope.drawHeart(center: Offset, size: Float) {
    val (cx, cy) = center
    val heart = Path().apply {
        moveTo(cx, cy + size * 0.45f)
        cubicTo(cx - size * 0.9f, cy - size * 0.05f, cx - size * 0.45f, cy - size * 0.65f, cx, cy - size * 0.25f)
        cubicTo(cx + size * 0.45f, cy - size * 0.65f, cx + size * 0.9f, cy - size * 0.05f, cx, cy + size * 0.45f)
        close()
    }
    drawPath(heart, HeartRed)
    drawCircle(Color.White.copy(alpha = 0.5f), radius = size * 0.07f, center = Offset(cx - size * 0.28f, cy - size * 0.2f))
}

/** Little paper pieces falling and spinning behind the gnome. */
private fun DrawScope.drawConfetti(slow: Float) {
    val t = slow / TWO_PI
    for (i in 0..7) {
        val p = (t + i * 0.37f) % 1f
        val x = 10f + i * 11.5f
        val y = 4f + p * 60f
        rotate(degrees = p * 360f + i * 40f, pivot = Offset(x, y)) {
            drawRect(
                Confetti[i % Confetti.size].copy(alpha = 1f - p),
                topLeft = Offset(x - 1.6f, y - 0.8f),
                size = Size(3.2f, 1.6f),
            )
        }
    }
}
