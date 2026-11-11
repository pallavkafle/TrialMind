package com.example.trailmind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The app is built with light screens, so it always uses this light brand scheme.
// It ignores the phone's dark mode and wallpaper colours on purpose.
private val BrandColors = lightColorScheme(
    primary = TrailBlue,
    onPrimary = Color.White,
    primaryContainer = TrailBlueFaint,
    onPrimaryContainer = TrailBlueDark,
    secondary = TrailBlueMid,
    onSecondary = Color.White,
    secondaryContainer = TrailBlueFaint,
    onSecondaryContainer = TrailBlueDark,
    tertiary = TrailBlueDark,
    onTertiary = Color.White,
    background = ScreenBackground,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = TextMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = TextDark,
    inverseOnSurface = Color.White,
    outline = Color(0xFFB8C0CC),
    outlineVariant = Color(0xFFE5E9F0),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = ErrorRed,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEEF1F5)
)

@Composable
fun TrailMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrandColors,
        typography = Typography,
        content = content
    )
}