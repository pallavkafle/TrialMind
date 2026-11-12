package com.example.trailmind.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Fixed brand colours: used where the colour never changes (splash, login, hero banner)
val TrailBlue = Color(0xFF1F5C8B)
val TrailBlueDark = Color(0xFF163D5C)
val TrailBlueMid = Color(0xFF2A72A8)
val TrailBlueFaint = Color(0xFFEBF3FB)

val TextDark = Color(0xFF1A1A2E)
val TextMuted = Color(0xFF6B7280)
val ScreenBackground = Color(0xFFF5F7FA)
val ErrorRed = Color(0xFFC62828)

// The colours that change between light and dark. Screens read them through Pal.
@Immutable
data class Palette(
    val blue: Color,
    val blueFaint: Color,
    val textDark: Color,
    val textMuted: Color,
    val textBody: Color,
    val red: Color,
    val redBar: Color,
    val green: Color,
    val amber: Color,
    val amberText: Color,
    val amberBg: Color,
    val errorBg: Color,
    val rain: Color
)

val LightPalette = Palette(
    blue = Color(0xFF1F5C8B),
    blueFaint = Color(0xFFEBF3FB),
    textDark = Color(0xFF1A1A2E),
    textMuted = Color(0xFF6B7280),
    textBody = Color(0xFF444444),
    red = Color(0xFFC62828),
    redBar = Color(0xFFC62828),
    green = Color(0xFF2E7D32),
    amber = Color(0xFFB26A00),
    amberText = Color(0xFF7A4A00),
    amberBg = Color(0xFFFFF4E0),
    errorBg = Color(0xFFFDECEA),
    rain = Color(0xFF1565C0)
)

val DarkPalette = Palette(
    blue = Color(0xFF4C8FCB),
    blueFaint = Color(0xFF1F3347),
    textDark = Color(0xFFECEFF4),
    textMuted = Color(0xFF9AA5B1),
    textBody = Color(0xFFC9D1DA),
    red = Color(0xFFEF7B7B),
    redBar = Color(0xFF8E1F1F),
    green = Color(0xFF6FCF8A),
    amber = Color(0xFFE0A44A),
    amberText = Color(0xFFF2CF8F),
    amberBg = Color(0xFF3A2F1A),
    errorBg = Color(0xFF3B1F22),
    rain = Color(0xFF64B5F6)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

// screens use Pal.textDark, Pal.blue ... and get the right colour for the current theme
object Pal {
    val blue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.blue
    val blueFaint: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.blueFaint
    val textDark: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textDark
    val textMuted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textMuted
    val textBody: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textBody
    val red: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.red
    val redBar: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.redBar
    val green: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.green
    val amber: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amber
    val amberText: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amberText
    val amberBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amberBg
    val errorBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.errorBg
    val rain: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.rain
}