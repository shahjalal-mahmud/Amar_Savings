package com.appriyo.amarsavings.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Slate900,

    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald100,

    secondary = Gold400,
    onSecondary = Slate900,

    secondaryContainer = Color(0xFF342A16),
    onSecondaryContainer = Gold400,

    error = Rose400,
    onError = Slate900,

    errorContainer = Color(0xFF3A1F25),
    onErrorContainer = Rose400,

    background = SurfaceDark,
    onBackground = Slate100,

    surface = SurfaceCardDark,
    onSurface = Slate100,

    surfaceVariant = SurfaceElevDark,
    onSurfaceVariant = Slate400,

    outline = Color(0xFF2B3647),
    outlineVariant = Color(0xFF202A39)
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = White,

    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,

    secondary = Gold500,
    onSecondary = White,

    secondaryContainer = Color(0xFFF9EFD8),
    onSecondaryContainer = Color(0xFF5B4518),

    error = Rose500,
    onError = White,

    errorContainer = Rose100,
    onErrorContainer = Color(0xFF7A3240),

    background = Color(0xFFFAFBFC),
    onBackground = Slate900,

    surface = White,
    onSurface = Slate900,

    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,

    outline = Slate300,
    outlineVariant = Slate200
)

@Composable
fun AmarSavingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}