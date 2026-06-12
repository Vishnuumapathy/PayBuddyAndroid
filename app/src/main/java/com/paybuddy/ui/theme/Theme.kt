package com.paybuddy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Dark UI Palette
val BackgroundDark = Color(0xFF0F172A)
val GlassBg = Color(0xFF1E293B)
val GlassEdge = Color(0xFF334155)
val NeonBlue = Color(0xFF38BDF8)
val NeonGreen = Color(0xFF4ADE80)
val NeonRed = Color(0xFFFB7185)
val NeonAmber = Color(0xFFFBBF24)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = Color.White,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    tertiary = NeonAmber,
    background = BackgroundDark,
    surface = GlassBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = GlassEdge,
    onSurfaceVariant = TextSecondary,
    error = NeonRed
)

private val LightColorScheme = DarkColorScheme // Keeping it dark for that "Modern" look as requested

@Composable
fun PayBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force dark theme for the premium look requested
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
