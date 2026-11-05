package com.waterleakage.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = Color(0xFF001E2B),
    primaryContainer = PrimaryCyanVariant,
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = SecondaryIndigo,
    onSecondary = Color(0xFF1E1B4B),
    tertiary = AccentTeal,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = StatusLeakRed,
    onError = Color.White
)

@Composable
fun WaterLeakageDetectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Defaulting to Dark industrial theme for high-tech aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
