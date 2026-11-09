package com.waterleakage.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinimalLightColorScheme = lightColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = SkyBlueLight,
    onPrimaryContainer = SkyBlueDark,
    secondary = SkyBlueAccent,
    onSecondary = Color.White,
    secondaryContainer = SkyBlueContainer,
    onSecondaryContainer = SkyBlueDark,
    tertiary = SkyBlueDark,
    onTertiary = Color.White,
    background = BackgroundWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderSubtle,
    error = StatusLeakRed,
    onError = Color.White,
    errorContainer = StatusLeakRedContainer,
    onErrorContainer = StatusLeakRed
)

@Composable
fun WaterLeakageDetectionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MinimalLightColorScheme,
        typography = Typography,
        content = content
    )
}
