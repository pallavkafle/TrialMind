package com.example.trailmind.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// the saved light/dark choice. Until the user taps the switch once, the app follows the phone.
object ThemePrefs {
    private const val FILE = "trailmind_settings"
    private const val KEY = "theme_mode"
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2

    var mode by mutableIntStateOf(SYSTEM)
        private set
    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        mode = context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY, SYSTEM)
    }

    fun setDark(context: Context, dark: Boolean) {
        mode = if (dark) DARK else LIGHT
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY, mode).apply()
    }
}

@Composable
fun isAppDark(): Boolean = when (ThemePrefs.mode) {
    ThemePrefs.LIGHT -> false
    ThemePrefs.DARK -> true
    else -> isSystemInDarkTheme()
}

private val LightScheme = lightColorScheme(
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

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF4C8FCB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1F3347),
    onPrimaryContainer = Color(0xFFD6E8F7),
    secondary = Color(0xFF6AA6D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1F3347),
    onSecondaryContainer = Color(0xFFD6E8F7),
    tertiary = Color(0xFF6AA6D6),
    onTertiary = Color.White,
    background = Color(0xFF12171D),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF1B222B),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF232B36),
    onSurfaceVariant = Color(0xFF9AA5B1),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFECEFF4),
    inverseOnSurface = Color(0xFF1B222B),
    outline = Color(0xFF4B5767),
    outlineVariant = Color(0xFF2C3540),
    error = Color(0xFFEF7B7B),
    onError = Color(0xFF3B0D0D),
    errorContainer = Color(0xFF3B1F22),
    onErrorContainer = Color(0xFFF5B5B5),
    surfaceContainerLowest = Color(0xFF0F1318),
    surfaceContainerLow = Color(0xFF1B222B),
    surfaceContainer = Color(0xFF1B222B),
    surfaceContainerHigh = Color(0xFF232B36),
    surfaceContainerHighest = Color(0xFF2A333F)
)

private tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}

@Composable
fun TrailMindTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    ThemePrefs.init(context)               // loads the saved choice once, before it is read below
    val dark = isAppDark()
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = context.hostActivity()?.window ?: return@SideEffect
            // the window colour shows during screen changes, so it must match the theme (no white flashes)
            window.setBackgroundDrawable(ColorDrawable(scheme.background.toArgb()))
            // dark icons on the light bottom bar, light icons on the dark one
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalPalette provides (if (dark) DarkPalette else LightPalette)) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}

// Login and Register are a blue gradient with a light card, so they keep the light look in both modes
@Composable
fun ForceLightTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides LightPalette) {
        MaterialTheme(colorScheme = LightScheme, typography = Typography, content = content)
    }
}