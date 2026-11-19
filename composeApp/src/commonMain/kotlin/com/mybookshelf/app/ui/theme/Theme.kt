package com.mybookshelf.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/*
 * "Gnome's forest bookshop" theme: warm paper background, forest green,
 * the red of the gnome's hat and a touch of amber. Change these colors to restyle the whole app.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6B4F),             // forest green
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE8D6),
    onPrimaryContainer = Color(0xFF0E2E1F),
    secondary = Color(0xFFB8433A),           // gnome hat red
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF8D9D2),
    onSecondaryContainer = Color(0xFF45100B),
    tertiary = Color(0xFFB07A1E),            // amber
    tertiaryContainer = Color(0xFFF7E1B5),
    onTertiaryContainer = Color(0xFF3A2600),
    background = Color(0xFFFBF3E4),          // old paper (instead of white)
    onBackground = Color(0xFF3A2E24),        // dark brown ink
    surface = Color(0xFFFBF3E4),
    onSurface = Color(0xFF3A2E24),
    surfaceVariant = Color(0xFFEDE0CB),
    onSurfaceVariant = Color(0xFF6B5B4A),
    surfaceContainerLowest = Color(0xFFFFFBF3),
    surfaceContainerLow = Color(0xFFF8EEDC),
    surfaceContainer = Color(0xFFF3E7D2),
    surfaceContainerHigh = Color(0xFFEFE2CA),
    surfaceContainerHighest = Color(0xFFFFF9EE), // cards: like a light page on the paper
    outline = Color(0xFFA8957D),
    outlineVariant = Color(0xFFDCCBB0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93D3AE),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1F5139),
    onPrimaryContainer = Color(0xFFCFE8D6),
    secondary = Color(0xFFFFB4A8),
    onSecondary = Color(0xFF5F1410),
    secondaryContainer = Color(0xFF7D2B23),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = Color(0xFFF0C06A),
    tertiaryContainer = Color(0xFF5C4100),
    onTertiaryContainer = Color(0xFFFFDEA6),
    background = Color(0xFF1C1814),          // cozy wood at night
    onBackground = Color(0xFFEDE1D2),
    surface = Color(0xFF1C1814),
    onSurface = Color(0xFFEDE1D2),
    surfaceVariant = Color(0xFF4A4036),
    onSurfaceVariant = Color(0xFFD3C4B1),
    surfaceContainerLowest = Color(0xFF15120F),
    surfaceContainerLow = Color(0xFF231E19),
    surfaceContainer = Color(0xFF28221D),
    surfaceContainerHigh = Color(0xFF322B25),
    surfaceContainerHighest = Color(0xFF2E2721),
    outline = Color(0xFF9C8D7B),
    outlineVariant = Color(0xFF4A4036),
)

/** Rounder corners everywhere, softer look. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Titles in a serif font (like a book), normal text stays easy to read. */
private fun appTypography(): Typography {
    val base = Typography()
    val serif = FontFamily.Serif
    return base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = serif, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = serif, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = serif, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = serif, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
    )
}

/** Applies the app colors (follows the phone's light/dark mode). */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        typography = appTypography(),
        content = content,
    )
}
