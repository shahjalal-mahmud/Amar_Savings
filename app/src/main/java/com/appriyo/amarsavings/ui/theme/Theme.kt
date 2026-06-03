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
    primary          = Emerald400,
    onPrimary        = Slate900,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald100,
    secondary        = Amber400,
    onSecondary      = Slate900,
    secondaryContainer = Color(0xFF3D2E00),
    onSecondaryContainer = Amber400,
    error            = Rose400,
    onError          = Slate900,
    errorContainer   = Color(0xFF4D0017),
    onErrorContainer = Rose400,
    background       = SurfaceDark,
    onBackground     = Slate100,
    surface          = SurfaceCardDark,
    onSurface        = Slate100,
    surfaceVariant   = SurfaceElevDark,
    onSurfaceVariant = Slate400,
    outline          = Slate700,
    outlineVariant   = Slate800
)

private val LightColorScheme = lightColorScheme(
    primary          = Emerald600,
    onPrimary        = White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary        = Amber500,
    onSecondary      = White,
    secondaryContainer = Color(0xFFFFF3CD),
    onSecondaryContainer = Color(0xFF5A3800),
    error            = Rose500,
    onError          = White,
    errorContainer   = Rose100,
    onErrorContainer = Color(0xFF7D0020),
    background       = Slate50,
    onBackground     = Slate900,
    surface          = White,
    onSurface        = Slate900,
    surfaceVariant   = Slate100,
    onSurfaceVariant = Slate600,
    outline          = Slate300,
    outlineVariant   = Slate200
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